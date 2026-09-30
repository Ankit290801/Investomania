import { formatCurrency } from './currencyFormatter';
import { CHART_COLORS } from '../config/theme';

/**
 * F1 chart theming: every chart pulls its chrome (grid, axes, tooltip)
 * from the active theme via these factories, so light/dark and the
 * palette stay consistent app-wide.
 */
export const chartGridProps = (theme) => ({
  stroke: theme.palette.divider,
  strokeDasharray: '3 3',
  vertical: false
});

export const chartAxisProps = (theme) => ({
  stroke: 'transparent',
  tick: { fill: theme.palette.text.secondary, fontSize: 11 },
  axisLine: { stroke: theme.palette.divider },
  tickLine: false
});

export const chartTooltipStyle = (theme) => ({
  contentStyle: {
    backgroundColor: theme.palette.background.paper,
    border: `1px solid ${theme.palette.divider}`,
    borderRadius: 10,
    fontSize: '0.78rem',
    boxShadow: '0 8px 24px rgba(0,0,0,0.18)'
  },
  labelStyle: { color: theme.palette.text.primary, fontWeight: 600, marginBottom: 4 },
  itemStyle: { color: theme.palette.text.secondary, padding: '2px 0' },
  cursor: { fill: theme.palette.action.hover }
});

/** Stable colour per series name (falls back through the palette). */
export const seriesColor = (index) => CHART_COLORS[index % CHART_COLORS.length];

/**
 * Shared chart helpers (backlog feature F4 — Portfolio Growth Visualizations).
 *
 * Single source for growth-series math (CAGR, period returns, drawdown) and
 * snapshot-based segment composition so per-page chart code stays thin
 * (modularity doctrine: shared logic is extracted, not duplicated).
 */

/**
 * Convert a date string (yyyy-MM-dd or ISO) to an Indian FY label.
 * Jan–Mar dates belong to the FY ending that year; Apr–Dec to the next.
 */
export const toFYLabel = (dateStr) => {
  const d = new Date(dateStr);
  const month = d.getMonth() + 1; // 1-indexed
  const year = d.getFullYear();
  if (month <= 3) {
    const startYY = String(year - 1).slice(-2);
    const endYY = String(year).slice(-2);
    return `FY ${startYY}-${endYY}`;
  }
  const startYY = String(year).slice(-2);
  const endYY = String(year + 1).slice(-2);
  return `FY ${startYY}-${endYY}`;
};

/**
 * Normalises a growth payload ({date, value, snapshotBased?}[]) into chart
 * rows with FY labels and numeric values, in chronological order.
 */
export const buildGrowthSeries = (data) =>
  (data || [])
    .map((item) => ({
      date: item.date,
      label: item.label || toFYLabel(item.date),
      value: parseFloat(item.value) || 0,
      isSnapshot: !!item.snapshotBased
    }))
    .sort((a, b) => new Date(a.date) - new Date(b.date));

/**
 * Filters a growth series to a window ending at the latest point.
 * years: fractional window length in years (e.g. 1, 3, 5) or null for ALL.
 */
export const filterGrowthSeries = (series, years) => {
  if (!series.length || !years) return series;
  const cutoff = new Date(series[series.length - 1].date);
  cutoff.setFullYear(cutoff.getFullYear() - years);
  return series.filter((p) => new Date(p.date) >= cutoff);
};

/**
 * CAGR over a series: (end/start)^(1/years) - 1. Years derived from the
 * first/last dates; needs ≥ 2 points and a positive start.
 */
export const computeCAGR = (series) => {
  if (!series || series.length < 2) return null;
  const first = series[0];
  const last = series[series.length - 1];
  if (!first.value || first.value <= 0) return null;
  const years = (new Date(last.date) - new Date(first.date)) / (365.25 * 24 * 3600 * 1000);
  if (years <= 0) return null;
  return Math.pow(last.value / first.value, 1 / years) - 1;
};

/**
 * Period returns between consecutive points: [{date, label, returnPct}].
 */
export const computePeriodReturns = (series) => {
  const returns = [];
  for (let i = 1; i < (series || []).length; i++) {
    const prev = series[i - 1];
    const curr = series[i];
    if (prev.value > 0) {
      returns.push({
        date: curr.date,
        label: curr.label,
        returnPct: ((curr.value - prev.value) / prev.value) * 100
      });
    }
  }
  return returns;
};

/**
 * Max drawdown over a series: {maxDrawdownPct, peakDate, troughDate} or null.
 */
export const computeMaxDrawdown = (series) => {
  if (!series || series.length < 2) return null;
  let peak = series[0];
  let worst = null;
  for (const point of series) {
    if (point.value > peak.value) peak = point;
    if (peak.value > 0) {
      const drawdownPct = ((point.value - peak.value) / peak.value) * 100;
      if (!worst || drawdownPct < worst.maxDrawdownPct) {
        worst = { maxDrawdownPct: drawdownPct, peakDate: peak.date, troughDate: point.date };
      }
    }
  }
  return worst;
};

/**
 * Builds stacked segment composition rows from snapshot objects.
 * Each snapshot contributes one row:
 *   { date, label, equity, safe, illiquid, crypto, total }.
 * Segment grouping mirrors PortfolioCalculationService:
 *   equity = EQUITY; safe = FD+RD+PPF+NPS+BOND+savings(CASH);
 *   illiquid = REAL_ESTATE; crypto = CRYPTO.
 */
export const buildSegmentComposition = (snapshots) =>
  (snapshots || [])
    .map((s) => {
      const num = (v) => parseFloat(v) || 0;
      return {
        date: s.snapshotDate,
        label: toFYLabel(s.snapshotDate),
        equity: num(s.equityValue),
        safe: [s.fixedDepositValue, s.rdValue, s.ppfValue, s.npsValue, s.bondValue, s.savingsValue]
          .reduce((acc, v) => acc + num(v), 0),
        illiquid: num(s.realEstateValue),
        crypto: num(s.cryptoValue),
        total: num(s.totalValue)
      };
    })
    .sort((a, b) => new Date(a.date) - new Date(b.date));

/** Compact value formatter for chart axes (₹1.2L / $1.2M style). */
export const formatAxisValue = (value) => {
  if (value == null || Number.isNaN(value)) return '';
  const abs = Math.abs(value);
  if (abs >= 1e7) return `${(value / 1e7).toFixed(1)}Cr`;
  if (abs >= 1e5) return `${(value / 1e5).toFixed(1)}L`;
  if (abs >= 1e3) return `${(value / 1e3).toFixed(1)}K`;
  return String(Math.round(value));
};

/** Tooltip formatter shared by growth charts. */
export const growthTooltipFormatter = (currency) => (value) => formatCurrency(value, currency);
