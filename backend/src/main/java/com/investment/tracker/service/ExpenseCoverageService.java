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
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Service for calculating expense coverage metrics.
 *
 * All aggregates are currency-aware: every amount is converted to the
 * requested target currency via {@link ForexService} before summing, so a
 * mixed-currency expense history produces correct totals instead of
 * symbol-swapped raw additions.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class ExpenseCoverageService {

    private final ExpenseRepository expenseRepository;
    private final TransactionRepository transactionRepository;
    private final ForexService forexService;

    /** Convert a single amount to the target currency (null-safe). */
    private BigDecimal toTarget(BigDecimal amount, String fromCurrency, String targetCurrency) {
        if (amount == null) {
            return BigDecimal.ZERO;
        }
        if (fromCurrency == null || fromCurrency.equalsIgnoreCase(targetCurrency)) {
            return amount;
        }
        return forexService.convert(amount, fromCurrency, targetCurrency);
    }

    /** Map an expense collection to (category -> converted total). */
    private Map<String, BigDecimal> categoryTotals(Collection<Expense> expenses, String targetCurrency) {
        return expenses.stream().collect(Collectors.groupingBy(
            e -> e.getCategory().name(),
            Collectors.mapping(
                (Function<Expense, BigDecimal>) e -> toTarget(e.getAmount(), e.getCurrency(), targetCurrency),
                Collectors.reducing(BigDecimal.ZERO, BigDecimal::add)
            )
        ));
    }

    /** Map an expense collection to (yyyy-MM -> converted total). */
    private Map<String, BigDecimal> monthlyTotals(Collection<Expense> expenses, String targetCurrency) {
        return expenses.stream().collect(Collectors.groupingBy(
            e -> YearMonth.from(e.getExpenseDate()).format(DateTimeFormatter.ofPattern("yyyy-MM")),
            TreeMap::new,
            Collectors.mapping(
                (Function<Expense, BigDecimal>) e -> toTarget(e.getAmount(), e.getCurrency(), targetCurrency),
                Collectors.reducing(BigDecimal.ZERO, BigDecimal::add)
            )
        ));
    }

    /**
     * Calculate expense coverage metrics for a user within date range.
     * All amounts are converted to {@code currency} before aggregation.
     */
    public ExpenseCoverageDTO calculateCoverage(Long userId, LocalDate startDate, LocalDate endDate, String currency) {
        log.info("Calculating expense coverage for user {} from {} to {} in {}", userId, startDate, endDate, currency);

        // Calculate total expenses (currency-converted)
        List<Expense> expenses = expenseRepository.findByUserIdAndDateRange(userId, startDate, endDate);
        BigDecimal totalExpenses = expenses.stream()
            .map(e -> toTarget(e.getAmount(), e.getCurrency(), currency))
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Calculate total income from investments (dividends, interest),
        // converted to the target currency
        List<Transaction> incomeTransactions = transactionRepository.findByUserId(userId)
            .stream()
            .filter(txn -> txn.getTransactionDate().isAfter(startDate.minusDays(1)) 
                        && txn.getTransactionDate().isBefore(endDate.plusDays(1)))
            .filter(txn -> txn.getType() == TransactionType.DIVIDEND 
                        || txn.getType() == TransactionType.INTEREST)
            .collect(Collectors.toList());

        BigDecimal totalIncome = incomeTransactions.stream()
            .map(txn -> toTarget(txn.getAmount(), txn.getCurrency(), currency))
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Calculate coverage percentage
        BigDecimal coveragePercentage = totalExpenses.compareTo(BigDecimal.ZERO) > 0
            ? totalIncome.divide(totalExpenses, 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100))
            : BigDecimal.ZERO;

        return ExpenseCoverageDTO.builder()
            .totalExpenses(totalExpenses)
            .totalIncome(totalIncome)
            .coveragePercentage(coveragePercentage)
            .categoryBreakdown(categoryTotals(expenses, currency))
            .monthlyTrend(monthlyTotals(expenses, currency))
            .currency(currency)
            .build();
    }

    /**
     * Get category breakdown for a user, converted to {@code currency}.
     */
    public Map<String, BigDecimal> getCategoryBreakdown(Long userId, LocalDate startDate, LocalDate endDate, String currency) {
        List<Expense> expenses = expenseRepository.findByUserIdAndDateRange(userId, startDate, endDate);
        return categoryTotals(expenses, currency);
    }

    /**
     * Get monthly expense trend, converted to {@code currency}.
     */
    public Map<String, BigDecimal> getMonthlyTrend(Long userId, int months, String currency) {
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusMonths(months);
        
        List<Expense> expenses = expenseRepository.findByUserIdAndDateRange(userId, startDate, endDate);
        return monthlyTotals(expenses, currency);
    }
}
