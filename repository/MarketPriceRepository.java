package com.investment.tracker.repository;

import com.investment.tracker.model.MarketPrice;
import com.investment.tracker.model.PriceSource;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository for MarketPrice entity
 */
@Repository
public interface MarketPriceRepository extends JpaRepository<MarketPrice, Long> {
    
    /**
     * Find the latest price for a symbol from any source
     */
    @Query("SELECT mp FROM MarketPrice mp WHERE mp.symbol = :symbol " +
           "ORDER BY mp.lastUpdated DESC")
    Optional<MarketPrice> findLatestBySymbol(@Param("symbol") String symbol);
    
    /**
     * Find the latest price for a symbol from a specific source
     */
    @Query("SELECT mp FROM MarketPrice mp WHERE mp.symbol = :symbol " +
           "AND mp.source = :source ORDER BY mp.lastUpdated DESC")
    Optional<MarketPrice> findLatestBySymbolAndSource(@Param("symbol") String symbol, 
                                                       @Param("source") PriceSource source);
    
    /**
     * Find all prices for a symbol (price history)
     */
    List<MarketPrice> findBySymbolOrderByLastUpdatedDesc(String symbol);
    
    /**
     * Find all prices updated after a specific date
     */
    List<MarketPrice> findByLastUpdatedAfter(LocalDateTime dateTime);
    
    /**
     * Find all prices for multiple symbols (bulk lookup)
     */
    @Query("SELECT mp FROM MarketPrice mp WHERE mp.symbol IN :symbols " +
           "AND mp.id IN (SELECT MAX(mp2.id) FROM MarketPrice mp2 " +
           "WHERE mp2.symbol = mp.symbol GROUP BY mp2.symbol)")
    List<MarketPrice> findLatestBySymbols(@Param("symbols") List<String> symbols);
    
    /**
     * Check if a price exists for a symbol and is recent (within last N hours)
     */
    @Query("SELECT CASE WHEN COUNT(mp) > 0 THEN true ELSE false END " +
           "FROM MarketPrice mp WHERE mp.symbol = :symbol " +
           "AND mp.lastUpdated > :cutoffTime")
    boolean existsRecentPrice(@Param("symbol") String symbol, 
                              @Param("cutoffTime") LocalDateTime cutoffTime);
    
    /**
     * Count prices by source
     */
    long countBySource(PriceSource source);
    
    /**
     * Delete old price records (cleanup)
     */
    void deleteByLastUpdatedBefore(LocalDateTime dateTime);
}
