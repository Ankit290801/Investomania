package com.investment.tracker.service;

import com.investment.tracker.model.BondInvestment;
import com.investment.tracker.model.CouponFrequency;
import com.investment.tracker.model.Transaction;
import com.investment.tracker.model.TransactionType;
import com.investment.tracker.repository.BondInvestmentRepository;
import com.investment.tracker.repository.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for the bond coupon auto-recording engine (backlog feature F3).
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class BondCouponServiceTest {

    @Mock
    private BondInvestmentRepository bondInvestmentRepository;

    @Mock
    private TransactionRepository transactionRepository;

    private CouponScheduleCalculator scheduleCalculator;
    private BondCouponService couponService;

    private static final LocalDate TODAY = LocalDate.of(2026, 9, 24);

    @BeforeEach
    void setUp() {
        scheduleCalculator = new CouponScheduleCalculator(transactionRepository);
        couponService = new BondCouponService(
                bondInvestmentRepository, transactionRepository, scheduleCalculator);
        when(transactionRepository
                .findByInvestmentIdAndTransactionDateLessThanEqualOrderByTransactionDateAsc(
                        anyLong(), any(LocalDate.class)))
                .thenReturn(Collections.emptyList());
    }

    private BondInvestment bond(LocalDate firstCouponDate, CouponFrequency frequency,
                                Boolean autoGenerate) {
        return BondInvestment.builder()
                .id(1L)
                .userId(10L)
                .name("GOI 8% 2030")
                .currency("INR")
                .faceValue(new BigDecimal("100000"))
                .couponRate(new BigDecimal("8.00"))
                .maturityDate(LocalDate.of(2030, 12, 31))
                .firstCouponDate(firstCouponDate)
                .couponFrequency(frequency)
                .autoGenerateIncome(autoGenerate)
                .build();
    }

    @Test
    void semiAnnualBondGeneratesAllDueCouponsWithCorrectAmount() {
        BondInvestment b = bond(LocalDate.of(2025, 1, 1), CouponFrequency.SEMI_ANNUAL, true);

        int created = couponService.recordDueCouponsForBond(b, TODAY);

        // Due dates: 2025-01-01, 2025-07-01, 2026-01-01, 2026-07-01 (next: 2027-01-01)
        assertEquals(4, created);
        ArgumentCaptor<Transaction> captor = ArgumentCaptor.forClass(Transaction.class);
        verify(transactionRepository, org.mockito.Mockito.times(4)).save(captor.capture());

        Transaction first = captor.getAllValues().get(0);
        assertEquals(TransactionType.INTEREST, first.getType());
        assertEquals(LocalDate.of(2025, 1, 1), first.getTransactionDate());
        assertEquals("INR", first.getCurrency());
        // (100000 × 8% ) / 2 = 4000.00
        assertEquals(0, new BigDecimal("4000.00").compareTo(first.getAmount()));
        assertTrue(first.getNotes().contains("coupon"));
    }

    @Test
    void generationIsIdempotentWhenTransactionsAlreadyExist() {
        BondInvestment b = bond(LocalDate.of(2025, 1, 1), CouponFrequency.SEMI_ANNUAL, true);
        Transaction existing = Transaction.builder()
                .investmentId(1L)
                .type(TransactionType.INTEREST)
                .amount(new BigDecimal("4000.00"))
                .transactionDate(LocalDate.of(2025, 1, 1))
                .currency("INR")
                .build();
        when(transactionRepository
                .findByInvestmentIdAndTransactionDateLessThanEqualOrderByTransactionDateAsc(
                        anyLong(), any(LocalDate.class)))
                .thenReturn(List.of(existing));

        int created = couponService.recordDueCouponsForBond(b, TODAY);

        // Only the three later dates are generated; 2025-01-01 already exists.
        assertEquals(3, created);
    }

    @Test
    void noCouponsAfterMaturity() {
        BondInvestment b = bond(LocalDate.of(2025, 1, 1), CouponFrequency.ANNUAL, true);
        b.setMaturityDate(LocalDate.of(2026, 6, 30));

        int created = couponService.recordDueCouponsForBond(b, TODAY);

        // 2025-01-01 and 2026-01-01 only; 2027-01-01 is past maturity.
        assertEquals(2, created);
    }

    @Test
    void lastCouponDateAnchorsResumePoint() {
        BondInvestment b = bond(LocalDate.of(2025, 1, 1), CouponFrequency.ANNUAL, true);
        b.setLastCouponDate(LocalDate.of(2025, 1, 1));

        int created = couponService.recordDueCouponsForBond(b, TODAY);

        // Resumes strictly after 2025-01-01 → only 2026-01-01 is generated.
        assertEquals(1, created);
        ArgumentCaptor<Transaction> captor = ArgumentCaptor.forClass(Transaction.class);
        verify(transactionRepository).save(captor.capture());
        assertEquals(LocalDate.of(2026, 1, 1), captor.getValue().getTransactionDate());
        assertEquals(LocalDate.of(2026, 1, 1), b.getLastCouponDate());
    }

    @Test
    void disabledBondAdvancesScheduleWithoutCreatingTransactions() {
        BondInvestment b = bond(LocalDate.of(2025, 1, 1), CouponFrequency.ANNUAL, false);

        int created = couponService.recordDueCouponsForBond(b, TODAY);

        assertEquals(0, created);
        verify(transactionRepository, never()).save(any(Transaction.class));
        // Schedule anchor still advances (2026-01-01) so a later re-enable
        // does not flood history.
        assertEquals(LocalDate.of(2026, 1, 1), b.getLastCouponDate());
    }

    @Test
    void incompleteScheduleIsSkipped() {
        BondInvestment missingFirstCoupon = bond(null, CouponFrequency.ANNUAL, true);
        assertEquals(0, couponService.recordDueCouponsForBond(missingFirstCoupon, TODAY));

        BondInvestment missingFrequency = bond(LocalDate.of(2025, 1, 1), null, true);
        assertEquals(0, couponService.recordDueCouponsForBond(missingFrequency, TODAY));

        verify(transactionRepository, never()).save(any(Transaction.class));
    }

    @Test
    void couponAmountMatchesFrequencyDivisor() {
        assertEquals(0, new BigDecimal("4000.00").compareTo(
                scheduleCalculator.couponAmount(new BigDecimal("100000"), new BigDecimal("8.00"),
                        CouponFrequency.SEMI_ANNUAL)));
        assertEquals(0, new BigDecimal("8000.00").compareTo(
                scheduleCalculator.couponAmount(new BigDecimal("100000"), new BigDecimal("8.00"),
                        CouponFrequency.ANNUAL)));
        assertEquals(0, new BigDecimal("666.67").compareTo(
                scheduleCalculator.couponAmount(new BigDecimal("100000"), new BigDecimal("8.00"),
                        CouponFrequency.MONTHLY)));
        assertEquals(0, BigDecimal.ZERO.compareTo(
                scheduleCalculator.couponAmount(null, new BigDecimal("8.00"), CouponFrequency.ANNUAL)));
    }

    @Test
    void quarterlyScheduleStepsEveryThreeMonths() {
        BondInvestment b = bond(LocalDate.of(2026, 3, 1), CouponFrequency.QUARTERLY, true);
        List<LocalDate> due = scheduleCalculator.dueCouponDates(b, LocalDate.of(2026, 9, 24));

        assertEquals(List.of(LocalDate.of(2026, 3, 1), LocalDate.of(2026, 6, 1),
                LocalDate.of(2026, 9, 1)), due);
    }
}
