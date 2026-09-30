import React from 'react';
import { useTheme } from '@mui/material/styles';
import { Card, CardContent, Typography, Box } from '@mui/material';
import { PieChart, Pie, Cell, ResponsiveContainer, Legend, Tooltip } from 'recharts';
import { formatCurrency } from '../../utils/currencyFormatter';
import { chartAxisProps, chartTooltipStyle, seriesColor } from '../../utils/chartHelpers';

/**
 * F1: segment breakdown as a flat donut with themed chrome, centred
 * total, and a right-side percentage list — denser and calmer than a
 * labelled pie with a detached legend.
 */
const SegmentBreakdownCard = ({ data, currency = 'INR' }) => {
  const theme = useTheme();

  const chartData = Object.entries(data || {})
    .filter(([_, value]) => value > 0)
    .map(([name, value]) => ({
      name,
      value: parseFloat(value)
    }))
    .sort((a, b) => b.value - a.value);

  const total = chartData.reduce((acc, d) => acc + d.value, 0);

  if (chartData.length === 0) {
    return (
      <Card sx={{ height: '100%' }}>
        <CardContent>
          <Typography variant="overline" component="div" sx={{ color: 'text.secondary', mb: 0.5 }}>
            Segment Breakdown
          </Typography>
          <Typography color="text.secondary" sx={{ textAlign: 'center', mt: 8 }}>
            No data available. Add investments to see segment breakdown.
          </Typography>
        </CardContent>
      </Card>
    );
  }

  return (
    <Card sx={{ height: '100%' }}>
      <CardContent>
        <Typography variant="overline" component="div" sx={{ color: 'text.secondary', mb: 0.5 }}>
          Segment Breakdown
        </Typography>
        <Box sx={{ position: 'relative', height: 210 }}>
          <ResponsiveContainer width="100%" height="100%">
            <PieChart>
              <Pie
                data={chartData}
                cx="50%"
                cy="50%"
                innerRadius={62}
                outerRadius={92}
                paddingAngle={2}
                dataKey="value"
                stroke="none"
              >
                {chartData.map((entry, index) => (
                  <Cell key={`cell-${index}`} fill={seriesColor(index)} />
                ))}
              </Pie>
              <Tooltip
                formatter={(value) => formatCurrency(value, currency)}
                {...chartTooltipStyle(theme)}
              />
            </PieChart>
          </ResponsiveContainer>
          <Box
            sx={{
              position: 'absolute',
              inset: 0,
              display: 'flex',
              flexDirection: 'column',
              alignItems: 'center',
              justifyContent: 'center',
              pointerEvents: 'none'
            }}
          >
            <Typography variant="caption" color="text.secondary">
              Total
            </Typography>
            <Typography
              variant="subtitle1"
              fontWeight={700}
              sx={{ fontVariantNumeric: 'tabular-nums' }}
            >
              {formatCurrency(total, currency)}
            </Typography>
          </Box>
        </Box>
        {/* Dense percentage list replaces the default legend */}
        <Box sx={{ mt: 1.5, display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 0.75 }}>
          {chartData.map((entry, index) => (
            <Box key={entry.name} sx={{ display: 'flex', alignItems: 'center', gap: 1, minWidth: 0 }}>
              <Box
                sx={{ width: 9, height: 9, borderRadius: '50%', backgroundColor: seriesColor(index), flexShrink: 0 }}
              />
              <Typography variant="caption" color="text.secondary" sx={{ textTransform: 'capitalize', minWidth: 0 }}>
                {entry.name.replace('_', ' ').toLowerCase()}
              </Typography>
              <Typography
                variant="caption"
                sx={{ ml: 'auto', fontWeight: 600, fontVariantNumeric: 'tabular-nums' }}
              >
                {total > 0 ? ((entry.value / total) * 100).toFixed(1) : 0}%
              </Typography>
            </Box>
          ))}
        </Box>
      </CardContent>
    </Card>
  );
};

export default SegmentBreakdownCard;
