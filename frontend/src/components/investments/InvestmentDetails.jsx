import React from 'react';
import {
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  Button,
  Typography,
  Box,
  Chip,
  Divider,
  Grid,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  Paper
} from '@mui/material';
import { TrendingUp, TrendingDown } from '@mui/icons-material';

const InvestmentDetails = ({ investment, open, onClose }) => {
  if (!investment) return null;

  const formatCurrency = (value, currency) => {
    const symbols = { INR: '₹', USD: '$', EUR: '€', GBP: '£' };
    return `${symbols[currency] || currency} ${value.toLocaleString()}`;
  };

  const getTypeColor = (type) => {
    const colors = {
      EQUITY: 'primary',
      BOND: 'secondary',
      FD: 'success',
      RD: 'info',
      NPS: 'warning',
      PPF: 'success',
      REAL_ESTATE: 'error',
      CRYPTO: 'warning'
    };
    return colors[type] || 'default';
  };

  const getTransactionTypeColor = (type) => {
    const colors = {
      BUY: 'success',
      SELL: 'error',
      DIVIDEND: 'info',
      INTEREST: 'info',
      CONTRIBUTION: 'success'
    };
    return colors[type] || 'default';
  };

  return (
    <Dialog open={open} onClose={onClose} maxWidth="md" fullWidth>
      <DialogTitle>
        <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
          <Typography variant="h5">{investment.name}</Typography>
          <Chip label={investment.type} color={getTypeColor(investment.type)} />
        </Box>
      </DialogTitle>
      <DialogContent dividers>
        <Grid container spacing={3}>
          <Grid item xs={12} sm={6}>
            <Typography variant="subtitle2" color="text.secondary">
              Current Value
            </Typography>
            <Typography variant="h4">
              {formatCurrency(investment.currentValue, investment.currency)}
            </Typography>
          </Grid>
          <Grid item xs={12} sm={6}>
            <Typography variant="subtitle2" color="text.secondary">
              Purchase Value
            </Typography>
            <Typography variant="h5">
              {formatCurrency(investment.purchaseValue, investment.currency)}
            </Typography>
          </Grid>
          <Grid item xs={12} sm={6}>
            <Typography variant="subtitle2" color="text.secondary">
              Gain/Loss
            </Typography>
            <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
              {investment.growth >= 0 ? (
                <TrendingUp color="success" />
              ) : (
                <TrendingDown color="error" />
              )}
              <Typography
                variant="h5"
                sx={{ color: investment.growth >= 0 ? 'success.main' : 'error.main' }}
              >
                {formatCurrency(
                  investment.currentValue - investment.purchaseValue,
                  investment.currency
                )}
                <Typography component="span" variant="body1" sx={{ ml: 1 }}>
                  ({investment.growth >= 0 ? '+' : ''}{investment.growth}%)
                </Typography>
              </Typography>
            </Box>
          </Grid>
          <Grid item xs={12} sm={6}>
            <Typography variant="subtitle2" color="text.secondary">
              Purchase Date
            </Typography>
            <Typography variant="h6">
              {new Date(investment.purchaseDate).toLocaleDateString()}
            </Typography>
          </Grid>
        </Grid>

        <Divider sx={{ my: 3 }} />

        <Typography variant="h6" gutterBottom>
          Transaction History
        </Typography>
        <TableContainer component={Paper} variant="outlined">
          <Table size="small">
            <TableHead>
              <TableRow>
                <TableCell>Date</TableCell>
                <TableCell>Type</TableCell>
                <TableCell align="right">Quantity</TableCell>
                <TableCell align="right">Price</TableCell>
                <TableCell align="right">Amount</TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {investment.transactions?.length > 0 ? (
                investment.transactions.map((transaction) => (
                  <TableRow key={transaction.id}>
                    <TableCell>{new Date(transaction.date).toLocaleDateString()}</TableCell>
                    <TableCell>
                      <Chip
                        label={transaction.type}
                        color={getTransactionTypeColor(transaction.type)}
                        size="small"
                      />
                    </TableCell>
                    <TableCell align="right">{transaction.quantity || '-'}</TableCell>
                    <TableCell align="right">
                      {transaction.price
                        ? formatCurrency(transaction.price, investment.currency)
                        : '-'}
                    </TableCell>
                    <TableCell align="right">
                      {formatCurrency(transaction.amount, investment.currency)}
                    </TableCell>
                  </TableRow>
                ))
              ) : (
                <TableRow>
                  <TableCell colSpan={5} align="center">
                    <Typography variant="body2" color="text.secondary">
                      No transactions found
                    </Typography>
                  </TableCell>
                </TableRow>
              )}
            </TableBody>
          </Table>
        </TableContainer>

        {investment.notes && (
          <>
            <Divider sx={{ my: 3 }} />
            <Typography variant="subtitle2" color="text.secondary" gutterBottom>
              Notes
            </Typography>
            <Typography variant="body2">{investment.notes}</Typography>
          </>
        )}
      </DialogContent>
      <DialogActions>
        <Button onClick={onClose}>Close</Button>
      </DialogActions>
    </Dialog>
  );
};

export default InvestmentDetails;
