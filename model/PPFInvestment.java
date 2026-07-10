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
import javax.validation.constraints.Positive;
import java.math.BigDecimal;

/**
 * Entity representing Public Provident Fund (PPF) investments.
 */
@Entity
@DiscriminatorValue("PPF")
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class PPFInvestment extends Investment {

    @NotBlank(message = "Account number is required")
    @Column(name = "account_number", length = 50)
    private String accountNumber;

    @NotBlank(message = "Contribution frequency is required")
    @Column(name = "contribution_frequency", length = 20)
    private String contributionFrequency; // MONTHLY, QUARTERLY, YEARLY

    @Positive(message = "Total contributed must be positive")
    @Column(name = "total_contributed", precision = 15, scale = 2)
    private BigDecimal totalContributed;
}
