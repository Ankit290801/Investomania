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
  Divider,
  FormControl,
  InputLabel,
  Select,
  MenuItem,
} from '@mui/material';
import {
  Add as AddIcon,
  Upload as UploadIcon,
  TrendingUp as TrendingUpIcon,
  TrendingDown as TrendingDownIcon,
} from '@mui/icons-material';
import ExpenseDialog from '../components/expenses/ExpenseDialog';
import ExpenseList from '../components/expenses/ExpenseList';
import ExpenseMappingDialog from '../components/expenses/ExpenseMappingDialog';
import ExpenseImportDialog from '../components/expenses/ExpenseImportDialog';
import { expenseService } from '../services/expenseService';
import { formatCurrency, formatCurrencyWithSign, formatPercent, formatPercentPlain } from '../utils/currencyFormatter';
import { seriesColor } from '../utils/chartHelpers';

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

  // Single refresh path for every mutation/import: the expense list and the
  // analytics panels always move together so the panels can never show stale
  // numbers after an expense is added, edited or deleted.
  const refreshAll = async () => {
    await Promise.all([fetchExpenses(), fetchAnalytics()]);
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
      await refreshAll();
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
      await refreshAll();
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
    refreshAll(); // Refresh the list and the analytics after mapping changes
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
      {/* E8: tour anchor — header row (filter + import/add buttons) is the
          Expenses stop of the site guide. */}
      <Box display="flex" justifyContent="space-between" alignItems="center" mb={3} data-tour="expenses-header">
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

      <ExpenseAnalyticsPanels
        analytics={analytics}
        categoryMix={categoryMix}
        currency={baseCurrency}
        loading={analyticsLoading}
      />

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
 * Sits above the expense table (Equity-screen shape: summary cards first,
 * detail table below) and follows the app's F1 card conventions — overline
 * headings, tabular figures, theme tokens and seriesColor category dots.
 *
 * Shows:
 *  - Latest month summary + month-over-month growth (the MoM change as both
 *    an absolute delta and a percentage).
 *  - Year-over-year growth for the two most recent full years.
 *  - Category mix for the latest full year as a ranked table.
 *  - Roll-ups: average monthly spend, the single biggest spending category,
 *    and how many months had any spend.
 */
const ExpenseAnalyticsPanels = ({ analytics, categoryMix, currency, loading }) => {
  if (loading) {
    return (
      <Box sx={{ display: 'flex', justifyContent: 'center', alignItems: 'center', py: 4, mb: 3 }}>
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
  const momGrowthText = momPct != null ? formatPercent(momPct) : '—';
  const yoyGrowthText = yoyPct != null ? formatPercent(yoyPct) : '—';

  // For expenses, "up" is the worrying direction: more spend reads red, less
  // spend reads green, flat reads neutral. Tokens only (no raw colours).
  const expenseTrendColor = (pct) =>
    pct == null || pct === 0 ? 'text.secondary' : pct > 0 ? 'error.main' : 'success.main';

  const momExplanation =
    momPct == null
      ? 'Not enough history to compare months yet.'
      : momPct > 0
      ? 'Spend increased versus the prior month.'
      : momPct < 0
      ? 'Spend decreased versus the prior month.'
      : 'No change versus the prior month.';

  const yoyExplanation =
    yoyPct == null
      ? 'Not enough history to compare years yet.'
      : yoyPct > 0
      ? 'Expenses grew year-on-year.'
      : yoyPct < 0
      ? 'Expenses fell year-on-year.'
      : 'Expenses were flat year-on-year.';

  const categoryRows = Object.entries(categoryTotals)
    .sort((a, b) => b[1] - a[1])
    .map(([category, amount]) => {
      const pct = categoryMix ? categoryMix[category] : undefined;
      const pctText = typeof pct === 'number' ? formatPercentPlain(pct, 1) : '—';
      return { category, amount, pctText };
    });

  const biggestText = biggestCategory
    ? `${formatCurrency(biggestCategory.amount, currency)} (${biggestCategory.category})`
    : '—';

  return (
    <Grid container spacing={3} sx={{ mb: 3 }}>
      {/* Latest month summary + MoM growth */}
      <Grid item xs={12} md={6}>
        <Card sx={{ height: '100%' }}>
          <CardContent>
            <Typography variant="overline" component="div" sx={{ color: 'text.secondary', mb: 0.5 }}>
              Latest month — {latestMonth}
            </Typography>

            <Typography variant="h5" sx={{ fontVariantNumeric: 'tabular-nums' }}>
              {formatCurrency(latestMonthTotal, currency)}
            </Typography>

            <Divider sx={{ my: 2 }} />

            <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'baseline' }}>
              <Typography variant="body2" color="text.secondary">
                vs {priorMonth}
              </Typography>
              <Typography
                variant="body2"
                fontWeight={600}
                sx={{ color: expenseTrendColor(latestMonthDelta), fontVariantNumeric: 'tabular-nums' }}
              >
                {formatCurrencyWithSign(latestMonthDelta, currency)}
              </Typography>
            </Box>

            <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mt: 1.5 }}>
              <Typography variant="body2" color="text.secondary">
                Month-over-month
              </Typography>
              <Box sx={{ display: 'flex', alignItems: 'center', gap: 0.5 }}>
                {momPct != null && momPct !== 0 &&
                  (momPct > 0 ? (
                    <TrendingUpIcon sx={{ fontSize: 18, color: 'error.main' }} />
                  ) : (
                    <TrendingDownIcon sx={{ fontSize: 18, color: 'success.main' }} />
                  ))}
                <Typography
                  variant="body1"
                  fontWeight={600}
                  sx={{ color: expenseTrendColor(momPct), fontVariantNumeric: 'tabular-nums' }}
                >
                  {momGrowthText}
                </Typography>
              </Box>
            </Box>

            <Typography variant="caption" color="text.secondary" sx={{ display: 'block', mt: 1 }}>
              {momExplanation}
            </Typography>
          </CardContent>
        </Card>
      </Grid>

      {/* Year-over-year growth */}
      <Grid item xs={12} md={6}>
        <Card sx={{ height: '100%' }}>
          <CardContent>
            <Typography variant="overline" component="div" sx={{ color: 'text.secondary', mb: 0.5 }}>
              Year-over-year growth
            </Typography>

            <Box sx={{ display: 'flex', alignItems: 'center', gap: 1, mt: 1 }}>
              {yoyPct != null && yoyPct !== 0 &&
                (yoyPct > 0 ? (
                  <TrendingUpIcon sx={{ fontSize: 28, color: 'error.main' }} />
                ) : (
                  <TrendingDownIcon sx={{ fontSize: 28, color: 'success.main' }} />
                ))}
              <Typography
                variant="h4"
                sx={{ color: expenseTrendColor(yoyPct), fontVariantNumeric: 'tabular-nums' }}
              >
                {yoyGrowthText}
              </Typography>
            </Box>

            <Typography variant="caption" color="text.secondary" sx={{ display: 'block', mt: 1 }}>
              Most recent full year vs the prior full year.
            </Typography>

            <Typography variant="body2" color="text.secondary" sx={{ mt: 2 }}>
              {yoyExplanation}
            </Typography>
          </CardContent>
        </Card>
      </Grid>

      {/* Category mix — ranked list */}
      <Grid item xs={12} md={6}>
        <Card sx={{ height: '100%' }}>
          <CardContent>
            <Typography variant="overline" component="div" sx={{ color: 'text.secondary', mb: 0.5 }}>
              Category mix — latest full year
            </Typography>

            {categoryRows.length === 0 ? (
              <Typography variant="body2" color="text.secondary" sx={{ mt: 1 }}>
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
                    {categoryRows.map((row, index) => (
                      <TableRow key={row.category} hover>
                        <TableCell>
                          <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                            <Box
                              sx={{
                                width: 9,
                                height: 9,
                                borderRadius: '50%',
                                backgroundColor: seriesColor(index),
                                flexShrink: 0,
                              }}
                            />
                            <Typography variant="body2">{row.category}</Typography>
                          </Box>
                        </TableCell>
                        <TableCell align="right" sx={{ fontVariantNumeric: 'tabular-nums' }}>
                          {formatCurrency(row.amount, currency)}
                        </TableCell>
                        <TableCell align="right" sx={{ fontVariantNumeric: 'tabular-nums' }}>
                          {row.pctText}
                        </TableCell>
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
      <Grid item xs={12} md={6}>
        <Card sx={{ height: '100%' }}>
          <CardContent>
            <Typography variant="overline" component="div" sx={{ color: 'text.secondary', mb: 0.5 }}>
              Roll-up stats
            </Typography>

            <Box sx={{ display: 'flex', flexDirection: 'column', gap: 2, mt: 1 }}>
              <Box>
                <Typography variant="body2" color="text.secondary">
                  Average monthly spend
                </Typography>
                <Typography variant="h6" sx={{ fontVariantNumeric: 'tabular-nums' }}>
                  {formatCurrency(avgMonthly, currency)}
                </Typography>
              </Box>
              <Box>
                <Typography variant="body2" color="text.secondary">
                  Biggest spending category
                </Typography>
                <Typography variant="body1" fontWeight={600}>
                  {biggestText}
                </Typography>
              </Box>
              <Box>
                <Typography variant="body2" color="text.secondary">
                  Active months in the reported window
                </Typography>
                <Typography variant="body1" fontWeight={600} sx={{ fontVariantNumeric: 'tabular-nums' }}>
                  {activeMonths} months
                </Typography>
              </Box>
            </Box>
          </CardContent>
        </Card>
      </Grid>
    </Grid>
  );
};

export default ExpensesPage;
