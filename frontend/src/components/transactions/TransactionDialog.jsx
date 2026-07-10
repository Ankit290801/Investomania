import React, { useState } from 'react';
import {
  Box,
  Dialog,
  DialogTitle,
  DialogContent,
  Alert,
  CircularProgress,
  FormControl,
  InputLabel,
  Select,
  MenuItem
} from '@mui/material';
import TransactionForm from './TransactionForm';
import { transactionService } from '../../services/transactionService';

const TransactionDialog = ({ open, onClose, investmentId, investmentType, investments, onSuccess }) => {
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);
  const [selectedInvestmentId, setSelectedInvestmentId] = useState(investmentId || '');

  const handleSubmit = async (data) => {
    setLoading(true);
    setError(null);

    try {
      const targetInvestmentId = investmentId || selectedInvestmentId;
      if (!targetInvestmentId) {
        setError('Please select an investment');
        setLoading(false);
        return;
      }

      // Get investment currency
      const investment = investments?.find(inv => inv.id === parseInt(targetInvestmentId));
      const currency = investment?.currency || 'USD';

      // Transform data to match backend API expectations
      const transactionData = {
        type: data.type,
        transactionDate: data.date,  // Backend expects 'transactionDate'
        amount: parseFloat(data.amount),
        currency: currency,
        notes: data.notes || null
      };

      // Add optional fields if present
      if (data.quantity) {
        transactionData.quantity = parseFloat(data.quantity);
      }
      if (data.price) {
        transactionData.pricePerUnit = parseFloat(data.price);  // Backend expects 'pricePerUnit'
      }

      await transactionService.addTransaction(targetInvestmentId, transactionData);
      
      if (onSuccess) {
        onSuccess();
      }
      onClose();
      setSelectedInvestmentId('');
    } catch (err) {
      console.error('Error adding transaction:', err);
      setError(err.response?.data?.error || err.message || 'Failed to add transaction');
    } finally {
      setLoading(false);
    }
  };

  const getInvestmentType = () => {
    if (investmentType) return investmentType;
    if (investments && selectedInvestmentId) {
      return investments.find(inv => inv.id === selectedInvestmentId)?.type;
    }
    return null;
  };

  return (
    <Dialog open={open} onClose={onClose} maxWidth="sm" fullWidth>
      <DialogTitle>Add Transaction</DialogTitle>
      <DialogContent>
        {error && (
          <Alert severity="error" sx={{ mb: 2 }} onClose={() => setError(null)}>
            {error}
          </Alert>
        )}
        
        {loading ? (
          <Box sx={{ display: 'flex', justifyContent: 'center', p: 4 }}>
            <CircularProgress />
          </Box>
        ) : (
          <Box sx={{ pt: 2 }}>
            {!investmentId && investments && (
              <FormControl fullWidth sx={{ mb: 3 }}>
                <InputLabel>Select Investment</InputLabel>
                <Select
                  value={selectedInvestmentId}
                  label="Select Investment"
                  onChange={(e) => setSelectedInvestmentId(e.target.value)}
                >
                  {investments.map((inv) => (
                    <MenuItem key={inv.id} value={inv.id}>
                      {inv.name} ({inv.type})
                    </MenuItem>
                  ))}
                </Select>
              </FormControl>
            )}
            
            {(investmentId || selectedInvestmentId) && (
              <TransactionForm
                onSubmit={handleSubmit}
                onCancel={onClose}
                investmentType={getInvestmentType()}
              />
            )}
          </Box>
        )}
      </DialogContent>
    </Dialog>
  );
};

export default TransactionDialog;
