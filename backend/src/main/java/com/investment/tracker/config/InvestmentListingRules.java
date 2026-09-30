package com.investment.tracker.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;
import org.yaml.snakeyaml.Yaml;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Typed accessor for {@code config/investment-rules.yml}.
 *
 * This is the ONLY module allowed to read that file (see
 * llm_agent_operating_guidelines.md Section 2 — single point of config access).
 * It is validated eagerly at startup so a malformed config fails loudly in
 * development instead of silently degrading behaviour.
 */
@Component
@Slf4j
public class InvestmentListingRules {

    private final ResourceLoader resourceLoader;
    private final String configLocation;

    private Set<String> autoPriceEligibleTypes = Collections.emptySet();
    private boolean listedByDefault = true;

    public InvestmentListingRules(ResourceLoader resourceLoader,
                                  @Value("${app.config.investment-rules-file:classpath:config/investment-rules.yml}")
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
                    "Cannot read investment rules config at " + configLocation, e);
        }

        Map<String, Object> rules = requiredMap(root, "investment-rules");
        Map<String, Object> listing = requiredMap(rules, "listing");

        Object typesValue = listing.get("auto-price-eligible-types");
        if (!(typesValue instanceof String) || ((String) typesValue).trim().isEmpty()) {
            throw new IllegalStateException(
                    "investment-rules.listing.auto-price-eligible-types must be a non-empty "
                            + "comma-separated type list in " + configLocation);
        }
        autoPriceEligibleTypes = Arrays.stream(((String) typesValue).split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(s -> s.toUpperCase(Locale.ROOT))
                .collect(Collectors.toCollection(LinkedHashSet::new));

        Object defaultValue = listing.get("listed-by-default");
        if (!(defaultValue instanceof Boolean)) {
            throw new IllegalStateException(
                    "investment-rules.listing.listed-by-default must be a boolean in "
                            + configLocation);
        }
        listedByDefault = (Boolean) defaultValue;

        log.info("Investment listing rules loaded from {}: auto-price-eligible={}, listed-by-default={}",
                configLocation, autoPriceEligibleTypes, listedByDefault);
    }

    /**
     * True when the investment type's value can be refreshed from live market
     * data and therefore may legitimately carry {@code is_listed = true}.
     */
    public boolean isAutoPriceEligible(String investmentType) {
        return investmentType != null
                && autoPriceEligibleTypes.contains(investmentType.toUpperCase(Locale.ROOT));
    }

    /** Effective {@code is_listed} for a type when the client omits the flag. */
    public boolean isListedByDefault(String investmentType) {
        return isAutoPriceEligible(investmentType) && listedByDefault;
    }

    private Map<String, Object> requiredMap(Map<String, Object> parent, String key) {
        if (parent == null || !(parent.get(key) instanceof Map)) {
            throw new IllegalStateException(
                    "investment-rules.yml is malformed: missing '" + key + "' section in "
                            + configLocation);
        }
        return (Map<String, Object>) parent.get(key);
    }
}
