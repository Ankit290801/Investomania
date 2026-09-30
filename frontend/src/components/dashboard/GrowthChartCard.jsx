import React, { useMemo, useState } from 'react';
import {
  Card,
  CardContent,
  Typography,
  Box,
  Tooltip as MuiTooltip,
  ToggleButtonGroup,
  ToggleButton,
  Divider
} from '@mui/material';
import { useTheme } from '@mui/material/styles';
import {
  AreaChart,
  Area,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  ResponsiveContainer
} from 'recharts';
import CircleIcon from '@mui/icons-material/Circle';
import {
  buildGrowthSeries,
  filterGrowthSeries,
  computeCAGR,
  computeMaxDrawdown,
  formatAxisValue,
  growthTooltipFormatter,
  chartAxisProps,
  chartGridProps,
  chartTooltipStyle
} from '../../utils/chartHelpers';
import { formatCurrency } from '../../utils/currencyFormatter';
import { semanticColor } from '../../config/theme';

const RANGE_OPTIONS = [
  { value: 1, label: '1Y' },
  { value: 3, label: '3Y' },
  { value: 5, label: '5Y' },
  // 'ALL' sentinel instead of null: MUI ToggleButton requires a non-null value.
  { value: 'ALL', label: 'ALL' }
];

/**
 * Portfolio growth card (backlog feature F4): area chart over the growth
 * series with a range selector (1Y/3Y/5Y/ALL), CAGR and max-drawdown stats,
 * and the snapshot-vs-estimated dot legend.
 */
