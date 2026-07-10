package com.investment.tracker.controller;

import com.investment.tracker.dto.PriceData;
import com.investment.tracker.model.MarketPrice;
import com.investment.tracker.model.PriceSource;
import com.investment.tracker.model.PriceUpdateResult;
import com.investment.tracker.repository.MarketPriceRepository;
import com.investment.tracker.service.ForexService;
import com.investment.tracker.service.MarketDataAggregatorService;
import com.investment.tracker.service.MarketDataSchedulerService;
import com.investment.tracker.util.SecurityUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * REST controller for market data operations.
 * Handles price fetching, currency conversion, and manual price updates.
 */
@RestController
@RequestMapping("/api/market-data")
public class MarketDataController {

    private static final Logger logger = LoggerFactory.getLogger(MarketDataController.class);
    
    @Autowired
    private MarketDataAggregatorService marketDataAggregatorService;
    
    @Autowired
    private ForexService forexService;
    
    @Autowired
    private MarketPriceRepository marketPriceRepository;
    
    @Autowired
    private MarketDataSchedulerService marketDataSchedulerService;
    
    @Autowired
    private SecurityUtil securityUtil;
    
    /**
     * Get current price for a symbol.
     * 
     * GET /api/market-data/price?symbol=RELIANCE&market=NSE
     */
    @GetMapping("/price")
    public ResponseEntity<PriceData> getPrice(
            @RequestParam @NotBlank String symbol,
            @RequestParam(required = false, defaultValue = "NSE") String market) {
        
        logger.info("API: Get price for symbol={}, market={}", symbol, market);
        
        try {
            PriceData priceData = marketDataAggregatorService.getPrice(symbol, market);
            return ResponseEntity.ok(priceData);
        } catch (Exception e) {
            logger.error("Failed to get price for {}: {}", symbol, e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }
    
    /**
     * Get prices for multiple symbols.
     * 
     * POST /api/market-data/prices/bulk
     * Body: { "RELIANCE": "NSE", "AAPL": "NASDAQ", "BTC-USD": "CRYPTO" }
     */
    @PostMapping("/prices/bulk")
    public ResponseEntity<Map<String, PriceData>> getBulkPrices(
            @RequestBody Map<String, String> symbols) {
        
        logger.info("API: Get bulk prices for {} symbols", symbols.size());
        
        try {
            Map<String, PriceData> prices = marketDataAggregatorService.getBulkPrices(symbols);
            return ResponseEntity.ok(prices);
        } catch (Exception e) {
            logger.error("Failed to get bulk prices: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }
    
    /**
     * Update investment value based on latest market price.
     * 
     * POST /api/market-data/investments/{id}/update-value
     */
    @PostMapping("/investments/{id}/update-value")
    public ResponseEntity<Map<String, Object>> updateInvestmentValue(@PathVariable Long id) {
        logger.info("API: Update value for investment {}", id);
        
        try {
            BigDecimal newValue = marketDataAggregatorService.updateInvestmentValue(id);
            
            Map<String, Object> response = new HashMap<>();
            response.put("investmentId", id);
            response.put("currentValue", newValue);
            response.put("updatedAt", LocalDateTime.now());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Failed to update investment {} value: {}", id, e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }
    
    /**
     * Manually set price for a symbol (for private equities, unlisted stocks, etc.)
     * 
     * POST /api/market-data/price/manual
     * Body: { "symbol": "ABC_PRIVATE", "price": 1500.00, "currency": "INR", "notes": "Series C valuation" }
     */
    @PostMapping("/price/manual")
    public ResponseEntity<MarketPrice> setManualPrice(@RequestBody @Valid ManualPriceRequest request) {
        logger.info("API: Set manual price for symbol={}, price={} {}", 
            request.symbol, request.price, request.currency);
        
        try {
            MarketPrice marketPrice = new MarketPrice();
            marketPrice.setSymbol(request.symbol);
            marketPrice.setPrice(request.price);
            marketPrice.setCurrency(request.currency);
            marketPrice.setSource(PriceSource.MANUAL);
            marketPrice.setChangePercent(BigDecimal.ZERO);
            marketPrice.setLastUpdated(LocalDateTime.now());
            
            MarketPrice saved = marketPriceRepository.save(marketPrice);
            
            return ResponseEntity.ok(saved);
        } catch (Exception e) {
            logger.error("Failed to set manual price: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }
    
    /**
     * Get price history for a symbol.
     * 
     * GET /api/market-data/price/history?symbol=RELIANCE&limit=30
     */
    @GetMapping("/price/history")
    public ResponseEntity<List<MarketPrice>> getPriceHistory(
            @RequestParam @NotBlank String symbol,
            @RequestParam(required = false, defaultValue = "30") int limit) {
        
        logger.info("API: Get price history for symbol={}, limit={}", symbol, limit);
        
        try {
            List<MarketPrice> history = marketPriceRepository.findBySymbolOrderByLastUpdatedDesc(symbol);
            
            // Limit results
            if (history.size() > limit) {
                history = history.subList(0, limit);
            }
            
            return ResponseEntity.ok(history);
        } catch (Exception e) {
            logger.error("Failed to get price history: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }
    
    /**
     * Convert currency amount.
     * 
     * GET /api/market-data/convert?amount=1000&from=USD&to=INR
     */
    @GetMapping("/convert")
    public ResponseEntity<Map<String, Object>> convertCurrency(
            @RequestParam BigDecimal amount,
            @RequestParam @NotBlank String from,
            @RequestParam @NotBlank String to) {
        
        logger.info("API: Convert {} {} to {}", amount, from, to);
        
        try {
            BigDecimal rate = forexService.getExchangeRate(from, to);
            BigDecimal converted = forexService.convert(amount, from, to);
            
            Map<String, Object> response = new HashMap<>();
            response.put("amount", amount);
            response.put("fromCurrency", from);
            response.put("toCurrency", to);
            response.put("rate", rate);
            response.put("convertedAmount", converted);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Failed to convert currency: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }
    
    /**
     * Get exchange rate between two currencies.
     * 
     * GET /api/market-data/exchange-rate?from=USD&to=INR
     */
    @GetMapping("/exchange-rate")
    public ResponseEntity<Map<String, Object>> getExchangeRate(
            @RequestParam @NotBlank String from,
            @RequestParam @NotBlank String to) {
        
        logger.info("API: Get exchange rate from {} to {}", from, to);
        
        try {
            BigDecimal rate = forexService.getExchangeRate(from, to);
            
            Map<String, Object> response = new HashMap<>();
            response.put("fromCurrency", from);
            response.put("toCurrency", to);
            response.put("rate", rate);
            response.put("timestamp", LocalDateTime.now());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Failed to get exchange rate: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }
    
    /**
     * Get supported currencies.
     * 
     * GET /api/market-data/currencies
     */
    @GetMapping("/currencies")
    public ResponseEntity<String[]> getSupportedCurrencies() {
        return ResponseEntity.ok(forexService.getSupportedCurrencies());
    }
    
    /**
     * Get cache statistics.
     * 
     * GET /api/market-data/cache/stats
     */
    @GetMapping("/cache/stats")
    public ResponseEntity<Map<String, Object>> getCacheStatistics() {
        logger.info("API: Get cache statistics");
        
        try {
            Map<String, Object> stats = marketDataAggregatorService.getCacheStatistics();
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            logger.error("Failed to get cache statistics: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }
    
    /**
     * Refresh currency rates for a base currency.
     * 
     * POST /api/market-data/forex/refresh?baseCurrency=USD
     */
    @PostMapping("/forex/refresh")
    public ResponseEntity<Map<String, String>> refreshForexRates(
            @RequestParam(required = false, defaultValue = "USD") String baseCurrency) {
        
        logger.info("API: Refresh forex rates for base currency: {}", baseCurrency);
        
        try {
            forexService.refreshRatesForCurrency(baseCurrency);
            
            Map<String, String> response = new HashMap<>();
            response.put("status", "success");
            response.put("baseCurrency", baseCurrency);
            response.put("message", "Currency rates refreshed successfully");
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Failed to refresh forex rates: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }
    
    /**
     * Manually trigger price update for all listed investments.
     * 
     * POST /api/market-data/update/trigger
     */
    @PostMapping("/update/trigger")
    public ResponseEntity<PriceUpdateResult> triggerPriceUpdate() {
        logger.info("API: Manual price update triggered");
        
        try {
            // Get current user (or use "MANUAL" if not authenticated)
            String triggeredBy;
            try {
                Long userId = securityUtil.getCurrentUserId();
                triggeredBy = "USER_" + userId;
            } catch (Exception e) {
                triggeredBy = "MANUAL";
            }
            
            PriceUpdateResult result = marketDataSchedulerService.triggerManualUpdate(triggeredBy);
            
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            logger.error("Failed to trigger price update: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }
    
    /**
     * Get the most recent price update result.
     * 
     * GET /api/market-data/update/last
     */
    @GetMapping("/update/last")
    public ResponseEntity<PriceUpdateResult> getLastUpdate() {
        logger.info("API: Get last price update result");
        
        try {
            return marketDataSchedulerService.getLastUpdateResult()
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.noContent().build());
        } catch (Exception e) {
            logger.error("Failed to get last update: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }
    
    /**
     * Get price update history.
     * 
     * GET /api/market-data/update/history?limit=10
     */
    @GetMapping("/update/history")
    public ResponseEntity<List<PriceUpdateResult>> getUpdateHistory(
            @RequestParam(required = false, defaultValue = "10") int limit) {
        
        logger.info("API: Get price update history (limit: {})", limit);
        
        try {
            List<PriceUpdateResult> history = marketDataSchedulerService.getUpdateHistory(limit);
            return ResponseEntity.ok(history);
        } catch (Exception e) {
            logger.error("Failed to get update history: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }
    
    /**
     * DTO for manual price setting
     */
    public static class ManualPriceRequest {
        @NotBlank
        public String symbol;
        
        public BigDecimal price;
        
        @NotBlank
        public String currency;
        
        public String notes;
    }
}
