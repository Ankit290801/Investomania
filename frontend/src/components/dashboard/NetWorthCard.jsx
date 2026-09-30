import React from 'react';
import { useTheme } from '@mui/material/styles';
import { Card, CardContent, Typography, Box } from '@mui/material';
import { TrendingUp, TrendingDown } from '@mui/icons-material';
import { formatCurrency, formatCurrencyWithSign } from '../../utils/currencyFormatter';
import { semanticColor } from '../../config/theme';

/**
 * F1: hero net-worth card. Flat bordered card, accent-tinted label,
 * tabular numerals for the headline, and theme-aware gain/loss colour.
 */
const NetWorthCard = ({ value, change, changePercent, currency = 'INR' }) => {
  const theme = useTheme();
  const isPositive = change >= 0;
  const changeColor = semanticColor(theme, isPositive);

  return (
    <Card sx={{ height: '100%' }}>
      <CardContent>
        <Typography
          variant="overline"
          component="div"
          sx={{ color: 'text.secondary', mb: 0.5 }}
        >
          Total Net Worth
        </Typography>
        <Typography
          variant="h3"
          component="div"
          sx={{
            my: 1.5,
            fontVariantNumeric: 'tabular-nums',
            letterSpacing: '-0.02em',
            fontWeight: 700
          }}
        >
          {formatCurrency(value, currency)}
        </Typography>
        <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
          <Box
            sx={{
              display: 'flex',
              alignItems: 'center',
              gap: 0.5,
              px: 1,
              py: 0.25,
              borderRadius: 2,
              backgroundColor: `${changeColor}1F`
            }}
          >
            {isPositive ? (
              <TrendingUp sx={{ fontSize: 18, color: changeColor }} />
            ) : (
              <TrendingDown sx={{ fontSize: 18, color: changeColor }} />
            )}
            <Typography variant="body2" sx={{ color: changeColor, fontWeight: 600 }}>
              {formatCurrencyWithSign(change, currency)} ({isPositive ? '+' : ''}
              {changePercent.toFixed(2)}%)
            </Typography>
          </Box>
        </Box>
        <Typography variant="caption" color="text.secondary" sx={{ mt: 1, display: 'block' }}>
          vs last FY-end
        </Typography>
      </CardContent>
    </Card>
  );
};

export default NetWorthCard;
