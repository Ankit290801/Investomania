import React from 'react';
import { useTheme } from '@mui/material/styles';
import { Card, CardContent, Typography } from '@mui/material';
import { LineChart, Line, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer } from 'recharts';
import { formatCurrency } from '../../utils/currencyFormatter';
import {
  chartAxisProps,
  chartGridProps,
  chartTooltipStyle,
  formatAxisValue
} from '../../utils/chartHelpers';

/**
 * F1: monthly expense trend on themed chart chrome with the primary
 * series colour from the design system (never a raw hex here).
 */
const ExpenseMonthlyTrendChart = ({ monthlyTrend, currency = 'INR' }) => {
  const theme = useTheme();

  if (!monthlyTrend || Object.keys(monthlyTrend).length === 0) {
    return (
      <Card sx={{ height: '100%' }}>
        <CardContent>
          <Typography variant="overline" component="div" sx={{ color: 'text.secondary', mb: 0.5 }}>
            Monthly Expense Trend
          </Typography>
          <Typography color="text.secondary" sx={{ textAlign: 'center', mt: 8 }}>
            No trend data available
          </Typography>
        </CardContent>
      </Card>
    );
  }

  const chartData = Object.entries(monthlyTrend)
    .map(([month, value]) => ({
      month,
      amount: parseFloat(value)
    }))
    .sort((a, b) => a.month.localeCompare(b.month));

  return (
    <Card sx={{ height: '100%' }}>
      <CardContent>
        <Typography variant="overline" component="div" sx={{ color: 'text.secondary', mb: 0.5 }}>
          Monthly Expense Trend
        </Typography>
        <ResponsiveContainer width="100%" height={300}>
          <LineChart data={chartData}>
            <CartesianGrid {...chartGridProps(theme)} />
            <XAxis dataKey="month" {...chartAxisProps(theme)} />
            <YAxis {...chartAxisProps(theme)} tickFormatter={formatAxisValue} />
            <Tooltip formatter={(value) => formatCurrency(value, currency)} {...chartTooltipStyle(theme)} />
            <Line
              type="monotone"
              dataKey="amount"
              stroke={theme.palette.primary.main}
              strokeWidth={2}
              name="Expenses"
            />
          </LineChart>
        </ResponsiveContainer>
      </CardContent>
    </Card>
  );
};

export default ExpenseMonthlyTrendChart;
