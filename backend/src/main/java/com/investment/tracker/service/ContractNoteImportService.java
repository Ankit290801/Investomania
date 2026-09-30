package com.investment.tracker.service;

import com.investment.tracker.config.ContractNoteRules;
import com.investment.tracker.dto.ContractNoteImportReportDTO;
import com.investment.tracker.dto.ContractNoteImportReportDTO.ContractNoteTradeDTO;
import com.investment.tracker.model.CashInvestment;
import com.investment.tracker.model.EquityInvestment;
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
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Broker contract note (CNOTE) import (Phase 9 / Task 8.3).
 *
 * <p>Portfolio semantics per trade:</p>
 * <ul>
 *   <li><b>Existing holding</b> — a BUY transaction is appended and the
 *       weighted-average purchase price and quantity are recomputed from the
 *       full transaction history ({@link #recomputeHolding}), so total P&amp;L
 *       (live value vs. avg price) updates automatically.</li>
 *   <li><b>New stock</b> — a new EQUITY investment is created (symbol, qty,
 *       avg price, purchase date) plus the initial BUY transaction, and a
 *       cash debit ({@code TRANSFER_OUT}) is booked on the broker's
 *       settlement account (auto-created when absent).</li>
 *   <li><b>Sell</b> — a SELL transaction is appended (partial sells leave the
 *       remaining quantity and its avg price intact via the same recompute),
 *       and a cash credit ({@code TRANSFER_IN}) is booked. Selling more than
 *       held is rejected.</li>
 * </ul>
 *
 * <p>The net portfolio increase/decrease follows automatically: listed
 * equity values refresh from live prices and analytics are transaction-driven
 * (existing features). Raw PDFs are parsed in memory and never persisted.</p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ContractNoteImportService {

    private final ContractNoteRules contractNoteRules;
    private final ContractNotePdfParser pdfParser;
    private final InvestmentRepository investmentRepository;
    private final TransactionRepository transactionRepository;
    private final SnapshotGenerationService snapshotGenerationService;

    /**
     * Dry-run: detect broker, parse trades and resolve portfolio actions.
     * Nothing is persisted.
     */
    public ContractNoteImportReportDTO preview(Long userId, MultipartFile file) throws IOException {
        return process(userId, file, false);
    }

    /**
     * Commit: apply every valid trade (transactions + holding recompute +
     * cash legs) and refresh FY-end snapshots retrospectively.
     */
    @Transactional
    public ContractNoteImportReportDTO commit(Long userId, MultipartFile file) throws IOException {
        return process(userId, file, true);
    }

    private ContractNoteImportReportDTO process(Long userId, MultipartFile file, boolean commit)
            throws IOException {
        validateFile(file);

        String text = pdfParser.extractText(file.getBytes());
        ContractNoteRules.BrokerTemplate template = contractNoteRules.resolveTemplate(text);
        String detectedBroker = template != null ? template.getName() : null;

        List<ContractNotePdfParser.ParsedTrade> trades = pdfParser.parseText(text);
        if (trades.size() > contractNoteRules.getMaxTrades()) {
            throw new IllegalArgumentException(
                    "Contract note has more than " + contractNoteRules.getMaxTrades() + " trades");
        }

        List<ContractNoteTradeDTO> rows = new ArrayList<>();
        BigDecimal netCash = BigDecimal.ZERO;
        BigDecimal netPortfolio = BigDecimal.ZERO;
        int valid = 0;
        int errors = 0;

        for (ContractNotePdfParser.ParsedTrade trade : trades) {
            ContractNoteTradeDTO row = resolveTrade(userId, trade, rows.size() + 1);
            rows.add(row);

            if (ContractNoteTradeDTO.STATUS_ERROR.equals(row.getStatus())) {
                errors++;
                continue;
            }
            valid++;
            BigDecimal gross = trade.getQuantity().multiply(trade.getPrice());
            if (trade.isBuy()) {
                netCash = netCash.subtract(gross);        // money out
                netPortfolio = netPortfolio.add(gross);   // holdings cost in
            } else {
                netCash = netCash.add(gross);             // money in
                netPortfolio = netPortfolio.subtract(gross); // holdings cost out
            }
        }

        Long cashAccountId = null;
        String cashAccountName = null;
        if (!rows.isEmpty() && valid > 0) {
            CashInvestment cash = resolveOrCreateCashAccount(userId, detectedBroker, commit);
            cashAccountId = cash != null ? cash.getId() : null;
            cashAccountName = cash != null ? cash.getName() : null;

            if (commit && cash != null) {
                for (ContractNoteTradeDTO row : rows) {
                    if (ContractNoteTradeDTO.STATUS_ERROR.equals(row.getStatus())) {
                        continue;
                    }
                    applyTrade(userId, cash, row);
                }
                refreshSnapshotsRetrospectively(userId);
            }
        }

        return ContractNoteImportReportDTO.builder()
                .detectedBroker(detectedBroker)
                .dryRun(!commit)
                .totalTrades(rows.size())
                .validTrades(valid)
                .errorTrades(errors)
                .commitable(valid > 0)
                .netCashImpact(netCash)
                .netPortfolioImpact(netPortfolio)
                .cashAccountId(cashAccountId)
                .cashAccountName(cashAccountName)
                .trades(rows)
                .build();
    }

    // ------------------------------------------------------------------
    // Per-trade resolution (dry-run) and application (commit)
    // ------------------------------------------------------------------

    /**
     * Resolves what a trade will do to the portfolio without persisting:
     * NEW (create investment) / UPDATED (existing holding) / ERROR.
     */
    private ContractNoteTradeDTO resolveTrade(Long userId, ContractNotePdfParser.ParsedTrade trade,
                                              int rowNumber) {
        ContractNoteTradeDTO.ContractNoteTradeDTOBuilder builder = ContractNoteTradeDTO.builder()
                .rowNumber(rowNumber)
                .symbol(trade.getSymbol())
                .buy(trade.isBuy())
                .quantity(trade.getQuantity())
                .price(trade.getPrice())
                .tradeDate(trade.getTradeDate());

        EquityInvestment existing = findHolding(userId, trade.getSymbol());

        if (existing == null) {
            if (!trade.isBuy()) {
                return builder.status(ContractNoteTradeDTO.STATUS_ERROR)
                        .action(ContractNoteTradeDTO.ACTION_ERROR)
                        .message("Cannot sell " + trade.getSymbol()
                                + " — no holding exists in the portfolio")
                        .build();
            }
            return builder.status(ContractNoteTradeDTO.STATUS_NEW)
                    .action(ContractNoteTradeDTO.ACTION_CREATE)
                    .message("New investment will be created (" + trade.getQuantity().stripTrailingZeros()
                            + " @ " + trade.getPrice() + ") + cash debit")
                    .build();
        }

        BigDecimal currentQty = existing.getQuantity() != null ? existing.getQuantity() : BigDecimal.ZERO;
        if (!trade.isBuy() && trade.getQuantity().compareTo(currentQty) > 0) {
            return builder.status(ContractNoteTradeDTO.STATUS_ERROR)
                    .action(ContractNoteTradeDTO.ACTION_ERROR)
                    .message("Cannot sell " + trade.getQuantity().stripTrailingZeros()
                            + " — only " + currentQty.stripTrailingZeros() + " held")
                    .build();
        }
        return builder.status(ContractNoteTradeDTO.STATUS_UPDATED)
                .action(ContractNoteTradeDTO.ACTION_UPDATE)
                .investmentId(existing.getId())
                .message(trade.isBuy()
                        ? "Weighted avg price of existing holding will be recomputed"
                        : "Partial/full sell — remaining quantity " + currentQty.subtract(trade.getQuantity()).stripTrailingZeros())
                .build();
    }

    /**
     * Applies one resolved trade on commit: equity transaction + holding
     * recompute + settlement cash leg.
     */
    private void applyTrade(Long userId, CashInvestment cash, ContractNoteTradeDTO row) {
        LocalDate tradeDate = row.getTradeDate();
        BigDecimal gross = row.getQuantity().multiply(row.getPrice())
                .setScale(2, RoundingMode.HALF_UP);
        String notes = "Contract note trade (" + row.getSymbol() + ")";

        EquityInvestment equity = findHolding(userId, row.getSymbol());
        Long investmentId;
        if (equity == null) {
            equity = EquityInvestment.builder()
                    .userId(userId)
                    .type(com.investment.tracker.model.InvestmentType.EQUITY)
                    .name(row.getSymbol())
                    .symbol(row.getSymbol())
                    .market(contractNoteRules.getDefaultMarket())
                    .currency(cash.getCurrency())
                    .purchaseDate(tradeDate)
                    .isListed(true)
                    .build();
            equity = investmentRepository.save(equity);
        }
        investmentId = equity.getId();

        Transaction trade = Transaction.builder()
                .investmentId(investmentId)
                .type(row.isBuy() ? TransactionType.BUY : TransactionType.SELL)
                .quantity(row.getQuantity())
                .pricePerUnit(row.getPrice())
                .amount(gross)
                .transactionDate(tradeDate)
                .currency(equity.getCurrency())
                .notes(notes)
                .build();
        trade = transactionRepository.save(trade);

        recomputeHolding(equity);

        // Settlement cash leg: buys debit, sells credit the settlement account.
        Transaction cashLeg = Transaction.builder()
                .investmentId(cash.getId())
                .type(row.isBuy() ? TransactionType.TRANSFER_OUT : TransactionType.TRANSFER_IN)
                .amount(gross)
                .transactionDate(tradeDate)
                .currency(cash.getCurrency())
                .notes(row.isBuy()
                        ? "Stock purchase settlement — " + row.getSymbol()
                        : "Stock sale proceeds — " + row.getSymbol())
                .build();
        cashLeg = transactionRepository.save(cashLeg);

        row.setInvestmentId(investmentId);
        row.setCashTransactionId(cashLeg.getId());
        log.info("CNOTE trade applied: {} {} {} @ {} on {} (equity txn {}, cash txn {}).",
                row.isBuy() ? "BUY" : "SELL", row.getQuantity(), row.getSymbol(),
                row.getPrice(), tradeDate, trade.getId(), cashLeg.getId());
    }

    /**
     * Weighted-average recompute from the full transaction history: avg price
     * = Σ(BUY amounts) / Σ(BUY quantities); quantity = Σ buys − Σ sells. A
     * partial sell therefore leaves the remaining units at the unchanged
     * average price, and total P&L (live value vs avg) follows automatically.
     */
    private void recomputeHolding(EquityInvestment equity) {
        List<Transaction> chronological = new ArrayList<>(
                transactionRepository.findByInvestmentIdOrderByTransactionDateDesc(equity.getId()));
        java.util.Collections.reverse(chronological);
        List<Transaction> txns = chronological;

        BigDecimal totalBuyQty = BigDecimal.ZERO;
        BigDecimal totalBuyAmount = BigDecimal.ZERO;
        BigDecimal totalSellQty = BigDecimal.ZERO;
        for (Transaction txn : txns) {
            if (txn.getQuantity() == null) {
                continue;
            }
            if (txn.getType() == TransactionType.BUY) {
                totalBuyQty = totalBuyQty.add(txn.getQuantity());
                totalBuyAmount = totalBuyAmount.add(txn.getAmount());
            } else if (txn.getType() == TransactionType.SELL) {
                totalSellQty = totalSellQty.add(txn.getQuantity());
            }
        }
        BigDecimal held = totalBuyQty.subtract(totalSellQty).max(BigDecimal.ZERO);

        equity.setQuantity(held);
        if (totalBuyQty.signum() > 0) {
            equity.setAvgPrice(totalBuyAmount.divide(totalBuyQty, 4, RoundingMode.HALF_UP));
        }
        if (held.signum() == 0) {
            // Fully exited: keep the last avg price for tax/reporting history.
        }
        investmentRepository.save(equity);
    }

    /**
     * Finds the user's EQUITY holding by symbol (case-insensitive).
     */
    private EquityInvestment findHolding(Long userId, String symbol) {
        for (Investment inv : investmentRepository.findByUserId(userId)) {
            if (inv instanceof EquityInvestment
                    && symbol.equalsIgnoreCase(((EquityInvestment) inv).getSymbol())) {
                return (EquityInvestment) inv;
            }
        }
        return null;
    }

    /**
     * Returns the broker's settlement cash account, creating it on commit
     * when absent. Auto-detection: CASH account whose bankName or name
     * contains the broker name; otherwise the single CASH account.
     */
    private CashInvestment resolveOrCreateCashAccount(Long userId, String brokerName,
                                                      boolean commit) {
        List<Investment> investments = investmentRepository.findByUserId(userId);
        List<CashInvestment> cashAccounts = new ArrayList<>();
        for (Investment inv : investments) {
            if (inv instanceof CashInvestment) {
                cashAccounts.add((CashInvestment) inv);
            }
        }

        if (brokerName != null) {
            for (CashInvestment cash : cashAccounts) {
                String bank = cash.getBankName() != null ? cash.getBankName() : "";
                String name = cash.getName() != null ? cash.getName() : "";
                if (bank.toLowerCase().contains(brokerName.toLowerCase())
                        || name.toLowerCase().contains(brokerName.toLowerCase())) {
                    return cash;
                }
            }
        }
        if (cashAccounts.size() == 1) {
            return cashAccounts.get(0);
        }
        if (commit) {
            CashInvestment created = CashInvestment.builder()
                    .userId(userId)
                    .type(com.investment.tracker.model.InvestmentType.CASH)
                    .name(contractNoteRules.cashAccountNameFor(brokerName))
                    .bankName(brokerName)
                    .accountType("SETTLEMENT")
                    .currency(contractNoteRules.getDefaultCurrency())
                    .purchaseDate(LocalDate.now())
                    .currentValue(BigDecimal.ZERO)
                    .isListed(false)
                    .build();
            created = investmentRepository.save(created);
            log.info("CNOTE import: created settlement cash account '{}' for user {}.",
                    created.getName(), userId);
            return created;
        }
        return null;
    }

    private void refreshSnapshotsRetrospectively(Long userId) {
        try {
            snapshotGenerationService.generateHistoricalSnapshots(userId, true);
            log.info("Retrospective snapshot refresh completed for user {}.", userId);
        } catch (Exception e) {
            log.error("Retrospective snapshot refresh failed for user {}: {}",
                    userId, e.getMessage(), e);
        }
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("No file uploaded");
        }
        long maxBytes = contractNoteRules.getMaxFileKb() * 1024;
        if (file.getSize() > maxBytes) {
            throw new IllegalArgumentException(
                    "File exceeds the " + contractNoteRules.getMaxFileKb() + " KB import limit");
        }
    }
}