const GrowthChartCard = ({ data, currency = 'INR' }) => {
  const [rangeYears, setRangeYears] = useState('ALL');
  const theme = useTheme();
  const seriesColor = theme.palette.primary.main;

  const fullSeries = useMemo(() => buildGrowthSeries(data), [data]);
  const series = useMemo(
    () => filterGrowthSeries(fullSeries, rangeYears === 'ALL' ? null : rangeYears),
    [fullSeries, rangeYears]
  );
  const cagr = useMemo(() => computeCAGR(series), [series]);
  const drawdown = useMemo(() => computeMaxDrawdown(series), [series]);

  if (fullSeries.length === 0) {
    return (
      <Card sx={{ height: '100%' }}>
        <CardContent>
          <Typography variant="overline" component="div" sx={{ color: 'text.secondary', mb: 0.5 }}>
            Portfolio Growth
          </Typography>
          <Typography color="text.secondary" sx={{ textAlign: 'center', mt: 8 }}>
            No data available. Add investments and transactions to see growth.
          </Typography>
        </CardContent>
      </Card>
    );
  }

  const hasSnapshotData = series.some((d) => d.isSnapshot);
  const first = series[0];
  const last = series[series.length - 1];
  const windowChange =
    first && last && first.value > 0 ? ((last.value - first.value) / first.value) * 100 : null;

  return (
    <Card sx={{ height: '100%' }}>
      <CardContent>
        <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 1, flexWrap: 'wrap', gap: 1 }}>
          <Typography variant="overline" component="div" sx={{ color: 'text.secondary' }}>
            Portfolio Growth
          </Typography>
          <ToggleButtonGroup
            size="small"
            exclusive
            value={rangeYears}
            onChange={(_, v) => v !== null && setRangeYears(v)}
          >
            {RANGE_OPTIONS.map((opt) => (
              <ToggleButton key={String(opt.value)} value={opt.value}>
                {opt.label}
              </ToggleButton>
            ))}
          </ToggleButtonGroup>
        </Box>

        {/* Headline stats */}
        <Box sx={{ display: 'flex', gap: 3, mb: 2, flexWrap: 'wrap' }}>
          <Box>              <Typography variant="caption" color="text.secondary">Latest value</Typography>
            <Typography variant="h6" fontWeight={700} sx={{ fontVariantNumeric: 'tabular-nums' }}>
              {formatCurrency(last?.value || 0, currency)}
            </Typography>
          </Box>
          {windowChange != null && (
            <Box>
              <Typography variant="caption" color="text.secondary">
                Change over window
              </Typography>
              <Typography
                variant="h6"
                fontWeight={700}
                sx={{ color: semanticColor(theme, windowChange >= 0), fontVariantNumeric: 'tabular-nums' }}
              >
                {windowChange >= 0 ? '+' : '-'}
                {formatCurrency(Math.abs(last.value - first.value), currency)}{' '}
                ({windowChange >= 0 ? '+' : ''}
                {windowChange.toFixed(1)}%)
              </Typography>
            </Box>
          )}
          {cagr != null && (
            <Box>
              <Typography variant="caption" color="text.secondary">CAGR</Typography>
              <Typography variant="h6" fontWeight={700}>
                {(cagr * 100).toFixed(1)}%
              </Typography>
            </Box>
          )}
          {drawdown && drawdown.maxDrawdownPct < 0 && (
            <MuiTooltip title={`Peak ${drawdown.peakDate} → trough ${drawdown.troughDate}`}>
              <Box>
                <Typography variant="caption" color="text.secondary">Max drawdown</Typography>
                <Typography variant="h6" fontWeight={700} color="error.main">
                  {drawdown.maxDrawdownPct.toFixed(1)}%
                </Typography>
              </Box>
            </MuiTooltip>
          )}
        </Box>

        <ResponsiveContainer width="100%" height={300}>
          <AreaChart data={series}>
            <defs>
              <linearGradient id="growthFill" x1="0" y1="0" x2="0" y2="1">
                <stop offset="0%" stopColor={seriesColor} stopOpacity={0.3} />
                <stop offset="100%" stopColor={seriesColor} stopOpacity={0.02} />
              </linearGradient>
            </defs>
            <CartesianGrid {...chartGridProps(theme)} />
            <XAxis dataKey="label" {...chartAxisProps(theme)} />
            <YAxis tickFormatter={formatAxisValue} width={64} {...chartAxisProps(theme)} />
            <Tooltip formatter={growthTooltipFormatter(currency)} {...chartTooltipStyle(theme)} />
            <Area
              type="monotone"
              dataKey="value"
              stroke={seriesColor}
              strokeWidth={2}
              fill="url(#growthFill)"
              name="Portfolio Value"
              dot={(props) => {
                const { cx, cy, payload } = props;
                if (!payload.isSnapshot && !hasSnapshotData) return <g key={`d-${cx}-${cy}`} />;
                const color = payload.isSnapshot
                  ? theme.palette.success.main
                  : theme.palette.warning.main;
                return (
                  <circle key={`dot-${cx}-${cy}`} cx={cx} cy={cy} r={4} fill={color} stroke={color} />
                );
              }}
            />
          </AreaChart>
        </ResponsiveContainer>

        {hasSnapshotData && (
          <>
            <Divider sx={{ my: 1 }} />
            <Box sx={{ display: 'flex', alignItems: 'center', gap: 2 }}>
              <MuiTooltip title="Snapshot-based (accurate market data)">
                <Box sx={{ display: 'flex', alignItems: 'center', gap: 0.5 }}>
                  <CircleIcon sx={{ fontSize: 10, color: 'success.main' }} />
                  <Typography variant="caption" color="text.secondary">Snapshot</Typography>
                </Box>
              </MuiTooltip>
              <MuiTooltip title="Estimated (no historical snapshot available)">
                <Box sx={{ display: 'flex', alignItems: 'center', gap: 0.5 }}>
                  <CircleIcon sx={{ fontSize: 10, color: 'warning.main' }} />
                  <Typography variant="caption" color="text.secondary">Estimated</Typography>
                </Box>
              </MuiTooltip>
            </Box>
          </>
        )}
      </CardContent>
    </Card>
  );
};

export default GrowthChartCard;
