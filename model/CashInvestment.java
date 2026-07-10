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

/**
 * Entity representing Cash holdings and Bank Accounts.
 * Used to track liquid cash, savings accounts, checking accounts, etc.
 */
@Entity
@DiscriminatorValue("CASH")
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class CashInvestment extends Investment {

    @NotBlank(message = "Bank/Institution name is required")
    @Column(name = "bank_name", length = 200)
    private String bankName;

    @Column(name = "account_type", length = 50)
    private String accountType; // SAVINGS, CHECKING, CURRENT, CASH, etc.

    @Column(name = "account_number", length = 100)
    private String accountNumber; // Last 4 digits or masked for security

    @Column(name = "ifsc_code", length = 20)
    private String ifscCode; // For Indian banks

    @Column(name = "routing_number", length = 20)
    private String routingNumber; // For US banks

    @Column(name = "interest_rate", precision = 5, scale = 2)
    private Double interestRate; // Annual interest rate for savings accounts (optional)

    @Column(length = 500)
    private String notes; // Additional notes
}
