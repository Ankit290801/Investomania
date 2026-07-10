package com.investment.tracker.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Positive;
import java.math.BigDecimal;

/**
 * DTO for Cryptocurrency investment data.
 */
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class CryptoInvestmentDTO extends InvestmentDTO {

    @NotBlank(message = "Symbol is required")
    private String symbol;

    @Positive(message = "Quantity must be positive")
    private BigDecimal quantity;

    @Positive(message = "Average price must be positive")
    private BigDecimal avgPrice;

    @NotBlank(message = "Exchange is required")
    private String exchange;
}
