package com.investment.tracker.config;

import com.investment.tracker.model.TransactionType;
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
import java.util.regex.Pattern;

/**
 * Typed accessor for {@code config/statement-import.yml} (backlog feature F6).
 *
 * Single point of config access for the bank-statement import settings and
 * the bank-detection registry (guidelines Section 2). Validated eagerly at
 * startup so a malformed config fails loudly in development.
 */
@Component
@Slf4j
public class StatementImportRules {

    private final ResourceLoader resourceLoader;
    private final String configLocation;

    private long maxFileKb;
    private int maxRows;
    private String defaultCurrency;
    private double minParseRatio;
    private TransactionType creditTransactionType;
    private TransactionType debitTransactionType;
    private List<BankEntry> banks = new ArrayList<>();

    /** One configured bank: detection markers + display name. */
    public static class BankEntry {
        private final String name;
        private final List<Pattern> markers;

        public BankEntry(String name, List<Pattern> markers) {
            this.name = name;
            this.markers = markers;
        }

        public String getName() {
            return name;
        }

        /** True when any marker regex matches the given statement text. */
        public boolean matches(String statementText) {
            return markers.stream().anyMatch(p -> p.matcher(statementText).find());
        }
    }

    public StatementImportRules(ResourceLoader resourceLoader,
                                @Value("${app.config.statement-import-file:classpath:config/statement-import.yml}")
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
                    "Cannot read statement import config at " + configLocation, e);
        }

        if (root == null || !(root.get("statement-import") instanceof Map)) {
            throw new IllegalStateException(
                    "statement-import.yml is malformed: missing 'statement-import' section in "
                            + configLocation);
        }
        Map<String, Object> cfg = (Map<String, Object>) root.get("statement-import");

        maxFileKb = requireNumber(cfg, "max-file-kb", configLocation).longValue();
        maxRows = requireNumber(cfg, "max-rows", configLocation).intValue();
        defaultCurrency = requireString(cfg, "default-currency", configLocation);
        minParseRatio = requireNumber(cfg, "min-parse-ratio", configLocation).doubleValue();

        creditTransactionType = transactionType(cfg, "credit-transaction-type", configLocation);
        debitTransactionType = transactionType(cfg, "debit-transaction-type", configLocation);

        if (!(cfg.get("banks") instanceof List)) {
            throw new IllegalStateException(
                    "statement-import.banks must be a list in " + configLocation);
        }
        for (Object entry : (List<Object>) cfg.get("banks")) {
            if (!(entry instanceof Map)) {
                throw new IllegalStateException(
                        "statement-import.banks entries must be maps in " + configLocation);
            }
            Map<String, Object> bank = (Map<String, Object>) entry;
            String name = requireString(bank, "name", configLocation);
            if (!(bank.get("markers") instanceof List) || ((List<Object>) bank.get("markers")).isEmpty()) {
                throw new IllegalStateException(
                        "statement-import.banks." + name + ".markers must be a non-empty "
                                + "regex list in " + configLocation);
            }
            List<Pattern> patterns = new ArrayList<>();
            for (Object marker : (List<Object>) bank.get("markers")) {
                if (!(marker instanceof String)) {
                    throw new IllegalStateException(
                            "statement-import.banks." + name
                                    + ".markers must contain only regex strings in " + configLocation);
                }
                patterns.add(Pattern.compile((String) marker));
            }
            banks.add(new BankEntry(name, patterns));
        }
        if (banks.isEmpty()) {
            throw new IllegalStateException(
                    "statement-import.banks must contain at least one bank in " + configLocation);
        }

        log.info("Statement import rules loaded from {}: max-file-kb={}, max-rows={}, "
                        + "default-currency={}, {} bank(s) registered",
                configLocation, maxFileKb, maxRows, defaultCurrency, banks.size());
    }

    /**
     * Detects the bank from the statement text. Returns the matching bank
     * entry or {@code null} when no marker matches (caller reports an
     * unsupported format).
     */
    public BankEntry detectBank(String statementText) {
        for (BankEntry bank : banks) {
            if (bank.matches(statementText)) {
                return bank;
            }
        }
        return null;
    }

    public long getMaxFileKb() {
        return maxFileKb;
    }

    public int getMaxRows() {
        return maxRows;
    }

    public String getDefaultCurrency() {
        return defaultCurrency;
    }

    public double getMinParseRatio() {
        return minParseRatio;
    }

    public TransactionType getCreditTransactionType() {
        return creditTransactionType;
    }

    public TransactionType getDebitTransactionType() {
        return debitTransactionType;
    }

    public List<BankEntry> getBanks() {
        return banks;
    }

    // ------------------------------------------------------------------
    // Config parsing helpers
    // ------------------------------------------------------------------

    private static Number requireNumber(Map<String, Object> map, String key, String location) {
        Object v = map.get(key);
        if (!(v instanceof Number)) {
            throw new IllegalStateException(
                    "statement-import." + key + " must be a number in " + location);
        }
        return (Number) v;
    }

    private static String requireString(Map<String, Object> map, String key, String location) {
        Object v = map.get(key);
        if (!(v instanceof String) || ((String) v).trim().isEmpty()) {
            throw new IllegalStateException(
                    "statement-import." + key + " must be a non-empty string in " + location);
        }
        return ((String) v).trim();
    }

    private static TransactionType transactionType(Map<String, Object> map, String key,
                                                   String location) {
        String name = requireString(map, key, location);
        try {
            return TransactionType.valueOf(name);
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException(
                    "statement-import." + key + " '" + name
                            + "' is not a valid TransactionType in " + location, e);
        }
    }
}
