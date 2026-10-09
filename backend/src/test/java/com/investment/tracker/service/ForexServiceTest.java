package com.investment.tracker.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.investment.tracker.model.CurrencyRate;
import com.investment.tracker.repository.CurrencyRateRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Regression-tests the ForexService duplicate-key fix (H2 error 23505 /
 * uk_currency_pair on currency_rates). Observed on the 'ankit' account: when a
 * cached USD -> INR rate expired, the old blind repository.save() raced with a
 * stale row for the same (from_currency, to_currency) pair and threw a unique
 * constraint violation that propagated through convert()/RealGrowthService.toInr()
 * and killed the whole /real-growth request (frontend showed the Real toggle as
 * disabled). With the idempotent upsertRate(...) MERGE the same scenario must
 * succeed, and even when the upsert itself fails the FX path must degrade
 * gracefully to the stale cached rate instead of throwing.
 *
 * ForexService is field-injected (no matching constructor), so the mocks are
 * wired in via ReflectionTestUtils.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ForexServiceTest {

    @Mock
    private CurrencyRateRepository repository;

    @Mock
    private RestTemplate restTemplate;

    private ForexService service;

    private static final String USD = "USD";
    private static final String INR = "INR";

    @BeforeEach
    void setUp() {
        service = new ForexService();
        ReflectionTestUtils.setField(service, "currencyRateRepository", repository);
        ReflectionTestUtils.setField(service, "restTemplate", restTemplate);
        ReflectionTestUtils.setField(service, "objectMapper", new ObjectMapper());
    }

    private void stubApiFetchReturningRate(double rate) {
        when(restTemplate.getForObject(anyString(), eq(String.class)))
                .thenReturn("{\"rates\": {\"INR\": " + rate + "}}");
    }

    private CurrencyRate staleUsdInrRate() {
        return CurrencyRate.builder()
                .id(1L)
                .fromCurrency(USD)
                .toCurrency(INR)
                .rate(new BigDecimal("95.400000"))
                .lastUpdated(LocalDateTime.now().minusHours(25))
                .source("ANY")
                .build();
    }

    @Test
    void refreshPathDoesNotThrowWhenStaleDuplicateRowAlreadyExists() {
        // Arrange: cache expired, but a stale row already exists for USD -> INR
        // (equivalent to the dormant rate rows that triggered the production 23505).
        stubApiFetchReturningRate(95.81);
        when(repository.findValidRate(eq(USD), eq(INR), any()))
                .thenReturn(Optional.empty());
        when(repository.findByFromCurrencyAndToCurrency(USD, INR))
                .thenReturn(Optional.of(staleUsdInrRate()));

        // Act: the refresh path must not throw even though a stale row already
        // exists — in the buggy implementation this threw 23505.
        BigDecimal result = assertDoesNotThrow(() -> service.getExchangeRate(USD, INR));

        // The caller still gets the freshly fetched rate back.
        assertEquals(new BigDecimal("95.810000"), result);
    }

    @Test
    void refreshPathUpsertsTheStaleRowRatherThanBlindlyInserting() {
        stubApiFetchReturningRate(95.81);
        when(repository.findValidRate(eq(USD), eq(INR), any()))
                .thenReturn(Optional.empty());
        when(repository.findByFromCurrencyAndToCurrency(USD, INR))
                .thenReturn(Optional.of(staleUsdInrRate()));

        service.getExchangeRate(USD, INR);

        // The repair must go through the idempotent upsert, not a blind save().
        verify(repository).upsertRate(eq(USD), eq(INR), any(BigDecimal.class),
                any(LocalDateTime.class), eq("EXCHANGERATE_API"));
    }

    @Test
    void refreshPathDegradesGracefullyWhenUpsertItselfThrows() {
        // Simulate a transient constraint failure on the upsert (extreme
        // concurrent-refresh edge case): the caller must still get a rate back.
        stubApiFetchReturningRate(95.81);
        when(repository.findValidRate(eq(USD), eq(INR), any()))
                .thenReturn(Optional.empty());
        when(repository.findByFromCurrencyAndToCurrency(USD, INR))
                .thenReturn(Optional.of(staleUsdInrRate()));
        doThrow(new RuntimeException("simulated 23505 on concurrent refresh"))
                .when(repository).upsertRate(anyString(), anyString(), any(BigDecimal.class),
                        any(LocalDateTime.class), anyString());

        BigDecimal result = assertDoesNotThrow(() -> service.getExchangeRate(USD, INR));

        // Falls back to the stale (expired) cached rate rather than throwing.
        assertEquals(new BigDecimal("95.400000"), result);
    }

    @Test
    void cachedRateIsUsedWhenFresh() {
        CurrencyRate freshRate = CurrencyRate.builder()
                .id(2L)
                .fromCurrency(USD)
                .toCurrency(INR)
                .rate(new BigDecimal("97.000000"))
                .lastUpdated(LocalDateTime.now())
                .source("ANY")
                .build();

        when(repository.findValidRate(eq(USD), eq(INR), any())).thenReturn(Optional.of(freshRate));

        assertEquals(new BigDecimal("97.000000"), service.getExchangeRate(USD, INR));
        verify(repository, never()).upsertRate(anyString(), anyString(), any(BigDecimal.class),
                any(LocalDateTime.class), anyString());
    }
}
