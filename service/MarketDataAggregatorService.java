package com.investment.tracker.service;

import com.investment.tracker.dto.PriceData;
import com.investment.tracker.dto.ResolvedSymbol;
import com.investment.tracker.model.Investment;
import com.investment.tracker.model.Market;
import com.investment.tracker.model.MarketPrice;
import com.investment.tracker.model.PriceSource;
import com.investment.tracker.repository.InvestmentRepository;
import com.investment.tracker.repository.MarketPriceRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Aggregator service that combines multiple price sources with intelligent fallback.
 * Priority: Yahoo Finance → Google Finance → Cached Price
 */
@Service
public class MarketDataAggregatorService {

    private static final Logger logger = LoggerFactory.getLogger(MarketDataAggregatorService.class);
    
    @Autowired
    private YahooFinanceService yahooFinanceService;
    
    @Autowired
    private GoogleFinanceService googleFinanceService;
    
    @Autowired
    private SymbolResolutionService symbolResolutionService;
    
    @Autowired
    private ForexService forexService;
    
    @Autowired
    private MarketPriceRepository marketPriceRepository;
    
    @Autowired
    private InvestmentRepository investmentRepository;
    
    /**
     * Get current price for a symbol with intelligent fallback.
     * Tries: Yahoo Finance → Google Finance → Cached Price
     * 
     * @param userSymbol User's symbol (e.g., "RELIANCE", "AAPL")
     * @param market Market/exchange (e.g., "NSE", "NASDAQ")
     * @return PriceData if successful
     * @throws RuntimeException if all sources fail and no cached price available
     */
    public PriceData getPrice(String userSymbol, String market) {
        logger.info("Fetching price for symbol: {} on market: {}", userSymbol, market);
        
        // Step 1: Resolve symbol to API-specific formats
        // Convert market string to Market enum
        Market marketEnum = Market.fromString(market);
        ResolvedSymbol resolved = symbolResolutionService.resolveSymbol(userSymbol, marketEnum);
        
        // Step 2: Try Yahoo Finance first
        try {
            PriceData yahooPrice = yahooFinanceService.getPrice(resolved.getYahooSymbol());
            if (yahooPrice != null && yahooPrice.isValid()) {
                logger.info("✓ Yahoo Finance success: {} = {} {}", 
                    userSymbol, yahooPrice.getPrice(), yahooPrice.getCurrency());
                
                // Store in database
                storePrice(yahooPrice);
                
                // Mark symbol as verified
                symbolResolutionService.markAsVerified(userSymbol);
                
                return yahooPrice;
            } else if (yahooPrice != null && yahooPrice.getErrorMessage() != null) {
                logger.warn("Yahoo Finance returned error for {}: {}", userSymbol, yahooPrice.getErrorMessage());
            }
        } catch (Exception e) {
            logger.warn("Yahoo Finance failed for {}: {}", userSymbol, e.getMessage());
        }
        
        // Step 3: Try Google Finance as fallback
        try {
            Optional<PriceData> googlePrice = googleFinanceService.getPrice(resolved.getGoogleSymbol());
            if (googlePrice.isPresent() && googlePrice.get().isValid()) {
                PriceData priceData = googlePrice.get();
                logger.info("✓ Google Finance fallback success: {} = {} {}", 
                    userSymbol, priceData.getPrice(), priceData.getCurrency());
                
                // Store in database
                storePrice(priceData);
                
                // Mark symbol as verified
                symbolResolutionService.markAsVerified(userSymbol);
                
                return priceData;
            }
        } catch (Exception e) {
            logger.warn("Google Finance failed for {}: {}", userSymbol, e.getMessage());
        }
        
        // Step 4: Try cached price as last resort
        Optional<MarketPrice> cachedPrice = marketPriceRepository
            .findLatestBySymbol(resolved.getYahooSymbol());
        
        if (cachedPrice.isPresent()) {
            MarketPrice mp = cachedPrice.get();
            logger.warn("⚠ Using cached price for {}: {} {} (updated: {})", 
                userSymbol, mp.getPrice(), mp.getCurrency(), mp.getLastUpdated());
            
            PriceData priceData = new PriceData();
            priceData.setSymbol(userSymbol);
            priceData.setPrice(mp.getPrice());
            priceData.setCurrency(mp.getCurrency());
            priceData.setChangePercent(mp.getChangePercent());
            priceData.setTimestamp(mp.getLastUpdated());
            priceData.setSource(mp.getSource());
            
            return priceData;
        }
        
        // All sources failed
        logger.error("✗ All price sources failed for: {}", userSymbol);
        throw new RuntimeException("Unable to fetch price for " + userSymbol + 
            " from any source (Yahoo, Google, or cache)");
    }
    
    /**
     * Get prices for multiple symbols efficiently.
     * Processes in parallel with delays to respect rate limits.
     * 
     * @param symbols Map of symbol → market
     * @return Map of symbol → PriceData (only successful fetches included)
     */
    public Map<String, PriceData> getBulkPrices(Map<String, String> symbols) {
        Map<String, PriceData> results = new HashMap<>();
        
        logger.info("Fetching bulk prices for {} symbols", symbols.size());
        
        for (Map.Entry<String, String> entry : symbols.entrySet()) {
            String symbol = entry.getKey();
            String market = entry.getValue();
            
            try {
                PriceData price = getPrice(symbol, market);
                results.put(symbol, price);
                
                // Small delay to respect rate limits
                Thread.sleep(500); // 0.5 seconds between requests
                
            } catch (Exception e) {
                logger.warn("Failed to fetch price for {}: {}", symbol, e.getMessage());
                // Continue with other symbols
            }
        }
        
        logger.info("Successfully fetched {} out of {} prices", results.size(), symbols.size());
        return results;
    }
    
