package com.investment.tracker.service;

import com.investment.tracker.config.StatementImportRules;
import com.investment.tracker.dto.StatementImportReportDTO;
import com.investment.tracker.dto.StatementImportReportDTO.StatementImportRowDTO;
import com.investment.tracker.model.CashInvestment;
import com.investment.tracker.model.Investment;
import com.investment.tracker.model.Transaction;
import com.investment.tracker.model.TransactionType;
import com.investment.tracker.repository.InvestmentRepository;
import com.investment.tracker.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Bank statement PDF import pipeline (backlog feature F6):
 * detect bank → parse → match/create CASH account → dedupe → commit
 * transactions + balance update → retrospective snapshot refresh.
 *
 * <p>Balance semantics: statement credits (money in) are recorded as
 * {@code TRANSFER_IN} transactions on the CASH account and debits as
 * {@code TRANSFER_OUT} (types are config-driven). The account's
 * {@code currentValue} is <b>replayed</b> from its opening transaction plus
 * all imported flows so the stored balance equals statement reality, and the
 * historical valuation engine re-derives past balances from the same
 * transactions — making FY-end net worths retrospective without any manual
 * correction.</p>
 *
 * <p>Statements are parsed fully in memory; raw PDF bytes are never persisted.</p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class BankStatementImportService {

    private final StatementImportRules importRules;
    private final StatementPdfParser pdfParser;
    private final InvestmentRepository investmentRepository;
    private final TransactionRepository transactionRepository;
    private final SnapshotGenerationService snapshotGenerationService;
    private final CashLedgerService cashLedgerService;

    /**
     * Dry-run: detect the bank, parse and validate the statement, return a
     * row-by-row report. Nothing is persisted.
     */
    public StatementImportReportDTO preview(Long userId, MultipartFile file) throws IOException {
        return process(userId, file, null, false);
    }

    /**
     * Dry-run against a specific cash account (UI selection overrides
     * auto-matching).
     */
    public StatementImportReportDTO preview(Long userId, MultipartFile file, Long cashAccountId)
            throws IOException {
        return process(userId, file, cashAccountId, false);
    }

    /**
     * Commit: persist every non-duplicate row, replay the account balance and
     * regenerate affected FY-end snapshots.
     */
    @Transactional
    public StatementImportReportDTO commit(Long userId, MultipartFile file, Long cashAccountId)
            throws IOException {
        return process(userId, file, cashAccountId, true);
    }

    private StatementImportReportDTO process(Long userId, MultipartFile file, Long cashAccountId,
                                             boolean commit) throws IOException {
        validateFile(file);

        // Extract text once; bank detection, parse-quality check and row
        // parsing all read the same in-memory text.
        byte[] bytes = file.getBytes();
        String text = pdfParser.extractText(bytes);
        StatementImportRules.BankEntry bank = importRules.detectBank(text);
        String detectedBank = bank != null ? bank.getName() : null;

        double parseRatio = pdfParser.parseRatio(text);
        if (parseRatio < importRules.getMinParseRatio()) {
            throw new IllegalArgumentException(
                    "Statement format not recognised (bank detected: "
                            + (detectedBank != null ? detectedBank : "none")
                            + "). See the import documentation for supported layouts.");
        }

        List<StatementPdfParser.ParsedTxn> parsed = pdfParser.parseText(text);

        CashInvestment account = resolveAccount(userId, cashAccountId, parsed, detectedBank);

        StatementImportReportDTO.StatementImportReportDTOBuilder report = StatementImportReportDTO.builder()
                .detectedBank(detectedBank)
                .dryRun(!commit);

        if (account == null) {
            return report
                    .totalRows(parsed.size())
                    .validRows(0).duplicateRows(0).errorRows(parsed.size())
                    .commitable(false)
                    .rows(parsed.isEmpty() ? List.of() : List.of(StatementImportRowDTO.builder()
                            .rowNumber(1)
                            .status(StatementImportRowDTO.STATUS_ERROR)
                            .message(detectedBank == null
                                    ? "Could not detect the bank — add a marker for it in statement-import.yml"
                                    : "No cash account found to import into. Create a Cash/Bank investment first.")
                            .build()))
                    .build();
        }

        if (parsed.size() > importRules.getMaxRows()) {
            throw new IllegalArgumentException(
                    "Statement has more than " + importRules.getMaxRows() + " transaction rows");
        }

        Set<String> existingKeys = loadExistingTransactionKeys(account.getId());
        List<StatementImportRowDTO> rows = new ArrayList<>();
        List<StatementImportRowDTO> committed = new ArrayList<>();

        for (StatementPdfParser.ParsedTxn txn : parsed) {
            StatementImportRowDTO row = buildRow(rows.size() + 1, txn, existingKeys);
            rows.add(row);
            if (commit && StatementImportRowDTO.STATUS_OK.equals(row.getStatus())) {
                Transaction saved = transactionRepository.save(toTransaction(account, txn));
                row.setTransactionId(saved.getId());
                committed.add(row);
                existingKeys.add(txnKey(txn.getDate(), txn.getDescription(), txn.getAmount()));
            }
        }

        BigDecimal netImpact = committed.stream()
                .map(r -> "CREDIT".equals(r.getDirection())
                        ? r.getAmount() : r.getAmount().negate())
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (commit && !committed.isEmpty()) {
            replayBalance(account, netImpact);
            refreshSnapshotsRetrospectively(userId);
            log.info("Statement import for user {}: {} transaction(s) committed to {} ({}), "
                            + "net balance impact {} {}.",
                    userId, committed.size(), account.getName(), account.getBankName(),
                    netImpact, account.getCurrency());
        }

        int valid = 0;
        int duplicates = 0;
        int errors = 0;
        for (StatementImportRowDTO row : rows) {
            if (StatementImportRowDTO.STATUS_OK.equals(row.getStatus())) valid++;
            else if (StatementImportRowDTO.STATUS_DUPLICATE.equals(row.getStatus())) duplicates++;
            else errors++;
        }

        return report
                .totalRows(rows.size())
                .validRows(valid)
                .duplicateRows(duplicates)
                .errorRows(errors)
                .commitable(valid > 0)
                .netBalanceImpact(netImpact)
                .cashAccountId(account.getId())
                .cashAccountName(account.getName())
                .rows(rows)
                .build();
    }

    // ------------------------------------------------------------------
    // Steps
    // ------------------------------------------------------------------

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("No file uploaded");
        }
        long maxBytes = importRules.getMaxFileKb() * 1024;
        if (file.getSize() > maxBytes) {
            throw new IllegalArgumentException(
                    "File exceeds the " + importRules.getMaxFileKb() + " KB import limit");
        }
    }



    /**
     * Auto-matching: prefer the user's CASH account whose bankName matches the
     * detected bank; otherwise the single CASH account; otherwise null (the
     * caller reports why). An explicit account override from the UI wins.
     */
    private CashInvestment resolveAccount(Long userId, Long cashAccountId,
                                          List<StatementPdfParser.ParsedTxn> parsed,
                                          String detectedBank) {
        List<Investment> investments = investmentRepository.findByUserId(userId);
        List<CashInvestment> cashAccounts = investments.stream()
                .filter(inv -> inv instanceof CashInvestment)
                .map(inv -> (CashInvestment) inv)
                .collect(Collectors.toList());

        if (cashAccountId != null) {
            return cashAccounts.stream()
                    .filter(c -> cashAccountId.equals(c.getId()))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException(
                            "Selected account is not a Cash/Bank investment of this user"));
        }
        if (detectedBank != null) {
            return cashAccounts.stream()
                    .filter(c -> c.getBankName() != null
                            && c.getBankName().toLowerCase().contains(detectedBank.toLowerCase()
                                    .split(" ")[0]))
                    .findFirst()
                    .orElse(null);
        }
        return cashAccounts.size() == 1 ? cashAccounts.get(0) : null;
    }

    private Set<String> loadExistingTransactionKeys(Long accountId) {
        Set<String> keys = new HashSet<>();
        for (Transaction txn : transactionRepository.findByInvestmentIdOrderByTransactionDateDesc(accountId)) {
            keys.add(txnKey(txn.getTransactionDate(), safe(txn.getNotes()), txn.getAmount()));
        }
        return keys;
    }

    private StatementImportRowDTO buildRow(int rowNumber, StatementPdfParser.ParsedTxn txn,
                                           Set<String> existingKeys) {
        StatementImportRowDTO.StatementImportRowDTOBuilder builder = StatementImportRowDTO.builder()
                .rowNumber(rowNumber)
                .date(txn.getDate())
                .description(txn.getDescription())
                .amount(txn.getAmount())
                .direction(txn.isCredit() ? "CREDIT" : "DEBIT");

        if (txn.getAmount() == null || txn.getAmount().signum() <= 0) {
            return builder.status(StatementImportRowDTO.STATUS_ERROR)
                    .message("Missing or non-positive amount").build();
        }
        if (txn.getDate() == null) {
            return builder.status(StatementImportRowDTO.STATUS_ERROR)
                    .message("Missing date").build();
        }
        if (txn.getDate().isAfter(LocalDate.now().plusDays(1))) {
            return builder.status(StatementImportRowDTO.STATUS_ERROR)
                    .message("Transaction date is in the future").build();
        }
        if (txn.getAmount().abs().compareTo(new BigDecimal("100000000")) > 0) {
            return builder.status(StatementImportRowDTO.STATUS_ERROR)
                    .message("Amount implausibly large (balance column parse?)").build();
        }
        String key = txnKey(txn.getDate(), txn.getDescription(), txn.getAmount());
        if (existingKeys.contains(key)) {
            return builder.status(StatementImportRowDTO.STATUS_DUPLICATE)
                    .message("Identical transaction already exists on this account").build();
        }
        return builder.status(StatementImportRowDTO.STATUS_OK).build();
    }

    private Transaction toTransaction(CashInvestment account, StatementPdfParser.ParsedTxn txn) {
        TransactionType type = txn.isCredit()
                ? importRules.getCreditTransactionType()
                : importRules.getDebitTransactionType();
        return Transaction.builder()
                .investmentId(account.getId())
                .type(type)
                .amount(txn.getAmount())
                .transactionDate(txn.getDate())
                .currency(account.getCurrency() != null
                        ? account.getCurrency()
                        : importRules.getDefaultCurrency())
                .notes(safe(txn.getDescription()))
                .build();
    }

    /**
     * Balance replay: the account's stored currentValue is recomputed from its
     * full transaction history — the opening DEPOSIT/BUY seeds the balance and
     * every cash flow (credit/debit) adjusts it. This makes the stored balance
     * consistent with the imported statement AND makes historical valuations
     * (which read the same transactions) retrospective.
     *
     * E10: delegates to the shared {@link CashLedgerService} replay so import,
     * snapshots and expense postings all recognise the same flows (incl. EXPENSE).
     */
    private void replayBalance(CashInvestment account, BigDecimal importedNetImpact) {
        cashLedgerService.refreshCurrentBalance(account);
        log.debug("Replayed balance for account {} ({}): {} {}.",
                account.getId(), account.getName(), account.getCurrentValue(),
                account.getCurrency());
    }

    private boolean isCreditFlow(TransactionType type) {
        return cashLedgerService.isCreditFlow(type);
    }

    private boolean isDebitFlow(TransactionType type) {
        return cashLedgerService.isDebitFlow(type);
    }

    /**
     * Retrospective net worth: after historical balances changed, every
     * FY-end snapshot is regenerated (force=true recalculates existing ones).
     */
    private void refreshSnapshotsRetrospectively(Long userId) {
        try {
            snapshotGenerationService.generateHistoricalSnapshots(userId, true);
            log.info("Retrospective snapshot refresh completed for user {}.", userId);
        } catch (Exception e) {
            // Snapshots are derived data — a regeneration failure must not roll
            // back the committed transactions; it is surfaced in the logs and
            // the user can regenerate from the Snapshots page.
            log.error("Retrospective snapshot refresh failed for user {}: {}",
                    userId, e.getMessage(), e);
        }
    }

    private String txnKey(LocalDate date, String description, BigDecimal amount) {
        return date + "|" + description.trim().toLowerCase() + "|" + amount.abs().stripTrailingZeros();
    }

    private String safe(String value) {
        return value != null ? value : "";
    }
}
