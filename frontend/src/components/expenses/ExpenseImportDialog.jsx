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
import { expenseService } from '../../services/expenseService';

const STATUS_COLORS = {
  OK: 'success',
  DUPLICATE: 'warning',
  ERROR: 'error'
};

/**
 * Excel expense import dialog (backlog feature F2):
 * file upload → dry-run preview with per-row validation report → commit.
 */
const ExpenseImportDialog = ({ open, onClose, onImported }) => {
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
      const data = await expenseService.importPreview(file);
      setReport(data);
      setPhase('preview');
    } catch (err) {
      setError(err.response?.data?.error || err.message || 'Failed to read the file');
    } finally {
      setBusy(false);
    }
  };

  const runCommit = async () => {
    if (!file) return;
    setBusy(true);
    setError(null);
    try {
      const data = await expenseService.importCommit(file);
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
      <DialogTitle>Import Expenses from Excel</DialogTitle>
      <DialogContent>
        {error && (
          <Alert severity="error" sx={{ mb: 2 }} onClose={() => setError(null)}>
            {error}
          </Alert>
        )}

        <Box sx={{ mb: 2 }}>
          <Typography variant="body2" color="text.secondary" gutterBottom>
            Upload an .xlsx sheet with columns: <strong>Date</strong> (YYYY-MM-DD or Excel
            date), <strong>Description</strong>, <strong>Amount</strong>, and optionally{' '}
            <strong>Category</strong> (inferred from the description when omitted),{' '}
            <strong>Currency</strong> (defaults to INR) and <strong>Notes</strong>. A dry-run
            preview shows exactly what will be imported before anything is saved. Committed
            expenses reduce your cash account balance.
          </Typography>
        </Box>

        <Box sx={{ display: 'flex', gap: 2, alignItems: 'center', mb: 2 }}>
          <Button variant="outlined" component="label">
            Choose File
            <input
              ref={fileInputRef}
              type="file"
              accept=".xlsx,.xls"
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
          <Box>
            <Box sx={{ display: 'flex', gap: 1, mb: 2, flexWrap: 'wrap' }}>
              <Chip label={`${report.totalRows} total`} variant="outlined" />
              <Chip
                label={`${report.validRows} valid`}
                color={report.validRows > 0 ? 'success' : 'default'}
                variant="outlined"
              />
              <Chip label={`${report.duplicateRows} duplicates`} color="warning" variant="outlined" />
              <Chip label={`${report.errorRows} errors`} color="error" variant="outlined" />
              {report.dryRun && <Chip label="DRY RUN — nothing saved" color="info" />}
            </Box>

            {report.rows?.length > 0 && (
              <TableContainer component={Paper} sx={{ maxHeight: 320 }}>
                <Table size="small" stickyHeader>
                  <TableHead>
                    <TableRow>
                      <TableCell>Row</TableCell>
                      <TableCell>Date</TableCell>
                      <TableCell>Description</TableCell>
                      <TableCell align="right">Amount</TableCell>
                      <TableCell>Category</TableCell>
                      <TableCell>Status</TableCell>
                      <TableCell>Message</TableCell>
                    </TableRow>
                  </TableHead>
                  <TableBody>
                    {report.rows.map((row) => (
                      <TableRow key={row.rowNumber}>
                        <TableCell>{row.rowNumber}</TableCell>
                        <TableCell>{row.expenseDate || '—'}</TableCell>
                        <TableCell>{row.description || '—'}</TableCell>
                        <TableCell align="right">
                          {row.amount != null ? row.amount.toLocaleString() : '—'}
                        </TableCell>
                        <TableCell>{row.category || '—'}</TableCell>
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
                Import complete — {report.validRows} expense(s) saved
                {report.duplicateRows > 0 ? `, ${report.duplicateRows} duplicate(s) skipped` : ''}.
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
          <Button onClick={runCommit} disabled={busy} variant="contained" color="primary">
            Import {report.validRows} Expense(s)
          </Button>
        )}
      </DialogActions>
    </Dialog>
  );
};

export default ExpenseImportDialog;
