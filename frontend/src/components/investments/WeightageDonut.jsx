import React from 'react';
import { useTheme } from '@mui/material/styles';
import { Card, CardContent, Typography, Box } from '@mui/material';
import { PieChart, Pie, Cell, ResponsiveContainer, Tooltip } from 'recharts';
import { formatCurrency } from '../../utils/currencyFormatter';
import { chartTooltipStyle, seriesColor } from '../../utils/chartHelpers';

/**
 * E2 — shared weightage donut used across asset-class sub-screens.
 * Same visual language as the F1 SegmentBreakdownCard: flat donut,
 * centred total, dense percentage list instead of a detached legend.
 * `title` names the breakdown ("Portfolio Weightage", "Sectoral
 * Weightage", "Market Cap Weightage", ...).
 */
const WeightageDonut = ({ title, data, currency = 'INR', height = 190 }) => {
  const theme = useTheme();

  const chartData = (data || [])
    .filter((d) => d.value > 0)
    .sort((a, b) => b.value - a.value);

  const total = chartData.reduce((acc, d) => acc + d.value, 0);

  return (
    <Card sx={{ height: '100%' }}>
      <CardContent>
        <Typography variant="overline" component="div" sx={{ color: 'text.secondary', mb: 0.5 }}>
          {title}
        </Typography>
        {chartData.length === 0 ? (
          <Typography color="text.secondary" sx={{ textAlign: 'center', mt: 6, mb: 4 }}>
            No data to display yet.
          </Typography>
        ) : (
          <>
            <Box sx={{ position: 'relative', height }}>
              <ResponsiveContainer width="100%" height="100%">
                <PieChart>
                  <Pie
                    data={chartData}
                    cx="50%"
                    cy="50%"
                    innerRadius={Math.round(height * 0.3)}
                    outerRadius={Math.round(height * 0.44)}
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
            <Box sx={{ mt: 1.5, display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 0.75 }}>
              {chartData.map((entry, index) => (
                <Box key={entry.name} sx={{ display: 'flex', alignItems: 'center', gap: 1, minWidth: 0 }}>
                  <Box
                    sx={{ width: 9, height: 9, borderRadius: '50%', backgroundColor: seriesColor(index), flexShrink: 0 }}
                  />
                  <Typography
                    variant="caption"
                    color="text.secondary"
                    sx={{ textTransform: 'capitalize', minWidth: 0, overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}
                  >
                    {entry.name.replace(/_/g, ' ').toLowerCase()}
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
          </>
        )}
      </CardContent>
    </Card>
  );
};

export default WeightageDonut;
