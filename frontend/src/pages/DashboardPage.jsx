import React, { useState, useEffect } from 'react';
import { Box, Typography, Grid, CircularProgress, Alert, FormControl, InputLabel, Select, MenuItem, Button, Snackbar } from '@mui/material';
import { Sync as SyncIcon } from '@mui/icons-material';
import NetWorthCard from '../components/dashboard/NetWorthCard';
import SegmentBreakdownCard from '../components/dashboard/SegmentBreakdownCard';
import GrowthChartCard from '../components/dashboard/GrowthChartCard';
import RecentTransactionsCard from '../components/dashboard/RecentTransactionsCard';
import AssetAllocationCard from '../components/dashboard/AssetAllocationCard';
import ExpenseCoverageCard from '../components/expenses/ExpenseCoverageCard';
import ExpenseCategoryChart from '../components/expenses/ExpenseCategoryChart';
import { analyticsService } from '../services/analyticsService';
import { transactionService } from '../services/transactionService';
import { expenseService } from '../services/expenseService';
import marketDataService from '../services/marketDataService';

const DashboardPage = () => {
  const [currency, setCurrency] = useState('INR');
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [updating, setUpdating] = useState(false);
  const [updateMessage, setUpdateMessage] = useState('');
  const [showUpdateSnackbar, setShowUpdateSnackbar] = useState(false);
  const [data, setData] = useState({
    netWorth: null,
    segments: null,
    growth: null,
    segmentImpact: null,
    recentTransactions: [],
    expenseCoverage: null,
    categoryBreakdown: null
  });

  useEffect(() => {
    fetchDashboardData();
  }, [currency]);

  const fetchDashboardData = async () => {
    setLoading(true);
    setError(null);
    
    try {
      // Fetch each piece of data with individual error handling
      const netWorthPromise = analyticsService.getNetWorth(currency).catch(err => {
        console.error('NetWorth error:', err);
        return { totalValue: 0, growth: 0, growthPercentage: 0, currency };
      });
      
      const segmentsPromise = analyticsService.getSegmentBreakdown(currency).catch(err => {
        console.error('Segments error:', err);
        return { segments: {}, total: 0, currency };
      });
      
      const growthPromise = analyticsService.getGrowthMetrics(5, currency).catch(err => {
        console.error('Growth error:', err);
        return { historicalData: [], yoyGrowth: 0, yoyGrowthPercentage: 0, cagr: 0, currency };
      });
      
      const segmentImpactPromise = analyticsService.getSegmentImpact(currency).catch(err => {
        console.error('Segment Impact error:', err);
        return { segmentContributions: {}, segmentReturns: {}, currency };
      });
      
      const transactionsPromise = transactionService.getAllTransactions().catch(err => {
        console.error('Transactions error:', err);
        return [];
      });
      
      const expenseCoveragePromise = expenseService.getCoverage(null, null, currency).catch(err => {
        console.error('Expense Coverage error:', err);
        return null;
      });
      
      const categoryBreakdownPromise = expenseService.getCategoryBreakdown().catch(err => {
        console.error('Category Breakdown error:', err);
        return null;
      });

      const [netWorth, segments, growth, segmentImpact, transactions, expenseCoverage, categoryBreakdown] = await Promise.all([
        netWorthPromise,
        segmentsPromise,
        growthPromise,
        segmentImpactPromise,
        transactionsPromise,
        expenseCoveragePromise,
        categoryBreakdownPromise
      ]);

      setData({
        netWorth,
        segments,
        growth,
        segmentImpact,
        recentTransactions: Array.isArray(transactions) ? transactions.slice(0, 5) : [],
        expenseCoverage,
        categoryBreakdown
      });
    } catch (err) {
      console.error('Error fetching dashboard data:', err);
      setError(`Failed to load dashboard data: ${err.message}`);
    } finally {
      setLoading(false);
    }
  };

  const handleCurrencyChange = (event) => {
    setCurrency(event.target.value);
  };

  const handleUpdatePrices = async () => {
    setUpdating(true);
    try {
      const result = await marketDataService.triggerPriceUpdate();
      
      if (result.status === 'SUCCESS') {
        setUpdateMessage(`✓ Prices updated successfully! ${result.successCount}/${result.totalCount} symbols updated.`);
      } else if (result.status === 'PARTIAL_SUCCESS') {
        setUpdateMessage(`⚠ Partial update: ${result.successCount}/${result.totalCount} symbols updated.`);
      } else {
        setUpdateMessage(`✗ Update failed: ${result.errorMessage || 'Unknown error'}`);
      }
      
      setShowUpdateSnackbar(true);
      
      // Refresh dashboard data after successful update
      if (result.status !== 'FAILURE') {
        setTimeout(() => fetchDashboardData(), 2000);
      }
    } catch (err) {
      console.error('Price update error:', err);
      setUpdateMessage(`✗ Failed to update prices: ${err.response?.data?.message || err.message}`);
      setShowUpdateSnackbar(true);
    } finally {
      setUpdating(false);
    }
  };

  const handleCloseSnackbar = () => {
    setShowUpdateSnackbar(false);
  };

  if (loading) {
    return (
      <Box display="flex" justifyContent="center" alignItems="center" minHeight="400px">
        <CircularProgress />
      </Box>
    );
  }

  if (error) {
    return (
      <Box>
        <Typography variant="h4" mb={3}>
          Portfolio Dashboard
        </Typography>
        <Alert severity="info" sx={{ mb: 2 }}>
          {error}
        </Alert>
        <Typography variant="body2" color="text.secondary">
          Make sure you have added some investments to see your dashboard data.
        </Typography>
      </Box>
    );
  }

  return (
    <Box>
      <Box display="flex" justifyContent="space-between" alignItems="center" mb={3}>
        <Typography variant="h4">
          Portfolio Dashboard
        </Typography>
        
        <Box display="flex" gap={2} alignItems="center">
          <Button
            variant="outlined"
            startIcon={updating ? <CircularProgress size={16} /> : <SyncIcon />}
            onClick={handleUpdatePrices}
            disabled={updating}
            size="small"
          >
            {updating ? 'Updating...' : 'Update Prices'}
          </Button>
          
          <FormControl size="small" sx={{ minWidth: 120 }}>
            <InputLabel>Currency</InputLabel>
            <Select
              value={currency}
              label="Currency"
              onChange={handleCurrencyChange}
            >
              <MenuItem value="INR">INR (₹)</MenuItem>
              <MenuItem value="USD">USD ($)</MenuItem>
              <MenuItem value="EUR">EUR (€)</MenuItem>
              <MenuItem value="GBP">GBP (£)</MenuItem>
            </Select>
          </FormControl>
        </Box>
      </Box>
      
      <Grid container spacing={3}>
        {/* Net Worth Card */}
        <Grid item xs={12} md={4}>
          <NetWorthCard
            value={data.netWorth?.totalValue || 0}
            change={data.netWorth?.growth || 0}
            changePercent={data.netWorth?.growthPercentage || 0}
            currency={currency}
          />
        </Grid>

        {/* Expense Coverage Card */}
        <Grid item xs={12} md={4}>
          <ExpenseCoverageCard
            coverage={data.expenseCoverage}
            currency={currency}
          />
        </Grid>

        {/* Segment Breakdown */}
        <Grid item xs={12} md={4}>
          <SegmentBreakdownCard 
            data={data.segments?.segments || {}}
            currency={currency}
          />
        </Grid>

        {/* Growth Chart */}
        <Grid item xs={12} md={8}>
          <GrowthChartCard 
            data={data.growth?.historicalData || []}
            currency={currency}
          />
        </Grid>

        {/* Expense Category Chart */}
        <Grid item xs={12} md={4}>
          <ExpenseCategoryChart
            categoryBreakdown={data.categoryBreakdown}
            currency={currency}
          />
        </Grid>

        {/* Recent Transactions */}
        <Grid item xs={12} md={6}>
          <RecentTransactionsCard transactions={data.recentTransactions} />
        </Grid>

        {/* Asset Allocation */}
        <Grid item xs={12} md={6}>
          <AssetAllocationCard 
            data={data.segments?.segments || {}}
            currency={currency}
          />
        </Grid>
      </Grid>

      <Snackbar
        open={showUpdateSnackbar}
        autoHideDuration={6000}
        onClose={handleCloseSnackbar}
        message={updateMessage}
        anchorOrigin={{ vertical: 'bottom', horizontal: 'right' }}
      />
    </Box>
  );
};

export default DashboardPage;

