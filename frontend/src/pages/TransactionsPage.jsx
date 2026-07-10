import React, { useState, useEffect } from 'react';
import { Box, Typography, Button, Paper, Alert } from '@mui/material';
import { Add as AddIcon } from '@mui/icons-material';
import TransactionHistory from '../components/transactions/TransactionHistory';
import TransactionDialog from '../components/transactions/TransactionDialog';
import { transactionService } from '../services/transactionService';
import { investmentService } from '../services/investmentService';
import Loading from '../components/common/Loading';

const TransactionsPage = () => {
  const [openDialog, setOpenDialog] = useState(false);
  const [transactions, setTransactions] = useState([]);
  const [investments, setInvestments] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [selectedInvestmentId, setSelectedInvestmentId] = useState(null);

  const fetchTransactions = async () => {
    setLoading(true);
    setError(null);
    try {
      const [transactionsData, investmentsData] = await Promise.all([
        transactionService.getAllTransactions(),
        investmentService.getAll()
      ]);
      setTransactions(transactionsData);
      setInvestments(investmentsData);
    } catch (err) {
      console.error('Error fetching transactions:', err);
      setError(err.response?.data?.error || err.message || 'Failed to load transactions');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchTransactions();
  }, []);

  const handleTransactionCreated = () => {
    fetchTransactions();
  };

  const handleEdit = (transaction) => {
    console.log('Edit transaction:', transaction);
    // TODO: Implement edit functionality
  };

  const handleDelete = async (transaction) => {
    if (window.confirm('Are you sure you want to delete this transaction?')) {
      try {
        await transactionService.deleteTransaction(transaction.id);
        await fetchTransactions();
      } catch (err) {
        console.error('Error deleting transaction:', err);
        setError(err.response?.data?.error || err.message || 'Failed to delete transaction');
      }
    }
  };

  if (loading) {
    return <Loading />;
  }

  return (
    <Box>
      <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 3 }}>
        <Typography variant="h4">
          Transactions
        </Typography>
        <Button
          variant="contained"
          startIcon={<AddIcon />}
          onClick={() => setOpenDialog(true)}
          disabled={investments.length === 0}
        >
          Add Transaction
        </Button>
      </Box>

      {error && (
        <Alert severity="error" sx={{ mb: 2 }} onClose={() => setError(null)}>
          {error}
        </Alert>
      )}

      <Paper sx={{ p: 2, mb: 3 }}>
        <Typography variant="body2" color="text.secondary">
          Track all your investment transactions including buys, sells, dividends, interest payments, and contributions.
        </Typography>
      </Paper>

      {investments.length === 0 ? (
        <Box sx={{ textAlign: 'center', py: 8 }}>
          <Typography variant="h6" color="text.secondary" gutterBottom>
            No investments yet
          </Typography>
          <Typography color="text.secondary" paragraph>
            Create an investment first before adding transactions
          </Typography>
        </Box>
      ) : transactions.length === 0 ? (
        <Box sx={{ textAlign: 'center', py: 8 }}>
          <Typography variant="h6" color="text.secondary" gutterBottom>
            No transactions yet
          </Typography>
          <Typography color="text.secondary" paragraph>
            Start by adding your first transaction
          </Typography>
          <Button
            variant="contained"
            startIcon={<AddIcon />}
            onClick={() => setOpenDialog(true)}
          >
            Add Transaction
          </Button>
        </Box>
      ) : (
        <TransactionHistory
          transactions={transactions}
          investments={investments}
          onEdit={handleEdit}
          onDelete={handleDelete}
        />
      )}

      {selectedInvestmentId && (
        <TransactionDialog
          open={openDialog}
          onClose={() => {
            setOpenDialog(false);
            setSelectedInvestmentId(null);
          }}
          investmentId={selectedInvestmentId}
          investmentType={investments.find(inv => inv.id === selectedInvestmentId)?.type}
          investments={investments}
          onSuccess={handleTransactionCreated}
        />
      )}

      <TransactionDialog
        open={openDialog && !selectedInvestmentId}
        onClose={() => setOpenDialog(false)}
        investments={investments}
        onSuccess={handleTransactionCreated}
      />
    </Box>
  );
};

export default TransactionsPage;
