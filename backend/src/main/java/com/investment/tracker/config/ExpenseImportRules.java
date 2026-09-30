package com.investment.tracker.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;
import org.yaml.snakeyaml.Yaml;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Typed accessor for the {@code expenses.import} section of
 * {@code config/business-rules.yml} (backlog feature F2).
 *
 * This is the ONLY module allowed to read that file (guidelines Section 2 —
 * single point of config access), and the first reader of the previously
 * unloaded business-rules-file (DB-audit flag). Validated eagerly at startup.
 */
@Component
@Slf4j
public class ExpenseImportRules {

    private final ResourceLoader resourceLoader;
    private final String configLocation;

    private int maxFileSizeRows;
    private String defaultCurrency;
    private String defaultCategory;
    private Map<String, java.util.List<String>> categoryKeywords = new LinkedHashMap<>();

    public ExpenseImportRules(ResourceLoader resourceLoader,
                              @Value("${app.config.business-rules-file:classpath:config/business-rules.yml}")
                              String configLocation) {
        this.resourceLoader = resourceLoader;
        this.configLocation = configLocation;
    }

    @SuppressWarnings("unchecked")
    @PostConstruct
    void load() {
        Map<String, Object> root;
        try (InputStream in = resourceLoader.getResource(configLocation).getInputStream()) {
            root = new Yaml().load(in);
        } catch (IOException e) {
            throw new IllegalStateException(
                    "Cannot read business rules config at " + configLocation, e);
        }

        if (root == null || !(root.get("business-rules") instanceof Map)) {
            throw new IllegalStateException(
                    "business-rules.yml is malformed: missing 'business-rules' section in "
                            + configLocation);
        }
        Map<String, Object> rules = (Map<String, Object>) root.get("business-rules");

        if (!(rules.get("expense-import") instanceof Map)) {
            throw new IllegalStateException(
                    "business-rules.yml is malformed: missing 'expense-import' section in "
                            + configLocation);
        }
        Map<String, Object> imprt = (Map<String, Object>) rules.get("expense-import");

        maxFileSizeRows = intOf(imprt, "max-rows", 5000, configLocation);
        defaultCurrency = stringOf(imprt, "default-currency", "INR", configLocation);
        defaultCategory = stringOf(imprt, "default-category", "OTHER", configLocation);

        Object keywords = imprt.get("category-keywords");
        if (!(keywords instanceof Map)) {
            throw new IllegalStateException(
                    "business-rules.expense-import.category-keywords must be a "
                            + "category → keyword-list map in " + configLocation);
        }
        for (Map.Entry<String, Object> entry : ((Map<String, Object>) keywords).entrySet()) {
            if (!(entry.getValue() instanceof List)) {
                throw new IllegalStateException(
                        "category-keywords." + entry.getKey() + " must be a keyword list in "
                                + configLocation);
            }
            for (Object kw : (List<Object>) entry.getValue()) {
                if (!(kw instanceof String) || ((String) kw).trim().isEmpty()) {
                    throw new IllegalStateException(
                            "category-keywords." + entry.getKey()
                                    + " contains a non-string keyword in " + configLocation);
                }
            }
            categoryKeywords.put(entry.getKey().trim().toUpperCase(Locale.ROOT),
                    (List<String>) entry.getValue());
        }

        log.info("Expense import rules loaded from {}: max-rows={}, default-currency={}, "
                        + "default-category={}, {} category keyword mappings",
                configLocation, maxFileSizeRows, defaultCurrency, defaultCategory,
                categoryKeywords.size());
    }

    /** Maximum rows accepted in one uploaded sheet. */
    public int getMaxRows() {
        return maxFileSizeRows;
    }

    /** Currency applied when a row omits one. */
    public String getDefaultCurrency() {
        return defaultCurrency;
    }

    /** Category applied when no keyword matches the description. */
    public String getDefaultCategory() {
        return defaultCategory;
    }

    /**
     * Infers the category from a description using the configured keyword
     * table. First category whose any keyword appears (case-insensitive) in
     * the description wins; falls back to the configured default.
     */
    public String inferCategory(String description) {
        if (description != null) {
            String lower = description.toLowerCase(Locale.ROOT);
            for (Map.Entry<String, String> entry : keywordIndex().entrySet()) {
                if (lower.contains(entry.getKey())) {
                    return entry.getValue();
                }
            }
        }
        return defaultCategory;
    }

    /** Flattened keyword → category index (config order preserved). */
    private Map<String, String> keywordIndexCache;

    private synchronized Map<String, String> keywordIndex() {
        if (keywordIndexCache == null) {
            Map<String, String> index = new LinkedHashMap<>();
            for (Map.Entry<String, java.util.List<String>> entry : categoryKeywords.entrySet()) {
                for (String kw : entry.getValue()) {
                    index.put(kw.trim().toLowerCase(Locale.ROOT), entry.getKey());
                }
            }
            keywordIndexCache = index;
        }
        return keywordIndexCache;
    }

    private static int intOf(Map<String, Object> map, String key, int fallback, String location) {
        Object v = map.get(key);
        if (v == null) return fallback;
        if (!(v instanceof Number)) {
            throw new IllegalStateException(
                    "business-rules.expense-import." + key + " must be a number in " + location);
        }
        return ((Number) v).intValue();
    }

    private static String stringOf(Map<String, Object> map, String key, String fallback,
                                   String location) {
        Object v = map.get(key);
        if (v == null) return fallback;
        if (!(v instanceof String) || ((String) v).trim().isEmpty()) {
            throw new IllegalStateException(
                    "business-rules.expense-import." + key + " must be a non-empty string in "
                            + location);
        }
        return ((String) v).trim();
    }
}
