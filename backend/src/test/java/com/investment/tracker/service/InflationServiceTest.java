package com.investment.tracker.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.YearMonth;
import java.util.TreeMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/**
 * Unit tests for the E13 InflationService: bundled-table loading, deflation
 * math, the labelled 4%-per-annum extrapolation tail, and the optional API
 * refresh semantics (full-coverage swap, never throws).
 */
@ExtendWith(MockitoExtension.class)
class InflationServiceTest {

    @Mock
    private RestTemplate restTemplate;

    private InflationService service;

    @BeforeEach
    void setUp() {
        service = new InflationService(null);
        service.loadBundledTable();
    }

    @Test
    void bundledTableLoadsAllMonths() {
        assertEquals(144, service.getCoveredMonthCount());
        assertEquals(YearMonth.of(2012, 1), service.getFirstCoveredMonth());
        assertEquals(YearMonth.of(2023, 12), service.getLastCoveredMonth());
        assertEquals(0, new BigDecimal("75.7412").compareTo(service.getIndex(YearMonth.of(2012, 1))));
        assertEquals(0, new BigDecimal("152.9243").compareTo(service.getIndex(YearMonth.of(2023, 12))));
        assertTrue(service.isCovered(YearMonth.of(2020, 7)));
        assertFalse(service.isCovered(YearMonth.of(2024, 1)));
    }

    @Test
    void deflatorMatchesCpiRatio() {
        // 152.9243 / 97.1629 = 1.57404...
        BigDecimal deflator = service.getDeflator(YearMonth.of(2015, 3), YearMonth.of(2023, 12));
        assertEquals(1.5740, deflator.doubleValue(), 0.0005);
    }

    @Test
    void deflateRestatesToTargetRupees() {
        BigDecimal real = service.deflate(
                new BigDecimal("1000000"), YearMonth.of(2012, 1), YearMonth.of(2023, 12));
        // 1,000,000 × 152.9243 / 75.7412 = 2,019,039
        assertEquals(2019039, real.doubleValue(), 50);
    }

    @Test
    void monthsBeyondTableAreExtrapolatedAtRbiMidpoint() {
        assertTrue(service.isExtrapolated(YearMonth.of(2024, 3)));
        // 152.9243 × 1.04^(3/12) = 154.43
        assertEquals(154.43, service.getEffectiveIndex(YearMonth.of(2024, 3)).doubleValue(), 0.01);
        // one full year beyond the table compounds to exactly +4%
        BigDecimal yoy = service.getEffectiveIndex(YearMonth.of(2024, 12))
                .divide(service.getEffectiveIndex(YearMonth.of(2023, 12)), 6, RoundingMode.HALF_UP);
        assertEquals(1.04, yoy.doubleValue(), 0.0001);
    }

    @Test
    void monthsBeforeTableAreBackProjected() {
        assertTrue(service.isExtrapolated(YearMonth.of(2011, 12)));
        // 75.7412 × 1.04^(-1/12) ≈ 75.49
        assertEquals(75.49, service.getEffectiveIndex(YearMonth.of(2011, 12)).doubleValue(), 0.01);
    }

    @Test
    void fiscalYearInflationMatchesPublishedYoY() {
        // OECD IXOB Mar-2022 → Mar-2023: 146.8646 / 138.8218 − 1 = 5.79%
        BigDecimal inflation = service.getEffectiveIndex(YearMonth.of(2023, 3))
                .divide(service.getEffectiveIndex(YearMonth.of(2022, 3)), 6, RoundingMode.HALF_UP)
                .subtract(BigDecimal.ONE);
        assertEquals(0.0579, inflation.doubleValue(), 0.0005);
    }

    @Test
    void refreshWithNullClientIsNoop() {
        assertFalse(service.refreshFromApi());
        assertEquals(144, service.getCoveredMonthCount());
        assertFalse(service.isRefreshedFromApi());
    }

    @Test
    void refreshFailureKeepsBundledTable() {
        InflationService withClient = new InflationService(restTemplate);
        withClient.loadBundledTable();
        when(restTemplate.getForObject(anyString(), eq(String.class)))
                .thenThrow(new ResourceAccessException("offline"));
        assertFalse(withClient.refreshFromApi());
        assertEquals(144, withClient.getCoveredMonthCount());
        assertFalse(withClient.isRefreshedFromApi());
    }

    @Test
    void refreshSwapsTableWhenCoverageIsComplete() {
        InflationService withClient = new InflationService(restTemplate);
        withClient.loadBundledTable();
        StringBuilder csv = new StringBuilder("period,value\n");
        YearMonth m = YearMonth.of(2012, 1);
        double v = 100.0;
        while (!m.isAfter(YearMonth.of(2024, 3))) {
            csv.append(m).append(',').append(String.format("%.4f", v)).append('\n');
            m = m.plusMonths(1);
            v += 0.1;
        }
        when(restTemplate.getForObject(anyString(), eq(String.class))).thenReturn(csv.toString());
        assertTrue(withClient.refreshFromApi());
        assertTrue(withClient.isRefreshedFromApi());
        assertEquals(147, withClient.getCoveredMonthCount());
        assertEquals(0, new BigDecimal("100.1000").compareTo(withClient.getIndex(YearMonth.of(2012, 2))));
        assertEquals(YearMonth.of(2024, 3), withClient.getLastCoveredMonth());
        assertFalse(withClient.isExtrapolated(YearMonth.of(2024, 3)));
    }

    @Test
    void refreshKeepsBundledTableWhenRemoteCoverageIsPartial() {
        InflationService withClient = new InflationService(restTemplate);
        withClient.loadBundledTable();
        when(restTemplate.getForObject(anyString(), eq(String.class)))
                .thenReturn("period,value\n2024-01,159.04\n2024-02,159.56\n");
        assertFalse(withClient.refreshFromApi());
        assertEquals(144, withClient.getCoveredMonthCount());
        assertThrows(IllegalArgumentException.class, () -> withClient.getIndex(YearMonth.of(2024, 1)));
    }

    @Test
    void parseRemoteCsvSkipsHeaderAndNaRows() {
        TreeMap<YearMonth, BigDecimal> parsed = InflationService.parseRemoteCsv(
                "period,value\n2024-01,159.04\n2024-02,NA\ngarbage\n2024-03, 160.08 \n");
        assertEquals(2, parsed.size());
        assertEquals(0, new BigDecimal("159.04").compareTo(parsed.get(YearMonth.of(2024, 1))));
        assertEquals(0, new BigDecimal("160.08").compareTo(parsed.get(YearMonth.of(2024, 3))));
        assertTrue(InflationService.parseRemoteCsv(null).isEmpty());
    }
}
