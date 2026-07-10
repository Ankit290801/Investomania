import React, { useState } from 'react';
import {
  Box,
  Paper,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  TablePagination,
  Chip,
  IconButton,
  MenuItem,
  Select,
  FormControl,
  InputLabel,
  TextField
} from '@mui/material';
import { Delete as DeleteIcon, Edit as EditIcon } from '@mui/icons-material';
import { formatCurrency } from '../../utils/currencyFormatter';

const TransactionHistory = ({ transactions, onEdit, onDelete }) => {
  const [page, setPage] = useState(0);
  const [rowsPerPage, setRowsPerPage] = useState(10);
  const [filterType, setFilterType] = useState('ALL');
  const [searchTerm, setSearchTerm] = useState('');

  const getTypeColor = (type) => {
    const colors = {
      BUY: 'success',
      SELL: 'error',
      DIVIDEND: 'info',
      INTEREST: 'info',
      CONTRIBUTION: 'success',
      MATURITY: 'warning'
    };
    return colors[type] || 'default';
  };

  // Prefer transactionDate; fall back to createdAt for legacy rows missing it.
  const getTxnDate = (t) => t?.transactionDate || t?.createdAt || null;

  const formatTxnDate = (t) => {
    const raw = getTxnDate(t);
    if (!raw) return '-';
    const d = new Date(raw);
    return isNaN(d.getTime()) ? '-' : d.toLocaleDateString();
  };

  const filteredTransactions = transactions
    .filter((transaction) => {
      if (filterType !== 'ALL' && transaction.type !== filterType) return false;
      if (searchTerm && !transaction.notes?.toLowerCase().includes(searchTerm.toLowerCase())) {
        return false;
      }
      return true;
    })
    .sort((a, b) => new Date(getTxnDate(b) || 0) - new Date(getTxnDate(a) || 0));

  const paginatedTransactions = filteredTransactions.slice(
    page * rowsPerPage,
    page * rowsPerPage + rowsPerPage
  );

  return (
    <Box>
      <Box sx={{ display: 'flex', gap: 2, mb: 2, flexWrap: 'wrap' }}>
        <FormControl size="small" sx={{ minWidth: 150 }}>
          <InputLabel>Filter by Type</InputLabel>
          <Select
            value={filterType}
            label="Filter by Type"
            onChange={(e) => setFilterType(e.target.value)}
          >
            <MenuItem value="ALL">All Types</MenuItem>
            <MenuItem value="BUY">Buy</MenuItem>
            <MenuItem value="SELL">Sell</MenuItem>
            <MenuItem value="DIVIDEND">Dividend</MenuItem>
            <MenuItem value="INTEREST">Interest</MenuItem>
            <MenuItem value="CONTRIBUTION">Contribution</MenuItem>
            <MenuItem value="MATURITY">Maturity</MenuItem>
          </Select>
        </FormControl>
        <TextField
          size="small"
          label="Search Notes"
          value={searchTerm}
          onChange={(e) => setSearchTerm(e.target.value)}
          sx={{ minWidth: 200 }}
        />
      </Box>

      <TableContainer component={Paper}>
        <Table>
          <TableHead>
            <TableRow>
              <TableCell>Date</TableCell>
              <TableCell>Type</TableCell>
              <TableCell>Investment</TableCell>
              <TableCell align="right">Quantity</TableCell>
              <TableCell align="right">Price</TableCell>
              <TableCell align="right">Amount</TableCell>
              <TableCell>Notes</TableCell>
              <TableCell align="center">Actions</TableCell>
            </TableRow>
          </TableHead>
          <TableBody>
            {paginatedTransactions.length > 0 ? (
              paginatedTransactions.map((transaction) => (
                <TableRow key={transaction.id} hover>
                  <TableCell>{formatTxnDate(transaction)}</TableCell>
                  <TableCell>
                    <Chip
                      label={transaction.type}
                      color={getTypeColor(transaction.type)}
                      size="small"
                    />
                  </TableCell>
                  <TableCell>{transaction.investmentName || '-'}</TableCell>
                  <TableCell align="right">{transaction.quantity || '-'}</TableCell>
                  <TableCell align="right">
                    {transaction.pricePerUnit
                      ? formatCurrency(transaction.pricePerUnit, transaction.currency)
                      : '-'}
                  </TableCell>
                  <TableCell align="right">
                    {formatCurrency(transaction.amount, transaction.currency)}
                  </TableCell>
                  <TableCell sx={{ maxWidth: 200, overflow: 'hidden', textOverflow: 'ellipsis' }}>
                    {transaction.notes || '-'}
                  </TableCell>
                  <TableCell align="center">
                    <IconButton size="small" onClick={() => onEdit(transaction)} color="primary">
                      <EditIcon fontSize="small" />
                    </IconButton>
                    <IconButton size="small" onClick={() => onDelete(transaction)} color="error">
                      <DeleteIcon fontSize="small" />
                    </IconButton>
                  </TableCell>
                </TableRow>
              ))
            ) : (
              <TableRow>
                <TableCell colSpan={8} align="center">
                  No transactions found
                </TableCell>
              </TableRow>
            )}
          </TableBody>
        </Table>
        <TablePagination
          component="div"
          count={filteredTransactions.length}
          page={page}
          onPageChange={(e, newPage) => setPage(newPage)}
          rowsPerPage={rowsPerPage}
          onRowsPerPageChange={(e) => {
            setRowsPerPage(parseInt(e.target.value, 10));
            setPage(0);
          }}
          rowsPerPageOptions={[5, 10, 25, 50]}
        />
      </TableContainer>
    </Box>
  );
};

export default TransactionHistory;
