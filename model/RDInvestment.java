package com.investment.tracker.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import javax.persistence.Column;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Positive;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Entity representing Recurring Deposit investments.
 */
@Entity
@DiscriminatorValue("RD")
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class RDInvestment extends Investment {

    @NotBlank(message = "Bank name is required")
    @Column(name = "bank_name", length = 200)
    private String bankName;

    @Positive(message = "Monthly contribution must be positive")
    @Column(name = "monthly_contribution", precision = 15, scale = 2)
    private BigDecimal monthlyContribution;

    @Positive(message = "Interest rate must be positive")
    @Column(name = "interest_rate", precision = 5, scale = 2)
    private BigDecimal interestRate; // Annual interest rate as percentage

    @Positive(message = "Tenure must be positive")
    @Column(name = "tenure_months")
    private Integer tenureMonths;

    @NotNull(message = "Maturity date is required")
    @Column(name = "maturity_date")
    private LocalDate maturityDate;
}
