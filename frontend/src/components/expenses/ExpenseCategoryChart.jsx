import React from 'react';
import { useTheme } from '@mui/material/styles';
import { Card, CardContent, Typography, Box } from '@mui/material';
import { PieChart, Pie, Cell, ResponsiveContainer, Tooltip } from 'recharts';
import { formatCurrency } from '../../utils/currencyFormatter';
import { chartTooltipStyle, seriesColor } from '../../utils/chartHelpers';

/**
 * F1: expense-by-category donut with themed chrome and a dense
 * value/percentage legend (shared style with SegmentBreakdownCard).
 */
const ExpenseCategoryChart = ({ categoryBreakdown, currency = 'INR' }) => {
  const theme = useTheme();

  if (!categoryBreakdown || Object.keys(categoryBreakdown).length === 0) {
    return (
      <Card sx={{ height: '100%' }}>
        <CardContent>
          <Typography variant="overline" component="div" sx={{ color: 'text.secondary', mb: 0.5 }}>
            Expense by Category
          </Typography>
          <Typography color="text.secondary" sx={{ textAlign: 'center', mt: 8 }}>
            No expense data available
          </Typography>
        </CardContent>
      </Card>
    );
  }

  const chartData = Object.entries(categoryBreakdown)
    .filter(([_, value]) => value > 0)
    .map(([name, value]) => ({
      name: name.replace('_', ' '),
      value: parseFloat(value)
    }))
    .sort((a, b) => b.value - a.value);

  const total = chartData.reduce((acc, d) => acc + d.value, 0);

  return (
    <Card sx={{ height: '100%' }}>
      <CardContent>
        <Typography variant="overline" component="div" sx={{ color: 'text.secondary', mb: 0.5 }}>
          Expense by Category
        </Typography>
        <Box sx={{ display: 'flex', alignItems: 'center', flexWrap: 'wrap' }}>
          <Box sx={{ width: 170, height: 170, flexShrink: 0 }}>
            <ResponsiveContainer width="100%" height="100%">
              <PieChart>
                <Pie
                  data={chartData}
                  cx="50%"
                  cy="50%"
                  innerRadius={48}
                  outerRadius={78}
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
          </Box>
          <Box sx={{ flex: 1, minWidth: 120, ml: 1 }}>
            {chartData.map((entry, index) => (
              <Box
                key={entry.name}
                sx={{ display: 'flex', alignItems: 'center', gap: 1, py: 0.4, minWidth: 0 }}
              >
                <Box
                  sx={{ width: 9, height: 9, borderRadius: '50%', backgroundColor: seriesColor(index), flexShrink: 0 }}
                />
                <Typography variant="caption" color="text.secondary" sx={{ minWidth: 0 }} noWrap>
                  {entry.name.toLowerCase()}
                </Typography>
                <Typography
                  variant="caption"
                  sx={{ ml: 'auto', fontWeight: 600, fontVariantNumeric: 'tabular-nums', whiteSpace: 'nowrap' }}
                >
                  {formatCurrency(entry.value, currency)}
                </Typography>
                <Typography
                  variant="caption"
                  color="text.secondary"
                  sx={{ fontVariantNumeric: 'tabular-nums', width: 38, textAlign: 'right' }}
                >
                  {total > 0 ? `${((entry.value / total) * 100).toFixed(0)}%` : ''}
                </Typography>
              </Box>
            ))}
          </Box>
        </Box>
      </CardContent>
    </Card>
  );
};

export default ExpenseCategoryChart;
