package com.investment.tracker.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.investment.tracker.dto.PriceData;
import com.investment.tracker.model.PriceSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Service to fetch stock prices from Google Finance.
 * Note: Google Finance doesn't have an official API, so this implementation
 * uses HTML parsing as a fallback when Yahoo Finance fails.
 */
@Service
public class GoogleFinanceService {

    private static final Logger logger = LoggerFactory.getLogger(GoogleFinanceService.class);
    private static final String GOOGLE_FINANCE_URL = "https://www.google.com/finance/quote/";
    
    // Cache to avoid repeated requests (5 minute expiration)
    private final ConcurrentHashMap<String, CachedPrice> priceCache = new ConcurrentHashMap<>();
    private static final long CACHE_DURATION_MS = 5 * 60 * 1000; // 5 minutes
    
    @Autowired
    private RestTemplate restTemplate;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    /**
     * Cached price data with timestamp
     */
    private static class CachedPrice {
        PriceData data;
        long timestamp;
        
        CachedPrice(PriceData data, long timestamp) {
            this.data = data;
            this.timestamp = timestamp;
        }
        
        boolean isExpired() {
            return System.currentTimeMillis() - timestamp > CACHE_DURATION_MS;
        }
    }
    
    /**
     * Get current price for a symbol from Google Finance.
     * Google Finance format: "EXCHANGE:SYMBOL" (e.g., "NASDAQ:AAPL", "NSE:RELIANCE")
     * 
     * @param googleSymbol Symbol in Google Finance format (EXCHANGE:SYMBOL)
     * @return PriceData if successful, empty Optional if failed
     */
    public Optional<PriceData> getPrice(String googleSymbol) {
        if (googleSymbol == null || googleSymbol.trim().isEmpty()) {
            logger.warn("Google Finance: Empty symbol provided");
            return Optional.empty();
        }
        
        // Check cache first
        CachedPrice cached = priceCache.get(googleSymbol);
        if (cached != null && !cached.isExpired()) {
            logger.debug("Returning cached price for {}", googleSymbol);
            return Optional.of(cached.data);
        }
        
        logger.info("Fetching price from Google Finance for symbol: {}", googleSymbol);
        
        try {
            // Google Finance URL format: https://www.google.com/finance/quote/AAPL:NASDAQ
            String url = buildGoogleFinanceUrl(googleSymbol);

            // Fetch the HTML page with a browser User-Agent (Google blocks the
            // default RestTemplate UA).
            HttpHeaders headers = new HttpHeaders();
            headers.set(HttpHeaders.USER_AGENT,
                    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 "
                    + "(KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36");
            headers.set(HttpHeaders.ACCEPT,
                    "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
            HttpEntity<Void> requestEntity = new HttpEntity<>(headers);
            ResponseEntity<String> response = restTemplate.exchange(
                    url, HttpMethod.GET, requestEntity, String.class);
            
            if (response.getStatusCode() != HttpStatus.OK || response.getBody() == null) {
                logger.warn("Google Finance returned non-OK status for {}: {}", googleSymbol, response.getStatusCode());
                return Optional.empty();
            }
            
            String html = response.getBody();
            
            // Parse price from HTML
            Optional<PriceData> priceData = parseGoogleFinanceHtml(googleSymbol, html);
            
            // Cache the result
            priceData.ifPresent(data -> {
                priceCache.put(googleSymbol, new CachedPrice(data, System.currentTimeMillis()));
                logger.info("Successfully fetched price from Google Finance: {} = {} {}", 
                    googleSymbol, data.getPrice(), data.getCurrency());
            });
            
            return priceData;
            
        } catch (HttpClientErrorException e) {
            if (e.getStatusCode() == HttpStatus.NOT_FOUND) {
                logger.warn("Google Finance: Symbol not found: {}", googleSymbol);
            } else {
                logger.error("Google Finance HTTP error for {}: {} - {}", 
                    googleSymbol, e.getStatusCode(), e.getMessage());
            }
            return Optional.empty();
            
        } catch (RestClientException e) {
            logger.error("Google Finance network error for {}: {}", googleSymbol, e.getMessage());
            return Optional.empty();
            
        } catch (Exception e) {
            logger.error("Google Finance unexpected error for {}: {}", googleSymbol, e.getMessage(), e);
            return Optional.empty();
        }
    }
    
    /**
     * Build Google Finance URL from symbol.
     * Converts "NSE:RELIANCE" to "https://www.google.com/finance/quote/RELIANCE:NSE"
     */
    private String buildGoogleFinanceUrl(String googleSymbol) {
        // Google Finance format is: EXCHANGE:SYMBOL
        // URL format is: /quote/SYMBOL:EXCHANGE
        
        if (googleSymbol.contains(":")) {
            String[] parts = googleSymbol.split(":", 2);
            String exchange = parts[0];
            String symbol = parts[1];
            return GOOGLE_FINANCE_URL + symbol + ":" + exchange;
        }
        
        // If no colon, assume it's just the symbol (US stock)
        return GOOGLE_FINANCE_URL + googleSymbol;
    }
    
    /**
     * Parse price data from Google Finance HTML.
     * This is fragile and may break if Google changes their HTML structure.
     */
    private Optional<PriceData> parseGoogleFinanceHtml(String symbol, String html) {
        try {
            // Pattern to find the price in the HTML
            // Google Finance typically has: <div class="YMlKec fxKbKc">$123.45</div>
            Pattern pricePattern = Pattern.compile("class=\"YMlKec fxKbKc\">([^<]+)</div>");
            Matcher priceMatcher = pricePattern.matcher(html);
            
            if (!priceMatcher.find()) {
                logger.warn("Could not find price pattern in Google Finance HTML for {}", symbol);
                return Optional.empty();
            }
            
            String priceStr = priceMatcher.group(1).trim();
            
            // Extract currency symbol and numeric value
            // Examples: "$123.45", "₹2,456.78", "€100.50"
            String currency = extractCurrency(priceStr);
            String numericValue = priceStr.replaceAll("[^0-9.]", ""); // Remove non-numeric chars
            
            BigDecimal price = new BigDecimal(numericValue);
            
            // Try to find change percentage
            Pattern changePattern = Pattern.compile("class=\"[^\"]*P2Luy[^\"]*\">([^<]+)%</span>");
            Matcher changeMatcher = changePattern.matcher(html);
            BigDecimal changePercent = BigDecimal.ZERO;
            
            if (changeMatcher.find()) {
                String changeStr = changeMatcher.group(1).trim().replace("%", "").replace("+", "");
                try {
                    changePercent = new BigDecimal(changeStr);
                } catch (NumberFormatException e) {
                    logger.debug("Could not parse change percent: {}", changeStr);
                }
            }
            
            PriceData priceData = new PriceData();
            priceData.setSymbol(symbol);
            priceData.setPrice(price);
            priceData.setCurrency(currency);
            priceData.setChangePercent(changePercent);
            priceData.setTimestamp(LocalDateTime.now());
            priceData.setSource(PriceSource.GOOGLE);
            
            return Optional.of(priceData);
            
        } catch (Exception e) {
            logger.error("Error parsing Google Finance HTML for {}: {}", symbol, e.getMessage(), e);
            return Optional.empty();
        }
    }
    
    /**
     * Extract currency code from price string.
     */
    private String extractCurrency(String priceStr) {
        if (priceStr.startsWith("$")) return "USD";
        if (priceStr.startsWith("₹")) return "INR";
        if (priceStr.startsWith("€")) return "EUR";
        if (priceStr.startsWith("£")) return "GBP";
        if (priceStr.startsWith("¥")) return "JPY";
        if (priceStr.startsWith("C$")) return "CAD";
        if (priceStr.startsWith("A$")) return "AUD";
        
        // Default to USD if unknown
        return "USD";
    }
    
    /**
     * Clear the price cache (useful for testing or forced refresh)
     */
    public void clearCache() {
        priceCache.clear();
        logger.info("Google Finance cache cleared");
    }
    
    /**
     * Get cache size (for monitoring)
     */
    public int getCacheSize() {
        // Remove expired entries
        priceCache.entrySet().removeIf(entry -> entry.getValue().isExpired());
        return priceCache.size();
    }
}
