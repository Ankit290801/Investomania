package com.investment.tracker.service;

import com.investment.tracker.config.CouponRules;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.core.annotation.Order;

/**
 * Orchestrates the automatic recording of bond coupon payments
 * (backlog feature F3 / Enhancement 2.1):
 *
 * <ul>
 *   <li>a daily job generates coupons due that day (and any earlier missed
 *       dates, covering app downtime), and</li>
 *   <li>a startup catch-up backfills payments missed since the last run,
 *       mirroring the {@code PortfolioBackfillRunner} pattern.</li>
 * </ul>
 *
 * All business logic lives in {@link BondCouponService}; this class only
 * wires it to the clock (modularity doctrine).
 */
@Component
@RequiredArgsConstructor
@Slf4j
@Order(30) // After DataInitializer and PortfolioBackfillRunner
public class BondCouponScheduler implements CommandLineRunner {

    private final BondCouponService bondCouponService;
    private final CouponRules couponRules;

    /**
     * Daily coupon run. Cron comes from {@code investment-rules.yml}
     * (bond-coupons.cron); no literal schedule in code.
     */
    @Scheduled(cron = "#{@couponRules.cron}")
    public void scheduledCouponRun() {
        if (!couponRules.isEnabled()) {
            log.debug("Coupon auto-recording disabled by config; skipping scheduled run.");
            return;
        }
        try {
            int created = bondCouponService.recordDueCoupons();
            if (created > 0) {
                log.info("Scheduled coupon run recorded {} coupon payment(s).", created);
            }
        } catch (Exception e) {
            log.error("Scheduled coupon run failed: {}", e.getMessage(), e);
        }
    }

    /**
     * Startup catch-up: generates coupons missed while the app was down.
     * Idempotent (BondCouponService skips already-recorded dates), so running
     * it on every startup is safe.
     */
    @Override
    public void run(String... args) {
        if (!couponRules.isEnabled()) {
            log.info("Coupon auto-recording disabled by config; skipping startup catch-up.");
            return;
        }
        try {
            int created = bondCouponService.recordDueCoupons();
            if (created > 0) {
                log.info("Startup coupon catch-up recorded {} missed coupon payment(s).", created);
            }
        } catch (Exception e) {
            log.error("Startup coupon catch-up failed: {}", e.getMessage(), e);
        }
    }
}
