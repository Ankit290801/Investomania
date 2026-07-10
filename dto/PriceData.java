package com.investment.tracker.dto;

import com.investment.tracker.model.PriceSource;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DTO representing market price data from external APIs
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PriceData {
    
    /**
     * Symbol for which price is fetched
     */
    private String symbol;
    
    /**
     * Current market price
     */
    private BigDecimal price;
    
    /**
     * Currency of the price
     */
    private String currency;
    
    /**
     * Percentage change from previous close
     */
    private BigDecimal changePercent;
    
    /**
     * Trading volume
     */
    private BigDecimal volume;
    
    /**
     * Previous close price
     */
    private BigDecimal previousClose;
    
    /**
     * Day's high price
     */
    private BigDecimal dayHigh;
    
    /**
     * Day's low price
     */
    private BigDecimal dayLow;
    
    /**
     * Timestamp when price was fetched
     */
    private LocalDateTime timestamp;
    
    /**
     * Source of the price data
     */
    private PriceSource source;
    
    /**
     * Error message if fetch failed
     */
    private String errorMessage;
    
    /**
     * Check if this price data is valid
     */
    public boolean isValid() {
        return price != null && price.compareTo(BigDecimal.ZERO) > 0 && errorMessage == null;
    }
}
