package com.investment.tracker.repository;

import com.investment.tracker.model.ExpenseAssetMapping;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

/**
 * Repository interface for ExpenseAssetMapping entity operations.
 */
@Repository
public interface ExpenseAssetMappingRepository extends JpaRepository<ExpenseAssetMapping, Long> {

    /**
     * Find all mappings for a specific expense.
     */
    List<ExpenseAssetMapping> findByExpenseId(Long expenseId);

    /**
     * Find all mappings for a specific investment.
     */
    List<ExpenseAssetMapping> findByInvestmentId(Long investmentId);

    /**
     * Delete all mappings for a specific expense.
     */
    void deleteByExpenseId(Long expenseId);

    /**
     * Delete all mappings for a specific investment.
     */
    void deleteByInvestmentId(Long investmentId);

    /**
     * Calculate total percentage mapped for an expense.
     */
    @Query("SELECT COALESCE(SUM(m.percentage), 0) FROM ExpenseAssetMapping m " +
           "WHERE m.expenseId = :expenseId")
    BigDecimal sumPercentageByExpenseId(@Param("expenseId") Long expenseId);

    /**
     * Check if expense is fully mapped (100%).
     */
    @Query("SELECT CASE WHEN COALESCE(SUM(m.percentage), 0) = 100 THEN true ELSE false END " +
           "FROM ExpenseAssetMapping m WHERE m.expenseId = :expenseId")
    boolean isExpenseFullyMapped(@Param("expenseId") Long expenseId);
}
