import React from 'react';
import { useTheme } from '@mui/material/styles';
import { Card, CardContent, Typography } from '@mui/material';
import {
  BarChart,
  Bar,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  ResponsiveContainer,
  Cell
} from 'recharts';
import { formatCurrency } from '../../utils/currencyFormatter';
import {
  chartAxisProps,
  chartGridProps,
  chartTooltipStyle,
  formatAxisValue,
  seriesColor
} from '../../utils/chartHelpers';

/**
 * F1: asset-class distribution as horizontal bars — labels stay
 * readable for long class names, values sit on the compact axis.
 */
const AssetAllocationCard = ({ data, currency = 'INR' }) => {
  const theme = useTheme();

  const chartData = Object.entries(data || {})
    .filter(([_, value]) => value > 0)
    .map(([name, value]) => ({
      name: name.replace('_', ' ').toLowerCase(),
      value: parseFloat(value)
    }))
    .sort((a, b) => b.value - a.value);

  if (chartData.length === 0) {
    return (
      <Card sx={{ height: '100%' }}>
        <CardContent>
          <Typography variant="overline" component="div" sx={{ color: 'text.secondary', mb: 0.5 }}>
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
        <Typography variant="overline" component="div" sx={{ color: 'text.secondary', mb: 0.5 }}>
          Asset Class Distribution
        </Typography>
        <ResponsiveContainer width="100%" height={chartData.length * 42 + 30}>
          <BarChart data={chartData} layout="vertical" margin={{ left: 8, right: 24 }}>
            <CartesianGrid {...chartGridProps(theme)} horizontal={false} />
            <XAxis type="number" {...chartAxisProps(theme)} tickFormatter={formatAxisValue} />
            <YAxis
              type="category"
              dataKey="name"
              width={90}
              orientation="left"
              {...chartAxisProps(theme)}
            />
            <Tooltip
              formatter={(value) => formatCurrency(value, currency)}
              {...chartTooltipStyle(theme)}
            />
            <Bar dataKey="value" radius={[0, 6, 6, 0]} barSize={16} name="Value">
              {chartData.map((entry, index) => (
                <Cell key={`cell-${index}`} fill={seriesColor(index)} />
              ))}
            </Bar>
          </BarChart>
        </ResponsiveContainer>
      </CardContent>
    </Card>
  );
};

export default AssetAllocationCard;
