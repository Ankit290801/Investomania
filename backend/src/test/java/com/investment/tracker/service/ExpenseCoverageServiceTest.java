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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

/**
 * Unit tests for the mapping-driven expense coverage metric (E14 polish,
 * October 3 2026): an expense's {@code ExpenseAssetMapping} percentages — not
 * the investment income total — determine the coverage percentage shown on
 * the dashboard, so adding/removing/updating a mapping always moves it.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ExpenseCoverageServiceTest {

    @Mock
    private ExpenseRepository expenseRepository;
    @Mock
    private TransactionRepository transactionRepository;
    @Mock
    private ExpenseAssetMappingRepository mappingRepository;
    @Mock
    private ForexService forexService;

    private ExpenseCoverageService service;

    private static final Long USER_ID = 7L;
    private static final LocalDate START = LocalDate.of(2026, 1, 1);
    private static final LocalDate END = LocalDate.of(2026, 12, 31);

    @BeforeEach
    void setUp() {
        service = new ExpenseCoverageService(
                expenseRepository, transactionRepository, mappingRepository, forexService);
        when(transactionRepository.findByUserId(USER_ID)).thenReturn(List.of());
    }

    private static Expense expense(long id, String amount) {
        return expense(id, amount, "INR");
    }

    private static Expense expense(long id, String amount, String currency) {
        return Expense.builder()
                .id(id)
                .userId(USER_ID)
                .description("expense " + id)
                .category(ExpenseCategory.OTHER)
                .amount(new BigDecimal(amount))
                .currency(currency)
                .expenseDate(LocalDate.of(2026, 6, 1))
                .build();
    }

    private static ExpenseAssetMapping mapping(long expenseId, String percentage) {
        return ExpenseAssetMapping.builder()
                .expenseId(expenseId)
                .investmentId(1L)
                .percentage(new BigDecimal(percentage))
                .build();
    }

    private static void assertBigDecimalEquals(String expected, BigDecimal actual) {
        assertEquals(0, new BigDecimal(expected).compareTo(actual),
                "expected " + expected + " but was " + actual);
    }

    @Test
    void unmappedExpensesGiveZeroCoverageEvenWhenIncomeExists() {
        when(expenseRepository.findByUserIdAndDateRange(USER_ID, START, END))
                .thenReturn(List.of(expense(1L, "1000")));
        when(mappingRepository.findAllForUser(USER_ID)).thenReturn(List.of());
        when(transactionRepository.findByUserId(USER_ID)).thenReturn(List.of(
                Transaction.builder()
                        .investmentId(1L)
                        .type(TransactionType.DIVIDEND)
                        .amount(new BigDecimal("5000"))
                        .currency("INR")
                        .transactionDate(LocalDate.of(2026, 5, 1))
                        .build()));

        ExpenseCoverageDTO coverage = service.calculateCoverage(USER_ID, START, END, "INR");

        assertBigDecimalEquals("1000", coverage.getTotalExpenses());
        assertBigDecimalEquals("0", coverage.getTotalCoveredExpenses());
        assertBigDecimalEquals("0", coverage.getCoveragePercentage());
        assertBigDecimalEquals("5000", coverage.getTotalIncome());
    }

    @Test
    void mappingsDriveTheCoveragePercentage() {
        when(expenseRepository.findByUserIdAndDateRange(USER_ID, START, END))
                .thenReturn(List.of(expense(1L, "1000"), expense(2L, "1000")));
        when(mappingRepository.findAllForUser(USER_ID)).thenReturn(List.of(
                mapping(1L, "50"),
                mapping(1L, "50"),
                mapping(2L, "25")));

        ExpenseCoverageDTO coverage = service.calculateCoverage(USER_ID, START, END, "INR");

        assertBigDecimalEquals("2000", coverage.getTotalExpenses());
        assertBigDecimalEquals("1250", coverage.getTotalCoveredExpenses());
        assertBigDecimalEquals("62.5", coverage.getCoveragePercentage());
    }

    @Test
    void mappingPercentagesAboveOneHundredAreCapped() {
        when(expenseRepository.findByUserIdAndDateRange(USER_ID, START, END))
                .thenReturn(List.of(expense(1L, "1000")));
        // The API rejects >100% in total, but the service must stay correct
        // if legacy/imported data ever contains it.
        when(mappingRepository.findAllForUser(USER_ID)).thenReturn(List.of(
                mapping(1L, "80"),
                mapping(1L, "70")));

        ExpenseCoverageDTO coverage = service.calculateCoverage(USER_ID, START, END, "INR");

        assertBigDecimalEquals("1000", coverage.getTotalCoveredExpenses());
        assertBigDecimalEquals("100", coverage.getCoveragePercentage());
    }

    @Test
    void coveredAmountIsConvertedToTheTargetCurrency() {
        when(expenseRepository.findByUserIdAndDateRange(USER_ID, START, END))
                .thenReturn(List.of(expense(1L, "100", "USD")));
        when(mappingRepository.findAllForUser(USER_ID)).thenReturn(List.of(mapping(1L, "50")));
        when(forexService.convert(new BigDecimal("100"), "USD", "INR"))
                .thenReturn(new BigDecimal("8000"));

        ExpenseCoverageDTO coverage = service.calculateCoverage(USER_ID, START, END, "INR");

        assertBigDecimalEquals("8000", coverage.getTotalExpenses());
        assertBigDecimalEquals("4000", coverage.getTotalCoveredExpenses());
        assertBigDecimalEquals("50", coverage.getCoveragePercentage());
    }
}
