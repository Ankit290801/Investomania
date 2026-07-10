import React from 'react';
import { Card, CardContent, Typography } from '@mui/material';
import { LineChart, Line, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer } from 'recharts';
import { formatCurrency } from '../../utils/currencyFormatter';

const ExpenseMonthlyTrendChart = ({ monthlyTrend, currency = 'INR' }) => {
  if (!monthlyTrend || Object.keys(monthlyTrend).length === 0) {
    return (
      <Card sx={{ height: '100%' }}>
        <CardContent>
          <Typography color="text.secondary" gutterBottom variant="h6">
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
      amount: parseFloat(value),
    }))
    .sort((a, b) => a.month.localeCompare(b.month));

  return (
    <Card sx={{ height: '100%' }}>
      <CardContent>
        <Typography color="text.secondary" gutterBottom variant="h6">
          Monthly Expense Trend
        </Typography>
        <ResponsiveContainer width="100%" height={300}>
          <LineChart data={chartData}>
            <CartesianGrid strokeDasharray="3 3" />
            <XAxis dataKey="month" />
            <YAxis />
            <Tooltip formatter={(value) => formatCurrency(value, currency)} />
            <Line
              type="monotone"
              dataKey="amount"
              stroke="#8884d8"
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
