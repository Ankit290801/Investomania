import React from 'react';
import { useTheme } from '@mui/material/styles';
import { Card, CardContent, Typography, Box } from '@mui/material';
import { TrendingUp, TrendingDown } from '@mui/icons-material';
import { formatCurrency } from '../../utils/currencyFormatter';

/**
 * F1: expense coverage card — stat rows with tabular numerals and a
 * slimmer, rounded progress bar; coverage % becomes the headline.
 */
const ExpenseCoverageCard = ({ coverage, currency = 'INR' }) => {
  if (!coverage) return null;

  const { totalExpenses, totalIncome, coveragePercentage } = coverage;
  const isCovered = coveragePercentage >= 100;

  return (
    <Card sx={{ height: '100%' }}>
      <CardContent>
        <Typography variant="overline" component="div" sx={{ color: 'text.secondary', mb: 0.5 }}>
          Expense Coverage
        </Typography>

        <Box sx={{ my: 2 }}>
          <Box sx={{ display: 'flex', justifyContent: 'space-between', mb: 1 }}>
            <Typography variant="body2" color="text.secondary">
              Total Expenses
            </Typography>
            <Typography variant="body2" fontWeight={600} sx={{ fontVariantNumeric: 'tabular-nums' }}>
              {formatCurrency(totalExpenses, currency)}
            </Typography>
          </Box>

          <Box sx={{ display: 'flex', justifyContent: 'space-between', mb: 2 }}>
            <Typography variant="body2" color="text.secondary">
              Investment Income
            </Typography>
            <Typography
              variant="body2"
              fontWeight={600}
              color="success.main"
              sx={{ fontVariantNumeric: 'tabular-nums' }}
            >
              {formatCurrency(totalIncome, currency)}
            </Typography>
          </Box>

          <Box sx={{ display: 'flex', alignItems: 'baseline', gap: 1 }}>
            <Box sx={{ display: 'flex', alignItems: 'center', gap: 0.5 }}>
              {isCovered ? (
                <TrendingUp color="success" sx={{ fontSize: 20 }} />
              ) : (
                <TrendingDown color="warning" sx={{ fontSize: 20 }} />
              )}
              <Typography variant="h4" fontWeight={700} sx={{ fontVariantNumeric: 'tabular-nums' }}>
                {coveragePercentage.toFixed(1)}%
              </Typography>
            </Box>
            <Typography variant="body2" color="text.secondary">
              {isCovered ? 'Fully Covered' : 'Partially Covered'}
            </Typography>
          </Box>

          <Box
            sx={{
              height: 8,
              borderRadius: 4,
              mt: 1.5,
              backgroundColor: 'action.hover',
              overflow: 'hidden'
            }}
          >
            <Box
              sx={{
                height: '100%',
                width: `${Math.min(Math.max(coveragePercentage, 0), 100)}%`,
                borderRadius: 4,
                backgroundColor: isCovered ? 'success.main' : 'warning.main',
                transition: 'width 0.4s ease'
              }}
            />
          </Box>
        </Box>

        <Typography variant="caption" color="text.secondary">
          {isCovered
            ? 'Your investment income covers all expenses'
            : 'Your investment income partially covers expenses'}
        </Typography>
      </CardContent>
    </Card>
  );
};

export default ExpenseCoverageCard;
