package com.investment.tracker.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.investment.tracker.dto.PriceData;
import com.investment.tracker.model.PriceSource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * Service for fetching stock prices from Yahoo Finance API
 * Uses unofficial Yahoo Finance query endpoint
 */
@Service
@Slf4j
public class YahooFinanceService {
    
    private static final String YAHOO_FINANCE_URL = "https://query1.finance.yahoo.com/v8/finance/chart/";
    // Yahoo blocks requests without a desktop browser User-Agent (returns 401/429).
    private static final String USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 " +
            "(KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36";
    private static final int MAX_RETRIES = 3;
    private static final long RETRY_DELAY_MS = 1000; // 1 second initial delay
    private static final long CACHE_DURATION_MS = 5 * 60 * 1000; // 5 minutes
    
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final Map<String, CachedPrice> priceCache;
    private final RateLimiter rateLimiter;
    
    public YahooFinanceService() {
        this.restTemplate = new RestTemplate();
        this.objectMapper = new ObjectMapper();
        this.priceCache = new ConcurrentHashMap<>();
        this.rateLimiter = new RateLimiter(2000, TimeUnit.HOURS); // 2000 requests per hour
    }
    
    /**
     * Fetch current price for a symbol
     * 
     * @param symbol Yahoo Finance formatted symbol (e.g., "RELIANCE.NS", "AAPL")
     * @return PriceData with current market data
     */
    public PriceData getPrice(String symbol) {
        if (symbol == null || symbol.trim().isEmpty()) {
            return createErrorPrice(symbol, "Symbol cannot be empty");
        }
        
        // Check cache first
        CachedPrice cached = priceCache.get(symbol);
        if (cached != null && !cached.isExpired()) {
            log.debug("Returning cached price for {}", symbol);
            return cached.priceData;
        }
        
        // Check rate limit
        if (!rateLimiter.tryAcquire()) {
            log.warn("Rate limit exceeded for Yahoo Finance API");
            return createErrorPrice(symbol, "Rate limit exceeded. Please try again later.");
        }
        
        // Fetch with retry logic
        PriceData priceData = fetchWithRetry(symbol);
        
        // Cache the result if valid
        if (priceData.isValid()) {
            priceCache.put(symbol, new CachedPrice(priceData));
            log.debug("Cached price for {}", symbol);
        }
        
        return priceData;
    }
    
    /**
     * Fetch prices for multiple symbols (bulk operation)
     */
    public Map<String, PriceData> getBulkPrices(List<String> symbols) {
        Map<String, PriceData> results = new HashMap<>();
        
        for (String symbol : symbols) {
            try {
                PriceData priceData = getPrice(symbol);
                results.put(symbol, priceData);
                
                // Small delay between requests to avoid hammering the API
                Thread.sleep(100);
            } catch (InterruptedException e) {
                log.error("Interrupted while fetching bulk prices", e);
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                log.error("Error fetching price for {}: {}", symbol, e.getMessage());
                results.put(symbol, createErrorPrice(symbol, e.getMessage()));
            }
        }
        
        return results;
    }
    
