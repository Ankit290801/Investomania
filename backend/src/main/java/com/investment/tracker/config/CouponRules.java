package com.investment.tracker.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;
import org.yaml.snakeyaml.Yaml;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.MonthDay;
import java.time.Year;
import java.util.Map;

/**
 * Typed accessor for the {@code bond-coupons} section of
 * {@code config/investment-rules.yml}.
 *
 * Single point of config access for coupon auto-generation settings
 * (see llm_agent_operating_guidelines.md Section 2). Validated eagerly at
 * startup so a malformed config fails loudly in development.
 */
@Component
@Slf4j
public class CouponRules {

    private final ResourceLoader resourceLoader;
    private final String configLocation;

    private boolean enabled;
    private String cron;
    private MonthDay defaultFirstCouponMonthDay;

    public CouponRules(ResourceLoader resourceLoader,
                       @Value("${app.config.investment-rules-file:classpath:config/investment-rules.yml}")
                       String configLocation) {
        this.resourceLoader = resourceLoader;
        this.configLocation = configLocation;
    }

    @PostConstruct
    void load() {
        Map<String, Object> root;
        try (InputStream in = resourceLoader.getResource(configLocation).getInputStream()) {
            root = new Yaml().load(in);
        } catch (IOException e) {
            throw new IllegalStateException(
                    "Cannot read investment rules config at " + configLocation, e);
        }

        if (root == null || !(root.get("investment-rules") instanceof Map)) {
            throw new IllegalStateException(
                    "investment-rules.yml is malformed: missing 'investment-rules' section in "
                            + configLocation);
        }
        @SuppressWarnings("unchecked")
        Map<String, Object> rules = (Map<String, Object>) root.get("investment-rules");

        if (!(rules.get("bond-coupons") instanceof Map)) {
            throw new IllegalStateException(
                    "investment-rules.yml is malformed: missing 'bond-coupons' section in "
                            + configLocation);
        }
        @SuppressWarnings("unchecked")
        Map<String, Object> coupons = (Map<String, Object>) rules.get("bond-coupons");

        Object enabledValue = coupons.get("enabled");
        if (!(enabledValue instanceof Boolean)) {
            throw new IllegalStateException(
                    "investment-rules.bond-coupons.enabled must be a boolean in " + configLocation);
        }
        enabled = (Boolean) enabledValue;

        Object cronValue = coupons.get("cron");
        if (!(cronValue instanceof String) || ((String) cronValue).trim().isEmpty()) {
            throw new IllegalStateException(
                    "investment-rules.bond-coupons.cron must be a non-empty cron expression in "
                            + configLocation);
        }
        cron = ((String) cronValue).trim();

        Object monthDayValue = coupons.get("default-first-coupon-month-day");
        if (!(monthDayValue instanceof String) || !((String) monthDayValue).trim().matches("\\d{2}-\\d{2}")) {
            throw new IllegalStateException(
                    "investment-rules.bond-coupons.default-first-coupon-month-day must be "
                            + "formatted MM-DD in " + configLocation);
        }
        String[] parts = ((String) monthDayValue).trim().split("-");
        defaultFirstCouponMonthDay = MonthDay.of(Integer.parseInt(parts[0]), Integer.parseInt(parts[1]));

        log.info("Coupon rules loaded from {}: enabled={}, cron={}, "
                        + "default-first-coupon-month-day={}",
                configLocation, enabled, cron, defaultFirstCouponMonthDay);
    }

    public boolean isEnabled() {
        return enabled;
    }

    /** Spring cron expression for the daily coupon generation job. */
    public String getCron() {
        return cron;
    }

    /**
     * Derives the first coupon date for a bond that doesn't specify one:
     * the first occurrence of the configured month/day on or after the
     * bond's purchase date.
     */
    public LocalDate deriveFirstCouponDate(LocalDate purchaseDate) {
        MonthDay md = defaultFirstCouponMonthDay;
        LocalDate candidate = md.atYear(purchaseDate.getYear());
        if (candidate.isBefore(purchaseDate)) {
            candidate = md.atYear(purchaseDate.getYear() + 1);
        }
        // Guard against absurd far-future years (purchaseDate far in the past).
        while (candidate.getYear() > Year.now().getValue() + 100) {
            candidate = md.atYear(Year.now().getValue() + 100);
        }
        return candidate;
    }
}
