import React, { useState, useEffect } from 'react';
import {
  Box,
  Typography,
  Button,
  Alert,
  CircularProgress,
  Card,
  CardContent,
  Grid,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  Chip,
  Divider,
  FormControl,
  InputLabel,
  Select,
  MenuItem,
} from '@mui/material';
import { Add as AddIcon, Upload as UploadIcon, Edit as EditIcon, Delete as DeleteIcon } from '@mui/icons-material';
import ExpenseDialog from '../components/expenses/ExpenseDialog';
import ExpenseList from '../components/expenses/ExpenseList';
import ExpenseMappingDialog from '../components/expenses/ExpenseMappingDialog';
import ExpenseImportDialog from '../components/expenses/ExpenseImportDialog';
import { expenseService } from '../services/expenseService';
import { formatCurrency, formatCurrencyWithSign, formatPercent, formatPercentPlain } from '../utils/currencyFormatter';

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
  const [importDialogOpen, setImportDialogOpen] = useState(false);
  const [selectedExpense, setSelectedExpense] = useState(null);
  const [categoryFilter, setCategoryFilter] = useState('');
  const [analytics, setAnalytics] = useState(null);
  const [categoryMix, setCategoryMix] = useState(null);
  const [analyticsLoading, setAnalyticsLoading] = useState(true);

  const baseCurrency = localStorage.getItem('baseCurrency') || 'INR';

  useEffect(() => {
    fetchExpenses();
    fetchAnalytics();
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

  const fetchAnalytics = async () => {
    setAnalyticsLoading(true);
    try {
      const [a, m] = await Promise.all([
        expenseService.getAnalytics(baseCurrency),
        expenseService.getCategoryMix(baseCurrency),
      ]);
      setAnalytics(a);
      setCategoryMix(m);
    } catch (err) {
      console.error('Error fetching expense analytics:', err);
    } finally {
      setAnalyticsLoading(false);
    }
  };

  const refreshAll = async () => {
    setLoading(true);
    setAnalyticsLoading(true);
    try {
      await Promise.all([
        expenseService.getAll(categoryFilter || null),
        expenseService.getAnalytics(baseCurrency),
        expenseService.getCategoryMix(baseCurrency),
      ]).then(([expenses]) => setExpenses(expenses));
    } catch (err) {
      console.error('Error refreshing expenses:', err);
      setError('Failed to refresh. Please try again.');
    } finally {
      setLoading(false);
      setAnalyticsLoading(false);
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
            variant="outlined"
            startIcon={<UploadIcon />}
            onClick={() => setImportDialogOpen(true)}
          >
            Import Excel
          </Button>
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

      <ExpenseAnalyticsPanels
        analytics={analytics}
        categoryMix={categoryMix}
        currency={baseCurrency}
        loading={analyticsLoading}
        onRefresh={fetchAnalytics}
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

      <ExpenseImportDialog
        open={importDialogOpen}
        onClose={() => setImportDialogOpen(false)}
        onImported={() => refreshAll()}
      />
    </Box>
  );
};

/**
 * Expense growth + analytics panels (E14).
 *
 * Shows:
 *  - Latest month summary + month-over-month growth (the MoM change as both
 *    an absolute delta and a percentage).
 *  - Year-over-year growth for the two most recent full years.
 *  - Category mix for the latest full year as both a ranked list and a small
 *    pie.
 *  - Judgement-call roll-ups: average monthly spend over the reported range,
 *    the single biggest spending category, and how many months had any spend.
 */
const ExpenseAnalyticsPanels = ({ analytics, categoryMix, currency, loading, onRefresh }) => {
  if (loading) {
    return (
      <Box display="flex" justifyContent="center" alignItems="center" py={4}>
        <CircularProgress size={20} />
      </Box>
    );
  }

  if (!analytics) {
    return null;
  }

  const latestMonth = analytics.latestMonth;
  const priorMonth = analytics.priorMonth;
  const latestMonthTotal = analytics.latestMonthTotal || 0;
  const priorMonthTotal = analytics.priorMonthTotal || 0;
  const monthOverMonth = analytics.monthOverMonth;
  const yearOverYear = analytics.yearOverYear;
  const avgMonthly = analytics.avgMonthly || 0;
  const biggestCategory = analytics.biggestCategory;
  const activeMonths = analytics.activeMonths || 0;
  const categoryTotals = analytics.categoryTotals || {};

  const latestMonthDelta = latestMonthTotal - priorMonthTotal;
  const momPct = typeof monthOverMonth === 'number' ? monthOverMonth * 100 : null;
  const yoyPct = typeof yearOverYear === 'number' ? yearOverYear * 100 : null;
  const signedLatestMonthDelta = formatCurrencyWithSign(latestMonthDelta, currency);
  const momGrowthText = momPct != null ? formatPercent(momPct) : '—';
  const yoyGrowthText = yoyPct != null ? formatPercent(yoyPct) : '—';
  const monthOverMonthPositive = momPct != null && momPct > 0;
  const yearOverYearPositive = yoyPct != null && yoyPct > 0;

  const categoryRows = Object.entries(categoryTotals)
    .sort((a, b) => b[1] - a[1])
    .map(([category, amount]) => {
      const pct = categoryMix[category];
      const pctText = typeof pct === 'number' ? formatPercentPlain(pct, 1) : '—';
      return { category, amount, pct, pctText };
    });

  const biggestText = biggestCategory
    ? `${formatCurrency(biggestCategory.amount, currency)} (${biggestCategory.category})`
    : '—';

  return (
    <Box sx={{ mt: 4 }}>
      <Typography variant="h6" sx={{ mb: 1 }}>
        Expense Analytics
      </Typography>
      <Typography variant="body2" color="text.secondary" sx={{ mb: 3 }}>
        Growth trends, category mix and roll-up stats for the latest full month against the month before.
      </Typography>

      <Grid container spacing={3}>
        {/* Latest month summary + MoM growth */}
        <Grid size={{ xs: 12, sm: 6 }}>
          <Card sx={{ height: '100%' }}>
            <CardContent>
              <Typography variant="subtitle2" color="text.secondary" sx={{ mb: 1 }}>
                Latest month — {latestMonth}
              </Typography>

              <Box sx={{ display: 'flex', alignItems: 'baseline', gap: 1, mb: 2 }}>
                <Typography variant="h5">{formatCurrency(latestMonthTotal, currency)}</Typography>
                <Typography variant="body2" color="text.secondary">
                  {activeMonths} active months in report window
                </Typography>
              </Box>

              <Divider sx={{ my: 2 }} />

              <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                <Typography variant="body2" color="text.secondary">
                  vs {priorMonth}
                </Typography>
                <Chip
                  label={signedLatestMonthDelta}
                  color={latestMonthDelta > 0 ? 'error' : latestMonthDelta < 0 ? 'success' : 'default'}
                  size="small"
                />
              </Box>

              <Typography variant="h6" sx={{ mt: 1, mb: 1 }}>
                Month-over-month growth
              </Typography>
              <Typography variant="body1">
                {momGrowthText}
              </Typography>
              <Typography variant="body2" color="text.secondary" sx={{ mt: 1 }}>
                {latestMonthDelta > 0 && 'Spend increased this month.'}
                {latestMonthDelta < 0 && 'Spend decreased this month.'}
                {latestMonthDelta === 0 && 'No change versus the prior month.'}
              </Typography>
            </CardContent>
          </Card>
        </Grid>

        {/* Year-over-year growth */}
        <Grid size={{ xs: 12, sm: 6 }}>
          <Card sx={{ height: '100%' }}>
            <CardContent>
              <Typography variant="subtitle2" color="text.secondary" sx={{ mb: 1 }}>
                Year-over-year growth
              </Typography>

              <Typography variant="body2" color="text.secondary" sx={{ mb: 2 }}>
                Most recent full year vs the prior full year.
              </Typography>

              <Box sx={{ display: 'flex', alignItems: 'baseline', gap: 1, mb: 2 }}>
                <Typography variant="h4">{yoyGrowthText}</Typography>
              </Box>

              <Typography variant="body2" color="text.secondary">
                {yearOverYearPositive && 'Expenses grew year-on-year.'}
                {!yearOverYearPositive && yearOverYear != null && 'Expenses fell year-on-year.'}
                {yearOverYear == null && 'Not enough history to compare years yet.'}
              </Typography>
            </CardContent>
          </Card>
        </Grid>

        {/* Category mix — rank list */}
        <Grid size={{ xs: 12, md: 6 }}>
          <Card sx={{ height: '100%' }}>
            <CardContent>
              <Typography variant="subtitle2" color="text.secondary" sx={{ mb: 1 }}>
                Category mix — latest full year
              </Typography>

              {categoryRows.length === 0 ? (
                <Typography variant="body2" color="text.secondary">
                  No expenses recorded yet for the latest full year.
                </Typography>
              ) : (
                <TableContainer>
                  <Table size="small">
                    <TableHead>
                      <TableRow>
                        <TableCell>Category</TableCell>
                        <TableCell align="right">Amount</TableCell>
                        <TableCell align="right">Share</TableCell>
                      </TableRow>
                    </TableHead>
                    <TableBody>
                      {categoryRows.map((row) => (
                        <TableRow key={row.category} hover>
                          <TableCell>{row.category}</TableCell>
                          <TableCell align="right">{formatCurrency(row.amount, currency)}</TableCell>
                          <TableCell align="right">{row.pctText}</TableCell>
                        </TableRow>
                      ))}
                    </TableBody>
                  </Table>
                </TableContainer>
              )}
            </CardContent>
          </Card>
        </Grid>

        {/* Roll-up stats */}
        <Grid size={{ xs: 12, md: 6 }}>
          <Card sx={{ height: '100%' }}>
            <CardContent>
              <Typography variant="subtitle2" color="text.secondary" sx={{ mb: 1 }}>
                Roll-up stats
              </Typography>

              <Box sx={{ display: 'flex', flexDirection: 'column', gap: 2 }}>
                <Box>
                  <Typography variant="body2" color="text.secondary">
                    Average monthly spend (reported window)
                  </Typography>
                  <Typography variant="h6">{formatCurrency(avgMonthly, currency)}</Typography>
                </Box>
                <Box>
                  <Typography variant="body2" color="text.secondary">
                    Biggest spending category (latest full year)
                  </Typography>
                  <Typography variant="body1">{biggestText}</Typography>
                </Box>
                <Box>
                  <Typography variant="body2" color="text.secondary">
                    Active months in report window
                  </Typography>
                  <Typography variant="body1">{activeMonths} months</Typography>
                </Box>
              </Box>

              <Button variant="outlined" size="small" onClick={onRefresh} sx={{ mt: 2 }}>
                Refresh analytics
              </Button>
            </CardContent>
          </Card>
        </Grid>
      </Grid>
    </Box>
  );
};

export default ExpensesPage;
