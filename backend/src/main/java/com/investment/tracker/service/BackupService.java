package com.investment.tracker.service;

import com.investment.tracker.dto.BackupEnvelopeDTO;
import com.investment.tracker.dto.ExpenseDTO;
import com.investment.tracker.dto.InvestmentDTO;
import com.investment.tracker.dto.TransactionDTO;
import com.investment.tracker.dto.UserSettingsDTO;
import com.investment.tracker.model.AssetValuation;
import com.investment.tracker.model.Expense;
import com.investment.tracker.model.ExpenseAssetMapping;
import com.investment.tracker.model.Investment;
import com.investment.tracker.model.PortfolioSnapshot;
import com.investment.tracker.model.SymbolMapping;
import com.investment.tracker.model.Transaction;
import com.investment.tracker.model.UserSettings;
import com.investment.tracker.repository.AssetValuationRepository;
import com.investment.tracker.repository.ExpenseAssetMappingRepository;
import com.investment.tracker.repository.ExpenseRepository;
import com.investment.tracker.repository.InvestmentRepository;
import com.investment.tracker.repository.PortfolioSnapshotRepository;
import com.investment.tracker.repository.SymbolMappingRepository;
import com.investment.tracker.repository.TransactionRepository;
import com.investment.tracker.repository.UserSettingsRepository;
import com.investment.tracker.util.InvestmentMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Phase 9 Task 8.4 — full-user-data JSON backup and restore.
 *
 * <p><b>Export</b> gathers every user-owned table (investments, transactions,
 * expenses, expense→asset mappings, snapshots, settings, asset valuations)
 * plus the shared symbol mapping table into a versioned envelope with row
 * counts. Cross-entity references are plain ids and are preserved exactly in
 * the file.</p>
 *
 * <p><b>Restore</b> validates the envelope (schema version, required
 * sections, cross-reference sanity) and then replaces the signed-in user's
 * data atomically. Two properties are essential to correctness:</p>
 * <ul>
 *   <li><b>Immediate bulk deletes:</b> the wipe uses {@code @Modifying} JPQL
 *       bulk deletes, which execute at query time. Entity-loaded deletes
 *       ({@code deleteAll(...)}) are deferred to transaction flush, and
 *       Hibernate flushes inserts before deletes — the re-inserted
 *       snapshots then collide with the not-yet-deleted rows on the
 *       {@code (user_id, snapshot_date)} unique index. This was the cause of
 *       the first restore failure (2026-10-02).</li>
 *   <li><b>Id remapping:</b> all entities use {@code GenerationType.IDENTITY};
 *       Hibernate silently discards preset ids on insert. Restore therefore
 *       inserts with fresh ids and rewrites every cross-row reference
 *       (transaction→investment, expense→paid-from investment,
 *       transaction→expense, mapping→expense/investment,
 *       valuation→investment) from backup ids to new ids. Presetting ids
 *       would "succeed" while leaving every reference dangling.</li>
 * </ul>
 *
 * <p>Identity (users/password hashes) and volatile caches (market prices)
 * are not touched; the UserSettings row is upserted rather than deleted.</p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class BackupService {

    private final InvestmentRepository investmentRepository;
    private final TransactionRepository transactionRepository;
    private final ExpenseRepository expenseRepository;
    private final ExpenseAssetMappingRepository mappingRepository;
    private final PortfolioSnapshotRepository snapshotRepository;
    private final UserSettingsRepository userSettingsRepository;
    private final AssetValuationRepository assetValuationRepository;
    private final SymbolMappingRepository symbolMappingRepository;
    private final InvestmentMapper investmentMapper;

    // ------------------------------------------------------------------
    // Export
    // ------------------------------------------------------------------

    @Transactional(readOnly = true)
    public BackupEnvelopeDTO exportUserData(Long userId, String username) {
        List<Investment> investmentEntities = investmentRepository.findByUserId(userId);
        List<InvestmentDTO> investments = new ArrayList<>();
        for (Investment entity : investmentEntities) {
            investments.add(investmentMapper.toDTO(entity));
        }

        List<TransactionDTO> transactions = new ArrayList<>();
        for (Transaction t : transactionRepository.findByUserId(userId)) {
            transactions.add(toTransactionDto(t));
        }

        List<ExpenseDTO> expenses = new ArrayList<>();
        for (Expense e : expenseRepository.findByUserIdOrderByExpenseDateDesc(userId)) {
            expenses.add(toExpenseDto(e));
        }

        List<ExpenseAssetMapping> mappings = mappingRepository.findAllForUser(userId);

        List<PortfolioSnapshot> snapshots = snapshotRepository.findByUserIdOrderBySnapshotDateAsc(userId);

        List<AssetValuation> valuations = new ArrayList<>();
        for (Investment inv : investmentEntities) {
            valuations.addAll(assetValuationRepository.findByInvestmentIdOrderByValuationDateAsc(inv.getId()));
        }

        List<SymbolMapping> symbolMappings = symbolMappingRepository.findAll(); // shared reference table

        List<UserSettingsDTO> settings = userSettingsRepository.findByUserId(userId)
                .map(s -> List.of(toSettingsDto(s)))
                .orElse(List.of());

        Map<String, Long> counts = new LinkedHashMap<>();
        counts.put("investments", (long) investments.size());
        counts.put("transactions", (long) transactions.size());
        counts.put("expenses", (long) expenses.size());
        counts.put("expenseAssetMappings", (long) mappings.size());
        counts.put("snapshots", (long) snapshots.size());
        counts.put("settings", (long) settings.size());
        counts.put("assetValuations", (long) valuations.size());
        counts.put("symbolMappings", (long) symbolMappings.size());

        return BackupEnvelopeDTO.builder()
                .schemaVersion(BackupEnvelopeDTO.SCHEMA_VERSION)
                .createdAt(LocalDateTime.now().toString())
                .exportedFor(username)
                .counts(counts)
                .investments(investments)
                .transactions(transactions)
                .expenses(expenses)
                .expenseAssetMappings(mappings)
                .snapshots(snapshots)
                .settings(settings)
                .assetValuations(valuations)
                .symbolMappings(symbolMappings)
                .build();
    }

    // ------------------------------------------------------------------
    // Restore
    // ------------------------------------------------------------------

    /**
     * Replace the user's data with the envelope's contents. Returns the
     * restored row counts (verified against the envelope's counts).
     */
    @Transactional
    public Map<String, Long> restoreUserData(Long userId, BackupEnvelopeDTO envelope) {
        validateEnvelope(envelope);

        wipeUserData(userId);

        // ---- Investments first: everything else references them. ----
        // IDENTITY id generation ignores preset ids, so rows are inserted
        // with fresh ids and backup→new mappings rewrite the references.
        Map<Long, Long> investmentIdMap = new HashMap<>();
        long investments = 0;
        for (InvestmentDTO dto : envelope.getInvestments()) {
            Investment entity = investmentMapper.toEntity(dto, userId);
            Investment saved = investmentRepository.save(entity);
            if (dto.getId() != null) {
                investmentIdMap.put(dto.getId(), saved.getId());
            }
            investments++;
        }

        // ---- Expenses (remap paid-from cash account). ----
        Map<Long, Long> expenseIdMap = new HashMap<>();
        long expenses = 0;
        for (ExpenseDTO dto : envelope.getExpenses()) {
            Expense e = Expense.builder()
                    .userId(userId)
                    .description(dto.getDescription())
                    .category(dto.getCategory())
                    .amount(dto.getAmount())
                    .currency(dto.getCurrency())
                    .expenseDate(dto.getExpenseDate())
                    .notes(dto.getNotes())
                    .paidFromInvestmentId(remap(dto.getPaidFromInvestmentId(), investmentIdMap, "expense.paidFromInvestmentId"))
                    .isRecurring(dto.getIsRecurring() != null ? dto.getIsRecurring() : false)
                    .recurrenceFrequency(dto.getRecurrenceFrequency())
                    .build();
            Expense saved = expenseRepository.save(e);
            if (dto.getId() != null) {
                expenseIdMap.put(dto.getId(), saved.getId());
            }
            expenses++;
        }

        // ---- Transactions (remap investment + linked expense). ----
        long transactions = 0;
        for (TransactionDTO dto : envelope.getTransactions()) {
            Transaction t = Transaction.builder()
                    .investmentId(remap(dto.getInvestmentId(), investmentIdMap, "transaction.investmentId"))
                    .type(dto.getType())
                    .quantity(dto.getQuantity())
                    .pricePerUnit(dto.getPricePerUnit())
                    .amount(dto.getAmount())
                    .transactionDate(dto.getTransactionDate())
                    .currency(dto.getCurrency())
                    .notes(dto.getNotes())
                    .expenseId(remap(dto.getExpenseId(), expenseIdMap, "transaction.expenseId"))
                    .build();
            transactionRepository.save(t);
            transactions++;
        }

        // ---- Expense→asset coverage mappings. ----
        long mappings = 0;
        if (envelope.getExpenseAssetMappings() != null) {
            for (ExpenseAssetMapping m : envelope.getExpenseAssetMappings()) {
                m.setId(null); // fresh id; references remapped below
                m.setExpenseId(remap(m.getExpenseId(), expenseIdMap, "expenseAssetMapping.expenseId"));
                m.setInvestmentId(remap(m.getInvestmentId(), investmentIdMap, "expenseAssetMapping.investmentId"));
                mappingRepository.save(m);
                mappings++;
            }
        }

        // ---- Snapshots: the (user_id, snapshot_date) unique index makes
        // correct sequencing essential — by this point the bulk wipe has
        // already executed its DELETEs, so these inserts cannot collide. ----
        long snapshots = 0;
        if (envelope.getSnapshots() != null) {
            for (PortfolioSnapshot s : envelope.getSnapshots()) {
                s.setId(null);
                s.setUserId(userId);
                snapshotRepository.save(s);
                snapshots++;
            }
        }

        // ---- Asset valuations (remap investment). ----
        long valuations = 0;
        if (envelope.getAssetValuations() != null) {
            for (AssetValuation v : envelope.getAssetValuations()) {
                v.setId(null);
                v.setInvestmentId(remap(v.getInvestmentId(), investmentIdMap, "assetValuation.investmentId"));
                assetValuationRepository.save(v);
                valuations++;
            }
        }

        // ---- Symbol mappings: shared reference table keyed by symbol —
        // insert only rows that do not already exist so a restore never
        // clobbers mappings created/verified since the backup. ----
        long symbolMappings = 0;
        if (envelope.getSymbolMappings() != null) {
            for (SymbolMapping m : envelope.getSymbolMappings()) {
                if (m.getUserSymbol() != null
                        && symbolMappingRepository.findByUserSymbol(m.getUserSymbol()).isEmpty()) {
                    m.setId(null);
                    symbolMappingRepository.save(m);
                }
                symbolMappings++;
            }
        }

        // ---- Settings: upsert (row survives the wipe). ----
        long settings = 0;
        if (envelope.getSettings() != null) {
            for (UserSettingsDTO dto : envelope.getSettings()) {
                UserSettings s = userSettingsRepository.findByUserId(userId)
                        .orElseGet(() -> UserSettings.builder().userId(userId).build());
                if (dto.getBaseCurrency() != null) s.setBaseCurrency(dto.getBaseCurrency());
                if (dto.getTheme() != null) s.setTheme(dto.getTheme());
                if (dto.getDateFormat() != null) s.setDateFormat(dto.getDateFormat());
                if (dto.getDefaultMarket() != null) s.setDefaultMarket(dto.getDefaultMarket());
                if (dto.getMarketDataRefreshSeconds() != null) s.setMarketDataRefreshSeconds(dto.getMarketDataRefreshSeconds());
                userSettingsRepository.save(s);
                settings++;
            }
        }

        Map<String, Long> restored = new LinkedHashMap<>();
        restored.put("investments", investments);
        restored.put("transactions", transactions);
        restored.put("expenses", expenses);
        restored.put("expenseAssetMappings", mappings);
        restored.put("snapshots", snapshots);
        restored.put("settings", settings);
        restored.put("assetValuations", valuations);
        restored.put("symbolMappings", symbolMappings);

        verifyCounts(envelope.getCounts(), restored);

        log.info("Backup restore complete for user {}: {}", userId, restored);
        return restored;
    }

    /**
     * Rewrites a backup-time reference to the freshly generated id. A miss
     * here means the validator let a dangling reference through — fail hard
     * (the transaction rolls back) rather than persisting corrupt links.
     */
    private Long remap(Long backupId, Map<Long, Long> idMap, String field) {
        if (backupId == null) {
            return null;
        }
        Long mapped = idMap.get(backupId);
        if (mapped == null) {
            throw new IllegalStateException(
                    "Restore integrity failure: " + field + " references backup id "
                            + backupId + " which was not restored.");
        }
        return mapped;
    }

    // ------------------------------------------------------------------
    // Validation
    // ------------------------------------------------------------------

    private void validateEnvelope(BackupEnvelopeDTO envelope) {
        if (envelope == null) {
            throw new IllegalArgumentException("Backup file is empty or not valid JSON");
        }
        int version = envelope.getSchemaVersion();
        if (version > BackupEnvelopeDTO.SCHEMA_VERSION) {
            throw new IllegalArgumentException(
                    "Backup schema version " + version + " is newer than this application supports ("
                            + BackupEnvelopeDTO.SCHEMA_VERSION + "). Please update the application.");
        }
        if (version < BackupEnvelopeDTO.MIN_SUPPORTED_VERSION) {
            throw new IllegalArgumentException(
                    "Backup schema version " + version + " is too old to restore.");
        }
        if (envelope.getInvestments() == null || envelope.getTransactions() == null
                || envelope.getExpenses() == null) {
            throw new IllegalArgumentException(
                    "Backup is missing required sections (investments/transactions/expenses).");
        }

        Set<Long> investmentIds = new HashSet<>();
        for (InvestmentDTO dto : envelope.getInvestments()) {
            if (dto.getId() != null) investmentIds.add(dto.getId());
        }
        Set<Long> expenseIds = new HashSet<>();
        for (ExpenseDTO dto : envelope.getExpenses()) {
            if (dto.getId() != null) expenseIds.add(dto.getId());
        }

        // Every transaction must point at an investment in the file.
        for (TransactionDTO dto : envelope.getTransactions()) {
            if (dto.getInvestmentId() != null && !investmentIds.contains(dto.getInvestmentId())) {
                throw new IllegalArgumentException(
                        "Backup is inconsistent: transaction references investment "
                                + dto.getInvestmentId() + " which is not present in the backup.");
            }
            if (dto.getExpenseId() != null && !expenseIds.contains(dto.getExpenseId())) {
                throw new IllegalArgumentException(
                        "Backup is inconsistent: transaction references expense "
                                + dto.getExpenseId() + " which is not present in the backup.");
            }
        }

        // Expense paid-from must be a cash account carried in the file.
        for (ExpenseDTO dto : envelope.getExpenses()) {
            if (dto.getPaidFromInvestmentId() != null && !investmentIds.contains(dto.getPaidFromInvestmentId())) {
                throw new IllegalArgumentException(
                        "Backup is inconsistent: expense \"" + dto.getDescription()
                                + "\" references cash account " + dto.getPaidFromInvestmentId()
                                + " which is not present in the backup.");
            }
        }

        // Coverage mappings must reference rows present in the file.
        if (envelope.getExpenseAssetMappings() != null) {
            for (ExpenseAssetMapping m : envelope.getExpenseAssetMappings()) {
                if (m.getExpenseId() != null && !expenseIds.contains(m.getExpenseId())) {
                    throw new IllegalArgumentException(
                            "Backup is inconsistent: expense mapping references expense "
                                    + m.getExpenseId() + " which is not present in the backup.");
                }
                if (m.getInvestmentId() != null && !investmentIds.contains(m.getInvestmentId())) {
                    throw new IllegalArgumentException(
                            "Backup is inconsistent: expense mapping references investment "
                                    + m.getInvestmentId() + " which is not present in the backup.");
                }
            }
        }

        // Valuations must hang off investments present in the file.
        if (envelope.getAssetValuations() != null) {
            for (AssetValuation v : envelope.getAssetValuations()) {
                if (v.getInvestmentId() != null && !investmentIds.contains(v.getInvestmentId())) {
                    throw new IllegalArgumentException(
                            "Backup is inconsistent: asset valuation references investment "
                                    + v.getInvestmentId() + " which is not present in the backup.");
                }
            }
        }

        // Snapshots carry a (user, date) unique index: reject in-file
        // duplicates up front with a clear message instead of a mid-restore
        // constraint violation (which used to abort with a misleading error).
        if (envelope.getSnapshots() != null) {
            Set<java.time.LocalDate> seen = new HashSet<>();
            for (PortfolioSnapshot s : envelope.getSnapshots()) {
                if (s.getSnapshotDate() != null && !seen.add(s.getSnapshotDate())) {
                    throw new IllegalArgumentException(
                            "Backup is inconsistent: more than one snapshot for date "
                                    + s.getSnapshotDate() + ".");
                }
            }
        }
    }

    private void verifyCounts(Map<String, Long> expected, Map<String, Long> actual) {
        if (expected == null) return;
        for (Map.Entry<String, Long> e : expected.entrySet()) {
            // Symbol mappings are shared/global: restore skips already-present rows,
            // so only that section is allowed to differ.
            boolean strict = !"symbolMappings".equals(e.getKey());
            Long got = actual.getOrDefault(e.getKey(), 0L);
            if (strict && !got.equals(e.getValue())) {
                throw new IllegalStateException(
                        "Restore verification failed for '" + e.getKey() + "': expected "
                                + e.getValue() + " rows, restored " + got + ".");
            }
        }
    }

    /**
     * Bulk wipe of every user-owned row. All deletes are {@code @Modifying}
     * JPQL — they execute immediately, in statement order, before any of the
     * re-inserts below. Never replace these with entity-loaded
     * {@code deleteAll(...)} calls: Hibernate flushes inserts before deletes
     * within a transaction, which re-introduces the unique-index collision
     * on portfolio_snapshots.
     */
    private void wipeUserData(Long userId) {
        mappingRepository.deleteAllForUser(userId);   // child of expenses + investments
        transactionRepository.deleteAllForUser(userId); // child of investments
        assetValuationRepository.deleteAllForUser(userId); // child of investments
        expenseRepository.deleteAllForUser(userId);
        investmentRepository.deleteAllForUser(userId);
        snapshotRepository.deleteAllForUser(userId);
        // UserSettings row is kept (upserted by restore) — per-user config, not portfolio data.
    }

    // ------------------------------------------------------------------
    // DTO helpers
    // ------------------------------------------------------------------

    private TransactionDTO toTransactionDto(Transaction t) {
        return TransactionDTO.builder()
                .id(t.getId())
                .investmentId(t.getInvestmentId())
                .type(t.getType())
                .quantity(t.getQuantity())
                .pricePerUnit(t.getPricePerUnit())
                .amount(t.getAmount())
                .transactionDate(t.getTransactionDate())
                .currency(t.getCurrency())
                .notes(t.getNotes())
                .expenseId(t.getExpenseId())
                .createdAt(t.getCreatedAt())
                .build();
    }

    private ExpenseDTO toExpenseDto(Expense e) {
        return ExpenseDTO.builder()
                .id(e.getId())
                .userId(e.getUserId())
                .description(e.getDescription())
                .category(e.getCategory())
                .amount(e.getAmount())
                .currency(e.getCurrency())
                .expenseDate(e.getExpenseDate())
                .notes(e.getNotes())
                .paidFromInvestmentId(e.getPaidFromInvestmentId())
                .isRecurring(e.getIsRecurring())
                .recurrenceFrequency(e.getRecurrenceFrequency())
                .createdAt(e.getCreatedAt())
                .updatedAt(e.getUpdatedAt())
                .build();
    }

    private UserSettingsDTO toSettingsDto(UserSettings s) {
        return UserSettingsDTO.builder()
                .userId(s.getUserId())
                .baseCurrency(s.getBaseCurrency())
                .theme(s.getTheme())
                .dateFormat(s.getDateFormat())
                .defaultMarket(s.getDefaultMarket())
                .marketDataRefreshSeconds(s.getMarketDataRefreshSeconds())
                .build();
    }
}
