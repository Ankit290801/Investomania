import React from 'react';
import { Card, CardContent, Typography, Box } from '@mui/material';
import { TrendingUp, TrendingDown } from '@mui/icons-material';
import { formatCurrency, formatCurrencyWithSign } from '../../utils/currencyFormatter';

const NetWorthCard = ({ value, change, changePercent, currency = 'INR' }) => {
  const isPositive = change >= 0;

  return (
    <Card sx={{ height: '100%' }}>
      <CardContent>
        <Typography color="text.secondary" gutterBottom variant="h6">
          Total Net Worth
        </Typography>
        <Typography variant="h3" component="div" sx={{ my: 2 }}>
          {formatCurrency(value, currency)}
        </Typography>
        <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
          {isPositive ? (
            <TrendingUp color="success" />
          ) : (
            <TrendingDown color="error" />
          )}
          <Typography
            variant="body2"
            color={isPositive ? 'success.main' : 'error.main'}
          >
            {formatCurrencyWithSign(change, currency)} ({isPositive ? '+' : ''}{changePercent.toFixed(2)}%)
          </Typography>
        </Box>
        <Typography variant="caption" color="text.secondary" sx={{ mt: 1, display: 'block' }}>
          Last year
        </Typography>
      </CardContent>
    </Card>
  );
};

export default NetWorthCard;
