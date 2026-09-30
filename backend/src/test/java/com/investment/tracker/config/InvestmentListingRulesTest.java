package com.investment.tracker.config;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;

import java.io.IOException;
import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.fail;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests for the investment-rules.yml config accessor.
 */
class InvestmentListingRulesTest {

    private static final String VALID_YAML =
            "investment-rules:\n"
            + "  listing:\n"
            + "    auto-price-eligible-types: EQUITY, CRYPTO\n"
            + "    listed-by-default: true\n";

    private InvestmentListingRules rulesFromYaml(String yaml) throws IOException {
        ResourceLoader resourceLoader = mock(ResourceLoader.class);
        Resource resource = mock(Resource.class);
        when(resourceLoader.getResource("classpath:config/investment-rules.yml")).thenReturn(resource);
        when(resource.getInputStream()).thenReturn(new java.io.ByteArrayInputStream(yaml.getBytes()));
        InvestmentListingRules rules =
                new InvestmentListingRules(resourceLoader, "classpath:config/investment-rules.yml");
        rules.load();
        return rules;
    }

    @Test
    void loadsValidConfigAndExposesEligibility() throws Exception {
        InvestmentListingRules rules = rulesFromYaml(VALID_YAML);

        assertTrue(rules.isAutoPriceEligible("EQUITY"));
        assertTrue(rules.isAutoPriceEligible("crypto")); // case-insensitive
        assertFalse(rules.isAutoPriceEligible("FD"));
        assertFalse(rules.isAutoPriceEligible("PPF"));
        assertFalse(rules.isAutoPriceEligible(null));

        assertTrue(rules.isListedByDefault("EQUITY"));
        assertFalse(rules.isListedByDefault("FD"));
    }

    @Test
    void missingSectionFailsLoudly() {
        assertThrows(IllegalStateException.class,
                () -> rulesFromYaml("something-else:\n  key: value\n"));
    }

    @Test
    void emptyTypeListFailsLoudly() {
        String yaml = "investment-rules:\n"
                + "  listing:\n"
                + "    auto-price-eligible-types: \"\"\n"
                + "    listed-by-default: true\n";
        assertThrows(IllegalStateException.class, () -> rulesFromYaml(yaml));
    }

    @Test
    void missingBooleanFlagFailsLoudly() {
        String yaml = "investment-rules:\n"
                + "  listing:\n"
                + "    auto-price-eligible-types: EQUITY\n";
        assertThrows(IllegalStateException.class, () -> rulesFromYaml(yaml));
    }

    @Test
    void invalidYamlSyntaxFailsLoudly() {
        String yaml = "investment-rules:\n  listing:\n   bad: [unclosed\n";
        try {
            rulesFromYaml(yaml);
            fail("Malformed YAML must fail loudly");
        } catch (Exception expected) {
            // SnakeYAML (or our wrapper) rejected the malformed document.
        }
    }

    @Test
    void unreadableResourceFailsLoudly() throws IOException {
        ResourceLoader resourceLoader = mock(ResourceLoader.class);
        Resource resource = mock(Resource.class);
        when(resourceLoader.getResource("classpath:config/investment-rules.yml")).thenReturn(resource);
        when(resource.getInputStream()).thenThrow(new IOException("boom"));
        InvestmentListingRules rules =
                new InvestmentListingRules(resourceLoader, "classpath:config/investment-rules.yml");

        assertThrows(IllegalStateException.class, rules::load);
    }

    @Test
    void emptyStreamFailsLoudly() throws IOException {
        ResourceLoader resourceLoader = mock(ResourceLoader.class);
        Resource resource = mock(Resource.class);
        when(resourceLoader.getResource("classpath:config/investment-rules.yml")).thenReturn(resource);
        when(resource.getInputStream()).thenReturn(new InputStream() {
            @Override
            public int read() {
                return -1;
            }
        });
        InvestmentListingRules rules =
                new InvestmentListingRules(resourceLoader, "classpath:config/investment-rules.yml");

        assertThrows(IllegalStateException.class, rules::load);
    }
}
