import React, { useEffect, useMemo, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useTheme, alpha } from '@mui/material/styles';
import {
  Box,
  CircularProgress,
  Typography,
  Button,
  Card,
  CardContent,
  Grid,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  IconButton,
  Tooltip as MuiTooltip,
  Alert
} from '@mui/material';
import {
  Add as AddIcon,
  Description as DescriptionIcon,
  Visibility as ViewIcon,
  Edit as EditIcon,
  Delete as DeleteIcon,
  ArrowBack as ArrowBackIcon
} from '@mui/icons-material';
import InvestmentFormDialog from './InvestmentFormDialog';
import InvestmentDetails from './InvestmentDetails';
import ContractNoteImportDialog from './ContractNoteImportDialog';
import WeightageDonut from './WeightageDonut';
import { investmentService } from '../../services/investmentService';
import { formatCurrency } from '../../utils/currencyFormatter';
import { getAssetClass } from '../../constants/assetClasses';

const toNumber = (v) => {
  const n = parseFloat(v);
  return Number.isFinite(n) ? n : 0;
};

/**
 * E2 — shared frame for every asset-class sub-screen (/investments/:type).
 * Loads all investments once, derives the class slice, and renders:
 * header (back to hub, class actions) → summary chips → weightage donuts →
 * holdings table with view/edit/delete actions. Class-specific columns and
 * breakdowns are injected by the thin per-class wrappers.
 *
 * @param {string} type        InvestmentType enum value (EQUITY, BOND, ...)
 * @param {Array}  columns     table columns: { id, label, align, render(inv) }
 * @param {Func}   breakdowns  (classInvestments, allInvestments) => [{ title, entries: [{name, value}] }]
 */
