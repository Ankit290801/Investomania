package com.investment.tracker.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.investment.tracker.model.CurrencyRate;
import com.investment.tracker.repository.CurrencyRateRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Service for currency conversion using ExchangeRate-API.io
 * Free tier: 1500 requests/month
 * We cache rates for 24 hours to minimize API calls.
 */
@Service
public class ForexService {

    private static final Logger logger = LoggerFactory.getLogger(ForexService.class);
    
    // ExchangeRate-API.io endpoint (no API key required for basic usage)
    private static final String EXCHANGE_RATE_API_URL = "https://api.exchangerate-api.com/v4/latest/";
    
    // Cache duration: 24 hours
    private static final int CACHE_HOURS = 24;
    
    @Autowired
    private CurrencyRateRepository currencyRateRepository;
    
    @Autowired
    private RestTemplate restTemplate;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    /**
     * Convert amount from one currency to another.
     * Uses cached rates if available (within 24 hours), otherwise fetches from API.
     * 
     * @param amount Amount to convert
     * @param fromCurrency Source currency (e.g., "USD")
     * @param toCurrency Target currency (e.g., "INR")
     * @return Converted amount
     */
    public BigDecimal convert(BigDecimal amount, String fromCurrency, String toCurrency) {
        if (amount == null || fromCurrency == null || toCurrency == null) {
            throw new IllegalArgumentException("Amount and currencies cannot be null");
        }
        
        // Same currency - no conversion needed
        if (fromCurrency.equalsIgnoreCase(toCurrency)) {
            return amount;
        }
        
        // Get exchange rate
        BigDecimal rate = getExchangeRate(fromCurrency, toCurrency);
        
        // Convert
        BigDecimal converted = amount.multiply(rate).setScale(2, RoundingMode.HALF_UP);
        
        logger.debug("Converted {} {} to {} {} (rate: {})", 
            amount, fromCurrency, converted, toCurrency, rate);
        
        return converted;
    }
    
    /**
     * Get exchange rate from fromCurrency to toCurrency.
     * Tries cache first, then API if needed.
     * 
     * @param fromCurrency Source currency
     * @param toCurrency Target currency
     * @return Exchange rate
     */
    public BigDecimal getExchangeRate(String fromCurrency, String toCurrency) {
        // Check cache first
        Optional<CurrencyRate> cachedRate = currencyRateRepository
            .findValidRate(fromCurrency, toCurrency, LocalDateTime.now().minusHours(CACHE_HOURS));
        
        if (cachedRate.isPresent()) {
            logger.debug("Using cached exchange rate: {} -> {} = {}", 
                fromCurrency, toCurrency, cachedRate.get().getRate());
            return cachedRate.get().getRate();
        }
        
        // Not in cache or expired - fetch from API
        logger.info("Fetching exchange rate from API: {} -> {}", fromCurrency, toCurrency);
        
        try {
            BigDecimal rate = fetchExchangeRateFromApi(fromCurrency, toCurrency);
            
            // Store in cache
            CurrencyRate currencyRate = new CurrencyRate();
            currencyRate.setFromCurrency(fromCurrency);
            currencyRate.setToCurrency(toCurrency);
            currencyRate.setRate(rate);
            currencyRate.setSource("EXCHANGERATE_API");
            currencyRate.setLastUpdated(LocalDateTime.now());
            currencyRateRepository.save(currencyRate);
            
            logger.info("Cached exchange rate: {} -> {} = {}", fromCurrency, toCurrency, rate);
            
            return rate;
            
        } catch (Exception e) {
            logger.error("Failed to fetch exchange rate from API: {} -> {}", fromCurrency, toCurrency, e);
            
            // Try to use any cached rate (even if expired) as fallback
            Optional<CurrencyRate> anyRate = currencyRateRepository
                .findByFromCurrencyAndToCurrency(fromCurrency, toCurrency);
            
            if (anyRate.isPresent()) {
                logger.warn("Using expired cached rate as fallback: {} -> {} = {}", 
                    fromCurrency, toCurrency, anyRate.get().getRate());
                return anyRate.get().getRate();
            }
            
            // Last resort: return 1.0 to avoid breaking calculations
            logger.error("No exchange rate available, returning 1.0");
            return BigDecimal.ONE;
        }
    }
    
    /**
     * Fetch exchange rate from ExchangeRate-API.io
     */
    private BigDecimal fetchExchangeRateFromApi(String fromCurrency, String toCurrency) {
        String url = EXCHANGE_RATE_API_URL + fromCurrency;
        
        try {
            String response = restTemplate.getForObject(url, String.class);
            
            if (response == null) {
                throw new RuntimeException("Empty response from ExchangeRate API");
            }
            
            // Parse JSON response
            JsonNode root = objectMapper.readTree(response);
            JsonNode ratesNode = root.get("rates");
            
            if (ratesNode == null || !ratesNode.has(toCurrency)) {
                throw new RuntimeException("Currency " + toCurrency + " not found in API response");
            }
            
            double rateValue = ratesNode.get(toCurrency).asDouble();
            
            return BigDecimal.valueOf(rateValue).setScale(6, RoundingMode.HALF_UP);
            
        } catch (RestClientException e) {
            throw new RuntimeException("Network error fetching exchange rate: " + e.getMessage(), e);
        } catch (Exception e) {
            throw new RuntimeException("Error parsing exchange rate response: " + e.getMessage(), e);
        }
    }
    
    /**
     * Fetch and cache all common exchange rates.
     * Useful for pre-warming the cache during startup or scheduled refresh.
     * 
     * @param baseCurrency Base currency to fetch rates for (e.g., "USD")
     */
    public void refreshRatesForCurrency(String baseCurrency) {
        logger.info("Refreshing exchange rates for base currency: {}", baseCurrency);
        
        String[] commonCurrencies = {"USD", "EUR", "GBP", "INR", "JPY", "AUD", "CAD", "CHF", "CNY"};
        
        for (String targetCurrency : commonCurrencies) {
            if (!baseCurrency.equals(targetCurrency)) {
                try {
                    getExchangeRate(baseCurrency, targetCurrency);
                    // Small delay to avoid rate limiting
                    Thread.sleep(100);
                } catch (Exception e) {
                    logger.warn("Failed to refresh rate {} -> {}: {}", 
                        baseCurrency, targetCurrency, e.getMessage());
                }
            }
        }
        
        logger.info("Completed refreshing exchange rates for {}", baseCurrency);
    }
    
    /**
     * Get all supported currencies from the API.
     * This is useful for populating dropdowns in the UI.
     */
    public String[] getSupportedCurrencies() {
        // Common currencies we support
        return new String[]{
            "USD", "EUR", "GBP", "INR", "JPY", 
            "AUD", "CAD", "CHF", "CNY", "HKD",
            "SGD", "NZD", "SEK", "NOK", "DKK",
            "MXN", "BRL", "ZAR", "KRW", "RUB"
        };
    }
    
    /**
     * Clear old cached rates (older than specified days)
     */
    public void clearOldRates(int olderThanDays) {
        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(olderThanDays);
        int deleted = currencyRateRepository.deleteOldRates(cutoffDate);
        logger.info("Cleared {} old currency rates (older than {} days)", deleted, olderThanDays);
    }
}
