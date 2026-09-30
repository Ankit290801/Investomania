import React, { useRef, useState } from 'react';
import {
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  Button,
  Box,
  Typography,
  Alert,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  Chip,
  CircularProgress,
  Paper
} from '@mui/material';
import DescriptionIcon from '@mui/icons-material/Description';
import { contractNoteService } from '../../services/contractNoteService';
import { formatCurrency } from '../../utils/currencyFormatter';

const STATUS_COLORS = {
  NEW: 'primary',
  UPDATED: 'success',
  ERROR: 'error'
};

/**
 * Broker contract note (CNOTE) import dialog (Phase 9 / Task 8.3):
 * upload → broker detection + dry-run trade preview (NEW / UPDATED / ERROR,
 * with portfolio actions) → commit. Buys/sells update holdings, weighted
 * average prices, and the settlement cash account; net worth follows.
 */
const ContractNoteImportDialog = ({ open, onClose, onImported }) => {
  const fileInputRef = useRef(null);
  const [file, setFile] = useState(null);
  const [report, setReport] = useState(null);
  const [error, setError] = useState(null);
  const [busy, setBusy] = useState(false);
  const [phase, setPhase] = useState('select'); // select | preview | done

  const reset = () => {
    setFile(null);
    setReport(null);
    setError(null);
    setBusy(false);
    setPhase('select');
    if (fileInputRef.current) fileInputRef.current.value = '';
  };

  const handleClose = () => {
    reset();
    onClose();
  };

  const handleFileChange = (event) => {
    const selected = event.target.files?.[0] || null;
    setFile(selected);
    setReport(null);
    setError(null);
    setPhase('select');
  };

  const runPreview = async () => {
    if (!file) return;
    setBusy(true);
    setError(null);
    try {
      const data = await contractNoteService.preview(file);
      setReport(data);
      setPhase('preview');
    } catch (err) {
      setError(err.response?.data?.error || err.message || 'Failed to read the PDF');
    } finally {
      setBusy(false);
    }
  };

  const runCommit = async () => {
    if (!file) return;
    setBusy(true);
    setError(null);
    try {
      const data = await contractNoteService.commit(file);
      setReport(data);
      setPhase('done');
      if (onImported) onImported(data);
    } catch (err) {
      setError(err.response?.data?.error || err.message || 'Import failed');
    } finally {
      setBusy(false);
    }
  };

  return (
    <Dialog open={open} onClose={handleClose} maxWidth="md" fullWidth>
      <DialogTitle sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
        <DescriptionIcon /> Import Contract Note (PDF)
      </DialogTitle>
      <DialogContent>
        {error && (
          <Alert severity="error" sx={{ mb: 2 }} onClose={() => setError(null)}>
            {error}
          </Alert>
        )}

        <Typography variant="body2" color="text.secondary" gutterBottom>
          Upload a broker contract note PDF. Trades are parsed automatically:
          buys on existing stocks update the weighted average price, new stocks
          are added as investments with a cash debit on the settlement account,
          and sells (partial or full) credit the cash and reduce the holding.
          The file is processed in memory and never stored.
        </Typography>

        <Box sx={{ display: 'flex', gap: 2, alignItems: 'center', mt: 2, mb: 1 }}>
          <Button variant="outlined" component="label">
            Choose PDF
            <input
              ref={fileInputRef}
              type="file"
              accept=".pdf,application/pdf"
              hidden
              onChange={handleFileChange}
            />
          </Button>
          {file && (
            <Typography variant="body2">
              {file.name} ({Math.round(file.size / 1024)} KB)
            </Typography>
          )}
        </Box>

        {busy && (
          <Box sx={{ display: 'flex', justifyContent: 'center', p: 3 }}>
            <CircularProgress />
          </Box>
        )}

        {report && !busy && (
          <Box sx={{ mt: 2 }}>
            <Box sx={{ display: 'flex', gap: 1, mb: 2, flexWrap: 'wrap', alignItems: 'center' }}>
              <Chip
                icon={<DescriptionIcon />}
                label={report.detectedBroker || 'Broker not detected (generic template)'}
                color={report.detectedBroker && report.detectedBroker !== 'Generic'
                  ? 'primary' : 'warning'}
                variant="outlined"
              />
              <Chip label={`${report.totalTrades} trades`} variant="outlined" />
              <Chip
                label={`${report.validTrades} applicable`}
                color={report.validTrades > 0 ? 'success' : 'default'}
                variant="outlined"
              />
              <Chip label={`${report.errorTrades} errors`} color="error" variant="outlined" />
              {report.dryRun && <Chip label="DRY RUN — nothing saved" color="info" />}
              {report.netCashImpact != null && report.validTrades > 0 && (
                <Chip
                  label={`Cash: ${report.netCashImpact >= 0 ? '+' : '−'}${formatCurrency(Math.abs(report.netCashImpact), 'INR')}`}
                  color={report.netCashImpact >= 0 ? 'success' : 'warning'}
                  variant="outlined"
                />
              )}
            </Box>

            {report.trades?.length > 0 && (
              <TableContainer component={Paper} sx={{ maxHeight: 300 }}>
                <Table size="small" stickyHeader>
                  <TableHead>
                    <TableRow>
                      <TableCell>#</TableCell>
                      <TableCell>Symbol</TableCell>
                      <TableCell>Side</TableCell>
                      <TableCell align="right">Qty</TableCell>
                      <TableCell align="right">Price</TableCell>
                      <TableCell>Action</TableCell>
                      <TableCell>Detail</TableCell>
                    </TableRow>
                  </TableHead>
                  <TableBody>
                    {report.trades.map((row) => (
                      <TableRow key={row.rowNumber}>
                        <TableCell>{row.rowNumber}</TableCell>
                        <TableCell><strong>{row.symbol}</strong></TableCell>
                        <TableCell>{row.buy ? 'BUY' : 'SELL'}</TableCell>
                        <TableCell align="right">{row.quantity?.toLocaleString()}</TableCell>
                        <TableCell align="right">{row.price?.toLocaleString()}</TableCell>
                        <TableCell>
                          <Chip
                            label={row.status}
                            color={STATUS_COLORS[row.status] || 'default'}
                            size="small"
                          />
                        </TableCell>
                        <TableCell>
                          <Typography variant="caption" color="text.secondary">
                            {row.message || ''}
                          </Typography>
                        </TableCell>
                      </TableRow>
                    ))}
                  </TableBody>
                </Table>
              </TableContainer>
            )}

            {phase === 'done' && (
              <Alert severity="success" sx={{ mt: 2 }}>
                Import complete — {report.validTrades} trade(s) applied
                {report.cashAccountName ? ` via ${report.cashAccountName}` : ''}.
                Holdings, average prices and balances updated; net worth and
                historical snapshots refreshed.
              </Alert>
            )}
          </Box>
        )}
      </DialogContent>
      <DialogActions>
        <Button onClick={handleClose}>{phase === 'done' ? 'Close' : 'Cancel'}</Button>
        {phase !== 'done' && (
          <Button onClick={runPreview} disabled={!file || busy}>
            Preview (Dry Run)
          </Button>
        )}
        {phase === 'preview' && report?.commitable && (
          <Button onClick={runCommit} disabled={busy} variant="contained">
            Apply {report.validTrades} Trade(s)
          </Button>
        )}
      </DialogActions>
    </Dialog>
  );
};

export default ContractNoteImportDialog;
