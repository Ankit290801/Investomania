import React, { useMemo, useRef, useState } from 'react';
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
  Paper,
  TextField,
  MenuItem,
  InputAdornment
} from '@mui/material';
import AccountBalanceIcon from '@mui/icons-material/AccountBalance';
import { statementImportService } from '../../services/statementImportService';
import { formatCurrency } from '../../utils/currencyFormatter';

const STATUS_COLORS = {
  OK: 'success',
  DUPLICATE: 'warning',
  ERROR: 'error'
};

/**
 * Bank statement PDF import dialog (backlog feature F6):
 * upload → auto bank detection + dry-run preview with per-row report →
 * optional account override → commit. Committed credits/debits land on the
 * Transactions screen and update the cash balance (hence net worth and
 * historical FY-end snapshots) automatically.
 */
const StatementImportDialog = ({ open, onClose, onImported, investments }) => {
  const fileInputRef = useRef(null);
  const [file, setFile] = useState(null);
  const [report, setReport] = useState(null);
  const [error, setError] = useState(null);
  const [busy, setBusy] = useState(false);
  const [phase, setPhase] = useState('select'); // select | preview | done
  const [accountOverride, setAccountOverride] = useState('');

  const cashAccounts = useMemo(
    () => (investments || []).filter((inv) => inv.type === 'CASH'),
    [investments]
  );

  const reset = () => {
    setFile(null);
    setReport(null);
    setError(null);
    setBusy(false);
    setPhase('select');
    setAccountOverride('');
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

  const selectedAccountId = accountOverride ? Number(accountOverride) : null;

  const runPreview = async () => {
    if (!file) return;
    setBusy(true);
    setError(null);
    try {
      const data = await statementImportService.preview(file, selectedAccountId);
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
      const data = await statementImportService.commit(file, selectedAccountId);
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
        <AccountBalanceIcon /> Import Bank Statement (PDF)
      </DialogTitle>
      <DialogContent>
        {error && (
          <Alert severity="error" sx={{ mb: 2 }} onClose={() => setError(null)}>
            {error}
          </Alert>
        )}

        <Typography variant="body2" color="text.secondary" gutterBottom>
          Upload a bank statement PDF. The bank is detected automatically from the
          document; credits and debits become transactions on the matching cash
          account, the balance is replayed, and past FY-end net worth values are
          refreshed accordingly. The file is processed in memory and never stored.
        </Typography>

        <Box sx={{ display: 'flex', gap: 2, alignItems: 'center', mt: 2, mb: 1, flexWrap: 'wrap' }}>
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
          <TextField
            select
            size="small"
            label="Cash account"
            value={accountOverride}
            onChange={(e) => setAccountOverride(e.target.value)}
            sx={{ minWidth: 240 }}
            helperText="Auto-detected when left empty"
          >
            <MenuItem value=""><em>Auto-detect</em></MenuItem>
            {cashAccounts.map((acc) => (
              <MenuItem key={acc.id} value={acc.id}>
                {acc.name}{acc.bankName ? ` — ${acc.bankName}` : ''}
              </MenuItem>
            ))}
          </TextField>
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
                icon={<AccountBalanceIcon />}
                label={report.detectedBank || 'Bank not detected'}
                color={report.detectedBank ? 'primary' : 'warning'}
                variant="outlined"
              />
              <Chip label={`${report.totalRows} rows`} variant="outlined" />
              <Chip
                label={`${report.validRows} new`}
                color={report.validRows > 0 ? 'success' : 'default'}
                variant="outlined"
              />
              <Chip label={`${report.duplicateRows} duplicates`} color="warning" variant="outlined" />
              <Chip label={`${report.errorRows} errors`} color="error" variant="outlined" />
              {report.dryRun && <Chip label="DRY RUN — nothing saved" color="info" />}
              {report.netBalanceImpact != null && report.cashAccountId && (
                <Chip
                  label={`Balance impact: ${formatCurrency(report.netBalanceImpact, 'INR')}`}
                  color={report.netBalanceImpact >= 0 ? 'success' : 'error'}
                  variant="outlined"
                />
              )}
            </Box>

            {report.cashAccountName && (
              <Alert severity="info" sx={{ mb: 2 }}>
                Target account: <strong>{report.cashAccountName}</strong>
              </Alert>
            )}

            {report.rows?.length > 0 && (
              <TableContainer component={Paper} sx={{ maxHeight: 300 }}>
                <Table size="small" stickyHeader>
                  <TableHead>
                    <TableRow>
                      <TableCell>#</TableCell>
                      <TableCell>Date</TableCell>
                      <TableCell>Description</TableCell>
                      <TableCell align="right">Amount</TableCell>
                      <TableCell>Direction</TableCell>
                      <TableCell>Status</TableCell>
                      <TableCell>Message</TableCell>
                    </TableRow>
                  </TableHead>
                  <TableBody>
                    {report.rows.map((row) => (
                      <TableRow key={row.rowNumber}>
                        <TableCell>{row.rowNumber}</TableCell>
                        <TableCell>{row.date || '—'}</TableCell>
                        <TableCell>{row.description || '—'}</TableCell>
                        <TableCell align="right">
                          {row.amount != null ? row.amount.toLocaleString() : '—'}
                        </TableCell>
                        <TableCell>{row.direction || '—'}</TableCell>
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
                Import complete — {report.validRows} transaction(s) saved
                {report.duplicateRows > 0 ? `, ${report.duplicateRows} duplicate(s) skipped` : ''}.
                Balance and historical snapshots updated.
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
            Import {report.validRows} Transaction(s)
          </Button>
        )}
      </DialogActions>
    </Dialog>
  );
};

export default StatementImportDialog;
