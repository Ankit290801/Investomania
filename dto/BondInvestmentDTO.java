package com.investment.tracker.dto;

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

    private String creditRating;
}
