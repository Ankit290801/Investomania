package com.investment.tracker.model;

/**
 * Coupon payment frequency for bond investments.
 * Number of coupon payments per year.
 */
public enum CouponFrequency {
    MONTHLY(12),
    QUARTERLY(4),
    SEMI_ANNUAL(2),
    ANNUAL(1);

    private final int paymentsPerYear;

    CouponFrequency(int paymentsPerYear) {
        this.paymentsPerYear = paymentsPerYear;
    }

    public int getPaymentsPerYear() {
        return paymentsPerYear;
    }
}
