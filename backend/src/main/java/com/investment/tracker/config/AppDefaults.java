package com.investment.tracker.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;
import org.yaml.snakeyaml.Yaml;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Typed accessor for {@code config/currency-config.yml}.
 *
 * Single point of config access for supported currencies and defaults
 * (see llm_agent_operating_guidelines.md Section 2). Validated eagerly at
 * startup so a malformed config fails loudly in development.
 */
@Component
@Slf4j
public class AppDefaults {

    private final ResourceLoader resourceLoader;
    private final String configLocation;

    private String defaultCurrency;
    private List<String> supportedCurrencies = new ArrayList<>();

    public AppDefaults(ResourceLoader resourceLoader,
                       @Value("${app.config.currency-config-file:classpath:config/currency-config.yml}")
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
                    "Cannot read currency config at " + configLocation, e);
        }

        if (root == null || !(root.get("currencies") instanceof Map)) {
            throw new IllegalStateException(
                    "currency-config.yml is malformed: missing 'currencies' section in "
                            + configLocation);
        }
        Map<String, Object> currencies = (Map<String, Object>) root.get("currencies");

        Object defaultValue = currencies.get("default-currency");
        if (!(defaultValue instanceof String) || ((String) defaultValue).trim().isEmpty()) {
            throw new IllegalStateException(
                    "currencies.default-currency must be a non-empty code in " + configLocation);
        }
        defaultCurrency = ((String) defaultValue).trim().toUpperCase();

        if (!(currencies.get("supported") instanceof List)) {
            throw new IllegalStateException(
                    "currencies.supported must be a list in " + configLocation);
        }
        List<Map<String, Object>> supported = (List<Map<String, Object>>) currencies.get("supported");
        for (Map<String, Object> entry : supported) {
            Object code = entry.get("code");
            if (code instanceof String && !((String) code).trim().isEmpty()) {
                supportedCurrencies.add(((String) code).trim().toUpperCase());
            }
        }
        if (!supportedCurrencies.contains(defaultCurrency)) {
            supportedCurrencies.add(0, defaultCurrency);
        }

        log.info("Currency defaults loaded from {}: default={}, supported={}",
                configLocation, defaultCurrency, supportedCurrencies);
    }

    public String getDefaultCurrency() {
        return defaultCurrency;
    }

    public List<String> getSupportedCurrencies() {
        return supportedCurrencies;
    }
}
