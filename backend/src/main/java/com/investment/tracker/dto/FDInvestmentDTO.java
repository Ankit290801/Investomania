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
 * DTO for Fixed Deposit investment data.
 */
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class FDInvestmentDTO extends InvestmentDTO {

    @NotBlank(message = "Bank name is required")
    private String bankName;

    @Positive(message = "Principal must be positive")
    private BigDecimal principal;

    @Positive(message = "Interest rate must be positive")
    private BigDecimal interestRate;

    @Positive(message = "Tenure must be positive")
    private Integer tenureMonths;

    @NotNull(message = "Maturity date is required")
    private LocalDate maturityDate;
}
