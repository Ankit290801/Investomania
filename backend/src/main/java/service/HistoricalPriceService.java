package com.investment.tracker.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Fetches historical market prices (closing price) from Yahoo Finance.
 * Uses the /v8/finance/chart endpoint with period1 / period2 epoch params.
 *
 * Cache TTL: 30 days (historical prices do not change).
 */
@Service
@Slf4j
public class HistoricalPriceService {

    private static final String YAHOO_HISTORY_URL =
            "https://query1.finance.yahoo.com/v8/finance/chart/%s?period1=%d&period2=%d&interval=1d&events=history";

    /** Yahoo blocks default Java/HttpClient UA; use a real browser UA. */
    private static final String USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 " +
            "(KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36";

    /** 30-day TTL for historical prices (they never change once settled). */
    private static final long CACHE_TTL_MS = 30L * 24 * 60 * 60 * 1000;

    /** Successive windows to try when looking for a price near the target date. */
    private static final int[] SEARCH_WINDOWS_DAYS = {7, 14, 30, 60};

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    /** Key: "SYMBOL::yyyy-MM-dd" → cached BigDecimal price */
    private final Map<String, CachedValue> cache = new ConcurrentHashMap<>();

    /**
     * Returns the adjusted closing price for {@code symbol} on {@code date}.
     * If the market was closed on that exact date the closest preceding trading
     * day's close is returned (windows widened progressively).
     *
     * @return price, or {@code null} if unavailable (flag for manual entry)
     */
    public BigDecimal getHistoricalPrice(String symbol, LocalDate date) {
        if (symbol == null || symbol.isBlank()) return null;

        String cacheKey = symbol + "::" + date;
        CachedValue cv = cache.get(cacheKey);
        if (cv != null && !cv.isExpired()) {
            log.debug("Cache hit for historical price {} on {}", symbol, date);
            return cv.value;
        }

        BigDecimal price = null;
        for (int window : SEARCH_WINDOWS_DAYS) {
            price = fetchFromYahoo(symbol, date, window);
            if (price != null) break;
            log.debug("No price found for {} on {} within ±{} days – widening window",
                    symbol, date, window);
        }

        cache.put(cacheKey, new CachedValue(price));
        if (price == null) {
            log.warn("Historical price unavailable for {} on {} after all windows", symbol, date);
        }
        return price;
    }

    /**
     * Batch version – more efficient for snapshot generation over many investments.
     *
     * @param symbolDatePairs list of [symbol, date] pairs (same date for a snapshot run)
     * @return map keyed by "symbol::date"
     */
    public Map<String, BigDecimal> getBulkHistoricalPrices(
            List<String[]> symbolDatePairs) {

        Map<String, BigDecimal> results = new LinkedHashMap<>();
        for (String[] pair : symbolDatePairs) {
            String symbol = pair[0];
            LocalDate date = LocalDate.parse(pair[1]);
            String key = symbol + "::" + date;
            BigDecimal price = getHistoricalPrice(symbol, date);
            results.put(key, price);
            // Polite delay to avoid rate-limiting
            try { Thread.sleep(300); } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        return results;
    }

    // ------------------------------------------------------------------
    // Internal helpers
    // ------------------------------------------------------------------

    private BigDecimal fetchFromYahoo(String symbol, LocalDate date, int windowDays) {
        long period1 = date.minusDays(windowDays).atStartOfDay(ZoneOffset.UTC).toEpochSecond();
        long period2 = date.plusDays(1).atStartOfDay(ZoneOffset.UTC).toEpochSecond();

        String url = String.format(YAHOO_HISTORY_URL, symbol, period1, period2);

        try {
            log.debug("Fetching historical price for {} on {} (±{} days)", symbol, date, windowDays);

            HttpHeaders headers = new HttpHeaders();
            headers.set(HttpHeaders.USER_AGENT, USER_AGENT);
            headers.set(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE);
            HttpEntity<Void> request = new HttpEntity<>(headers);

            ResponseEntity<String> response =
                    restTemplate.exchange(url, HttpMethod.GET, request, String.class);
            String json = response.getBody();
            if (json == null || json.isBlank()) return null;

            return parseClosestClose(json, date);

        } catch (Exception e) {
            log.warn("Yahoo historical fetch failed for {} on {} (±{}d): {}",
                    symbol, date, windowDays, e.getMessage());
            return null;
        }
    }

    /**
     * Parses the Yahoo Finance chart response and returns the adjusted close
     * price on or immediately before the requested date.
     */
    private BigDecimal parseClosestClose(String json, LocalDate targetDate) throws Exception {
        JsonNode root = objectMapper.readTree(json);
        JsonNode result = root.path("chart").path("result");
        if (result.isMissingNode() || !result.isArray() || result.isEmpty()) return null;

        JsonNode data = result.get(0);
        JsonNode timestamps = data.path("timestamp");
        JsonNode adjClose = data.path("indicators").path("adjclose").path(0).path("adjclose");
        JsonNode close = data.path("indicators").path("quote").path(0).path("close");

        if (!timestamps.isArray() || timestamps.isEmpty()) return null;

        long targetEpoch = targetDate.atStartOfDay(ZoneOffset.UTC).toEpochSecond();

        // Walk backwards to find the last trading day on or before targetDate
        for (int i = timestamps.size() - 1; i >= 0; i--) {
            long ts = timestamps.get(i).asLong();
            if (ts <= targetEpoch + 86400 /* +1 day tolerance */) {
                BigDecimal price = readPrice(adjClose, i);
                if (price == null) price = readPrice(close, i); // fallback to unadjusted close
                if (price != null) return price;
            }
        }
        // If nothing on/before target, return the earliest available
        for (int i = 0; i < timestamps.size(); i++) {
            BigDecimal price = readPrice(adjClose, i);
            if (price == null) price = readPrice(close, i);
            if (price != null) return price;
        }
        return null;
    }

    private BigDecimal readPrice(JsonNode arr, int idx) {
        if (arr == null || !arr.isArray() || idx >= arr.size()) return null;
        JsonNode node = arr.get(idx);
        if (node == null || node.isNull() || node.isMissingNode()) return null;
        try {
            return new BigDecimal(node.asText());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    // ------------------------------------------------------------------
    // Cache helper
    // ------------------------------------------------------------------

    private static class CachedValue {
        final BigDecimal value;
        final long expiry;

        CachedValue(BigDecimal value) {
            this.value = value;
            this.expiry = System.currentTimeMillis() + CACHE_TTL_MS;
        }

        boolean isExpired() {
            return System.currentTimeMillis() > expiry;
        }
    }
}
