package com.investment.tracker.repository;

import com.investment.tracker.model.CurrencyRate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository for CurrencyRate entity
 */
@Repository
public interface CurrencyRateRepository extends JpaRepository<CurrencyRate, Long> {
    
    /**
     * Find exchange rate for a specific currency pair
     */
    Optional<CurrencyRate> findByFromCurrencyAndToCurrency(String fromCurrency, String toCurrency);
    
    /**
     * Find all rates from a specific currency
     */
    List<CurrencyRate> findByFromCurrency(String fromCurrency);
    
    /**
     * Find all rates to a specific currency
     */
    List<CurrencyRate> findByToCurrency(String toCurrency);
    
    /**
     * Find valid (recent) rate for a currency pair
     */
    @Query("SELECT cr FROM CurrencyRate cr WHERE cr.fromCurrency = :fromCurrency " +
           "AND cr.toCurrency = :toCurrency " +
           "AND cr.lastUpdated > :cutoffTime " +
           "ORDER BY cr.lastUpdated DESC")
    Optional<CurrencyRate> findValidRate(@Param("fromCurrency") String fromCurrency,
                                         @Param("toCurrency") String toCurrency,
                                         @Param("cutoffTime") LocalDateTime cutoffTime);
    
    /**
     * Find all rates that need refresh (older than 24 hours)
     */
    @Query("SELECT cr FROM CurrencyRate cr WHERE cr.lastUpdated < :cutoffTime")
    List<CurrencyRate> findStaleRates(@Param("cutoffTime") LocalDateTime cutoffTime);
    
    /**
     * Idempotent upsert for H2 (and PostgreSQL via its INSERT ... ON CONFLICT).
     * Insert a fresh rate for a (from_currency, to_currency) pair, or silently
     * replace any stale/duplicate row so a concurrent or stale-refresh thread
     * never hits the uk_currency_pair unique constraint. H2 2.x native MERGE:
     *   MERGE INTO currency_rates (cols...) KEY (from_currency, to_currency) VALUES (...)
     * The explicit column list is REQUIRED on H2: without it the VALUES clause
     * must supply a value for every table column (including the generated id),
     * otherwise H2 fails with error 21002 "Column count does not match".
     * REQUIRES_NEW isolation: a failed upsert must never mark the caller's
     * transaction rollback-only (an inner failure inside an outer @Transactional
     * request silently poisons the whole transaction — observed as
     * UnexpectedRollbackException on /real-growth when this query was wrong).
     */
    @Modifying(clearAutomatically = true)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @Query(value =
           "MERGE INTO currency_rates "
           + "(from_currency, to_currency, rate, last_updated, source, created_at) "
           + "KEY (from_currency, to_currency) "
           + "VALUES (:fromCurrency, :toCurrency, :rate, :lastUpdated, :source, CURRENT_TIMESTAMP)",
           nativeQuery = true)
    void upsertRate(@Param("fromCurrency") String fromCurrency,
                    @Param("toCurrency") String toCurrency,
                    @Param("rate") BigDecimal rate,
                    @Param("lastUpdated") LocalDateTime lastUpdated,
                    @Param("source") String source);

    /**
     * Check if a valid rate exists for a currency pair
     */
    @Query("SELECT CASE WHEN COUNT(cr) > 0 THEN true ELSE false END " +
           "FROM CurrencyRate cr WHERE cr.fromCurrency = :fromCurrency " +
           "AND cr.toCurrency = :toCurrency " +
           "AND cr.lastUpdated > :cutoffTime")
    boolean existsValidRate(@Param("fromCurrency") String fromCurrency,
                           @Param("toCurrency") String toCurrency,
                           @Param("cutoffTime") LocalDateTime cutoffTime);
    
    /**
     * Delete old rates (cleanup - keep only latest)
     */
    @Modifying
    @Transactional
    @Query("DELETE FROM CurrencyRate cr WHERE cr.lastUpdated < :cutoffTime")
    int deleteOldRates(@Param("cutoffTime") LocalDateTime cutoffTime);
}
