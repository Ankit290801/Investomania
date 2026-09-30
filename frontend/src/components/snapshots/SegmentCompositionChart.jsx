import React, { useMemo, useState } from 'react';
import {
  Card,
  CardContent,
  Typography,
  Box,
  ToggleButtonGroup,
  ToggleButton
} from '@mui/material';
import { useTheme } from '@mui/material/styles';
import {
  BarChart,
  Bar,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  ResponsiveContainer,
  Legend
} from 'recharts';
import {
  buildSegmentComposition,
  formatAxisValue,
  growthTooltipFormatter,
  chartAxisProps,
  chartGridProps,
  chartTooltipStyle,
  seriesColor
} from '../../utils/chartHelpers';

const SEGMENTS = [
  { key: 'equity', name: 'Equity' },
  { key: 'safe', name: 'Safe Assets (FD/RD/PPF/NPS/Bond/Cash)' },
  { key: 'illiquid', name: 'Illiquid (Real Estate)' },
  { key: 'crypto', name: 'Crypto' }
];

const RANGE_OPTIONS = [
  { value: 3, label: '3Y' },
  { value: 5, label: '5Y' },
  { value: null, label: 'ALL' }
];

/**
 * Stacked segment composition over time (backlog feature F4): one stacked bar
 * per snapshot, showing how equity / safe / illiquid / crypto allocations
 * evolved between FY-ends.
 */
const SegmentCompositionChart = ({ snapshots, currency = 'INR' }) => {
  const [rangeYears, setRangeYears] = useState(null);
  const theme = useTheme();

  const fullSeries = useMemo(() => buildSegmentComposition(snapshots), [snapshots]);
  const series = useMemo(() => {
    if (!rangeYears || fullSeries.length === 0) return fullSeries;
    const cutoff = new Date(fullSeries[fullSeries.length - 1].date);
    cutoff.setFullYear(cutoff.getFullYear() - rangeYears);
    return fullSeries.filter((p) => new Date(p.date) >= cutoff);
  }, [fullSeries, rangeYears]);

  if (fullSeries.length === 0) {
    return null; // The parent page already renders an empty state for no snapshots.
  }

  return (
    <Card sx={{ mt: 3 }}>
      <CardContent>
        <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 2, flexWrap: 'wrap', gap: 1 }}>
          <Typography variant="overline" component="div" sx={{ color: 'text.secondary' }}>
            Portfolio Composition Over Time
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
        <ResponsiveContainer width="100%" height={320}>
          <BarChart data={series}>
            <CartesianGrid {...chartGridProps(theme)} />
            <XAxis dataKey="label" {...chartAxisProps(theme)} />
            <YAxis tickFormatter={formatAxisValue} width={64} {...chartAxisProps(theme)} />
            <Tooltip formatter={growthTooltipFormatter(currency)} {...chartTooltipStyle(theme)} />
            <Legend iconType="circle" iconSize={8} wrapperStyle={{ fontSize: 11 }} />
            {SEGMENTS.map((seg, i) => (
              <Bar
                key={seg.key}
                dataKey={seg.key}
                name={seg.name}
                stackId="composition"
                fill={seriesColor(i)}
              />
            ))}
          </BarChart>
        </ResponsiveContainer>
      </CardContent>
    </Card>
  );
};

export default SegmentCompositionChart;
