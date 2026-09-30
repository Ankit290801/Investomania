package com.investment.tracker.service;

import com.investment.tracker.model.BondInvestment;
import com.investment.tracker.model.Transaction;
import com.investment.tracker.model.TransactionType;
import com.investment.tracker.repository.BondInvestmentRepository;
import com.investment.tracker.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Coupon schedule engine for bond investments (backlog feature F3 /
 * Enhancement 2.1 — Automated Coupon Transaction Generation).
 *
 * <p>Generates {@link TransactionType#INTEREST} transactions for bond coupon
 * payments on their scheduled dates: a daily scheduled job records payments
 * that fall due, and a startup catch-up backfills anything missed while the
 * application was down. Generation is idempotent — a (bond, date) coupon is
 * recorded at most once, verified against existing transactions before insert.</p>
 *
 * <p>Pure date/amount arithmetic lives in {@link CouponScheduleCalculator};
 * this class owns persistence and orchestration only (modularity doctrine).</p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class BondCouponService {

    private final BondInvestmentRepository bondInvestmentRepository;
    private final TransactionRepository transactionRepository;
    private final CouponScheduleCalculator scheduleCalculator;

    /**
     * Records every due coupon for all coupon-bearing bonds.
     * Called by the daily scheduler and at startup (catch-up).
     *
     * @return number of coupon transactions created
     */
    @Transactional
    public int recordDueCoupons() {
        return recordDueCoupons(LocalDate.now());
    }

    /**
     * Records every coupon due on or before {@code today} for all
     * coupon-bearing bonds. Idempotent: existing INTEREST transactions on a
     * scheduled date suppress regeneration.
     *
     * @return number of coupon transactions created
     */
    @Transactional
    public int recordDueCoupons(LocalDate today) {
        List<BondInvestment> bonds = bondInvestmentRepository.findAll();
        int created = 0;
        for (BondInvestment bond : bonds) {
            created += recordDueCouponsForBond(bond, today);
        }
        if (created > 0) {
            log.info("Coupon auto-recording: {} coupon transaction(s) created.", created);
        }
        return created;
    }

    /**
     * Records due coupons for a single bond and updates its
     * {@code lastCouponDate}. Silently skips bonds without a complete coupon
     * schedule (no frequency, no first coupon date) or with auto-generation
     * disabled.
     *
     * @return number of coupon transactions created for this bond
     */
    @Transactional
    public int recordDueCouponsForBond(BondInvestment bond, LocalDate today) {
        List<LocalDate> dueDates = scheduleCalculator.dueCouponDates(bond, today);
        if (dueDates.isEmpty()) {
            return 0;
        }

        if (!Boolean.TRUE.equals(bond.getAutoGenerateIncome())) {
            // Auto-generation opted out: still advance the schedule anchor so a
            // later re-enable does not flood history with missed payments.
            bond.setLastCouponDate(dueDates.get(dueDates.size() - 1));
            bondInvestmentRepository.save(bond);
            log.debug("Coupon generation disabled for bond {}; schedule advanced to {}.",
                    bond.getId(), bond.getLastCouponDate());
            return 0;
        }

        Set<LocalDate> existingDates =
                new HashSet<>(scheduleCalculator.existingCouponDates(bond.getId(), dueDates));
        int created = 0;
        for (LocalDate dueDate : dueDates) {
            if (existingDates.contains(dueDate)) {
                continue;
            }
            transactionRepository.save(buildCouponTransaction(bond, dueDate));
            created++;
            log.info("Auto-recorded coupon of {} {} for bond {} ({}) due {}.",
                    couponAmount(bond), bond.getCurrency(), bond.getId(), bond.getName(), dueDate);
        }

        if (created > 0 || bond.getLastCouponDate() == null
                || dueDates.get(dueDates.size() - 1).isAfter(bond.getLastCouponDate())) {
            bond.setLastCouponDate(dueDates.get(dueDates.size() - 1));
            bondInvestmentRepository.save(bond);
        }
        return created;
    }

    /**
     * Builds the INTEREST transaction representing one coupon payment.
     */
    private Transaction buildCouponTransaction(BondInvestment bond, LocalDate dueDate) {
        return Transaction.builder()
                .investmentId(bond.getId())
                .type(TransactionType.INTEREST)
                .amount(couponAmount(bond))
                .transactionDate(dueDate)
                .currency(bond.getCurrency())
                .notes("Auto-generated coupon payment")
                .build();
    }

    private BigDecimal couponAmount(BondInvestment bond) {
        return scheduleCalculator.couponAmount(
                bond.getFaceValue(), bond.getCouponRate(), bond.getCouponFrequency());
    }
}
