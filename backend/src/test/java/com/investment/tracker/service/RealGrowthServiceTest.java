package com.investment.tracker.service;

import com.investment.tracker.dto.FiscalRealBreakdownDTO;
import com.investment.tracker.dto.GrowthMetricsDTO;
import com.investment.tracker.dto.HistoricalNetWorthDTO;
import com.investment.tracker.dto.RealGrowthMetricsDTO;
import com.investment.tracker.dto.RealGrowthPointDTO;
import com.investment.tracker.model.Transaction;
import com.investment.tracker.model.TransactionType;
import com.investment.tracker.repository.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/**
 * Unit tests for the E13 RealGrowthService: today's-rupee deflation of the
 * nominal INR series, the per-fiscal-year breakdown (inflation, nominal and
 * real returns) and the realized FY XIRR (nominal + CPI-deflated). Uses the
 * real bundled InflationService with a pinned "as of" month so all expected
 * values are deterministic and fall inside the official CPI table.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class RealGrowthServiceTest {

    private static final Long USER_ID = 7L;
    private static final YearMonth AS_OF = YearMonth.of(2023, 4); // fully covered by the bundled table

    @Mock
    private PortfolioCalculationService portfolioCalculationService;
    @Mock
    private TransactionRepository transactionRepository;
    @Mock
    private ForexService forexService;

    private InflationService inflationService;
    private RealGrowthService service;

    private final HistoricalNetWorthDTO mar2022 = point(LocalDate.of(2022, 3, 31), "800000");
    private final HistoricalNetWorthDTO dec2022 = point(LocalDate.of(2022, 12, 15), "850000");
    private final HistoricalNetWorthDTO mar2023 = point(LocalDate.of(2023, 3, 31), "900000");

    @BeforeEach
    void setUp() {
        inflationService = new InflationService(null);
        inflationService.loadBundledTable();
        service = new RealGrowthService(
                portfolioCalculationService, inflationService, transactionRepository, forexService);

        when(portfolioCalculationService.calculateYoYGrowth(USER_ID, 5, "INR"))
                .thenReturn(GrowthMetricsDTO.builder()
                        .historicalData(List.of(mar2022, dec2022, mar2023))
                        .currency("INR")
                        .build());
        when(forexService.convert(any(), anyString(), anyString()))
                .thenAnswer(inv -> inv.getArgument(0));
        when(transactionRepository.findByUserIdAndTransactionDateBetween(
                        eq(USER_ID), eq(LocalDate.of(2021, 4, 1)), eq(LocalDate.of(2022, 3, 31))))
                .thenReturn(List.of());
        when(transactionRepository.findByUserIdAndTransactionDateBetween(
                        eq(USER_ID), eq(LocalDate.of(2022, 4, 1)), eq(LocalDate.of(2023, 3, 31))))
                .thenReturn(List.of(
                        Transaction.builder()
                                .type(TransactionType.BUY)
                                .amount(new BigDecimal("50000"))
                                .transactionDate(LocalDate.of(2022, 6, 10))
                                .currency("INR")
                                .build(),
                        Transaction.builder()
                                .type(TransactionType.SELL)
                                .amount(new BigDecimal("120000"))
                                .transactionDate(LocalDate.of(2022, 12, 20))
                                .currency("INR")
                                .build(),
                        Transaction.builder()
                                .type(TransactionType.DIVIDEND)
                                .amount(new BigDecimal("15000"))
                                .transactionDate(LocalDate.of(2023, 2, 15))
                                .currency("INR")
                                .build()));
    }

    private static HistoricalNetWorthDTO point(LocalDate date, String value) {
        return HistoricalNetWorthDTO.builder().date(date).value(new BigDecimal(value)).currency("INR").build();
    }

    @Test
    void deflatesSeriesIntoTodaysRupees() {
        RealGrowthMetricsDTO result = service.getRealGrowthMetrics(USER_ID, 5, AS_OF);

        assertEquals("INR", result.getCurrency());
        assertEquals(3, result.getRealSeries().size());

        RealGrowthPointDTO first = result.getRealSeries().get(0);
        assertEquals(LocalDate.of(2022, 3, 31), first.getDate());
        // 800,000 × 147.8562 / 138.8218 = 852,063
        assertEquals(852063, first.getValue().doubleValue(), 2);
        assertFalse(first.isExtrapolated());

        RealGrowthPointDTO mid = result.getRealSeries().get(1);
        // 850,000 × 147.8562 / 145.7629 = 862,207
        assertEquals(862207, mid.getValue().doubleValue(), 2);

        RealGrowthPointDTO last = result.getRealSeries().get(2);
        // 900,000 × 147.8562 / 146.8646 = 906,076
        assertEquals(906076, last.getValue().doubleValue(), 2);
    }

    @Test
    void fiscalBreakdownReportsInflationAndReturns() {
        RealGrowthMetricsDTO result = service.getRealGrowthMetrics(USER_ID, 5, AS_OF);

        List<FiscalRealBreakdownDTO> rows = result.getFiscalYears();
        assertEquals(2, rows.size());

        // Earliest FY has no prior year → nulls, and only a terminal flow → no XIRR.
        FiscalRealBreakdownDTO fy2122 = rows.get(0);
        assertEquals("FY 21-22", fy2122.getFyLabel());
        assertEquals(LocalDate.of(2022, 3, 31), fy2122.getFyEndDate());
        assertEquals(0, new BigDecimal("800000").compareTo(fy2122.getNominalValue()));
        assertNull(fy2122.getInflationPct());
        assertNull(fy2122.getNominalReturnPct());
        assertNull(fy2122.getRealReturnPct());
        assertFalse(fy2122.getHasCashFlows());
        assertNull(fy2122.getXirrNominalPct());

        // FY 22-23: Mar-2023 / Mar-2022 CPI = +5.79%; nominal +12.50%; real = 1.125/1.0579 − 1 = +6.34%.
        FiscalRealBreakdownDTO fy2223 = rows.get(1);
        assertEquals("FY 22-23", fy2223.getFyLabel());
        assertEquals(5.79, fy2223.getInflationPct().doubleValue(), 0.01);
        assertEquals(12.50, fy2223.getNominalReturnPct().doubleValue(), 0.01);
        assertEquals(6.34, fy2223.getRealReturnPct().doubleValue(), 0.01);
        assertTrue(fy2223.getHasCashFlows());
        assertNotNull(fy2223.getXirrNominalPct());
        assertNotNull(fy2223.getXirrRealPct());
        // Deflating the flows reduces the realized rate under positive inflation.
        assertTrue(fy2223.getXirrRealPct().doubleValue() < fy2223.getXirrNominalPct().doubleValue());
        assertTrue(fy2223.getXirrNominalPct().doubleValue() > 0);
        assertTrue(fy2223.getXirrNominalPct().doubleValue() < 200);
    }

    @Test
    void inflationMetaDescribesBundledSeries() {
        RealGrowthMetricsDTO result = service.getRealGrowthMetrics(USER_ID, 5, AS_OF);

        assertNotNull(result.getInflation());
        assertEquals("2012-01", result.getInflation().getTableStart());
        assertEquals("2023-12", result.getInflation().getTableEnd());
        assertEquals(144, result.getInflation().getMonthCount());
        assertEquals(0, new BigDecimal("4.00").compareTo(result.getInflation().getAssumedAnnualRatePct()));
        assertFalse(result.getInflation().getRefreshedFromApi());
        assertFalse(result.getInflation().getExtrapolatesCurrentData());
    }

    @Test
    void xirrSolvesExactKnownCase() {
        // −100 at t0, +110 exactly one year later → 10% annualized.
        BigDecimal rate = RealGrowthService.xirr(List.of(
                new RealGrowthService.CashFlow(LocalDate.of(2022, 4, 1), new BigDecimal("-100"), "OPENING"),
                new RealGrowthService.CashFlow(LocalDate.of(2023, 4, 1), new BigDecimal("110"), "TERMINAL")));
        assertEquals(0, new BigDecimal("0.100000").compareTo(rate));
    }

    @Test
    void xirrReturnsNullWithoutSignChange() {
        assertNull(RealGrowthService.xirr(List.of(
                new RealGrowthService.CashFlow(LocalDate.of(2022, 4, 1), new BigDecimal("-100"), "A"),
                new RealGrowthService.CashFlow(LocalDate.of(2022, 6, 1), new BigDecimal("-50"), "B"))));
        assertNull(RealGrowthService.xirr(List.of(
                new RealGrowthService.CashFlow(LocalDate.of(2022, 4, 1), new BigDecimal("100"), "A"))));
    }

    @Test
    void fyLabelMatchesFrontendConvention() {
        assertEquals("FY 22-23", RealGrowthService.fyLabel(2023));
        assertEquals("FY 99-00", RealGrowthService.fyLabel(2000));
    }
}
