import React, { useState, useEffect } from 'react';
import {
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  Button,
  IconButton,
  Box,
  Typography,
  TextField,
  MenuItem,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  Paper,
  Alert,
  CircularProgress,
  LinearProgress,
} from '@mui/material';
import {
  Close as CloseIcon,
  Delete as DeleteIcon,
  Add as AddIcon,
} from '@mui/icons-material';
import { expenseService } from '../../services/expenseService';
import { investmentService } from '../../services/investmentService';
import { formatCurrency } from '../../utils/currencyFormatter';

const ExpenseMappingDialog = ({ open, onClose, expense }) => {
  const [investments, setInvestments] = useState([]);
  const [mappings, setMappings] = useState([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);
  const [selectedInvestment, setSelectedInvestment] = useState('');
  const [percentage, setPercentage] = useState('');
  const [notes, setNotes] = useState('');

  useEffect(() => {
    if (open && expense) {
      fetchData();
    }
  }, [open, expense]);

  const fetchData = async () => {
    setLoading(true);
    setError(null);
    
    try {
      const [investmentsData, mappingsData] = await Promise.all([
        investmentService.getAllInvestments(),
        expenseService.getMappings(expense.id),
      ]);
      
      setInvestments(investmentsData);
      setMappings(mappingsData);
    } catch (err) {
      console.error('Error fetching data:', err);
      setError('Failed to load data. Please try again.');
    } finally {
      setLoading(false);
    }
  };

  const totalMapped = mappings.reduce((sum, m) => sum + parseFloat(m.percentage), 0);
  const remainingPercentage = 100 - totalMapped;

  const handleAddMapping = async () => {
    if (!selectedInvestment || !percentage) {
      setError('Please select an investment and enter a percentage');
      return;
    }

    const percentageValue = parseFloat(percentage);
    
    if (percentageValue <= 0 || percentageValue > remainingPercentage) {
      setError(`Percentage must be between 0 and ${remainingPercentage}%`);
      return;
    }

    try {
      await expenseService.addMapping({
        expenseId: expense.id,
        investmentId: selectedInvestment,
        percentage: percentageValue,
        notes,
      });

      // Reset form
      setSelectedInvestment('');
      setPercentage('');
      setNotes('');
      
      // Refresh mappings
      fetchData();
    } catch (err) {
      console.error('Error adding mapping:', err);
      setError(err.response?.data?.message || 'Failed to add mapping. Please try again.');
    }
  };

  const handleDeleteMapping = async (mappingId) => {
    try {
      await expenseService.deleteMapping(mappingId);
      fetchData();
    } catch (err) {
      console.error('Error deleting mapping:', err);
      setError('Failed to delete mapping. Please try again.');
    }
  };

  if (!expense) return null;

  return (
    <Dialog open={open} onClose={onClose} maxWidth="md" fullWidth>
      <DialogTitle>
        Map Expense to Investments
        <IconButton
          onClick={onClose}
          sx={{ position: 'absolute', right: 8, top: 8 }}
        >
          <CloseIcon />
        </IconButton>
      </DialogTitle>
      
      <DialogContent>
        {loading ? (
          <Box display="flex" justifyContent="center" py={4}>
            <CircularProgress />
          </Box>
        ) : (
          <>
            <Typography variant="body2" color="text.secondary" gutterBottom>
              <strong>Expense:</strong> {expense.description} ({formatCurrency(expense.amount, expense.currency)})
            </Typography>

            <Box sx={{ my: 2 }}>
              <Typography variant="body2" gutterBottom>
                Total Mapped: {totalMapped.toFixed(2)}%
              </Typography>
              <LinearProgress
                variant="determinate"
                value={totalMapped}
                color={totalMapped === 100 ? 'success' : 'primary'}
                sx={{ height: 10, borderRadius: 1 }}
              />
              <Typography variant="caption" color="text.secondary">
                Remaining: {remainingPercentage.toFixed(2)}%
              </Typography>
            </Box>

            {error && (
              <Alert severity="error" sx={{ mb: 2 }} onClose={() => setError(null)}>
                {error}
              </Alert>
            )}

            {/* Existing Mappings */}
            {mappings.length > 0 && (
              <Box sx={{ mb: 3 }}>
                <Typography variant="subtitle2" gutterBottom>
                  Current Mappings
                </Typography>
                <TableContainer component={Paper} variant="outlined">
                  <Table size="small">
                    <TableHead>
                      <TableRow>
                        <TableCell>Investment</TableCell>
                        <TableCell align="right">Percentage</TableCell>
                        <TableCell>Notes</TableCell>
                        <TableCell align="center">Actions</TableCell>
                      </TableRow>
                    </TableHead>
                    <TableBody>
                      {mappings.map((mapping) => (
                        <TableRow key={mapping.id}>
                          <TableCell>{mapping.investmentName}</TableCell>
                          <TableCell align="right">{mapping.percentage}%</TableCell>
                          <TableCell>{mapping.notes || '-'}</TableCell>
                          <TableCell align="center">
                            <IconButton
                              size="small"
                              onClick={() => handleDeleteMapping(mapping.id)}
                              color="error"
                            >
                              <DeleteIcon fontSize="small" />
                            </IconButton>
                          </TableCell>
                        </TableRow>
                      ))}
                    </TableBody>
                  </Table>
                </TableContainer>
              </Box>
            )}

            {/* Add New Mapping */}
            {remainingPercentage > 0 && (
              <Box>
                <Typography variant="subtitle2" gutterBottom>
                  Add Mapping
                </Typography>
                <Box sx={{ display: 'flex', flexDirection: 'column', gap: 2 }}>
                  <TextField
                    select
                    label="Investment"
                    value={selectedInvestment}
                    onChange={(e) => setSelectedInvestment(e.target.value)}
                    fullWidth
                    size="small"
                  >
                    {investments.map((inv) => (
                      <MenuItem key={inv.id} value={inv.id}>
                        {inv.name} ({inv.type})
                      </MenuItem>
                    ))}
                  </TextField>

                  <TextField
                    label="Percentage"
                    type="number"
                    value={percentage}
                    onChange={(e) => setPercentage(e.target.value)}
                    fullWidth
                    size="small"
                    inputProps={{ min: 0, max: remainingPercentage, step: 0.01 }}
                    helperText={`Available: ${remainingPercentage.toFixed(2)}%`}
                  />

                  <TextField
                    label="Notes (Optional)"
                    value={notes}
                    onChange={(e) => setNotes(e.target.value)}
                    fullWidth
                    size="small"
                    multiline
                    rows={2}
                  />

                  <Button
                    variant="contained"
                    startIcon={<AddIcon />}
                    onClick={handleAddMapping}
                    disabled={!selectedInvestment || !percentage}
                  >
                    Add Mapping
                  </Button>
                </Box>
              </Box>
            )}
          </>
        )}
      </DialogContent>

      <DialogActions>
        <Button onClick={onClose}>Close</Button>
      </DialogActions>
    </Dialog>
  );
};

export default ExpenseMappingDialog;
