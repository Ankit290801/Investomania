package com.investment.tracker.repository;

import com.investment.tracker.model.SymbolMapping;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

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
}
