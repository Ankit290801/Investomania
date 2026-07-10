package com.investment.tracker.model;

/**
 * Enumeration of supported stock markets/exchanges
 */
public enum Market {
    /**
     * National Stock Exchange of India
     */
    NSE("NSE", ".NS", "NSE:"),
    
    /**
     * Bombay Stock Exchange
     */
    BSE("BSE", ".BO", "BOM:"),
    
    /**
     * New York Stock Exchange
     */
    NYSE("NYSE", "", "NYSE:"),
    
    /**
     * NASDAQ Stock Exchange
     */
    NASDAQ("NASDAQ", "", "NASDAQ:"),
    
    /**
     * Cryptocurrency
     */
    CRYPTO("CRYPTO", "", "CURRENCY:"),
    
    /**
     * Other/Unknown market
     */
    OTHER("OTHER", "", "");
    
    private final String code;
    private final String yahooSuffix;
    private final String googlePrefix;
    
    Market(String code, String yahooSuffix, String googlePrefix) {
        this.code = code;
        this.yahooSuffix = yahooSuffix;
        this.googlePrefix = googlePrefix;
    }
    
    public String getCode() {
        return code;
    }
    
    public String getYahooSuffix() {
        return yahooSuffix;
    }
    
    public String getGooglePrefix() {
        return googlePrefix;
    }
    
    /**
     * Apply Yahoo Finance symbol formatting
     * @param baseSymbol The base symbol (e.g., "RELIANCE")
     * @return Formatted symbol for Yahoo Finance (e.g., "RELIANCE.NS")
     */
    public String formatForYahoo(String baseSymbol) {
        return baseSymbol + yahooSuffix;
    }
    
    /**
     * Apply Google Finance symbol formatting
     * @param baseSymbol The base symbol (e.g., "RELIANCE")
     * @return Formatted symbol for Google Finance (e.g., "NSE:RELIANCE")
     */
    public String formatForGoogle(String baseSymbol) {
        return googlePrefix + baseSymbol;
    }
    
    /**
     * Convert string to Market enum
     * @param marketStr Market name (e.g., "NSE", "NASDAQ")
     * @return Corresponding Market enum value
     */
    public static Market fromString(String marketStr) {
        if (marketStr == null || marketStr.trim().isEmpty()) {
            return OTHER;
        }
        
        String normalized = marketStr.trim().toUpperCase();
        
        try {
            return Market.valueOf(normalized);
        } catch (IllegalArgumentException e) {
            // Return OTHER if not found
            return OTHER;
        }
    }
}
