package com.investment.tracker.service;

import com.investment.tracker.dto.FiscalRealBreakdownDTO;
import com.investment.tracker.dto.GrowthMetricsDTO;
import com.investment.tracker.dto.HistoricalNetWorthDTO;
import com.investment.tracker.dto.InflationMetaDTO;
import com.investment.tracker.dto.RealGrowthMetricsDTO;
import com.investment.tracker.dto.RealGrowthPointDTO;
import com.investment.tracker.model.Transaction;
import com.investment.tracker.model.TransactionType;
import com.investment.tracker.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * E13 — real (inflation-adjusted) portfolio growth, per fiscal.
 *
 * Scope agreed with the stakeholder on October 3, 2026 (IMPLEMENTATION_TRACKER.md
 * → E13): FY-end values restated to <em>today's rupees</em> via the bundled
 * monthly CPI, a per-FY breakdown (nominal, real, Y-o-Y inflation, nominal and
 * real returns) and realized FY XIRR reported both nominal and CPI-deflated.
 * Real figures are always computed in INR — the portfolio series is requested
 * at currency=INR and every transaction is converted before use — so the
 * dashboard currency selector does not affect them.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class RealGrowthService {

    private final PortfolioCalculationService portfolioCalculationService;
    private final InflationService inflationService;
    private final TransactionRepository transactionRepository;
    private final ForexService forexService;

    public RealGrowthMetricsDTO getRealGrowthMetrics(Long userId, int years) {
        return getRealGrowthMetrics(userId, years, YearMonth.now());
    }

    /** {@code asOfMonth} is injectable so tests are deterministic. */
    RealGrowthMetricsDTO getRealGrowthMetrics(Long userId, int years, YearMonth asOfMonth) {
        GrowthMetricsDTO nominal = portfolioCalculationService.calculateYoYGrowth(userId, years, "INR");
        List<HistoricalNetWorthDTO> points = nominal.getHistoricalData() == null
                ? Collections.emptyList() : nominal.getHistoricalData();

        List<RealGrowthPointDTO> realSeries = new ArrayList<>();
        for (HistoricalNetWorthDTO p : points) {
            if (p.getDate() == null || p.getValue() == null) continue;
            YearMonth month = YearMonth.from(p.getDate());
            BigDecimal deflator = inflationService.getDeflator(month, asOfMonth);
            realSeries.add(RealGrowthPointDTO.builder()
                    .date(p.getDate())
                    .value(p.getValue().multiply(deflator).setScale(2, RoundingMode.HALF_UP))
                    .nominalValue(p.getValue())
                    .deflator(deflator)
                    .extrapolated(inflationService.isExtrapolated(month))
                    .currency("INR")
                    .build());
        }
        realSeries.sort(Comparator.comparing(RealGrowthPointDTO::getDate));

        List<FiscalRealBreakdownDTO> fiscalYears = buildFiscalBreakdown(userId, realSeries, asOfMonth);

        return RealGrowthMetricsDTO.builder()
                .realSeries(realSeries)
                .fiscalYears(fiscalYears)
                .inflation(buildMeta(asOfMonth))
                .currency("INR")
                .build();
    }

    private InflationMetaDTO buildMeta(YearMonth asOfMonth) {
        return InflationMetaDTO.builder()
                .source(InflationService.SOURCE_LABEL)
                .seriesNote(InflationService.SERIES_NOTE)
                .tableStart(inflationService.getFirstCoveredMonth().toString())
                .tableEnd(inflationService.getLastCoveredMonth().toString())
                .monthCount(inflationService.getCoveredMonthCount())
                .assumedAnnualRatePct(InflationService.RBI_TARGET_MIDPOINT
                        .multiply(BigDecimal.valueOf(100)).setScale(2, RoundingMode.HALF_UP))
                .refreshedFromApi(inflationService.isRefreshedFromApi())
                .extrapolatesCurrentData(inflationService.isExtrapolated(asOfMonth))
                .build();
    }

    /**
     * Per-FY roll-up over the deflated series. The FY-end representative is the
     * latest point dated on or before 31 March of each fiscal year; the current
     * (incomplete) FY — whose fiscal end is still in the future — is excluded.
     */
    private List<FiscalRealBreakdownDTO> buildFiscalBreakdown(
            Long userId, List<RealGrowthPointDTO> sortedSeries, YearMonth asOfMonth) {
        if (sortedSeries.isEmpty()) return Collections.emptyList();

        LocalDate asOfDate = asOfMonth.atEndOfMonth();
        Map<Integer, RealGrowthPointDTO> fyEndByYear = new TreeMap<>();
        for (RealGrowthPointDTO p : sortedSeries) {
            int endYear = p.getDate().getMonthValue() <= 3 ? p.getDate().getYear() : p.getDate().getYear() + 1;
            if (LocalDate.of(endYear, 3, 31).isAfter(asOfDate)) continue; // incomplete FY
            fyEndByYear.put(endYear, p); // sorted series → last point ≤ FY end wins
        }

        List<Map.Entry<Integer, RealGrowthPointDTO>> entries = new ArrayList<>(fyEndByYear.entrySet());
        List<FiscalRealBreakdownDTO> rows = new ArrayList<>();
        for (int i = 0; i < entries.size(); i++) {
            int endYear = entries.get(i).getKey();
            RealGrowthPointDTO point = entries.get(i).getValue();
            LocalDate fyStart = LocalDate.of(endYear - 1, 4, 1);
            LocalDate fyEnd = LocalDate.of(endYear, 3, 31);

            RealGrowthPointDTO previous = i > 0 ? entries.get(i - 1).getValue() : null;
            int previousEndYear = i > 0 ? entries.get(i - 1).getKey() : -1;

            FiscalRealBreakdownDTO.FiscalRealBreakdownDTOBuilder builder = FiscalRealBreakdownDTO.builder()
                    .fyLabel(fyLabel(endYear))
                    .fyEndDate(point.getDate())
                    .nominalValue(point.getNominalValue())
                    .realValue(point.getValue());

            if (previous != null) {
                // CPI inflation is portfolio-independent — always reported when
                // a prior fiscal year exists, even if the portfolio was empty.
                BigDecimal inflation = inflationService.getEffectiveIndex(YearMonth.of(endYear, 3))
                        .divide(inflationService.getEffectiveIndex(YearMonth.of(previousEndYear, 3)),
                                6, RoundingMode.HALF_UP)
                        .subtract(BigDecimal.ONE);
                builder.inflationPct(asPct(inflation));
                if (previous.getValue().signum() > 0 && previous.getNominalValue().signum() > 0) {
                    BigDecimal nominalReturn = point.getNominalValue()
                            .divide(previous.getNominalValue(), 6, RoundingMode.HALF_UP)
                            .subtract(BigDecimal.ONE);
                    BigDecimal realReturn = point.getValue()
                            .divide(previous.getValue(), 6, RoundingMode.HALF_UP)
                            .subtract(BigDecimal.ONE);
                    builder.nominalReturnPct(asPct(nominalReturn))
                            .realReturnPct(asPct(realReturn));
                }
            }

            XirrResult xirr = computeFyXirr(userId, fyStart, fyEnd, point, previous, asOfMonth);
            builder.hasCashFlows(xirr != null && xirr.hasCashFlows)
                    .xirrNominalPct(xirr == null ? null : asPct(xirr.nominal))
                    .xirrRealPct(xirr == null ? null : asPct(xirr.real));

            // Skip noise rows: an FY with a zero FY-end value, a zero (or absent)
            // prior value, and no realized cash flows says nothing about growth.
            boolean meaningful = point.getNominalValue().signum() > 0
                    || (previous != null && previous.getNominalValue().signum() > 0)
                    || (xirr != null && xirr.hasCashFlows);
            if (meaningful) {
                rows.add(builder.build());
            }
        }
        return rows;
    }

    /** "FY 22-23" for a fiscal year ending 31 March 2023 (matches frontend toFYLabel). */
    static String fyLabel(int endYear) {
        return String.format("FY %02d-%02d", (endYear - 1) % 100, endYear % 100);
    }

    private static BigDecimal asPct(BigDecimal fraction) {
        return fraction.multiply(BigDecimal.valueOf(100)).setScale(2, RoundingMode.HALF_UP);
    }

    // ------------------------------------------------------------------
    // Realized FY XIRR
    // ------------------------------------------------------------------

    static final class CashFlow {
        final LocalDate date;
        final BigDecimal amount;
        final String tag;

        CashFlow(LocalDate date, BigDecimal amount, String tag) {
            this.date = date;
            this.amount = amount;
            this.tag = tag;
        }
    }

    static final class XirrResult {
        final BigDecimal nominal;
        final BigDecimal real;
        final boolean hasCashFlows;

        XirrResult(BigDecimal nominal, BigDecimal real, boolean hasCashFlows) {
            this.nominal = nominal;
            this.real = real;
            this.hasCashFlows = hasCashFlows;
        }
    }

    /**
     * Realized XIRR for one fiscal year: the opening position (latest point
     * before the FY starts) as the initial outflow, the FY's investment cash
     * flows (BUY/CONTRIBUTION out; SELL/WITHDRAWAL/DIVIDEND/INTEREST in), and
     * the FY-end portfolio value as the terminal inflow. Cash-account
     * operations (SALARY/EXPENSE/DEPOSIT/TRANSFER_*) and non-cash corporate
     * actions (BONUS/SPLIT/MERGER) are not investment flows and are skipped.
     * The real variant restates every flow in today's rupees before the rate
     * search. Returns null when fewer than two usable flows exist.
     */
    private XirrResult computeFyXirr(Long userId, LocalDate fyStart, LocalDate fyEnd,
                                     RealGrowthPointDTO fyEndPoint, RealGrowthPointDTO priorPoint,
                                     YearMonth asOfMonth) {
        List<CashFlow> flows = new ArrayList<>();
        if (priorPoint != null && priorPoint.getNominalValue().signum() > 0) {
            flows.add(new CashFlow(fyStart, priorPoint.getNominalValue().negate(), "OPENING"));
        }

        boolean anyTxn = false;
        List<Transaction> txns =
                transactionRepository.findByUserIdAndTransactionDateBetween(userId, fyStart, fyEnd);
        if (txns != null) {
            for (Transaction t : txns) {
                if (t.getType() == null || t.getAmount() == null
                        || t.getTransactionDate() == null || t.getCurrency() == null) continue;
                BigDecimal inr;
                switch (t.getType()) {
                    case BUY:
                    case CONTRIBUTION:
                        inr = toInr(t).negate();
                        break;
                    case SELL:
                    case WITHDRAWAL:
                    case DIVIDEND:
                    case INTEREST:
                        inr = toInr(t);
                        break;
                    default:
                        continue; // not an investment cash flow
                }
                anyTxn = true;
                flows.add(new CashFlow(t.getTransactionDate(), inr, t.getType().name()));
            }
        }

        if (fyEndPoint != null && fyEndPoint.getNominalValue().signum() > 0) {
            flows.add(new CashFlow(fyEndPoint.getDate(), fyEndPoint.getNominalValue(), "TERMINAL"));
        }
        if (flows.size() < 2) return null;

        BigDecimal nominalXirr = xirr(flows);
        if (nominalXirr == null) return null;

        List<CashFlow> realFlows = new ArrayList<>();
        for (CashFlow f : flows) {
            realFlows.add(new CashFlow(f.date,
                    inflationService.deflate(f.amount, YearMonth.from(f.date), asOfMonth), f.tag));
        }
        BigDecimal realXirr = xirr(realFlows);

        return new XirrResult(nominalXirr, realXirr, anyTxn);
    }

    private BigDecimal toInr(Transaction t) {
        try {
            return forexService.convert(t.getAmount(), t.getCurrency(), "INR");
        } catch (Exception e) {
            // Defensive: if a stale/refreshing currency pair throws (e.g. a
            // duplicate-row constraint fault that survived the upsert fix),
            // degrade to the nominal amount rather than take down the whole
            // real-growth call. Logged at warn so it is visible without… creating
            // a cascade for the rest of the series.
            log.warn("Forex convert failed for {} ({}) -> INR: {}",
                    t.getType(), t.getAmount(), e.getMessage());
            return t.getAmount();
        }
    }

    /**
     * Annualized IRR (ACT/365) via bisection on rate ∈ [-0.9999, 10].
     * Bisection over a sign-change bracket is unconditionally stable for the
     * monotone NPV function; returns null when the flows lack a sign change
     * (no root) or the bracket fails.
     */
    static BigDecimal xirr(List<CashFlow> flows) {
        if (flows == null || flows.size() < 2) return null;
        List<CashFlow> sorted = new ArrayList<>(flows);
        sorted.sort(Comparator.comparing(f -> f.date));

        boolean hasNegative = false;
        boolean hasPositive = false;
        for (CashFlow f : sorted) {
            int s = f.amount.signum();
            if (s < 0) hasNegative = true;
            if (s > 0) hasPositive = true;
        }
        if (!hasNegative || !hasPositive) return null;

        LocalDate t0 = sorted.get(0).date;
        int n = sorted.size();
        double[] amounts = new double[n];
        double[] years = new double[n];
        for (int i = 0; i < n; i++) {
            amounts[i] = sorted.get(i).amount.doubleValue();
            years[i] = ChronoUnit.DAYS.between(t0, sorted.get(i).date) / 365.0;
        }

        double lo = -0.9999;
        double hi = 10.0;
        double fLo = npv(amounts, years, lo);
        double fHi = npv(amounts, years, hi);
        if (fLo == 0.0) return BigDecimal.valueOf(lo).setScale(6, RoundingMode.HALF_UP);
        if (fHi == 0.0) return BigDecimal.valueOf(hi).setScale(6, RoundingMode.HALF_UP);
        if ((fLo > 0) == (fHi > 0)) return null; // no sign change → no root in range

        for (int i = 0; i < 200; i++) {
            double mid = (lo + hi) / 2.0;
            double fMid = npv(amounts, years, mid);
            if (fMid == 0.0) break;
            if ((fMid > 0) == (fLo > 0)) {
                lo = mid;
                fLo = fMid;
            } else {
                hi = mid;
            }
            if (hi - lo < 1e-10) break;
        }
        return BigDecimal.valueOf((lo + hi) / 2.0).setScale(6, RoundingMode.HALF_UP);
    }

    /** NPV of the flows discounted at annual rate {@code r}: Σ aᵢ·(1+r)^(−tᵢ). */
    private static double npv(double[] amounts, double[] years, double r) {
        double sum = 0.0;
        for (int i = 0; i < amounts.length; i++) {
            sum += amounts[i] / Math.pow(1 + r, years[i]);
        }
        return sum;
    }
}
