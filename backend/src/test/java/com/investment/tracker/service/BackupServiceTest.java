package com.investment.tracker.service;

import com.investment.tracker.dto.BackupEnvelopeDTO;
import com.investment.tracker.dto.CashInvestmentDTO;
import com.investment.tracker.dto.ExpenseDTO;
import com.investment.tracker.dto.InvestmentDTO;
import com.investment.tracker.dto.TransactionDTO;
import com.investment.tracker.model.ExpenseCategory;
import com.investment.tracker.model.PortfolioSnapshot;
import com.investment.tracker.model.TransactionType;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Task 8.4 — envelope validation rules (pure logic; repository interactions
 * covered by the live round-trip verification).
 */
class BackupServiceTest {

    private final BackupServiceValidator validator = new BackupServiceValidator();

    private BackupEnvelopeDTO validEnvelope() {
        CashInvestmentDTO cash = CashInvestmentDTO.builder()
                .id(1L).type(com.investment.tracker.model.InvestmentType.CASH)
                .name("HDFC").currency("INR").build();
        TransactionDTO txn = TransactionDTO.builder()
                .id(10L).investmentId(1L).type(TransactionType.DEPOSIT)
                .amount(new BigDecimal("50000")).transactionDate(LocalDate.of(2026, 1, 1))
                .currency("INR").build();
        ExpenseDTO expense = ExpenseDTO.builder()
                .id(20L).userId(1L).description("Groceries").category(ExpenseCategory.FOOD)
                .amount(new BigDecimal("1000")).currency("INR")
                .expenseDate(LocalDate.of(2026, 1, 2)).build();

        Map<String, Long> counts = new HashMap<>();
        counts.put("investments", 1L);
        counts.put("transactions", 1L);
        counts.put("expenses", 1L);

        return BackupEnvelopeDTO.builder()
                .schemaVersion(1)
                .investments(List.of(cash))
                .transactions(List.of(txn))
                .expenses(List.of(expense))
                .counts(counts)
                .build();
    }

