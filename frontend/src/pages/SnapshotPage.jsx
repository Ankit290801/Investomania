import React, { useState, useEffect, useCallback } from 'react';
import {
  Box,
  Button,
  Card,
  CardContent,
  Chip,
  CircularProgress,
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
  IconButton,
  Paper,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  TextField,
  Tooltip,
  Typography,
  Alert,
  LinearProgress,
} from '@mui/material';
import RefreshIcon from '@mui/icons-material/Refresh';
import DeleteIcon from '@mui/icons-material/Delete';
import ReplayIcon from '@mui/icons-material/Replay';
import CheckCircleIcon from '@mui/icons-material/CheckCircle';
import WarningAmberIcon from '@mui/icons-material/WarningAmber';
import snapshotService from '../services/snapshotService';
import { formatCurrency } from '../utils/currencyFormatter';

/**
 * Snapshot Management Page
 *
 * Lets users generate / view / recalculate historical portfolio snapshots
 * (FY-end March 31 values) that power the YoY growth chart.
 */
export default function SnapshotPage() {
  const [snapshots, setSnapshots] = useState([]);
  const [missingData, setMissingData] = useState(null);
  const [loading, setLoading] = useState(false);
  const [generating, setGenerating] = useState(false);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');

  // Recalculate dialog state
  const [recalcOpen, setRecalcOpen] = useState(false);
  const [recalcDate, setRecalcDate] = useState('');
  const [recalcLoading, setRecalcLoading] = useState(false);

  const load = useCallback(async () => {
    setLoading(true);
    setError('');
    try {
      const [snaps, report] = await Promise.all([
        snapshotService.getSnapshots(),
        snapshotService.getMissingDataReport(),
      ]);
      setSnapshots(snaps);
      setMissingData(report);
    } catch (e) {
      setError('Failed to load snapshots: ' + (e.response?.data?.message || e.message));
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => { load(); }, [load]);

  const handleGenerate = async (force = false) => {
    setGenerating(true);
    setError('');
    setSuccess('');
    try {
      const result = await snapshotService.generateSnapshots(force);
      setSuccess(`Generated ${result.generated} snapshot(s).`);
      await load();
    } catch (e) {
      setError('Generation failed: ' + (e.response?.data?.message || e.message));
    } finally {
      setGenerating(false);
    }
  };

  const handleDelete = async (date) => {
    if (!window.confirm(`Delete snapshot for ${date}?`)) return;
    try {
      await snapshotService.deleteSnapshot(date);
      setSuccess(`Deleted snapshot for ${date}.`);
      await load();
    } catch (e) {
      setError('Delete failed: ' + (e.response?.data?.message || e.message));
    }
  };

  const handleRecalculate = async () => {
    if (!recalcDate) return;
    setRecalcLoading(true);
    setError('');
    try {
      await snapshotService.recalculateSnapshot(recalcDate);
      setSuccess(`Snapshot for ${recalcDate} recalculated.`);
      setRecalcOpen(false);
      await load();
    } catch (e) {
      setError('Recalculation failed: ' + (e.response?.data?.message || e.message));
    } finally {
      setRecalcLoading(false);
    }
  };

  const statusChip = (status) => {
    if (status === 'CALCULATED')
      return <Chip label="Calculated" color="success" size="small" icon={<CheckCircleIcon />} />;
    if (status === 'PARTIAL')
      return <Chip label="Partial" color="warning" size="small" icon={<WarningAmberIcon />} />;
    return <Chip label="Manual Override" color="info" size="small" />;
  };

  return (
    <Box sx={{ p: 3 }}>
      <Typography variant="h5" fontWeight={600} gutterBottom>
        Historical Portfolio Snapshots
      </Typography>
      <Typography variant="body2" color="text.secondary" mb={3}>
        FY-end (March 31) snapshots power the YoY growth chart. Generate them once;
        they are automatically kept up-to-date.
      </Typography>

      {/* Summary cards */}
      {missingData && (
        <Box sx={{ display: 'flex', gap: 2, mb: 3, flexWrap: 'wrap' }}>
          <Card sx={{ minWidth: 160 }}>
            <CardContent sx={{ pb: '12px !important' }}>
              <Typography variant="h4" fontWeight={700}>{missingData.totalSnapshots}</Typography>
              <Typography variant="caption" color="text.secondary">Total Snapshots</Typography>
            </CardContent>
          </Card>
          <Card sx={{ minWidth: 160 }}>
            <CardContent sx={{ pb: '12px !important' }}>
              <Typography variant="h4" fontWeight={700} color="warning.main">
                {missingData.partialSnapshots}
              </Typography>
              <Typography variant="caption" color="text.secondary">Partial (Est. data)</Typography>
            </CardContent>
          </Card>
          <Card sx={{ minWidth: 160 }}>
            <CardContent sx={{ pb: '12px !important' }}>
              <Typography variant="h4" fontWeight={700} color="info.main">
                {missingData.estimatedAssets}
              </Typography>
              <Typography variant="caption" color="text.secondary">Estimated Assets</Typography>
            </CardContent>
          </Card>
        </Box>
      )}

      {/* Alerts */}
      {error && <Alert severity="error" sx={{ mb: 2 }} onClose={() => setError('')}>{error}</Alert>}
      {success && <Alert severity="success" sx={{ mb: 2 }} onClose={() => setSuccess('')}>{success}</Alert>}

      {/* Actions */}
      <Box sx={{ display: 'flex', gap: 2, mb: 3, flexWrap: 'wrap' }}>
        <Button
          variant="contained"
          startIcon={generating ? <CircularProgress size={16} color="inherit" /> : <RefreshIcon />}
          onClick={() => handleGenerate(false)}
          disabled={generating || loading}
        >
          Generate Missing Snapshots
        </Button>
        <Button
          variant="outlined"
          startIcon={<ReplayIcon />}
          onClick={() => handleGenerate(true)}
          disabled={generating || loading}
        >
          Regenerate All
        </Button>
        <Button
          variant="outlined"
          onClick={() => { setRecalcDate(''); setRecalcOpen(true); }}
          disabled={generating || loading}
        >
          Recalculate Specific Date
        </Button>
        <Tooltip title="Refresh list">
          <IconButton onClick={load} disabled={loading}>
            <RefreshIcon />
          </IconButton>
        </Tooltip>
      </Box>

      {(loading || generating) && <LinearProgress sx={{ mb: 2 }} />}

      {/* Snapshots table */}
      {snapshots.length === 0 && !loading ? (
        <Paper sx={{ p: 4, textAlign: 'center' }}>
          <Typography color="text.secondary">
            No snapshots yet. Click <strong>Generate Missing Snapshots</strong> to create them.
          </Typography>
        </Paper>
      ) : (
        <TableContainer component={Paper}>
          <Table size="small">
            <TableHead>
              <TableRow>
                <TableCell>Financial Year End</TableCell>
                <TableCell align="right">Total Value</TableCell>
                <TableCell align="right">Equity</TableCell>
                <TableCell align="right">FD / PPF / Bonds</TableCell>
                <TableCell align="right">Crypto</TableCell>
                <TableCell align="right">Real Estate</TableCell>
                <TableCell>Status</TableCell>
                <TableCell>Est. Assets</TableCell>
                <TableCell align="center">Actions</TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {snapshots.map((s) => {
                const safeAssets = [s.fixedDepositValue, s.rdValue, s.ppfValue, s.npsValue, s.bondValue, s.savingsValue]
                  .filter(Boolean)
                  .reduce((a, b) => a + parseFloat(b || 0), 0);
                return (
                  <TableRow key={s.id} hover>
                    <TableCell><strong>{s.snapshotDate}</strong></TableCell>
                    <TableCell align="right">
                      <strong>{formatCurrency(s.totalValue, s.currency)}</strong>
                    </TableCell>
                    <TableCell align="right">
                      {formatCurrency(
                        (parseFloat(s.equityValue || 0) + parseFloat(s.privateEquityValue || 0)),
                        s.currency
                      )}
                    </TableCell>
                    <TableCell align="right">{formatCurrency(safeAssets, s.currency)}</TableCell>
                    <TableCell align="right">{formatCurrency(s.cryptoValue || 0, s.currency)}</TableCell>
                    <TableCell align="right">{formatCurrency(s.realEstateValue || 0, s.currency)}</TableCell>
                    <TableCell>{statusChip(s.status)}</TableCell>
                    <TableCell align="center">{s.estimatedCount ?? 0}</TableCell>
                    <TableCell align="center">
                      <Tooltip title="Recalculate this snapshot">
                        <IconButton
                          size="small"
                          onClick={() => { setRecalcDate(s.snapshotDate); setRecalcOpen(true); }}
                        >
                          <ReplayIcon fontSize="small" />
                        </IconButton>
                      </Tooltip>
                      <Tooltip title="Delete snapshot">
                        <IconButton size="small" color="error" onClick={() => handleDelete(s.snapshotDate)}>
                          <DeleteIcon fontSize="small" />
                        </IconButton>
                      </Tooltip>
                    </TableCell>
                  </TableRow>
                );
              })}
            </TableBody>
          </Table>
        </TableContainer>
      )}

      {/* Recalculate dialog */}
      <Dialog open={recalcOpen} onClose={() => setRecalcOpen(false)} maxWidth="xs" fullWidth>
        <DialogTitle>Recalculate Snapshot</DialogTitle>
        <DialogContent>
          <Typography variant="body2" color="text.secondary" mb={2}>
            Enter a date to (re)calculate the snapshot. Use March 31 for FY-end snapshots.
          </Typography>
          <TextField
            label="Date"
            type="date"
            fullWidth
            value={recalcDate}
            onChange={(e) => setRecalcDate(e.target.value)}
            InputLabelProps={{ shrink: true }}
          />
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setRecalcOpen(false)}>Cancel</Button>
          <Button
            variant="contained"
            onClick={handleRecalculate}
            disabled={!recalcDate || recalcLoading}
            startIcon={recalcLoading ? <CircularProgress size={16} color="inherit" /> : null}
          >
            Recalculate
          </Button>
        </DialogActions>
      </Dialog>
    </Box>
  );
}
