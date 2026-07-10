package com.investment.tracker.model;

/**
 * Source of a historical asset valuation.
 */
public enum ValuationSource {
    MARKET_DATA,   // Yahoo Finance / Google Finance historical API
    CALCULATED,    // Computed from interest formulas (FD, PPF, RD, Bond)
    MANUAL,        // Entered manually by the user
    ESTIMATED      // Fallback – purchase price used when no data available
}
