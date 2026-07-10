import React, { useState, useEffect } from 'react';
import {
  Box,
  Typography,
  Button,
  Alert,
  CircularProgress,
  FormControl,
  InputLabel,
  Select,
  MenuItem,
} from '@mui/material';
import { Add as AddIcon } from '@mui/icons-material';
import ExpenseDialog from '../components/expenses/ExpenseDialog';
import ExpenseList from '../components/expenses/ExpenseList';
import ExpenseMappingDialog from '../components/expenses/ExpenseMappingDialog';
import { expenseService } from '../services/expenseService';

const categories = [
  { value: '', label: 'All Categories' },
  { value: 'HOUSING', label: 'Housing' },
  { value: 'FOOD', label: 'Food' },
  { value: 'TRANSPORT', label: 'Transport' },
  { value: 'HEALTHCARE', label: 'Healthcare' },
  { value: 'ENTERTAINMENT', label: 'Entertainment' },
  { value: 'EDUCATION', label: 'Education' },
  { value: 'UTILITIES', label: 'Utilities' },
  { value: 'INSURANCE', label: 'Insurance' },
  { value: 'OTHER', label: 'Other' },
];

const ExpensesPage = () => {
  const [expenses, setExpenses] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [dialogOpen, setDialogOpen] = useState(false);
  const [mappingDialogOpen, setMappingDialogOpen] = useState(false);
  const [selectedExpense, setSelectedExpense] = useState(null);
  const [categoryFilter, setCategoryFilter] = useState('');

  useEffect(() => {
    fetchExpenses();
  }, [categoryFilter]);

  const fetchExpenses = async () => {
    setLoading(true);
    setError(null);
    
    try {
      const data = await expenseService.getAll(categoryFilter || null);
      setExpenses(data);
    } catch (err) {
      console.error('Error fetching expenses:', err);
      setError('Failed to load expenses. Please try again.');
    } finally {
      setLoading(false);
    }
  };

  const handleOpenDialog = (expense = null) => {
    setSelectedExpense(expense);
    setDialogOpen(true);
  };

  const handleCloseDialog = () => {
    setSelectedExpense(null);
    setDialogOpen(false);
  };

  const handleSubmit = async (data) => {
    try {
      if (selectedExpense) {
        await expenseService.update(selectedExpense.id, data);
      } else {
        await expenseService.create(data);
      }
      
      handleCloseDialog();
      fetchExpenses();
    } catch (err) {
      console.error('Error saving expense:', err);
      setError('Failed to save expense. Please try again.');
    }
  };

  const handleDelete = async (id) => {
    if (!window.confirm('Are you sure you want to delete this expense?')) {
      return;
    }

    try {
      await expenseService.delete(id);
      fetchExpenses();
    } catch (err) {
      console.error('Error deleting expense:', err);
      setError('Failed to delete expense. Please try again.');
    }
  };

  const handleOpenMapping = (expense) => {
    setSelectedExpense(expense);
    setMappingDialogOpen(true);
  };

  const handleCloseMapping = () => {
    setSelectedExpense(null);
    setMappingDialogOpen(false);
    fetchExpenses(); // Refresh to show updated mappings
  };

  if (loading) {
    return (
      <Box display="flex" justifyContent="center" alignItems="center" minHeight="400px">
        <CircularProgress />
      </Box>
    );
  }

  return (
    <Box>
      <Box display="flex" justifyContent="space-between" alignItems="center" mb={3}>
        <Typography variant="h4">
          Expenses
        </Typography>
        
        <Box display="flex" gap={2}>
          <FormControl size="small" sx={{ minWidth: 200 }}>
            <InputLabel>Category Filter</InputLabel>
            <Select
              value={categoryFilter}
              label="Category Filter"
              onChange={(e) => setCategoryFilter(e.target.value)}
            >
              {categories.map((cat) => (
                <MenuItem key={cat.value} value={cat.value}>
                  {cat.label}
                </MenuItem>
              ))}
            </Select>
          </FormControl>
          
          <Button
            variant="contained"
            startIcon={<AddIcon />}
            onClick={() => handleOpenDialog()}
          >
            Add Expense
          </Button>
        </Box>
      </Box>

      {error && (
        <Alert severity="error" sx={{ mb: 2 }} onClose={() => setError(null)}>
          {error}
        </Alert>
      )}

      <ExpenseList
        expenses={expenses}
        onEdit={handleOpenDialog}
        onDelete={handleDelete}
        onMap={handleOpenMapping}
      />

      <ExpenseDialog
        open={dialogOpen}
        onClose={handleCloseDialog}
        expense={selectedExpense}
        onSubmit={handleSubmit}
      />

      <ExpenseMappingDialog
        open={mappingDialogOpen}
        onClose={handleCloseMapping}
        expense={selectedExpense}
      />
    </Box>
  );
};

export default ExpensesPage;
