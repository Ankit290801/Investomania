import React from 'react';
import { Card, CardContent, Typography, Box, LinearProgress } from '@mui/material';
import { TrendingUp, TrendingDown } from '@mui/icons-material';
import { formatCurrency } from '../../utils/currencyFormatter';

const ExpenseCoverageCard = ({ coverage, currency = 'INR' }) => {
  if (!coverage) return null;

  const { totalExpenses, totalIncome, coveragePercentage } = coverage;
  const isCovered = coveragePercentage >= 100;

  return (
    <Card sx={{ height: '100%' }}>
      <CardContent>
        <Typography color="text.secondary" gutterBottom variant="h6">
          Expense Coverage
        </Typography>

        <Box sx={{ my: 3 }}>
          <Box sx={{ display: 'flex', justifyContent: 'space-between', mb: 1 }}>
            <Typography variant="body2" color="text.secondary">
              Total Expenses
            </Typography>
            <Typography variant="body2" fontWeight="medium">
              {formatCurrency(totalExpenses, currency)}
            </Typography>
          </Box>

          <Box sx={{ display: 'flex', justifyContent: 'space-between', mb: 2 }}>
            <Typography variant="body2" color="text.secondary">
              Investment Income
            </Typography>
            <Typography variant="body2" fontWeight="medium" color="success.main">
              {formatCurrency(totalIncome, currency)}
            </Typography>
          </Box>

          <LinearProgress
            variant="determinate"
            value={Math.min(coveragePercentage, 100)}
            color={isCovered ? 'success' : 'warning'}
            sx={{ height: 10, borderRadius: 1, mb: 1 }}
          />

          <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
            {isCovered ? (
              <TrendingUp color="success" />
            ) : (
              <TrendingDown color="warning" />
            )}
            <Typography
              variant="h5"
              color={isCovered ? 'success.main' : 'warning.main'}
            >
              {coveragePercentage.toFixed(1)}%
            </Typography>
            <Typography variant="body2" color="text.secondary">
              {isCovered ? 'Fully Covered' : 'Partially Covered'}
            </Typography>
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