    /**
     * Store price in database (MarketPrice table).
     */
    @Transactional
    protected void storePrice(PriceData priceData) {
        if (priceData == null || !priceData.isValid()) {
            // Refuse to persist invalid/error prices; the @NotNull columns on
            // MarketPrice would otherwise throw and mark the surrounding
            // transaction rollback-only.
            return;
        }
        try {
            MarketPrice marketPrice = new MarketPrice();
            marketPrice.setSymbol(priceData.getSymbol());
            marketPrice.setPrice(priceData.getPrice());
            marketPrice.setCurrency(priceData.getCurrency());
            marketPrice.setSource(priceData.getSource());
            marketPrice.setChangePercent(priceData.getChangePercent());
            marketPrice.setVolume(priceData.getVolume());
            marketPrice.setLastUpdated(priceData.getTimestamp());
            
            marketPriceRepository.save(marketPrice);
            
            logger.debug("Stored market price in database: {} = {} {}", 
                priceData.getSymbol(), priceData.getPrice(), priceData.getCurrency());
                
        } catch (Exception e) {
            // Don't fail the whole operation if storage fails
            logger.error("Failed to store price in database: {}", e.getMessage());
        }
    }
    
    /**
     * Update investment's current value based on latest market price.
     * For equity/crypto: currentValue = quantity × current price
     * For others: no automatic update (manual only)
     * 
     * @param investmentId Investment ID to update
     * @return Updated current value
     */
    @Transactional
    public BigDecimal updateInvestmentValue(Long investmentId) {
        Investment investment = investmentRepository.findById(investmentId)
            .orElseThrow(() -> new RuntimeException("Investment not found: " + investmentId));
        
        // Only auto-update for equity and crypto
        if (!investment.getType().name().equals("EQUITY") && 
            !investment.getType().name().equals("CRYPTO")) {
            logger.debug("Skipping auto-update for investment type: {}", investment.getType());
            return investment.getCurrentValue();
        }
        
        try {
            // Get the symbol from investment (depends on type)
            String symbol = extractSymbol(investment);
            String market = extractMarket(investment);
            
            if (symbol == null || symbol.isEmpty()) {
                logger.warn("No symbol found for investment {}", investmentId);
                return investment.getCurrentValue();
            }
            
            // Fetch current price
            PriceData priceData = getPrice(symbol, market);
            
            // Calculate current value based on quantity
            BigDecimal quantity = extractQuantity(investment);
            BigDecimal currentValue = priceData.getPrice().multiply(quantity);
            
            // Convert to investment's currency if different
            if (!priceData.getCurrency().equals(investment.getCurrency())) {
                currentValue = forexService.convert(
                    currentValue, 
                    priceData.getCurrency(), 
                    investment.getCurrency()
                );
            }
            
            // Update investment
            investment.setCurrentValue(currentValue);
            investmentRepository.save(investment);
            
            logger.info("Updated investment {} value: {} {}", 
                investmentId, currentValue, investment.getCurrency());
            
            return currentValue;
            
        } catch (Exception e) {
            logger.error("Failed to update investment {} value: {}", investmentId, e.getMessage());
            return investment.getCurrentValue();
        }
    }
    
    /**
     * Extract symbol from investment based on type.
     */
    private String extractSymbol(Investment investment) {
        // This is a simplified version - in reality, you'd cast to specific types
        // For now, we'll use reflection or switch on type
        switch (investment.getType().name()) {
            case "EQUITY":
                return (String) getField(investment, "symbol");
            case "CRYPTO":
                return (String) getField(investment, "symbol");
            default:
                return null;
        }
    }
    
    /**
     * Extract market from investment based on type.
     */
    private String extractMarket(Investment investment) {
        switch (investment.getType().name()) {
            case "EQUITY":
                String market = (String) getField(investment, "market");
                return market != null ? market : "NSE"; // Default to NSE
            case "CRYPTO":
                return "CRYPTO";
            default:
                return null;
        }
    }
    
    /**
     * Extract quantity from investment.
     */
    private BigDecimal extractQuantity(Investment investment) {
        switch (investment.getType().name()) {
            case "EQUITY":
            case "CRYPTO":
                Object qty = getField(investment, "quantity");
                return qty != null ? (BigDecimal) qty : BigDecimal.ONE;
            default:
                return BigDecimal.ONE;
        }
    }
    
    /**
     * Helper to get field value using reflection.
     */
    private Object getField(Investment investment, String fieldName) {
        try {
            java.lang.reflect.Field field = investment.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            return field.get(investment);
        } catch (Exception e) {
            logger.debug("Failed to get field {}: {}", fieldName, e.getMessage());
            return null;
        }
    }
    
    /**
     * Get statistics about cached prices.
     */
    public Map<String, Object> getCacheStatistics() {
        Map<String, Object> stats = new HashMap<>();
        
        long totalCached = marketPriceRepository.count();
        long yahooCached = marketPriceRepository.countBySource(PriceSource.YAHOO);
        long googleCached = marketPriceRepository.countBySource(PriceSource.GOOGLE);
        long manualCached = marketPriceRepository.countBySource(PriceSource.MANUAL);
        
        stats.put("total", totalCached);
        stats.put("yahoo", yahooCached);
        stats.put("google", googleCached);
        stats.put("manual", manualCached);
        stats.put("googleServiceCache", googleFinanceService.getCacheSize());
        
        return stats;
    }
}
