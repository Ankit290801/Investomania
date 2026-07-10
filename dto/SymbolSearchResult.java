package com.investment.tracker.dto;

import com.investment.tracker.model.InvestmentType;
import com.investment.tracker.model.Market;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO representing a symbol search result
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SymbolSearchResult {
    
    /**
     * Symbol
     */
    private String symbol;
    
    /**
     * Company/security name
     */
    private String name;
    
    /**
     * Market/exchange
     */
    private Market market;
    
    /**
     * Asset type
     */
    private InvestmentType assetType;
    
    /**
     * Whether mapping is verified
     */
    private boolean verified;
    
    /**
     * ISIN code (if available)
     */
    private String isin;
}
