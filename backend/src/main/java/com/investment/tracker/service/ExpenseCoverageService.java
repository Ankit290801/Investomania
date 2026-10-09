package com.investment.tracker.service;

import com.investment.tracker.dto.ExpenseCoverageDTO;
import com.investment.tracker.model.Expense;
import com.investment.tracker.model.ExpenseAssetMapping;
import com.investment.tracker.model.ExpenseCategory;
import com.investment.tracker.model.Transaction;
import com.investment.tracker.model.TransactionType;
import com.investment.tracker.repository.ExpenseAssetMappingRepository;
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
    private final ExpenseAssetMappingRepository mappingRepository;
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

        // Declared coverage: an ExpenseAssetMapping states what share of an
        // expense is covered by a mapped investment (e.g. rent 50% from FD
        // interest + 50% from dividend income), so the mappings — not the
        // income total — drive the coverage percentage. The mapping API
        // already caps the sum per expense at 100%.
        Map<Long, BigDecimal> mappedPercentByExpense = mappingRepository.findAllForUser(userId).stream()
            .collect(Collectors.groupingBy(
                ExpenseAssetMapping::getExpenseId,
                Collectors.mapping(
                    ExpenseAssetMapping::getPercentage,
                    Collectors.reducing(BigDecimal.ZERO, BigDecimal::add)
                )
            ));

        BigDecimal totalCoveredExpenses = expenses.stream()
            .map(e -> {
                BigDecimal mappedPercentage = mappedPercentByExpense.getOrDefault(e.getId(), BigDecimal.ZERO);
                if (mappedPercentage.compareTo(BigDecimal.ZERO) <= 0) {
                    return BigDecimal.ZERO;
                }
                BigDecimal share = mappedPercentage.min(BigDecimal.valueOf(100))
                    .divide(BigDecimal.valueOf(100), 6, RoundingMode.HALF_UP);
                return toTarget(e.getAmount(), e.getCurrency(), currency).multiply(share);
            })
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Calculate coverage percentage from the mapped (declared) portion
        BigDecimal coveragePercentage = totalExpenses.compareTo(BigDecimal.ZERO) > 0
            ? totalCoveredExpenses.divide(totalExpenses, 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100))
            : BigDecimal.ZERO;

        return ExpenseCoverageDTO.builder()
            .totalExpenses(totalExpenses)
            .totalCoveredExpenses(totalCoveredExpenses)
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

    /**
     * Year-over-year expense growth: for the two most recent full calendar
     * years found in the user's history, return the percentage change in
     * total spend. Returns {@code null} when there's not enough history.
     * Amounts are converted to {@code currency} before summing, so a mixed-
     * currency history still produces a meaningful comparison.
     */
    public BigDecimal getYearOverYearGrowth(Long userId, String currency) {
        List<Expense> all = expenseRepository.findByUserIdAndDateRange(
                userId, LocalDate.of(2000, 1, 1), LocalDate.now());
        Map<Integer, BigDecimal> byYear = all.stream()
            .collect(Collectors.groupingBy(
                e -> e.getExpenseDate().getYear(),
                TreeMap::new,
                Collectors.mapping(
                    e -> toTarget(e.getAmount(), e.getCurrency(), currency),
                    Collectors.reducing(BigDecimal.ZERO, BigDecimal::add)
                )
            ));
        List<Integer> years = new ArrayList<>(byYear.keySet());
        if (years.size() < 2) {
            return null;
        }
        int recent = years.get(years.size() - 1);
        int prior = years.get(years.size() - 2);
        BigDecimal recentTotal = byYear.get(recent);
        BigDecimal priorTotal = byYear.get(prior);
        if (priorTotal.compareTo(BigDecimal.ZERO) == 0) {
            return recentTotal.compareTo(BigDecimal.ZERO) > 0 ? BigDecimal.ONE : BigDecimal.ZERO;
        }
        return recentTotal.subtract(priorTotal)
                .divide(priorTotal, 4, RoundingMode.HALF_UP);
    }

    /**
     * Month-over-month change for the most recent complete month against the
     * month before it. Returns the percentage change (positive = spend went up).
     * When there aren't two complete months of data, returns {@code null}.
     */
    public BigDecimal getMonthOverMonthGrowth(Long userId, String currency) {
        YearMonth now = YearMonth.now();
        YearMonth recent = now.isAfter(YearMonth.from(LocalDate.now())) ? now : now;
        YearMonth m2 = recent;
        YearMonth m1 = m2.minusMonths(1);

        // Use only expenses dated strictly within each month so a partial
        // current month does not distort the comparison.
        LocalDate start2 = m2.atDay(1);
        LocalDate end2 = m2.atEndOfMonth();
        LocalDate start1 = m1.atDay(1);
        LocalDate end1 = m1.atEndOfMonth();

        BigDecimal sum2 = expenseRepository.findByUserIdAndDateRange(userId, start2, end2).stream()
            .map(e -> toTarget(e.getAmount(), e.getCurrency(), currency))
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal sum1 = expenseRepository.findByUserIdAndDateRange(userId, start1, end1).stream()
            .map(e -> toTarget(e.getAmount(), e.getCurrency(), currency))
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (sum2.compareTo(BigDecimal.ZERO) == 0 && sum1.compareTo(BigDecimal.ZERO) == 0) {
            return null;
        }
        if (sum1.compareTo(BigDecimal.ZERO) == 0) {
            return sum2.compareTo(BigDecimal.ZERO) > 0 ? BigDecimal.ONE : BigDecimal.ZERO;
        }
        return sum2.subtract(sum1).divide(sum1, 4, RoundingMode.HALF_UP);
    }

    /**
     * Analytics read-model for the expense panels. One call returns the set
     * of numbers the frontend panels need: latest month total, prior month
     * total, latest month's month-over-month change, year-over-year change
     * for the two most recent full years, per-category totals with each
     * category's percentage of the latest full year, plus judgement-call
     * roll-ups (average monthly spend over the reported range, the single
     * largest spending category, and how many months had any spend).
     */
    public Map<String, Object> getExpenseAnalytics(Long userId, String currency) {
        YearMonth now = YearMonth.now();
        LocalDate today = LocalDate.now();
        YearMonth latestFull = YearMonth.from(today);
        if (today.getDayOfMonth() < today.lengthOfMonth() && today.isBefore(latestFull.atDay(today.getDayOfMonth()))) {
            latestFull = latestFull.minusMonths(1);
        }

        YearMonth priorFull = latestFull.minusMonths(1);
        YearMonth yearAgo = latestFull.minusYears(1);

        LocalDate rangeStart = latestFull.atDay(1).minusYears(2);
        LocalDate rangeEnd = latestFull.atEndOfMonth();

        List<Expense> inRange = expenseRepository.findByUserIdAndDateRange(userId, rangeStart, rangeEnd);

        Map<String, BigDecimal> categoryTotals = categoryTotals(inRange, currency);

        YearMonth lfFinal = latestFull;
        YearMonth pfFinal = priorFull;

        BigDecimal latestMonthTotal = inRange.stream()
            .filter(e -> YearMonth.from(e.getExpenseDate()).equals(lfFinal))
            .map(e -> toTarget(e.getAmount(), e.getCurrency(), currency))
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal priorMonthTotal = inRange.stream()
            .filter(e -> YearMonth.from(e.getExpenseDate()).equals(pfFinal))
            .map(e -> toTarget(e.getAmount(), e.getCurrency(), currency))
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal latestMonthMoM = getMonthOverMonthGrowth(userId, currency);

        BigDecimal yearOverYear = getYearOverYearGrowth(userId, currency);

        // Average monthly spend over the reported two-year range (only months
        // that actually have expenses are counted, so sparse histories aren't
        // artificially depressed by empty months).
        Set<YearMonth> activeMonths = inRange.stream()
            .map(e -> YearMonth.from(e.getExpenseDate()))
            .collect(Collectors.toCollection(TreeSet::new));
        BigDecimal totalInRange = inRange.stream()
            .map(e -> toTarget(e.getAmount(), e.getCurrency(), currency))
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal avgMonthly = activeMonths.isEmpty()
            ? BigDecimal.ZERO
            : totalInRange.divide(BigDecimal.valueOf(activeMonths.size()), 2, RoundingMode.HALF_UP);

        // Largest spending category by total in the reported range.
        Map.Entry<String, BigDecimal> biggest = categoryTotals.entrySet().stream()
            .max(Comparator.comparing(Map.Entry::getValue))
            .orElse(null);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("currency", currency);
        result.put("latestMonth", latestFull.format(DateTimeFormatter.ofPattern("MMMM yyyy")));
        result.put("latestMonthTotal", latestMonthTotal);
        result.put("priorMonth", priorFull.format(DateTimeFormatter.ofPattern("MMMM yyyy")));
        result.put("priorMonthTotal", priorMonthTotal);
        result.put("monthOverMonth", latestMonthMoM);
        result.put("yearOverYear", yearOverYear);
        result.put("categoryTotals", categoryTotals);
        result.put("avgMonthly", avgMonthly);
        result.put(" biggestCategory", biggest == null ? null : Map.of(
                "category", biggest.getKey(), "amount", biggest.getValue()));
        result.put("activeMonths", activeMonths.size());
        return result;
    }

    /**
     * Category mix percentages for the latest full year. Returns
     * (category -> percentage of total) so the frontend can render a simple
     * ranking or a pie without doing its own currency/concurrency math.
     */
    public Map<String, BigDecimal> getCategoryMix(Long userId, String currency) {
        YearMonth now = YearMonth.now();
        LocalDate today = LocalDate.now();
        YearMonth latestFull = YearMonth.from(today);
        if (today.getDayOfMonth() < today.lengthOfMonth() && today.isBefore(latestFull.atDay(today.getDayOfMonth()))) {
            latestFull = latestFull.minusMonths(1);
        }

        LocalDate yearStart = latestFull.atDay(1);
        LocalDate yearEnd = latestFull.atEndOfMonth();
        List<Expense> inYear = expenseRepository.findByUserIdAndDateRange(userId, yearStart, yearEnd);

        Map<String, BigDecimal> totals = categoryTotals(inYear, currency);
        BigDecimal grandTotal = totals.values().stream()
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (grandTotal.compareTo(BigDecimal.ZERO) == 0) {
            return new LinkedHashMap<>();
        }

        Map<String, BigDecimal> mix = new LinkedHashMap<>();
        for (Map.Entry<String, BigDecimal> e : totals.entrySet()) {
            mix.put(e.getKey(), e.getValue().divide(grandTotal, 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100)));
        }
        return mix;
    }
}
