package com.investment.tracker.dto;

import com.investment.tracker.model.Market;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO representing a resolved symbol with API-specific formats
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResolvedSymbol {
    
    /**
     * User's input symbol
     */
    private String userSymbol;
    
    /**
     * Yahoo Finance formatted symbol
     */
    private String yahooSymbol;
    
    /**
     * Google Finance formatted symbol
     */
    private String googleSymbol;
    
    /**
     * Market/exchange
     */
    private Market market;
    
    /**
     * Confidence level of the resolution
     */
    private ConfidenceLevel confidence;
    
    /**
     * Whether this mapping has been verified with actual API calls
     */
    private boolean verified;
    
    /**
     * Company/security name (if known)
     */
    private String name;
    
    /**
     * Source of the resolution (CACHED, AUTO_RESOLVED, USER_PROVIDED)
     */
    private ResolutionSource source;
    
    public enum ConfidenceLevel {
        /**
         * High confidence - mapping exists in cache and is verified
         */
        HIGH,
        
        /**
         * Medium confidence - mapping auto-generated but not verified
         */
        MEDIUM,
        
        /**
         * Low confidence - fallback or uncertain mapping
         */
        LOW
    }
    
    public enum ResolutionSource {
        /**
         * Found in cached symbol mappings
         */
        CACHED,
        
        /**
         * Auto-resolved using market rules
         */
        AUTO_RESOLVED,
        
        /**
         * Provided by user input
         */
        USER_PROVIDED
    }
}
