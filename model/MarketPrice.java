package com.investment.tracker.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Entity representing market price data for a security
 */
@Entity
@Table(name = "market_prices", indexes = {
    @Index(name = "idx_symbol_source", columnList = "symbol,source"),
    @Index(name = "idx_symbol_updated", columnList = "symbol,last_updated")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MarketPrice {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    /**
     * Symbol for which this price is stored (can be user symbol or API-specific symbol)
     */
    @NotNull
    @Column(nullable = false, length = 50)
    private String symbol;
    
    /**
     * Current market price
     */
    @NotNull
    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal price;
    
    /**
     * Currency of the price (INR, USD, EUR, etc.)
     */
    @NotNull
    @Column(nullable = false, length = 3)
    private String currency;
    
    /**
     * Source of the price data
     */
    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PriceSource source;
    
    /**
     * Timestamp when price was last updated
     */
    @NotNull
    @Column(name = "last_updated", nullable = false)
    private LocalDateTime lastUpdated;
    
    /**
     * Percentage change from previous close
     */
    @Column(name = "change_percent", precision = 10, scale = 2)
    private BigDecimal changePercent;
    
    /**
     * Trading volume (if available)
     */
    @Column(precision = 20, scale = 0)
    private BigDecimal volume;
    
    /**
     * Previous close price
     */
    @Column(name = "previous_close", precision = 15, scale = 2)
    private BigDecimal previousClose;
    
    /**
     * Day's high price
     */
    @Column(name = "day_high", precision = 15, scale = 2)
    private BigDecimal dayHigh;
    
    /**
     * Day's low price
     */
    @Column(name = "day_low", precision = 15, scale = 2)
    private BigDecimal dayLow;
    
    /**
     * Additional metadata or error messages
     */
    @Column(length = 500)
    private String metadata;
    
    @PrePersist
    @PreUpdate
    protected void onUpdate() {
        if (lastUpdated == null) {
            lastUpdated = LocalDateTime.now();
        }
    }
}
