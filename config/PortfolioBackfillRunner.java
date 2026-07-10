package com.investment.tracker.config;

import com.investment.tracker.model.Investment;
import com.investment.tracker.model.Transaction;
import com.investment.tracker.repository.InvestmentRepository;
import com.investment.tracker.repository.TransactionRepository;
import com.investment.tracker.service.InitialTransactionFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

/**
 * Backfills missing data for investments created before the historical-valuation
 * fix shipped:
 *   1. Sets {@code purchaseDate} from the earliest BUY transaction (or createdAt).
 *   2. Creates the missing initial BUY/CONTRIBUTION/DEPOSIT transaction for any
 *      investment that has zero transactions.
 *
 * Runs once on every startup; the operations are idempotent.
 */
@Component
@RequiredArgsConstructor
@Slf4j
@Order(20) // After DataInitializer (no explicit order = default LOWEST)
public class PortfolioBackfillRunner implements CommandLineRunner {

    private final InvestmentRepository investmentRepository;
    private final TransactionRepository transactionRepository;
    private final InitialTransactionFactory initialTransactionFactory;
    private final JdbcTemplate jdbcTemplate;

    @Override
    @Transactional
    public void run(String... args) {
        // (0) One-time migration: copy legacy avg_price → purchase_price for
        // EQUITY/CRYPTO rows where ddl-auto:update kept the old avg_price column.
        migrateAvgPriceToPurchasePrice();

        List<Investment> all = investmentRepository.findAll();
        if (all.isEmpty()) {
            log.info("Backfill: no investments to process.");
            return;
        }

        int purchaseDateBackfilled = 0;
        int transactionsBackfilled = 0;

        for (Investment inv : all) {
            long txnCount = transactionRepository.countByInvestmentId(inv.getId());

            // (1) Backfill purchaseDate from earliest existing transaction (or createdAt)
            if (inv.getPurchaseDate() == null) {
                LocalDate derived = null;
                if (txnCount > 0) {
                    derived = transactionRepository
                            .findByInvestmentIdOrderByTransactionDateDesc(inv.getId())
                            .stream()
                            .map(Transaction::getTransactionDate)
                            .min(LocalDate::compareTo)
                            .orElse(null);
                }
                if (derived == null && inv.getCreatedAt() != null) {
                    derived = inv.getCreatedAt().toLocalDate();
                }
                if (derived != null) {
                    inv.setPurchaseDate(derived);
                    investmentRepository.save(inv);
                    purchaseDateBackfilled++;
                }
            }

            // (2) Create the missing initial transaction if none exist
            if (txnCount == 0) {
                Transaction initial = initialTransactionFactory.buildInitialTransaction(inv);
                if (initial != null) {
                    transactionRepository.save(initial);
                    transactionsBackfilled++;
                    log.debug("Backfilled {} txn for investment {} ({}) on {}",
                            initial.getType(), inv.getId(), inv.getName(), initial.getTransactionDate());
                }
            }
        }

        if (purchaseDateBackfilled > 0 || transactionsBackfilled > 0) {
            log.info("Backfill complete. purchaseDate set on {} investment(s); " +
                     "{} initial transaction(s) created.",
                    purchaseDateBackfilled, transactionsBackfilled);
        } else {
            log.info("Backfill: nothing to do ({} investments already consistent).", all.size());
        }
    }

    /**
     * Copy data from legacy {@code avg_price} column into the unified
     * {@code purchase_price} column for EQUITY/CRYPTO rows. The avg_price
     * column is left behind by ddl-auto:update; we just stop reading it.
     */
    private void migrateAvgPriceToPurchasePrice() {
        try {
            Integer hasOld = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS " +
                            "WHERE UPPER(TABLE_NAME) = 'INVESTMENTS' AND UPPER(COLUMN_NAME) = 'AVG_PRICE'",
                    Integer.class);
            if (hasOld == null || hasOld == 0) {
                return; // Column already dropped or never existed
            }
            int updated = jdbcTemplate.update(
                    "UPDATE investments SET purchase_price = avg_price " +
                            "WHERE investment_type IN ('EQUITY', 'CRYPTO') " +
                            "  AND purchase_price IS NULL AND avg_price IS NOT NULL");
            if (updated > 0) {
                log.info("Backfill: migrated avg_price → purchase_price for {} equity/crypto row(s).", updated);
            }
        } catch (Exception e) {
            log.warn("avg_price → purchase_price migration skipped: {}", e.getMessage());
        }
    }
}
