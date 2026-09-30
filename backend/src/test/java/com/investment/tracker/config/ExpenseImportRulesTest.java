package com.investment.tracker.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.yaml.snakeyaml.Yaml;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Unit tests for the expense-import config rules (backlog feature F2).
 */
class ExpenseImportRulesTest {

    private ResourceLoader resourceLoader;
    private String yaml;

    @BeforeEach
    void setUp() {
        resourceLoader = new ResourceLoader() {
            @Override
            public Resource getResource(String location) {
                return new ByteArrayResource(yaml.getBytes(StandardCharsets.UTF_8));
            }

            @Override
            public ClassLoader getClassLoader() {
                return Thread.currentThread().getContextClassLoader();
            }
        };
        yaml = String.join("\n",
                "business-rules:",
                "  expense-import:",
                "    max-rows: 100",
                "    default-currency: USD",
                "    default-category: OTHER",
                "    category-keywords:",
                "      FOOD:",
                "        - grocery",
                "        - restaurant",
                "      TRANSPORT:",
                "        - fuel",
                "        - uber",
                "      UTILITIES:",
                "        - electricity");
    }

    private ExpenseImportRules load() {
        ExpenseImportRules rules = new ExpenseImportRules(resourceLoader, "test");
        rules.load();
        return rules;
    }

    @Test
    void loadsConfiguredValues() {
        ExpenseImportRules rules = load();
        assertEquals(100, rules.getMaxRows());
        assertEquals("USD", rules.getDefaultCurrency());
        assertEquals("OTHER", rules.getDefaultCategory());
    }

    @Test
    void infersCategoryByKeywordMatch() {
        ExpenseImportRules rules = load();
        assertEquals("FOOD", rules.inferCategory("Weekly grocery run"));
        assertEquals("FOOD", rules.inferCategory("Dinner at a RESTAURANT"));
        assertEquals("TRANSPORT", rules.inferCategory("Uber to airport"));
        assertEquals("UTILITIES", rules.inferCategory("Electricity bill for June"));
    }

    @Test
    void fallsBackToDefaultCategoryWhenNoKeywordMatches() {
        ExpenseImportRules rules = load();
        assertEquals("OTHER", rules.inferCategory("Something unrelated"));
        assertEquals("OTHER", rules.inferCategory(null));
        assertEquals("OTHER", rules.inferCategory(""));
    }

    @Test
    void malformedConfigFailsLoudly() {
        yaml = "business-rules:\n  other-section: {}";
        assertThrows(IllegalStateException.class, this::load);
    }

    @Test
    void nonListKeywordTableFailsLoudly() {
        yaml = String.join("\n",
                "business-rules:",
                "  expense-import:",
                "    max-rows: 100",
                "    category-keywords: FOOD");
        assertThrows(IllegalStateException.class, this::load);
    }
}
