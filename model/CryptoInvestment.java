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
 * Entity representing cryptocurrency investments.
 */
@Entity
@DiscriminatorValue("CRYPTO")
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class CryptoInvestment extends Investment {

    @NotBlank(message = "Symbol is required")
    @Column(length = 20)
    private String symbol; // BTC, ETH, etc.

    @Positive(message = "Quantity must be positive")
    @Column(precision = 20, scale = 8)
    private BigDecimal quantity; // Higher precision for crypto

    @Positive(message = "Average price must be positive")
    @Column(name = "purchase_price", precision = 15, scale = 2)
    private BigDecimal avgPrice;

    @NotBlank(message = "Exchange is required")
    @Column(length = 100)
    private String exchange; // Binance, Coinbase, WazirX, etc.
}
