import React from 'react';
import {
  Card,
  CardContent,
  Typography,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  Chip
} from '@mui/material';
import { formatCurrency } from '../../utils/currencyFormatter';

const RecentTransactionsCard = ({ transactions }) => {
  const getTypeColor = (type) => {
    switch (type) {
      case 'BUY':
        return 'success';
      case 'SELL':
        return 'error';
      case 'DIVIDEND':
      case 'INTEREST':
        return 'info';
      case 'CONTRIBUTION':
        return 'success';
      default:
        return 'default';
    }
  };

  // Prefer transactionDate; fall back to createdAt for legacy rows missing it.
  const getTxnDate = (t) => t?.transactionDate || t?.createdAt || null;

  const formatTxnDate = (t) => {
    const raw = getTxnDate(t);
    if (!raw) return '-';
    const d = new Date(raw);
    return isNaN(d.getTime()) ? '-' : d.toLocaleDateString();
  };

  return (
    <Card sx={{ height: '100%' }}>
      <CardContent>
        <Typography variant="overline" component="div" sx={{ color: 'text.secondary', mb: 0.5 }}>
          Recent Transactions
        </Typography>
        <TableContainer sx={{ mt: 1.5 }}>
          <Table size="small">
            <TableHead>
              <TableRow>
                <TableCell>Date</TableCell>
                <TableCell>Asset</TableCell>
                <TableCell>Type</TableCell>
                <TableCell align="right">Amount</TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {transactions.length === 0 ? (
                <TableRow>
                  <TableCell colSpan={4} align="center">
                    <Typography variant="body2" color="text.secondary">
                      No transactions yet
                    </Typography>
                  </TableCell>
                </TableRow>
              ) : (
                transactions.map((transaction) => (
                  <TableRow key={transaction.id}>
                    <TableCell>{formatTxnDate(transaction)}</TableCell>
                    <TableCell>{transaction.investmentName || '-'}</TableCell>
                    <TableCell>
                      <Chip
                        label={transaction.type}
                        size="small"
                        color={getTypeColor(transaction.type)}
                      />
                    </TableCell>
                    <TableCell align="right" sx={{ fontVariantNumeric: 'tabular-nums' }}>
                      {formatCurrency(transaction.amount, transaction.currency)}
                    </TableCell>
                  </TableRow>
                ))
              )}
            </TableBody>
          </Table>
        </TableContainer>
      </CardContent>
    </Card>
  );
};

export default RecentTransactionsCard;
