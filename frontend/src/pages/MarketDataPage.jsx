import React, { useState, useEffect } from 'react';
import {
  Box,
  Typography,
  Paper,
  Tabs,
  Tab,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  Button,
  Chip,
  CircularProgress,
  Alert,
  TextField,
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  FormControl,
  InputLabel,
  Select,
  MenuItem,
  IconButton,
  Tooltip
} from '@mui/material';
import {
  Refresh as RefreshIcon,
  Edit as EditIcon,
  Check as CheckIcon,
  Close as CloseIcon,
  TrendingUp as TrendingUpIcon,
  TrendingDown as TrendingDownIcon
} from '@mui/icons-material';
import marketDataService from '../services/marketDataService';
import { useInvestments } from '../hooks/useInvestments';
import { formatCurrency } from '../utils/currencyFormatter';
import { formatDate } from '../utils/dateFormatter';

const MarketDataPage = () => {
  const [activeTab, setActiveTab] = useState(0);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);
  const [prices, setPrices] = useState([]);
  const [updateHistory, setUpdateHistory] = useState([]);
  const [cacheStats, setCacheStats] = useState(null);
  const [manualPriceDialog, setManualPriceDialog] = useState(false);
  const [manualPriceForm, setManualPriceForm] = useState({
    symbol: '',
    price: '',
    currency: 'INR',
    source: 'MANUAL'
  });

  const { investments, loading: investmentsLoading } = useInvestments();

  useEffect(() => {
    if (activeTab === 0) {
      fetchCurrentPrices();
    } else if (activeTab === 2) {
      fetchUpdateHistory();
    } else if (activeTab === 3) {
      fetchCacheStats();
    }
  }, [activeTab]);

  const fetchCurrentPrices = async () => {
    if (!investments || investments.length === 0) return;
    
    setLoading(true);
    setError(null);
    
    try {
      // Build symbol map from investments
      const symbolMap = {};
      investments.forEach(inv => {
        if (inv.symbol) {
          symbolMap[inv.symbol] = inv.market || 'NSE';
        }
      });

      const pricesData = await marketDataService.getBulkPrices(symbolMap);
      
      // Merge with investment data
      const enrichedPrices = investments
        .filter(inv => inv.symbol)
        .map(inv => {
          const priceInfo = pricesData[inv.symbol] || {};
          return {
            ...inv,
            currentPrice: priceInfo.price,
            change: priceInfo.change,
            changePercent: priceInfo.changePercent,
            lastUpdated: priceInfo.lastUpdated,
            source: priceInfo.source
          };
        });

      setPrices(enrichedPrices);
    } catch (err) {
      console.error('Error fetching prices:', err);
      setError(`Failed to fetch prices: ${err.response?.data?.message || err.message}`);
    } finally {
      setLoading(false);
    }
  };

  const fetchUpdateHistory = async () => {
    setLoading(true);
    setError(null);
    
    try {
      const history = await marketDataService.getUpdateHistory(20);
      setUpdateHistory(history);
    } catch (err) {
      console.error('Error fetching update history:', err);
      setError(`Failed to fetch update history: ${err.response?.data?.message || err.message}`);
    } finally {
      setLoading(false);
    }
  };

  const fetchCacheStats = async () => {
    setLoading(true);
    setError(null);
    
    try {
      const stats = await marketDataService.getCacheStatistics();
      setCacheStats(stats);
    } catch (err) {
      console.error('Error fetching cache stats:', err);
      setError(`Failed to fetch cache stats: ${err.response?.data?.message || err.message}`);
    } finally {
      setLoading(false);
    }
  };

  const handleRefreshPrices = async () => {
    await fetchCurrentPrices();
  };

  const handleTriggerUpdate = async () => {
    setLoading(true);
    try {
      const result = await marketDataService.triggerPriceUpdate();
      alert(`Update completed: ${result.successCount}/${result.totalCount} symbols updated`);
      await fetchCurrentPrices();
      await fetchUpdateHistory();
    } catch (err) {
      alert(`Update failed: ${err.response?.data?.message || err.message}`);
    } finally {
      setLoading(false);
    }
  };

  const handleOpenManualPriceDialog = (investment) => {
    setManualPriceForm({
      symbol: investment?.symbol || '',
      price: '',
      currency: investment?.currency || 'INR',
      source: 'MANUAL'
    });
    setManualPriceDialog(true);
  };

  const handleCloseManualPriceDialog = () => {
    setManualPriceDialog(false);
  };

  const handleManualPriceSubmit = async () => {
    try {
      await marketDataService.setManualPrice(manualPriceForm);
      alert('Manual price set successfully');
      setManualPriceDialog(false);
      await fetchCurrentPrices();
    } catch (err) {
      alert(`Failed to set manual price: ${err.response?.data?.message || err.message}`);
    }
  };

  const handleTabChange = (event, newValue) => {
    setActiveTab(newValue);
  };

  const getStatusColor = (status) => {
    switch (status) {
      case 'SUCCESS':
        return 'success';
      case 'PARTIAL_SUCCESS':
        return 'warning';
      case 'FAILURE':
        return 'error';
      default:
        return 'default';
    }
  };

  const getSourceColor = (source) => {
    switch (source) {
      case 'YAHOO_FINANCE':
        return 'primary';
      case 'GOOGLE_FINANCE':
        return 'secondary';
      case 'CACHED':
        return 'default';
      case 'MANUAL':
        return 'info';
      default:
        return 'default';
    }
  };

  const renderCurrentPricesTab = () => (
    <Box>
      <Box display="flex" justifyContent="space-between" alignItems="center" mb={2}>
        <Typography variant="h6">Current Market Prices</Typography>
        <Box display="flex" gap={1}>
          <Button
            variant="outlined"
            size="small"
            startIcon={<RefreshIcon />}
            onClick={handleRefreshPrices}
            disabled={loading}
          >
            Refresh
          </Button>
          <Button
            variant="contained"
            size="small"
            onClick={handleTriggerUpdate}
            disabled={loading}
          >
            Update All Prices
          </Button>
        </Box>
      </Box>

      {loading && (
        <Box display="flex" justifyContent="center" p={4}>
          <CircularProgress />
        </Box>
      )}

      {error && (
        <Alert severity="error" sx={{ mb: 2 }}>
          {error}
        </Alert>
      )}

      {!loading && !error && prices.length === 0 && (
        <Alert severity="info">
          No investments with symbols found. Add investments with ticker symbols to see market prices.
        </Alert>
      )}

      {!loading && !error && prices.length > 0 && (
        <TableContainer>
          <Table>
            <TableHead>
              <TableRow>
                <TableCell>Investment</TableCell>
                <TableCell>Symbol</TableCell>
                <TableCell>Type</TableCell>
                <TableCell align="right">Current Price</TableCell>
                <TableCell align="right">Change</TableCell>
                <TableCell align="right">Change %</TableCell>
                <TableCell>Last Updated</TableCell>
                <TableCell>Source</TableCell>
                <TableCell align="center">Actions</TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {prices.map((inv) => (
                <TableRow key={inv.id}>
                  <TableCell>{inv.name || inv.assetName || 'N/A'}</TableCell>
                  <TableCell>
                    <Chip label={inv.symbol} size="small" variant="outlined" />
                  </TableCell>
                  <TableCell>{inv.investmentType}</TableCell>
                  <TableCell align="right">
                    {inv.currentPrice 
                      ? formatCurrency(inv.currentPrice, inv.currency) 
                      : '—'}
                  </TableCell>
                  <TableCell align="right">
                    {inv.change && (
                      <Box display="flex" alignItems="center" justifyContent="flex-end">
                        {inv.change > 0 ? (
                          <TrendingUpIcon fontSize="small" color="success" />
                        ) : (
                          <TrendingDownIcon fontSize="small" color="error" />
                        )}
                        <Typography 
                          variant="body2" 
                          color={inv.change > 0 ? 'success.main' : 'error.main'}
                        >
                          {formatCurrency(Math.abs(inv.change), inv.currency)}
                        </Typography>
                      </Box>
                    )}
                  </TableCell>
                  <TableCell align="right">
                    {inv.changePercent && (
                      <Chip
                        label={`${inv.changePercent > 0 ? '+' : ''}${inv.changePercent.toFixed(2)}%`}
                        size="small"
                        color={inv.changePercent > 0 ? 'success' : 'error'}
                      />
                    )}
                  </TableCell>
                  <TableCell>
                    {inv.lastUpdated ? formatDate(inv.lastUpdated) : '—'}
                  </TableCell>
                  <TableCell>
                    {inv.source && (
                      <Chip 
                        label={inv.source} 
                        size="small" 
                        color={getSourceColor(inv.source)} 
                      />
                    )}
                  </TableCell>
                  <TableCell align="center">
                    <Tooltip title="Set Manual Price">
                      <IconButton 
                        size="small" 
                        onClick={() => handleOpenManualPriceDialog(inv)}
                      >
                        <EditIcon fontSize="small" />
                      </IconButton>
                    </Tooltip>
                  </TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        </TableContainer>
      )}
    </Box>
  );

  const renderSymbolMappingsTab = () => (
    <Box>
      <Typography variant="h6" mb={2}>Symbol Mappings</Typography>
      <Alert severity="info">
        Symbol mappings are automatically created when you add investments with ticker symbols. 
        The system resolves symbols for Yahoo Finance and Google Finance APIs automatically.
      </Alert>
      
      <Box mt={3}>
        <Typography variant="body2" color="text.secondary">
          This feature will show all symbol mappings with options to edit or verify them.
          Implementation coming soon...
        </Typography>
      </Box>
    </Box>
  );

  const renderUpdateHistoryTab = () => (
    <Box>
      <Box display="flex" justifyContent="space-between" alignItems="center" mb={2}>
        <Typography variant="h6">Update History</Typography>
        <Button
          variant="outlined"
          size="small"
          startIcon={<RefreshIcon />}
          onClick={fetchUpdateHistory}
          disabled={loading}
        >
          Refresh
        </Button>
      </Box>

      {loading && (
        <Box display="flex" justifyContent="center" p={4}>
          <CircularProgress />
        </Box>
      )}

      {error && (
        <Alert severity="error" sx={{ mb: 2 }}>
          {error}
        </Alert>
      )}

      {!loading && !error && updateHistory.length === 0 && (
        <Alert severity="info">
          No update history found. Trigger a price update to see results here.
        </Alert>
      )}

      {!loading && !error && updateHistory.length > 0 && (
        <TableContainer>
          <Table>
            <TableHead>
              <TableRow>
                <TableCell>Date/Time</TableCell>
                <TableCell>Trigger</TableCell>
                <TableCell>Status</TableCell>
                <TableCell align="right">Success Count</TableCell>
                <TableCell align="right">Failed Count</TableCell>
                <TableCell align="right">Total</TableCell>
                <TableCell align="right">Success Rate</TableCell>
                <TableCell>Duration</TableCell>
                <TableCell>Error Message</TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {updateHistory.map((result) => (
                <TableRow key={result.id}>
                  <TableCell>{formatDate(result.updateDate)}</TableCell>
                  <TableCell>
                    <Chip 
                      label={result.triggerType} 
                      size="small" 
                      variant="outlined"
                    />
                  </TableCell>
                  <TableCell>
                    <Chip 
                      label={result.status} 
                      size="small" 
                      color={getStatusColor(result.status)}
                    />
                  </TableCell>
                  <TableCell align="right">
                    <Chip 
                      label={result.successCount} 
                      size="small" 
                      color="success" 
                      variant="outlined"
                    />
                  </TableCell>
                  <TableCell align="right">
                    {result.failedCount > 0 && (
                      <Chip 
                        label={result.failedCount} 
                        size="small" 
                        color="error" 
                        variant="outlined"
                      />
                    )}
                  </TableCell>
                  <TableCell align="right">{result.totalCount}</TableCell>
                  <TableCell align="right">
                    {((result.successCount / result.totalCount) * 100).toFixed(1)}%
                  </TableCell>
                  <TableCell>{result.durationSeconds}s</TableCell>
                  <TableCell>
                    {result.errorMessage && (
                      <Typography variant="caption" color="error">
                        {result.errorMessage}
                      </Typography>
                    )}
                  </TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        </TableContainer>
      )}
    </Box>
  );

  const renderCacheStatsTab = () => (
    <Box>
      <Typography variant="h6" mb={2}>Cache Statistics</Typography>

      {loading && (
        <Box display="flex" justifyContent="center" p={4}>
          <CircularProgress />
        </Box>
      )}

      {error && (
        <Alert severity="error" sx={{ mb: 2 }}>
          {error}
        </Alert>
      )}

      {!loading && cacheStats && (
        <Box>
          <Paper sx={{ p: 2, mb: 2 }}>
            <Typography variant="subtitle2" gutterBottom>
              Price Cache
            </Typography>
            <Box display="grid" gridTemplateColumns="repeat(3, 1fr)" gap={2} mt={2}>
              <Box>
                <Typography variant="caption" color="text.secondary">
                  Total Entries
                </Typography>
                <Typography variant="h6">
                  {cacheStats.priceCache?.size || 0}
                </Typography>
              </Box>
              <Box>
                <Typography variant="caption" color="text.secondary">
                  Hit Rate
                </Typography>
                <Typography variant="h6">
                  {cacheStats.priceCache?.hitRate 
                    ? `${(cacheStats.priceCache.hitRate * 100).toFixed(1)}%`
                    : '—'}
                </Typography>
              </Box>
              <Box>
                <Typography variant="caption" color="text.secondary">
                  Eviction Count
                </Typography>
                <Typography variant="h6">
                  {cacheStats.priceCache?.evictionCount || 0}
                </Typography>
              </Box>
            </Box>
          </Paper>

          <Paper sx={{ p: 2 }}>
            <Typography variant="subtitle2" gutterBottom>
              Forex Cache
            </Typography>
            <Box display="grid" gridTemplateColumns="repeat(3, 1fr)" gap={2} mt={2}>
              <Box>
                <Typography variant="caption" color="text.secondary">
                  Total Entries
                </Typography>
                <Typography variant="h6">
                  {cacheStats.forexCache?.size || 0}
                </Typography>
              </Box>
              <Box>
                <Typography variant="caption" color="text.secondary">
                  Hit Rate
                </Typography>
                <Typography variant="h6">
                  {cacheStats.forexCache?.hitRate 
                    ? `${(cacheStats.forexCache.hitRate * 100).toFixed(1)}%`
                    : '—'}
                </Typography>
              </Box>
              <Box>
                <Typography variant="caption" color="text.secondary">
                  Eviction Count
                </Typography>
                <Typography variant="h6">
                  {cacheStats.forexCache?.evictionCount || 0}
                </Typography>
              </Box>
            </Box>
          </Paper>
        </Box>
      )}

      {!loading && !cacheStats && (
        <Alert severity="info">
          No cache statistics available.
        </Alert>
      )}
    </Box>
  );

  return (
    <Box>
      <Typography variant="h4" mb={3}>
        Market Data Management
      </Typography>

      <Paper>
        <Tabs value={activeTab} onChange={handleTabChange}>
          <Tab label="Current Prices" />
          <Tab label="Symbol Mappings" />
          <Tab label="Update History" />
          <Tab label="Cache Statistics" />
        </Tabs>

        <Box p={3}>
          {activeTab === 0 && renderCurrentPricesTab()}
          {activeTab === 1 && renderSymbolMappingsTab()}
          {activeTab === 2 && renderUpdateHistoryTab()}
          {activeTab === 3 && renderCacheStatsTab()}
        </Box>
      </Paper>

      {/* Manual Price Dialog */}
      <Dialog open={manualPriceDialog} onClose={handleCloseManualPriceDialog} maxWidth="sm" fullWidth>
        <DialogTitle>Set Manual Price</DialogTitle>
        <DialogContent>
          <Box display="flex" flexDirection="column" gap={2} mt={1}>
            <TextField
              label="Symbol"
              value={manualPriceForm.symbol}
              onChange={(e) => setManualPriceForm({ ...manualPriceForm, symbol: e.target.value })}
              fullWidth
            />
            <TextField
              label="Price"
              type="number"
              value={manualPriceForm.price}
              onChange={(e) => setManualPriceForm({ ...manualPriceForm, price: e.target.value })}
              fullWidth
            />
            <FormControl fullWidth>
              <InputLabel>Currency</InputLabel>
              <Select
                value={manualPriceForm.currency}
                label="Currency"
                onChange={(e) => setManualPriceForm({ ...manualPriceForm, currency: e.target.value })}
              >
                <MenuItem value="INR">INR (₹)</MenuItem>
                <MenuItem value="USD">USD ($)</MenuItem>
                <MenuItem value="EUR">EUR (€)</MenuItem>
                <MenuItem value="GBP">GBP (£)</MenuItem>
              </Select>
            </FormControl>
          </Box>
        </DialogContent>
        <DialogActions>
          <Button onClick={handleCloseManualPriceDialog}>Cancel</Button>
          <Button onClick={handleManualPriceSubmit} variant="contained">
            Save
          </Button>
        </DialogActions>
      </Dialog>
    </Box>
  );
};

export default MarketDataPage;
