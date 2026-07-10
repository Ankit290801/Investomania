package com.investment.tracker.repository;

import com.investment.tracker.model.Transaction;
import com.investment.tracker.model.TransactionType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Repository interface for Transaction entity operations.
 * Provides CRUD operations and custom queries for transaction management.
 */
@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    /**
     * Find all transactions for a specific investment.
     */
    List<Transaction> findByInvestmentId(Long investmentId);

    /**
     * Find transactions for an investment filtered by type.
     */
    List<Transaction> findByInvestmentIdAndType(Long investmentId, TransactionType type);

    /**
     * Find transactions for an investment ordered by date descending.
     */
    List<Transaction> findByInvestmentIdOrderByTransactionDateDesc(Long investmentId);

    /**
     * Find all transactions for a user within a date range.
     * Requires join with Investment table to filter by userId.
     */
    @Query("SELECT t FROM Transaction t WHERE t.investmentId IN " +
           "(SELECT i.id FROM Investment i WHERE i.userId = :userId) " +
           "AND t.transactionDate BETWEEN :startDate AND :endDate " +
           "ORDER BY t.transactionDate DESC")
    List<Transaction> findByUserIdAndTransactionDateBetween(
        @Param("userId") Long userId,
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate
    );

    /**
     * Find all transactions for a specific user.
     */
    @Query("SELECT t FROM Transaction t WHERE t.investmentId IN " +
           "(SELECT i.id FROM Investment i WHERE i.userId = :userId) " +
           "ORDER BY t.transactionDate DESC")
    List<Transaction> findByUserId(@Param("userId") Long userId);

    /**
     * Count transactions for an investment.
     */
    long countByInvestmentId(Long investmentId);

    /**
     * Delete all transactions for a specific investment.
     */
    void deleteByInvestmentId(Long investmentId);

    /**
     * Find transactions for an investment up to a specific date (inclusive).
     */
    List<Transaction> findByInvestmentIdAndTransactionDateLessThanEqualOrderByTransactionDateAsc(
        Long investmentId, LocalDate date);

    /**
     * Returns the earliest BUY transaction date for each investment in the given list.
     * Result: list of Object[]{investmentId (Long), earliestDate (LocalDate)}
     */
    @Query("SELECT t.investmentId, MIN(t.transactionDate) FROM Transaction t " +
           "WHERE t.investmentId IN :ids AND t.type = com.investment.tracker.model.TransactionType.BUY " +
           "GROUP BY t.investmentId")
    List<Object[]> findEarliestBuyDatesByInvestmentIds(@Param("ids") List<Long> ids);

    /**
     * Convenience helper: returns a map of investmentId → earliest BUY date.
     */
    default Map<Long, LocalDate> getEarliestBuyDateMap(List<Long> ids) {
        if (ids == null || ids.isEmpty()) return java.util.Collections.emptyMap();
        return findEarliestBuyDatesByInvestmentIds(ids).stream()
                .collect(Collectors.toMap(
                        row -> (Long) row[0],
                        row -> (LocalDate) row[1]
                ));
    }
}
