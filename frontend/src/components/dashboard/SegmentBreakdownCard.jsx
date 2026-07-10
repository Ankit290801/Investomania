import React from 'react';
import { Card, CardContent, Typography } from '@mui/material';
import { PieChart, Pie, Cell, ResponsiveContainer, Legend, Tooltip } from 'recharts';
import { formatCurrency } from '../../utils/currencyFormatter';

const COLORS = ['#0088FE', '#00C49F', '#FFBB28', '#FF8042'];

const SegmentBreakdownCard = ({ data, currency = 'INR' }) => {
  // Convert object to array format for Recharts
  const chartData = Object.entries(data || {})
    .filter(([_, value]) => value > 0)
    .map(([name, value]) => ({
      name,
      value: parseFloat(value)
    }));

  if (chartData.length === 0) {
    return (
      <Card sx={{ height: '100%' }}>
        <CardContent>
          <Typography color="text.secondary" gutterBottom variant="h6">
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
        <Typography color="text.secondary" gutterBottom variant="h6">
          Segment Breakdown
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

export default SegmentBreakdownCard;
