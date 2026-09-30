package com.investment.tracker.service;

import com.investment.tracker.model.BondInvestment;
import com.investment.tracker.model.CouponFrequency;
import com.investment.tracker.model.TransactionType;
import com.investment.tracker.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Pure date/amount arithmetic for bond coupon schedules — no persistence,
 * no Spring scheduling (modularity doctrine: single responsibility).
 *
 * <p>The coupon calendar of a bond is fully derived from its
 * {@code firstCouponDate} and payment frequency: payment n falls on
 * {@code firstCouponDate + n × (12 / paymentsPerYear) months}, capped at
 * maturity. A bond's {@code lastCouponDate} (or its purchase date when no
 * coupon has been recorded yet) anchors where catch-up generation resumes.</p>
 */
@Component
@RequiredArgsConstructor
public class CouponScheduleCalculator {

    private final TransactionRepository transactionRepository;

    /**
     * All scheduled coupon dates on or before {@code today} that have not yet
     * been anchored by the bond's {@code lastCouponDate}. Callers must check
     * existing transactions for idempotency (see
     * {@link BondCouponService#recordDueCouponsForBond}).
     */
    public List<LocalDate> dueCouponDates(BondInvestment bond, LocalDate today) {
        CouponFrequency frequency = bond.getCouponFrequency();
        LocalDate firstCouponDate = bond.getFirstCouponDate();
        if (frequency == null || firstCouponDate == null
                || bond.getMaturityDate() == null
                || bond.getFaceValue() == null
                || bond.getCouponRate() == null) {
            return List.of(); // Incomplete coupon schedule — nothing to generate.
        }

        LocalDate maturityDate = bond.getMaturityDate();
        LocalDate firstDue = firstCouponDate;
        if (bond.getLastCouponDate() != null
                && !bond.getLastCouponDate().isBefore(firstCouponDate)) {
            // Resume strictly after the last recorded coupon (which may itself
            // be the first coupon date).
            firstDue = nextCouponDate(bond.getLastCouponDate(), frequency);
        }
        if (firstDue.isBefore(firstCouponDate)) {
            firstDue = firstCouponDate;
        }

        List<LocalDate> dueDates = new java.util.ArrayList<>();
        LocalDate cursor = firstDue;
        int stepMonths = 12 / frequency.getPaymentsPerYear();
        int safety = 0;
        while (!cursor.isAfter(today) && !cursor.isAfter(maturityDate) && safety < 1200) {
            dueDates.add(cursor);
            cursor = cursor.plusMonths(stepMonths);
            safety++;
        }
        return dueDates;
    }

    /**
     * The next coupon date strictly after {@code afterDate} for the given
     * frequency, derived by stepping whole payment periods.
     */
    public LocalDate nextCouponDate(LocalDate afterDate, CouponFrequency frequency) {
        int stepMonths = Math.max(1, 12 / frequency.getPaymentsPerYear());
        return afterDate.plusMonths(stepMonths);
    }

    /**
     * Coupon amount per payment:
     * {@code faceValue × couponRate / 100 / paymentsPerYear}, rounded to 2 dp.
     */
    public BigDecimal couponAmount(BigDecimal faceValue, BigDecimal couponRate,
                                   CouponFrequency frequency) {
        if (faceValue == null || couponRate == null || frequency == null) {
            return BigDecimal.ZERO;
        }
        BigDecimal annualCoupon = faceValue
                .multiply(couponRate.divide(BigDecimal.valueOf(100), 10, RoundingMode.HALF_UP));
        return annualCoupon
                .divide(BigDecimal.valueOf(frequency.getPaymentsPerYear()), 2, RoundingMode.HALF_UP);
    }

    /**
     * Existing INTEREST transaction dates for the bond among the given
     * candidate dates — the idempotency guard. A manually entered coupon on
     * the same date suppresses auto-generation for that date (Enhancement 2.1
     * duplicate-transaction mitigation).
     */
    public List<LocalDate> existingCouponDates(Long bondId, List<LocalDate> candidateDates) {
        if (candidateDates == null || candidateDates.isEmpty()) {
            return List.of();
        }
        return transactionRepository
                .findByInvestmentIdAndTransactionDateLessThanEqualOrderByTransactionDateAsc(
                        bondId, candidateDates.get(candidateDates.size() - 1))
                .stream()
                .filter(t -> t.getType() == TransactionType.INTEREST)
                .map(t -> t.getTransactionDate())
                .filter(candidateDates::contains)
                .collect(Collectors.toList());
    }
}
