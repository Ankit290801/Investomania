package com.investment.tracker.model;

/**
 * Enumeration of market data price sources
 */
public enum PriceSource {
    /**
     * Yahoo Finance API
     */
    YAHOO,
    
    /**
     * Google Finance API or web scraping
     */
    GOOGLE,
    
    /**
     * Manually entered by user
     */
    MANUAL,
    
    /**
     * System calculated (e.g., for bonds, FDs based on maturity)
     */
    CALCULATED
}
