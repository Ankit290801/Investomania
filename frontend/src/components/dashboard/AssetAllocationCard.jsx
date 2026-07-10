import React from 'react';
import { Card, CardContent, Typography } from '@mui/material';
import { BarChart, Bar, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer, Legend } from 'recharts';
import { formatCurrency } from '../../utils/currencyFormatter';

const AssetAllocationCard = ({ data, currency = 'INR' }) => {
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
            Asset Class Distribution
          </Typography>
          <Typography color="text.secondary" sx={{ textAlign: 'center', mt: 8 }}>
            No data available. Add investments to see distribution.
          </Typography>
        </CardContent>
      </Card>
    );
  }

  return (
    <Card sx={{ height: '100%' }}>
      <CardContent>
        <Typography color="text.secondary" gutterBottom variant="h6">
          Asset Class Distribution
        </Typography>
        <ResponsiveContainer width="100%" height={300}>
          <BarChart data={chartData}>
            <CartesianGrid strokeDasharray="3 3" />
            <XAxis dataKey="name" />
            <YAxis />
            <Tooltip formatter={(value) => formatCurrency(value, currency)} />
            <Legend />
            <Bar dataKey="value" fill="#8884d8" name="Value" />
          </BarChart>
        </ResponsiveContainer>
      </CardContent>
    </Card>
  );
};

export default AssetAllocationCard;
