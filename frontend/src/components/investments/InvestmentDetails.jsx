import React, { useState, useEffect } from 'react';
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
  Paper,
  Alert
} from '@mui/material';
import { TrendingUp, TrendingDown } from '@mui/icons-material';
import { investmentService } from '../../services/investmentService';
import { transactionService } from '../../services/transactionService';
import { computePnl } from '../../utils/investmentMetrics';
import { formatCurrency } from '../../utils/currencyFormatter';

const InvestmentDetails = ({ investment, open, onClose }) => {
  // (formatting helpers imported from utils/currencyFormatter)
  const [transactions, setTransactions] = useState([]);
  const [txnLoading, setTxnLoading] = useState(false);
  const [txnError, setTxnError] = useState(null);

  // INV-1: the row payload no longer carries transactions — fetch the real
  // history from the API so the section is never silently empty.
  useEffect(() => {
    if (!open || !investment?.id) {
      setTransactions([]);
      setTxnError(null);
      return;
    }
    let cancelled = false;
    setTxnLoading(true);
    setTxnError(null);
    transactionService
      .getTransactionHistory(investment.id)
      .then((data) => {
        if (!cancelled) setTransactions(Array.isArray(data) ? data : []);
      })
      .catch((err) => {
        if (!cancelled) {
          console.error('Error fetching transaction history:', err);
          setTxnError(err.response?.data?.error || err.message || 'Failed to load transactions');
        }
      })
      .finally(() => {
        if (!cancelled) setTxnLoading(false);
      });
    return () => {
      cancelled = true;
    };
  }, [open, investment?.id]);

  if (!investment) return null;

  const { cost, pnl, pnlPct } = computePnl(investment);
  const hasPnl = pnl != null;
  const pnlPositive = hasPnl && pnl >= 0;

  const getTypeColor = (type) => {
    const colors = {
      EQUITY: 'primary',
      BOND: 'secondary',
      FD: 'success',
      RD: 'info',
      NPS: 'warning',
      PPF: 'success',
      REAL_ESTATE: 'error',
      CRYPTO: 'warning',
      CASH: 'default'
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

  // Prefer transactionDate; fall back to createdAt for legacy rows missing it.
  const formatTxnDate = (t) => {
    const raw = t?.transactionDate || t?.createdAt || null;
    if (!raw) return '-';
    const d = new Date(raw);
    return isNaN(d.getTime()) ? '-' : d.toLocaleDateString();
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
              {investment.currentValue != null
                ? formatCurrency(investment.currentValue, investment.currency)
                : '\u2014'}
            </Typography>
          </Grid>
          <Grid item xs={12} sm={6}>
            <Typography variant="subtitle2" color="text.secondary">
              Cost Basis
            </Typography>
            <Typography variant="h5">
              {cost != null ? formatCurrency(cost, investment.currency) : '\u2014'}
            </Typography>
          </Grid>
          <Grid item xs={12} sm={6}>
            <Typography variant="subtitle2" color="text.secondary">
              Gain/Loss
            </Typography>
            {!hasPnl ? (
              <Typography variant="h5" color="text.secondary">
                {'\u2014'}
              </Typography>
            ) : (
              <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                {pnlPositive ? <TrendingUp color="success" /> : <TrendingDown color="error" />}
                <Typography
                  variant="h5"
                  sx={{ color: pnlPositive ? 'success.main' : 'error.main' }}
                >
                  {formatCurrency(pnl, investment.currency)}
                  {pnlPct != null && (
                    <Typography component="span" variant="body1" sx={{ ml: 1 }}>
                      ({pnlPositive ? '+' : ''}
                      {pnlPct.toFixed(2)}%)
                    </Typography>
                  )}
                </Typography>
              </Box>
            )}
          </Grid>
          <Grid item xs={12} sm={6}>
            <Typography variant="subtitle2" color="text.secondary">
              Purchase Date
            </Typography>
            <Typography variant="h6">
              {investment.purchaseDate
                ? new Date(`${investment.purchaseDate}T00:00:00`).toLocaleDateString()
                : '\u2014'}
            </Typography>
          </Grid>
        </Grid>

        <Divider sx={{ my: 3 }} />

        <Typography variant="h6" gutterBottom>
          Transaction History
        </Typography>
        {txnError && (
          <Alert severity="error" sx={{ mb: 2 }} onClose={() => setTxnError(null)}>
            {txnError}
          </Alert>
        )}
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
              {txnLoading ? (
                <TableRow>
                  <TableCell colSpan={5} align="center">
                    <Typography variant="body2" color="text.secondary">
                      Loading transactions…
                    </Typography>
                  </TableCell>
                </TableRow>
              ) : transactions.length > 0 ? (
                transactions.map((transaction) => (
                  <TableRow key={transaction.id}>
                    <TableCell>{formatTxnDate(transaction)}</TableCell>
                    <TableCell>
                      <Chip
                        label={transaction.type}
                        color={getTransactionTypeColor(transaction.type)}
                        size="small"
                      />
                    </TableCell>
                    <TableCell align="right">{transaction.quantity || '-'}</TableCell>
                    <TableCell align="right">
                      {transaction.pricePerUnit
                        ? formatCurrency(transaction.pricePerUnit, transaction.currency)
                        : '-'}
                    </TableCell>
                    <TableCell align="right">
                      {formatCurrency(transaction.amount, transaction.currency)}
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
