package com.investment.tracker.repository;

import com.investment.tracker.model.Market;
import com.investment.tracker.model.SymbolMapping;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository for SymbolMapping entity
 */
@Repository
public interface SymbolMappingRepository extends JpaRepository<SymbolMapping, Long> {
    
    /**
     * Find mapping by user's input symbol
     */
    Optional<SymbolMapping> findByUserSymbol(String userSymbol);
    
    /**
     * Find mapping by Yahoo symbol
     */
    Optional<SymbolMapping> findByYahooSymbol(String yahooSymbol);
    
    /**
     * Find mapping by Google symbol
     */
    Optional<SymbolMapping> findByGoogleSymbol(String googleSymbol);
    
    /**
     * Find all mappings for a specific market
     */
    List<SymbolMapping> findByMarket(Market market);
    
    /**
     * Find all verified mappings
     */
    List<SymbolMapping> findByIsVerifiedTrue();
    
    /**
     * Find all unverified mappings
     */
    List<SymbolMapping> findByIsVerifiedFalse();
    
    /**
     * Find mappings that need re-verification (older than N days)
     */
    @Query("SELECT sm FROM SymbolMapping sm WHERE sm.isVerified = true " +
           "AND (sm.lastVerified IS NULL OR sm.lastVerified < :cutoffDate)")
    List<SymbolMapping> findMappingsNeedingReVerification(@Param("cutoffDate") LocalDateTime cutoffDate);
    
    /**
     * Search symbols by partial match (for autocomplete)
     */
    @Query("SELECT sm FROM SymbolMapping sm WHERE " +
           "LOWER(sm.userSymbol) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(sm.name) LIKE LOWER(CONCAT('%', :query, '%')) " +
           "ORDER BY sm.userSymbol")
    List<SymbolMapping> searchSymbols(@Param("query") String query);
    
    /**
     * Check if a user symbol already exists
     */
    boolean existsByUserSymbol(String userSymbol);
    
    /**
     * Get all mappings for a specific market (for bulk updates)
     */
    @Query("SELECT sm FROM SymbolMapping sm WHERE sm.market = :market " +
           "AND sm.isVerified = true ORDER BY sm.userSymbol")
    List<SymbolMapping> findVerifiedByMarket(@Param("market") Market market);
}
