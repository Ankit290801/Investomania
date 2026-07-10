import React, { useState } from 'react';
import {
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  Paper,
  IconButton,
  Chip,
  Typography,
  Box,
  TablePagination,
  Tooltip,
} from '@mui/material';
import {
  Delete as DeleteIcon,
  Edit as EditIcon,
  Link as LinkIcon,
} from '@mui/icons-material';
import { formatCurrency } from '../../utils/currencyFormatter';
import { format } from 'date-fns';

const categoryColors = {
  HOUSING: 'primary',
  FOOD: 'success',
  TRANSPORT: 'info',
  HEALTHCARE: 'error',
  ENTERTAINMENT: 'secondary',
  EDUCATION: 'warning',
  UTILITIES: 'default',
  INSURANCE: 'primary',
  OTHER: 'default',
};

const ExpenseList = ({ expenses, onEdit, onDelete, onMap }) => {
  const [page, setPage] = useState(0);
  const [rowsPerPage, setRowsPerPage] = useState(10);

  const handleChangePage = (event, newPage) => {
    setPage(newPage);
  };

  const handleChangeRowsPerPage = (event) => {
    setRowsPerPage(parseInt(event.target.value, 10));
    setPage(0);
  };

  if (!expenses || expenses.length === 0) {
    return (
      <Box sx={{ textAlign: 'center', py: 8 }}>
        <Typography variant="h6" color="text.secondary">
          No expenses found
        </Typography>
        <Typography variant="body2" color="text.secondary" sx={{ mt: 1 }}>
          Add your first expense to start tracking
        </Typography>
      </Box>
    );
  }

  const paginatedExpenses = expenses.slice(
    page * rowsPerPage,
    page * rowsPerPage + rowsPerPage
  );

  return (
    <>
      <TableContainer component={Paper}>
        <Table>
          <TableHead>
            <TableRow>
              <TableCell>Date</TableCell>
              <TableCell>Description</TableCell>
              <TableCell>Category</TableCell>
              <TableCell align="right">Amount</TableCell>
              <TableCell align="center">Mapped</TableCell>
              <TableCell align="center">Actions</TableCell>
            </TableRow>
          </TableHead>
          <TableBody>
            {paginatedExpenses.map((expense) => (
              <TableRow key={expense.id} hover>
                <TableCell>
                  {format(new Date(expense.expenseDate), 'MMM dd, yyyy')}
                </TableCell>
                <TableCell>
                  <Typography variant="body2">{expense.description}</Typography>
                  {expense.notes && (
                    <Typography variant="caption" color="text.secondary">
                      {expense.notes}
                    </Typography>
                  )}
                </TableCell>
                <TableCell>
                  <Chip
                    label={expense.category.replace('_', ' ')}
                    color={categoryColors[expense.category] || 'default'}
                    size="small"
                  />
                </TableCell>
                <TableCell align="right">
                  <Typography variant="body2" fontWeight="medium">
                    {formatCurrency(expense.amount, expense.currency)}
                  </Typography>
                </TableCell>
                <TableCell align="center">
                  <Chip
                    label={`${expense.mappedPercentage || 0}%`}
                    size="small"
                    color={
                      expense.mappedPercentage === 100
                        ? 'success'
                        : expense.mappedPercentage > 0
                        ? 'warning'
                        : 'default'
                    }
                  />
                </TableCell>
                <TableCell align="center">
                  <Tooltip title="Map to investments">
                    <IconButton
                      size="small"
                      onClick={() => onMap(expense)}
                      color="primary"
                    >
                      <LinkIcon />
                    </IconButton>
                  </Tooltip>
                  <Tooltip title="Edit">
                    <IconButton
                      size="small"
                      onClick={() => onEdit(expense)}
                      color="primary"
                    >
                      <EditIcon />
                    </IconButton>
                  </Tooltip>
                  <Tooltip title="Delete">
                    <IconButton
                      size="small"
                      onClick={() => onDelete(expense.id)}
                      color="error"
                    >
                      <DeleteIcon />
                    </IconButton>
                  </Tooltip>
                </TableCell>
              </TableRow>
            ))}
          </TableBody>
        </Table>
      </TableContainer>
      <TablePagination
        component="div"
        count={expenses.length}
        page={page}
        onPageChange={handleChangePage}
        rowsPerPage={rowsPerPage}
        onRowsPerPageChange={handleChangeRowsPerPage}
        rowsPerPageOptions={[5, 10, 25, 50]}
      />
    </>
  );
};

export default ExpenseList;