    /**
     * Fetch price with exponential backoff retry logic
     */
    private PriceData fetchWithRetry(String symbol) {
        Exception lastException = null;
        
        for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
            try {
                return fetchPrice(symbol);
            } catch (Exception e) {
                lastException = e;
                log.warn("Attempt {}/{} failed for {}: {}", attempt, MAX_RETRIES, symbol, e.getMessage());
                
                if (attempt < MAX_RETRIES) {
                    // Exponential backoff
                    long delay = RETRY_DELAY_MS * (long) Math.pow(2, attempt - 1);
                    try {
                        Thread.sleep(delay);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
        }
        
        // All retries failed
        String errorMsg = lastException != null ? lastException.getMessage() : "Unknown error";
        log.error("Failed to fetch price for {} after {} retries: {}", symbol, MAX_RETRIES, errorMsg);
        return createErrorPrice(symbol, "Failed after " + MAX_RETRIES + " retries: " + errorMsg);
    }
    
    /**
     * Fetch price from Yahoo Finance API
     */
    private PriceData fetchPrice(String symbol) {
        try {
            // Build URL: https://query1.finance.yahoo.com/v8/finance/chart/SYMBOL?interval=1d&range=1d
            String url = YAHOO_FINANCE_URL + symbol + "?interval=1d&range=1d";

            log.debug("Fetching price from Yahoo Finance: {}", url);

            HttpHeaders headers = new HttpHeaders();
            headers.set(HttpHeaders.USER_AGENT, USER_AGENT);
            headers.set(HttpHeaders.ACCEPT, "application/json,text/plain,*/*");
            HttpEntity<Void> entity = new HttpEntity<>(headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    url, HttpMethod.GET, entity, String.class);
            
            if (response.getStatusCode() != HttpStatus.OK) {
                throw new RuntimeException("HTTP " + response.getStatusCode());
            }
            
            String responseBody = response.getBody();
            if (responseBody == null || responseBody.isEmpty()) {
                throw new RuntimeException("Empty response from Yahoo Finance");
            }
            
            // Parse JSON response
            return parseYahooResponse(symbol, responseBody);
            
        } catch (HttpClientErrorException e) {
            if (e.getStatusCode() == HttpStatus.NOT_FOUND) {
                throw new RuntimeException("Symbol not found: " + symbol);
            }
            throw new RuntimeException("HTTP error: " + e.getStatusCode());
        } catch (Exception e) {
            throw new RuntimeException("Error fetching price: " + e.getMessage(), e);
        }
    }
    
    /**
     * Parse Yahoo Finance JSON response
     */
    private PriceData parseYahooResponse(String symbol, String json) {
        try {
            JsonNode root = objectMapper.readTree(json);
            
            // Navigate JSON structure
            JsonNode chart = root.path("chart");
            JsonNode result = chart.path("result");
            
            if (result.isEmpty() || !result.isArray() || result.size() == 0) {
                throw new RuntimeException("No data in Yahoo Finance response");
            }
            
            JsonNode data = result.get(0);
            JsonNode meta = data.path("meta");
            
            // Extract price data
            BigDecimal currentPrice = getBigDecimal(meta, "regularMarketPrice");
            BigDecimal previousClose = getBigDecimal(meta, "previousClose");
            BigDecimal dayHigh = getBigDecimal(meta, "regularMarketDayHigh");
            BigDecimal dayLow = getBigDecimal(meta, "regularMarketDayLow");
            String currency = getString(meta, "currency", "USD");
            
            // Calculate change percentage
            BigDecimal changePercent = null;
            if (currentPrice != null && previousClose != null && previousClose.compareTo(BigDecimal.ZERO) != 0) {
                changePercent = currentPrice.subtract(previousClose)
                    .divide(previousClose, 4, BigDecimal.ROUND_HALF_UP)
                    .multiply(new BigDecimal("100"));
            }
            
            // Extract volume if available
            JsonNode indicators = data.path("indicators").path("quote");
            BigDecimal volume = null;
            if (indicators.isArray() && indicators.size() > 0) {
                JsonNode quote = indicators.get(0);
                JsonNode volumeArray = quote.path("volume");
                if (volumeArray.isArray() && volumeArray.size() > 0) {
                    JsonNode lastVolume = volumeArray.get(volumeArray.size() - 1);
                    if (!lastVolume.isNull()) {
                        volume = new BigDecimal(lastVolume.asLong());
                    }
                }
            }
            
            return PriceData.builder()
                .symbol(symbol)
                .price(currentPrice)
                .currency(currency != null ? currency.toUpperCase() : "USD")
                .changePercent(changePercent)
                .volume(volume)
                .previousClose(previousClose)
                .dayHigh(dayHigh)
                .dayLow(dayLow)
                .timestamp(LocalDateTime.now())
                .source(PriceSource.YAHOO)
                .build();
            
        } catch (Exception e) {
            log.error("Error parsing Yahoo Finance response for {}: {}", symbol, e.getMessage());
            throw new RuntimeException("Failed to parse Yahoo Finance response: " + e.getMessage(), e);
        }
    }
    
    /**
     * Helper method to extract BigDecimal from JSON
     */
    private BigDecimal getBigDecimal(JsonNode node, String fieldName) {
        JsonNode field = node.path(fieldName);
        if (field.isMissingNode() || field.isNull()) {
            return null;
        }
        try {
            return new BigDecimal(field.asText());
        } catch (NumberFormatException e) {
            return null;
        }
    }
    
    /**
     * Helper method to extract String from JSON
     */
    private String getString(JsonNode node, String fieldName, String defaultValue) {
        JsonNode field = node.path(fieldName);
        if (field.isMissingNode() || field.isNull()) {
            return defaultValue;
        }
        return field.asText();
    }
    
    /**
     * Create error PriceData
     */
    private PriceData createErrorPrice(String symbol, String errorMessage) {
        return PriceData.builder()
            .symbol(symbol)
            .timestamp(LocalDateTime.now())
            .source(PriceSource.YAHOO)
            .errorMessage(errorMessage)
            .build();
    }
    
    /**
     * Clear cache (for testing or manual refresh)
     */
    public void clearCache() {
        priceCache.clear();
        log.info("Yahoo Finance price cache cleared");
    }
    
    /**
     * Cached price entry with expiration
     */
    private static class CachedPrice {
        private final PriceData priceData;
        private final long expiryTime;
        
        CachedPrice(PriceData priceData) {
            this.priceData = priceData;
            this.expiryTime = System.currentTimeMillis() + CACHE_DURATION_MS;
        }
        
        boolean isExpired() {
            return System.currentTimeMillis() > expiryTime;
        }
    }
    
    /**
     * Simple rate limiter using token bucket algorithm
     */
    private static class RateLimiter {
        private final int maxRequests;
        private final long windowMs;
        private final Queue<Long> requestTimes;
        
        RateLimiter(int maxRequests, TimeUnit timeUnit) {
            this.maxRequests = maxRequests;
            this.windowMs = timeUnit.toMillis(1);
            this.requestTimes = new LinkedList<>();
        }
        
        synchronized boolean tryAcquire() {
            long now = System.currentTimeMillis();
            
            // Remove old requests outside the time window
            while (!requestTimes.isEmpty() && requestTimes.peek() < now - windowMs) {
                requestTimes.poll();
            }
            
            // Check if we can make a new request
            if (requestTimes.size() < maxRequests) {
                requestTimes.offer(now);
                return true;
            }
            
            return false;
        }
    }
}
