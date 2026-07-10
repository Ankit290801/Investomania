package com.investment.tracker.repository;

import com.investment.tracker.model.Expense;
import com.investment.tracker.model.ExpenseCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Repository interface for Expense entity operations.
 */
@Repository
public interface ExpenseRepository extends JpaRepository<Expense, Long> {

    /**
     * Find all expenses for a specific user.
     */
    List<Expense> findByUserIdOrderByExpenseDateDesc(Long userId);

    /**
     * Find expenses by user and category.
     */
    List<Expense> findByUserIdAndCategoryOrderByExpenseDateDesc(Long userId, ExpenseCategory category);

    /**
     * Find expenses by user within a date range.
     */
    @Query("SELECT e FROM Expense e WHERE e.userId = :userId " +
           "AND e.expenseDate BETWEEN :startDate AND :endDate " +
           "ORDER BY e.expenseDate DESC")
    List<Expense> findByUserIdAndDateRange(
        @Param("userId") Long userId,
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate
    );

    /**
     * Find expenses by user, category, and date range.
     */
    @Query("SELECT e FROM Expense e WHERE e.userId = :userId " +
           "AND e.category = :category " +
           "AND e.expenseDate BETWEEN :startDate AND :endDate " +
           "ORDER BY e.expenseDate DESC")
    List<Expense> findByUserIdCategoryAndDateRange(
        @Param("userId") Long userId,
        @Param("category") ExpenseCategory category,
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate
    );

    /**
     * Calculate total expenses for a user within date range.
     */
    @Query("SELECT COALESCE(SUM(e.amount), 0) FROM Expense e " +
           "WHERE e.userId = :userId " +
           "AND e.expenseDate BETWEEN :startDate AND :endDate")
    BigDecimal sumExpensesByUserAndDateRange(
        @Param("userId") Long userId,
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate
    );

    /**
     * Count expenses for a user.
     */
    long countByUserId(Long userId);
}