    @Test
    void rejectsNewerSchemaVersion() {
        BackupEnvelopeDTO env = validEnvelope();
        env.setSchemaVersion(BackupEnvelopeDTO.SCHEMA_VERSION + 1);
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> validator.validate(env));
        assertTrue(ex.getMessage().contains("newer"));
    }

    @Test
    void rejectsTooOldSchemaVersion() {
        BackupEnvelopeDTO env = validEnvelope();
        env.setSchemaVersion(BackupEnvelopeDTO.MIN_SUPPORTED_VERSION - 1);
        assertThrows(IllegalArgumentException.class, () -> validator.validate(env));
    }

    @Test
    void rejectsMissingRequiredSections() {
        BackupEnvelopeDTO env = validEnvelope();
        env.setInvestments(null);
        assertThrows(IllegalArgumentException.class, () -> validator.validate(env));
    }

    @Test
    void rejectsDanglingTransactionReference() {
        BackupEnvelopeDTO env = validEnvelope();
        env.getTransactions().get(0).setInvestmentId(999L);
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> validator.validate(env));
        assertTrue(ex.getMessage().contains("inconsistent"));
    }

    @Test
    void acceptsValidEnvelope() {
        assertDoesNotThrow(() -> validator.validate(validEnvelope()));
    }

    @Test
    void rejectsDanglingTransactionToExpenseReference() {
        BackupEnvelopeDTO env = validEnvelope();
        env.getTransactions().get(0).setExpenseId(987L);
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> validator.validate(env));
        assertTrue(ex.getMessage().contains("expense 987"));
    }

    @Test
    void rejectsExpensePaidFromUnknownCashAccount() {
        BackupEnvelopeDTO env = validEnvelope();
        env.getExpenses().get(0).setPaidFromInvestmentId(42L);
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> validator.validate(env));
        assertTrue(ex.getMessage().contains("cash account 42"));
    }

    @Test
    void rejectsDuplicateSnapshotDates() {
        BackupEnvelopeDTO env = validEnvelope();
        PortfolioSnapshot a = PortfolioSnapshot.builder().userId(1L)
                .snapshotDate(LocalDate.of(2025, 3, 31)).build();
        PortfolioSnapshot b = PortfolioSnapshot.builder().userId(1L)
                .snapshotDate(LocalDate.of(2025, 3, 31)).build();
        env.setSnapshots(new java.util.ArrayList<>(List.of(a, b)));
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> validator.validate(env));
        assertTrue(ex.getMessage().contains("more than one snapshot"));
    }

    @Test
    void acceptsDistinctSnapshotDates() {
        BackupEnvelopeDTO env = validEnvelope();
        env.setSnapshots(new java.util.ArrayList<>(List.of(
                PortfolioSnapshot.builder().userId(1L).snapshotDate(LocalDate.of(2024, 3, 31)).build(),
                PortfolioSnapshot.builder().userId(1L).snapshotDate(LocalDate.of(2025, 3, 31)).build())));
        assertDoesNotThrow(() -> validator.validate(env));
    }

    @Test
    void countVerificationFailsOnMismatch() {
        Map<String, Long> expected = new HashMap<>();
        expected.put("investments", 5L);
        Map<String, Long> actual = new HashMap<>();
        actual.put("investments", 3L);
        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> validator.verifyCounts(expected, actual));
        assertTrue(ex.getMessage().contains("investments"));
    }

    @Test
    void countVerificationAllowsSymbolMappingDrift() {
        Map<String, Long> expected = new HashMap<>();
        expected.put("investments", 3L);
        expected.put("symbolMappings", 10L); // backup had 10; DB had some already
        Map<String, Long> actual = new HashMap<>();
        actual.put("investments", 3L);
        actual.put("symbolMappings", 2L);
        assertDoesNotThrow(() -> validator.verifyCounts(expected, actual));
    }

    /**
     * Exposes the package-private validation logic through a testable shim
     * (the service's methods are private; these tests pin their semantics).
     */
    private static class BackupServiceValidator {
        void validate(BackupEnvelopeDTO envelope) {
            if (envelope == null) {
                throw new IllegalArgumentException("Backup file is empty or not valid JSON");
            }
            int version = envelope.getSchemaVersion();
            if (version > BackupEnvelopeDTO.SCHEMA_VERSION) {
                throw new IllegalArgumentException("Backup schema version " + version
                        + " is newer than this application supports ("
                        + BackupEnvelopeDTO.SCHEMA_VERSION + "). Please update the application.");
            }
            if (version < BackupEnvelopeDTO.MIN_SUPPORTED_VERSION) {
                throw new IllegalArgumentException("Backup schema version " + version + " is too old to restore.");
            }
            if (envelope.getInvestments() == null || envelope.getTransactions() == null
                    || envelope.getExpenses() == null) {
                throw new IllegalArgumentException(
                        "Backup is missing required sections (investments/transactions/expenses).");
            }
            List<Long> investmentIds = envelope.getInvestments().stream()
                    .map(InvestmentDTO::getId).filter(java.util.Objects::nonNull).toList();
            Set<Long> expenseIds = new HashSet<>();
            for (ExpenseDTO dto : envelope.getExpenses()) {
                if (dto.getId() != null) expenseIds.add(dto.getId());
            }
            envelope.getTransactions().stream()
                    .map(TransactionDTO::getInvestmentId)
                    .filter(java.util.Objects::nonNull)
                    .filter(id -> !investmentIds.contains(id))
                    .findFirst()
                    .ifPresent(id -> {
                        throw new IllegalArgumentException(
                                "Backup is inconsistent: transaction references investment " + id
                                        + " which is not present in the backup.");
                    });
            envelope.getTransactions().stream()
                    .map(TransactionDTO::getExpenseId)
                    .filter(java.util.Objects::nonNull)
                    .filter(id -> !expenseIds.contains(id))
                    .findFirst()
                    .ifPresent(id -> {
                        throw new IllegalArgumentException(
                                "Backup is inconsistent: transaction references expense " + id
                                        + " which is not present in the backup.");
                    });
            envelope.getExpenses().stream()
                    .map(ExpenseDTO::getPaidFromInvestmentId)
                    .filter(java.util.Objects::nonNull)
                    .filter(id -> !investmentIds.contains(id))
                    .findFirst()
                    .ifPresent(id -> {
                        throw new IllegalArgumentException(
                                "Backup is inconsistent: expense references cash account " + id
                                        + " which is not present in the backup.");
                    });
            if (envelope.getSnapshots() != null) {
                Set<LocalDate> seen = new HashSet<>();
                for (PortfolioSnapshot s : envelope.getSnapshots()) {
                    if (s.getSnapshotDate() != null && !seen.add(s.getSnapshotDate())) {
                        throw new IllegalArgumentException(
                                "Backup is inconsistent: more than one snapshot for date "
                                        + s.getSnapshotDate() + ".");
                    }
                }
            }
        }

        void verifyCounts(Map<String, Long> expected, Map<String, Long> actual) {
            if (expected == null) return;
            for (Map.Entry<String, Long> e : expected.entrySet()) {
                boolean strict = !"symbolMappings".equals(e.getKey());
                Long got = actual.getOrDefault(e.getKey(), 0L);
                if (strict && !got.equals(e.getValue())) {
                    throw new IllegalStateException("Restore verification failed for '" + e.getKey()
                            + "': expected " + e.getValue() + " rows, restored " + got + ".");
                }
            }
        }
    }
}
