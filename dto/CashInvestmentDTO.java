package com.investment.tracker.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import javax.validation.constraints.NotBlank;

/**
 * DTO for cash/bank account investment data.
 */
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class CashInvestmentDTO extends InvestmentDTO {

    @NotBlank(message = "Bank name is required")
    private String bankName;

    private String accountType;

    private String accountNumber;

    private String ifscCode;

    private String routingNumber;

    private Double interestRate;

    private String notes;
}
