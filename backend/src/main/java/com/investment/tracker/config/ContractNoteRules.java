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
import java.util.regex.Pattern;

/**
 * Typed accessor for {@code config/contract-note.yml} (Phase 9 — contract
 * note import). Single point of config access for the broker template
 * registry (guidelines Section 2); validated eagerly at startup.
 */
@Component
@Slf4j
public class ContractNoteRules {

    private final ResourceLoader resourceLoader;
    private final String configLocation;

    private long maxFileKb;
    private int maxTrades;
    private String defaultMarket;
    private String defaultCurrency;
    private String autoCashAccountName;
    private List<BrokerTemplate> brokers = new ArrayList<>();

    /** One configured broker: detection markers + trade-row template. */
    public static class BrokerTemplate {
        private final String name;
        private final List<Pattern> markers;
        private final Pattern tradeRow;

        public BrokerTemplate(String name, List<Pattern> markers, Pattern tradeRow) {
            this.name = name;
            this.markers = markers;
            this.tradeRow = tradeRow;
        }

        public String getName() {
            return name;
        }

        /** True when any marker regex matches the document text. */
        public boolean matches(String text) {
            return markers.stream().anyMatch(p -> p.matcher(text).find());
        }

        public Pattern getTradeRow() {
            return tradeRow;
        }
    }

    public ContractNoteRules(ResourceLoader resourceLoader,
                             @Value("${app.config.contract-note-file:classpath:config/contract-note.yml}")
                             String configLocation) {
        this.resourceLoader = resourceLoader;
        this.configLocation = configLocation;
    }

    @SuppressWarnings("unchecked")
    @PostConstruct
    public void load() {
        Map<String, Object> root;
        try (InputStream in = resourceLoader.getResource(configLocation).getInputStream()) {
            root = new Yaml().load(in);
        } catch (IOException e) {
            throw new IllegalStateException(
                    "Cannot read contract note config at " + configLocation, e);
        }

        if (root == null || !(root.get("contract-note") instanceof Map)) {
            throw new IllegalStateException(
                    "contract-note.yml is malformed: missing 'contract-note' section in "
                            + configLocation);
        }
        Map<String, Object> cfg = (Map<String, Object>) root.get("contract-note");

        maxFileKb = requireNumber(cfg, "max-file-kb").longValue();
        maxTrades = requireNumber(cfg, "max-trades").intValue();
        defaultMarket = requireString(cfg, "default-market");
        defaultCurrency = requireString(cfg, "default-currency");
        autoCashAccountName = requireString(cfg, "auto-cash-account-name");

        if (!(cfg.get("brokers") instanceof List) || ((List<Object>) cfg.get("brokers")).isEmpty()) {
            throw new IllegalStateException(
                    "contract-note.brokers must be a non-empty list in " + configLocation);
        }
        for (Object entry : (List<Object>) cfg.get("brokers")) {
            if (!(entry instanceof Map)) {
                throw new IllegalStateException(
                        "contract-note.brokers entries must be maps in " + configLocation);
            }
            Map<String, Object> broker = (Map<String, Object>) entry;
            String name = requireString(broker, "name");
            if (!(broker.get("trade-row") instanceof String)) {
                throw new IllegalStateException(
                        "contract-note.brokers." + name + ".trade-row must be a regex string in "
                                + configLocation);
            }
            List<Pattern> markers = new ArrayList<>();
            if (broker.get("markers") instanceof List) {
                for (Object marker : (List<Object>) broker.get("markers")) {
                    if (!(marker instanceof String)) {
                        throw new IllegalStateException(
                                "contract-note.brokers." + name
                                        + ".markers must contain only regex strings in " + configLocation);
                    }
                    markers.add(Pattern.compile((String) marker));
                }
            }
            brokers.add(new BrokerTemplate(name, markers,
                    Pattern.compile((String) broker.get("trade-row"))));
        }

        log.info("Contract note rules loaded from {}: max-file-kb={}, max-trades={}, "
                        + "default-market={}, {} broker template(s)",
                configLocation, maxFileKb, maxTrades, defaultMarket, brokers.size());
    }

    /**
     * Resolves the broker template for a document: the first broker whose
     * markers match, else the template named "Generic", else {@code null}.
     */
    public BrokerTemplate resolveTemplate(String text) {
        for (BrokerTemplate broker : brokers) {
            if (broker.matches(text)) {
                return broker;
            }
        }
        for (BrokerTemplate broker : brokers) {
            if ("Generic".equalsIgnoreCase(broker.getName())) {
                return broker;
            }
        }
        return null;
    }

    public long getMaxFileKb() {
        return maxFileKb;
    }

    public int getMaxTrades() {
        return maxTrades;
    }

    public String getDefaultMarket() {
        return defaultMarket;
    }

    public String getDefaultCurrency() {
        return defaultCurrency;
    }

    /** Cash account display name for a broker, with {broker} substituted. */
    public String cashAccountNameFor(String brokerName) {
        return autoCashAccountName.replace("{broker}",
                brokerName != null ? brokerName : "Broker");
    }

    private Number requireNumber(Map<String, Object> map, String key) {
        Object v = map.get(key);
        if (!(v instanceof Number)) {
            throw new IllegalStateException(
                    "contract-note." + key + " must be a number in " + configLocation);
        }
        return (Number) v;
    }

    private String requireString(Map<String, Object> map, String key) {
        Object v = map.get(key);
        if (!(v instanceof String) || ((String) v).trim().isEmpty()) {
            throw new IllegalStateException(
                    "contract-note." + key + " must be a non-empty string in " + configLocation);
        }
        return ((String) v).trim();
    }
}
