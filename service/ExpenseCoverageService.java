package com.investment.tracker.service;

import com.investment.tracker.dto.ExpenseCoverageDTO;
import com.investment.tracker.model.Expense;
import com.investment.tracker.model.ExpenseCategory;
import com.investment.tracker.model.Transaction;
import com.investment.tracker.model.TransactionType;
import com.investment.tracker.repository.ExpenseRepository;
import com.investment.tracker.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for calculating expense coverage metrics.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class ExpenseCoverageService {

    private final ExpenseRepository expenseRepository;
    private final TransactionRepository transactionRepository;

    /**
     * Calculate expense coverage metrics for a user within date range.
     */
    public ExpenseCoverageDTO calculateCoverage(Long userId, LocalDate startDate, LocalDate endDate, String currency) {
        log.info("Calculating expense coverage for user {} from {} to {}", userId, startDate, endDate);

        // Calculate total expenses
        List<Expense> expenses = expenseRepository.findByUserIdAndDateRange(userId, startDate, endDate);
        BigDecimal totalExpenses = expenses.stream()
            .map(Expense::getAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Calculate total income from investments (dividends, interest)
        List<Transaction> incomeTransactions = transactionRepository.findByUserId(userId)
            .stream()
            .filter(txn -> txn.getTransactionDate().isAfter(startDate.minusDays(1)) 
                        && txn.getTransactionDate().isBefore(endDate.plusDays(1)))
            .filter(txn -> txn.getType() == TransactionType.DIVIDEND 
                        || txn.getType() == TransactionType.INTEREST)
            .collect(Collectors.toList());

        BigDecimal totalIncome = incomeTransactions.stream()
            .map(Transaction::getAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Calculate coverage percentage
        BigDecimal coveragePercentage = totalExpenses.compareTo(BigDecimal.ZERO) > 0
            ? totalIncome.divide(totalExpenses, 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100))
            : BigDecimal.ZERO;

        // Category breakdown
        Map<String, BigDecimal> categoryBreakdown = expenses.stream()
            .collect(Collectors.groupingBy(
                expense -> expense.getCategory().name(),
                Collectors.mapping(Expense::getAmount, Collectors.reducing(BigDecimal.ZERO, BigDecimal::add))
            ));

        // Monthly trend
        Map<String, BigDecimal> monthlyTrend = expenses.stream()
            .collect(Collectors.groupingBy(
                expense -> YearMonth.from(expense.getExpenseDate()).format(DateTimeFormatter.ofPattern("yyyy-MM")),
                Collectors.mapping(Expense::getAmount, Collectors.reducing(BigDecimal.ZERO, BigDecimal::add))
            ));

        return ExpenseCoverageDTO.builder()
            .totalExpenses(totalExpenses)
            .totalIncome(totalIncome)
            .coveragePercentage(coveragePercentage)
            .categoryBreakdown(categoryBreakdown)
            .monthlyTrend(monthlyTrend)
            .currency(currency)
            .build();
    }

    /**
     * Get category breakdown for a user.
     */
    public Map<String, BigDecimal> getCategoryBreakdown(Long userId, LocalDate startDate, LocalDate endDate) {
        List<Expense> expenses = expenseRepository.findByUserIdAndDateRange(userId, startDate, endDate);
        
        return expenses.stream()
            .collect(Collectors.groupingBy(
                expense -> expense.getCategory().name(),
                Collectors.mapping(Expense::getAmount, Collectors.reducing(BigDecimal.ZERO, BigDecimal::add))
            ));
    }

    /**
     * Get monthly expense trend.
     */
    public Map<String, BigDecimal> getMonthlyTrend(Long userId, int months) {
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusMonths(months);
        
        List<Expense> expenses = expenseRepository.findByUserIdAndDateRange(userId, startDate, endDate);
        
        return expenses.stream()
            .collect(Collectors.groupingBy(
                expense -> YearMonth.from(expense.getExpenseDate()).format(DateTimeFormatter.ofPattern("yyyy-MM")),
                TreeMap::new,
                Collectors.mapping(Expense::getAmount, Collectors.reducing(BigDecimal.ZERO, BigDecimal::add))
            ));
    }
}
