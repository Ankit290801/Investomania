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
 * Entity representing currency exchange rates
 * Cached for 24 hours to reduce API calls
 */
@Entity
@Table(name = "currency_rates",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_currency_pair", columnNames = {"from_currency", "to_currency"})
    },
    indexes = {
        @Index(name = "idx_from_currency", columnList = "from_currency"),
        @Index(name = "idx_last_updated", columnList = "last_updated")
    })
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CurrencyRate {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    /**
     * Source currency code (e.g., "USD", "INR")
     */
    @NotNull
    @Column(name = "from_currency", nullable = false, length = 3)
    private String fromCurrency;
    
    /**
     * Target currency code (e.g., "INR", "EUR")
     */
    @NotNull
    @Column(name = "to_currency", nullable = false, length = 3)
    private String toCurrency;
    
    /**
     * Exchange rate (how many units of toCurrency per unit of fromCurrency)
     * Example: If fromCurrency=USD, toCurrency=INR, rate=83.25 means 1 USD = 83.25 INR
     */
    @NotNull
    @Column(nullable = false, precision = 15, scale = 6)
    private BigDecimal rate;
    
    /**
     * Source of the exchange rate (API name)
     */
    @NotNull
    @Column(nullable = false, length = 50)
    private String source;
    
    /**
     * Timestamp when this rate was last updated
     */
    @NotNull
    @Column(name = "last_updated", nullable = false)
    private LocalDateTime lastUpdated;
    
    /**
     * Timestamp when this rate was created
     */
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (lastUpdated == null) {
            lastUpdated = LocalDateTime.now();
        }
    }
    
    @PreUpdate
    protected void onUpdate() {
        lastUpdated = LocalDateTime.now();
    }
    
    /**
     * Check if this rate is still valid (less than 24 hours old)
     */
    public boolean isValid() {
        return lastUpdated != null && 
               lastUpdated.isAfter(LocalDateTime.now().minusHours(24));
    }
    
    /**
     * Check if this rate needs refresh
     */
    public boolean needsRefresh() {
        return !isValid();
    }
}
