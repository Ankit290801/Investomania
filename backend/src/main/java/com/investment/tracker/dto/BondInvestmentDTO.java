package com.investment.tracker.dto;

import com.investment.tracker.model.CouponFrequency;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Positive;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * DTO for bond investment data.
 */
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class BondInvestmentDTO extends InvestmentDTO {

    @NotBlank(message = "Issuer is required")
    private String issuer;

    @Positive(message = "Face value must be positive")
    private BigDecimal faceValue;

    @Positive(message = "Coupon rate must be positive")
    private BigDecimal couponRate;

    @NotNull(message = "Maturity date is required")
    private LocalDate maturityDate;

    /** Number of coupon payments per year; null = no recurring coupons. */
    private CouponFrequency couponFrequency;

    /** First scheduled coupon payment date; the coupon calendar is derived from it. */
    private LocalDate firstCouponDate;

    /** Date the most recent auto-recorded coupon was generated (read-only in the UI). */
    private LocalDate lastCouponDate;

    /** When true, the scheduler auto-generates INTEREST transactions for this bond. */
    private Boolean autoGenerateIncome;

    private String creditRating;
}
