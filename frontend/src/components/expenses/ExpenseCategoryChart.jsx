import React from 'react';
import { Card, CardContent, Typography } from '@mui/material';
import { PieChart, Pie, Cell, ResponsiveContainer, Legend, Tooltip } from 'recharts';
import { formatCurrency } from '../../utils/currencyFormatter';

const COLORS = ['#0088FE', '#00C49F', '#FFBB28', '#FF8042', '#8884D8', '#82CA9D', '#FFC658', '#FF6B9D', '#C4C4C4'];

const ExpenseCategoryChart = ({ categoryBreakdown, currency = 'INR' }) => {
  if (!categoryBreakdown || Object.keys(categoryBreakdown).length === 0) {
    return (
      <Card sx={{ height: '100%' }}>
        <CardContent>
          <Typography color="text.secondary" gutterBottom variant="h6">
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
      value: parseFloat(value),
    }))
    .sort((a, b) => b.value - a.value);

  return (
    <Card sx={{ height: '100%' }}>
      <CardContent>
        <Typography color="text.secondary" gutterBottom variant="h6">
          Expense by Category
        </Typography>
        <ResponsiveContainer width="100%" height={300}>
          <PieChart>
            <Pie
              data={chartData}
              cx="50%"
              cy="50%"
              labelLine={false}
              label={({ name, percent }) => `${name} ${(percent * 100).toFixed(0)}%`}
              outerRadius={80}
              fill="#8884d8"
              dataKey="value"
            >
              {chartData.map((entry, index) => (
                <Cell key={`cell-${index}`} fill={COLORS[index % COLORS.length]} />
              ))}
            </Pie>
            <Tooltip formatter={(value) => formatCurrency(value, currency)} />
            <Legend />
          </PieChart>
        </ResponsiveContainer>
      </CardContent>
    </Card>
  );
};

export default ExpenseCategoryChart;
