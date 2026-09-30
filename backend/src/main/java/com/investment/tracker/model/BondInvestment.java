package com.investment.tracker.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import javax.persistence.Column;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Positive;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Entity representing bond investments (government bonds, corporate bonds, debentures).
 */
@Entity
@DiscriminatorValue("BOND")
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class BondInvestment extends Investment {

    @NotBlank(message = "Issuer is required")
    @Column(length = 200)
    private String issuer; // Government, company name

    @Positive(message = "Face value must be positive")
    @Column(name = "face_value", precision = 15, scale = 2)
    private BigDecimal faceValue;

    @Positive(message = "Coupon rate must be positive")
    @Column(name = "coupon_rate", precision = 5, scale = 2)
    private BigDecimal couponRate; // Annual interest rate as percentage

    @NotNull(message = "Maturity date is required")
    @Column(name = "maturity_date")
    private LocalDate maturityDate;

    /**
     * Number of coupon payments per year. Drives the auto-generation
     * schedule: payment dates are {@code firstCouponDate + n / paymentsPerYear}
     * up to maturity. Null means the bond pays no recurring coupons.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "coupon_frequency", length = 20)
    private CouponFrequency couponFrequency;

    /**
     * First scheduled coupon payment date. The whole coupon calendar
     * (and therefore every subsequent payment) is derived from it.
     * Required when {@code couponFrequency} is set.
     */
    @Column(name = "first_coupon_date")
    private LocalDate firstCouponDate;

    /**
     * Date the most recent coupon was recorded (set by the auto-generation
     * job). Null until the first coupon is generated.
     */
    @Column(name = "last_coupon_date")
    private LocalDate lastCouponDate;

    /**
     * When true, the coupon scheduler generates INTEREST transactions for
     * this bond automatically. Users can opt out per bond.
     *
     * The DB default is required: {@code ddl-auto: update} must add this
     * column to a pre-populated table, and a bare {@code NOT NULL} ADD COLUMN
     * fails on existing rows. With the default, H2 backfills TRUE and the
     * constraint holds (existing bonds keep auto-generation on, matching the
     * F3 spec default).
     */
    @Column(name = "auto_generate_income", nullable = false, columnDefinition = "boolean default true not null")
    @Builder.Default
    private Boolean autoGenerateIncome = Boolean.TRUE;

    @Column(name = "credit_rating", length = 10)
    private String creditRating; // AAA, AA+, etc.
}