const AssetClassPage = ({ type, columns, breakdowns: computeBreakdowns, emptyHint }) => {
  const theme = useTheme();
  const navigate = useNavigate();
  const [openDialog, setOpenDialog] = useState(false);
  const [cnoteDialogOpen, setCnoteDialogOpen] = useState(false);
  const [editInvestment, setEditInvestment] = useState(null);
  const [selectedInvestment, setSelectedInvestment] = useState(null);
  const [openDetails, setOpenDetails] = useState(false);
  const [allInvestments, setAllInvestments] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const classInfo = getAssetClass(type);

  const fetchInvestments = async () => {
    setLoading(true);
    setError(null);
    try {
      const data = await investmentService.getAll();
      setAllInvestments(Array.isArray(data) ? data : []);
    } catch (err) {
      console.error('Error fetching investments:', err);
      setError(err.response?.data?.error || err.message || 'Failed to load investments');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchInvestments();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [type]);

  const handleView = (investment) => {
    setSelectedInvestment(investment);
    setOpenDetails(true);
  };

  const handleEdit = (investment) => {
    setEditInvestment(investment);
    setOpenDialog(true);
  };

  const handleDelete = async (investment) => {
    if (window.confirm(`Are you sure you want to delete ${investment.name}?`)) {
      try {
        await investmentService.delete(investment.id);
        await fetchInvestments();
      } catch (err) {
        console.error('Error deleting investment:', err);
        setError(err.response?.data?.error || err.message || 'Failed to delete investment');
      }
    }
  };

  const handleInvestmentSaved = () => fetchInvestments();

  const investments = useMemo(
    () => allInvestments.filter((inv) => inv.type === type),
    [allInvestments, type]
  );

  const classTotal = useMemo(
    () => investments.reduce((sum, inv) => sum + toNumber(inv.currentValue), 0),
    [investments]
  );

  const portfolioTotal = useMemo(
    () => allInvestments.reduce((sum, inv) => sum + toNumber(inv.currentValue), 0),
    [allInvestments]
  );

  const currency = investments[0]?.currency || 'INR';

  const breakdowns = useMemo(
    () =>
      computeBreakdowns
        ? computeBreakdowns(investments, allInvestments)
        : [],
    [computeBreakdowns, investments, allInvestments]
  );

  const rows = useMemo(
    () =>
      investments.map((inv) => ({
        ...inv,
        weight: portfolioTotal > 0 ? (toNumber(inv.currentValue) / portfolioTotal) * 100 : 0
      })),
    [investments, portfolioTotal]
  );

  const portfolioShare = portfolioTotal > 0 ? (classTotal / portfolioTotal) * 100 : 0;

  const donutCols = !breakdowns || breakdowns.length === 0
    ? 0
    : breakdowns.length >= 3 ? 4 : breakdowns.length === 2 ? 6 : 12;

  return (
    <Box>
      {/* Header */}
      <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 3, flexWrap: 'wrap', gap: 2 }}>
        <Box sx={{ display: 'flex', alignItems: 'center', gap: 1.5 }}>
          <IconButton
            onClick={() => navigate('/investments')}
            size="small"
            aria-label="Back to investments hub"
            sx={{ border: `1px solid ${theme.palette.divider}` }}
          >
            <ArrowBackIcon fontSize="small" />
          </IconButton>
          {classInfo && (
            <Box
              sx={{
                width: 40,
                height: 40,
                borderRadius: 2,
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                bgcolor: alpha(theme.palette.primary.main, 0.12),
                color: 'primary.main'
              }}
            >
              <classInfo.icon fontSize="small" />
            </Box>
          )}
          <Box>
            <Typography variant="h4">{classInfo?.label || type}</Typography>
            <Typography variant="body2" color="text.secondary">
              {classInfo?.description}
            </Typography>
          </Box>
        </Box>
        <Box sx={{ display: 'flex', gap: 1.5, alignItems: 'center' }}>
          {type === 'EQUITY' && (
            <Button variant="outlined" startIcon={<DescriptionIcon />} onClick={() => setCnoteDialogOpen(true)}>
              Import CNOTE
            </Button>
          )}
          <Button variant="contained" startIcon={<AddIcon />} onClick={() => setOpenDialog(true)}>
            Add {classInfo?.label || 'Investment'}
          </Button>
        </Box>
      </Box>

      {error && (
        <Alert severity="error" sx={{ mb: 2 }} onClose={() => setError(null)}>
          {error}
        </Alert>
      )}

      {/* Summary chips */}
      <Grid container spacing={2} sx={{ mb: 2 }}>
        <Grid item xs={6} md={3}>
          <SummaryChip label="Class Total" value={formatCurrency(classTotal, currency)} />
        </Grid>
        <Grid item xs={6} md={3}>
          <SummaryChip label="Portfolio Share" value={`${portfolioShare.toFixed(1)}%`} />
        </Grid>
        <Grid item xs={6} md={3}>
          <SummaryChip label="Holdings" value={String(investments.length)} />
        </Grid>
        <Grid item xs={6} md={3}>
          <SummaryChip label="Currency" value={currency} />
        </Grid>
      </Grid>

      {/* Weightage donuts */}
      {donutCols > 0 && (
        <Grid container spacing={2} sx={{ mb: 2 }}>
          {breakdowns.map((b) => (
            <Grid item xs={12} md={donutCols} key={b.title}>
              <WeightageDonut title={b.title} data={b.entries} currency={currency} />
            </Grid>
          ))}
        </Grid>
      )}

      {/* Holdings table */}
      {loading ? (
        <Box sx={{ display: 'flex', justifyContent: 'center', py: 6 }}>
          <CircularProgress />
        </Box>
      ) : rows.length === 0 ? (
        <Card>
          <CardContent sx={{ textAlign: 'center', py: 8 }}>
            <Typography variant="h6" color="text.secondary" gutterBottom>
              No {classInfo?.label || ''} investments yet
            </Typography>
            <Typography color="text.secondary" paragraph>
              {emptyHint || `Start by adding your first ${classInfo?.label || ''} investment.`}
            </Typography>
            <Button variant="contained" startIcon={<AddIcon />} onClick={() => setOpenDialog(true)}>
              Add {classInfo?.label || 'Investment'}
            </Button>
          </CardContent>
        </Card>
      ) : (
        <TableContainer component={Card}>
          <Table size="small">
            <TableHead>
              <TableRow>
                {columns.map((col) => (
                  <TableCell key={col.id} align={col.align || 'left'}>
                    {col.label}
                  </TableCell>
                ))}
                <TableCell align="right">Weight</TableCell>
                <TableCell align="right">Actions</TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {rows.map((inv) => (
                <TableRow key={inv.id} hover>
                  {columns.map((col) => (
                    <TableCell key={col.id} align={col.align || 'left'}>
                      {col.render ? col.render(inv) : inv[col.id]}
                    </TableCell>
                  ))}
                  <TableCell align="right" sx={{ fontVariantNumeric: 'tabular-nums' }}>
                    {inv.weight.toFixed(1)}%
                  </TableCell>
                  <TableCell align="right" sx={{ whiteSpace: 'nowrap' }}>
                    <MuiTooltip title="View">
                      <IconButton size="small" onClick={() => handleView(inv)}>
                        <ViewIcon fontSize="small" />
                      </IconButton>
                    </MuiTooltip>
                    <MuiTooltip title="Edit">
                      <IconButton size="small" onClick={() => handleEdit(inv)}>
                        <EditIcon fontSize="small" />
                      </IconButton>
                    </MuiTooltip>
                    <MuiTooltip title="Delete">
                      <IconButton size="small" onClick={() => handleDelete(inv)}>
                        <DeleteIcon fontSize="small" />
                      </IconButton>
                    </MuiTooltip>
                  </TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        </TableContainer>
      )}

      {/* Shared dialogs — create/edit via the typed sub-forms, details, CNOTE */}
      <InvestmentFormDialog
        open={openDialog}
        onClose={() => {
          setOpenDialog(false);
          setEditInvestment(null);
        }}
        onSuccess={handleInvestmentSaved}
        editInvestment={editInvestment}
        defaultType={type}
      />
      <InvestmentDetails
        investment={selectedInvestment}
        open={openDetails}
        onClose={() => setOpenDetails(false)}
      />
      {type === 'EQUITY' && (
        <ContractNoteImportDialog
          open={cnoteDialogOpen}
          onClose={() => setCnoteDialogOpen(false)}
          onImported={handleInvestmentSaved}
        />
      )}
    </Box>
  );
};

const SummaryChip = ({ label, value }) => (
  <Card sx={{ height: '100%' }}>
    <CardContent sx={{ py: 1.5, px: 2, '&:last-child': { paddingBottom: 1.5 } }}>
      <Typography variant="overline" component="div" sx={{ color: 'text.secondary', mb: 0.25, lineHeight: 1.2 }}>
        {label}
      </Typography>
      <Typography variant="h6" sx={{ fontVariantNumeric: 'tabular-nums', fontWeight: 700 }}>
        {value}
      </Typography>
    </CardContent>
  </Card>
);

export default AssetClassPage;
