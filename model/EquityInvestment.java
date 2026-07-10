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
 * Entity representing equity investments (stocks, shares).
 * Supports both listed (NSE/BSE/NYSE/NASDAQ) and unlisted equities.
 */
@Entity
@DiscriminatorValue("EQUITY")
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class EquityInvestment extends Investment {

    @NotBlank(message = "Symbol is required")
    @Column(length = 50)
    private String symbol; // Stock ticker symbol (e.g., TCS.NS, AAPL)

    @Positive(message = "Quantity must be positive")
    @Column(precision = 15, scale = 4)
    private BigDecimal quantity;

    @Positive(message = "Average price must be positive")
    @Column(name = "purchase_price", precision = 15, scale = 2)
    private BigDecimal avgPrice; // Weighted average purchase price across lots

    @NotBlank(message = "Market is required")
    @Column(length = 20)
    private String market; // NSE, BSE, NYSE, NASDAQ, UNLISTED
}
