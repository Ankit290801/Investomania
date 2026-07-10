import React from 'react';
import { Card, CardContent, Typography, Box, Tooltip as MuiTooltip } from '@mui/material';
import { LineChart, Line, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer, Legend } from 'recharts';
import CircleIcon from '@mui/icons-material/Circle';
import { formatCurrency } from '../../utils/currencyFormatter';

/**
 * Convert a date string (yyyy-MM-dd or ISO) to an Indian FY label.
 * March 31 → "FY YY-YY" (e.g. 2024-03-31 → "FY 23-24")
 * Other dates → plain year string for fallback points.
 */
function toFYLabel(dateStr) {
  const d = new Date(dateStr);
  const month = d.getMonth() + 1; // 1-indexed
  const year = d.getFullYear();
  if (month <= 3) {
    // Jan-Mar: FY ends this year, started previous year
    const startYY = String(year - 1).slice(-2);
    const endYY = String(year).slice(-2);
    return `FY ${startYY}-${endYY}`;
  }
  // Apr-Dec: FY started this year, ends next year
  const startYY = String(year).slice(-2);
  const endYY = String(year + 1).slice(-2);
  return `FY ${startYY}-${endYY}`;
}

const GrowthChartCard = ({ data, currency = 'INR' }) => {
  const chartData = (data || []).map(item => ({
    label: toFYLabel(item.date),
    value: parseFloat(item.value),
    isSnapshot: !!item.snapshotBased, // backend may include this flag
  }));

  if (chartData.length === 0) {
    return (
      <Card sx={{ height: '100%' }}>
        <CardContent>
          <Typography color="text.secondary" gutterBottom variant="h6">
            Portfolio Growth (YoY)
          </Typography>
          <Typography color="text.secondary" sx={{ textAlign: 'center', mt: 8 }}>
            No data available. Add investments and transactions to see growth.
          </Typography>
        </CardContent>
      </Card>
    );
  }

  const hasSnapshotData = chartData.some(d => d.isSnapshot);

  return (
    <Card sx={{ height: '100%' }}>
      <CardContent>
        <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 1 }}>
          <Typography color="text.secondary" variant="h6">
            Portfolio Growth (YoY)
          </Typography>
          {hasSnapshotData && (
            <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
              <MuiTooltip title="Snapshot-based (accurate market data)">
                <Box sx={{ display: 'flex', alignItems: 'center', gap: 0.5 }}>
                  <CircleIcon sx={{ fontSize: 10, color: 'success.main' }} />
                  <Typography variant="caption" color="text.secondary">Snapshot</Typography>
                </Box>
              </MuiTooltip>
              <MuiTooltip title="Estimated (no historical snapshot available)">
                <Box sx={{ display: 'flex', alignItems: 'center', gap: 0.5 }}>
                  <CircleIcon sx={{ fontSize: 10, color: 'warning.main' }} />
                  <Typography variant="caption" color="text.secondary">Estimated</Typography>
                </Box>
              </MuiTooltip>
            </Box>
          )}
        </Box>
        <ResponsiveContainer width="100%" height={300}>
          <LineChart data={chartData}>
            <CartesianGrid strokeDasharray="3 3" />
            <XAxis dataKey="label" />
            <YAxis />
            <Tooltip formatter={(value) => formatCurrency(value, currency)} />
            <Legend />
            <Line
              type="monotone"
              dataKey="value"
              stroke="#8884d8"
              strokeWidth={2}
              name="Portfolio Value"
              dot={(props) => {
                const { cx, cy, payload } = props;
                const color = payload.isSnapshot ? '#4caf50' : '#ff9800';
                return <circle key={`dot-${cx}-${cy}`} cx={cx} cy={cy} r={4} fill={color} stroke={color} />;
              }}
            />
          </LineChart>
        </ResponsiveContainer>
      </CardContent>
    </Card>
  );
};

export default GrowthChartCard;

