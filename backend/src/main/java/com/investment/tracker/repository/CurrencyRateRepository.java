package com.investment.tracker.repository;

import com.investment.tracker.model.CurrencyRate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

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
