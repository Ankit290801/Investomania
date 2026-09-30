import React, { useMemo, useState } from 'react';
import { Routes, Route, useNavigate } from 'react-router-dom';
import { useTheme, alpha } from '@mui/material/styles';
import {
  Box,
  Typography,
  Button,
  Card,
  CardContent,
  Grid,
  LinearProgress,
  Alert,
  Skeleton
} from '@mui/material';
import {
  Add as AddIcon,
  Description as DescriptionIcon,
  ChevronRight as ChevronRightIcon
} from '@mui/icons-material';
import InvestmentFormDialog from '../components/investments/InvestmentFormDialog';
import ContractNoteImportDialog from '../components/investments/ContractNoteImportDialog';
import { ASSET_CLASSES } from '../constants/assetClasses';
import { InvestmentsProvider, useSharedInvestments } from '../contexts/InvestmentsContext';
import {
  EquityScreen,
  BondsScreen,
  CashScreen,
  FDScreen,
  RDScreen,
  NPSScreen,
  PPFScreen,
  RealEstateScreen,
  CryptoScreen
} from './assetClassScreens';

const toNumber = (v) => {
  const n = parseFloat(v);
  return Number.isFinite(n) ? n : 0;
};

/** Compact portfolio-total formatter for the hub (e.g. ₹12.40Cr, ₹12.40L). */
const formatTotal = (value) => {
  const abs = Math.abs(value);
  const sign = value < 0 ? '-' : '';
  if (abs >= 1e7) return `${sign}₹${(abs / 1e7).toFixed(2)}Cr`;
  if (abs >= 1e5) return `${sign}₹${(abs / 1e5).toFixed(2)}L`;
  if (abs >= 1e3) return `${sign}₹${(abs / 1e3).toFixed(1)}K`;
  return `${sign}₹${abs.toFixed(0)}`;
};

/**
 * E2 Investments Revamp — /investments is now a hub of asset-class cards
 * (total, share, holding count, weightage glance) instead of one flat table;
 * each card routes to a dedicated sub-screen (/investments/:type) built on
 * the shared AssetClassPage frame with class-specific columns, weightage
 * donuts, and edit/delete/view actions. The old flat table view is retired.
 */
const InvestmentsHub = () => {
  const theme = useTheme();
  const navigate = useNavigate();
  const { investments, loading, error, setError } = useSharedInvestments();
  const [openDialog, setOpenDialog] = useState(false);
  const [cnoteDialogOpen, setCnoteDialogOpen] = useState(false);

  const portfolioTotal = useMemo(
    () => investments.reduce((sum, inv) => sum + toNumber(inv.currentValue), 0),
    [investments]
  );

  const cards = useMemo(
    () =>
      ASSET_CLASSES.map((cls) => {
        const items = investments.filter((inv) => inv.type === cls.type);
        const total = items.reduce((sum, inv) => sum + toNumber(inv.currentValue), 0);
        return {
          ...cls,
          count: items.length,
          total,
          share: portfolioTotal > 0 ? (total / portfolioTotal) * 100 : 0
        };
      }),
    [investments, portfolioTotal]
  );

  const activeCount = cards.filter((c) => c.count > 0).length;

  return (
    <Box>
      <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 3, flexWrap: 'wrap', gap: 2 }}>
        <Box>
          <Typography variant="h4">Investments</Typography>
          <Typography variant="body2" color="text.secondary">
            {portfolioTotal > 0
              ? `${activeCount} asset class${activeCount === 1 ? '' : 'es'} · ${formatTotal(portfolioTotal)} portfolio`
              : 'Browse your portfolio by asset class'}
          </Typography>
        </Box>
        <Box sx={{ display: 'flex', gap: 1.5, alignItems: 'center' }}>
          <Button variant="outlined" startIcon={<DescriptionIcon />} onClick={() => setCnoteDialogOpen(true)}>
            Import CNOTE
          </Button>
          <Button variant="contained" startIcon={<AddIcon />} onClick={() => setOpenDialog(true)}>
            Add Investment
          </Button>
        </Box>
      </Box>

      {error && (
        <Alert severity="error" sx={{ mb: 2 }} onClose={() => setError(null)}>
          {error}
        </Alert>
      )}

      {loading ? (
        <Grid container spacing={2}>
          {ASSET_CLASSES.map((cls) => (
            <Grid item xs={12} sm={6} md={4} key={cls.type}>
              <Card>
                <CardContent>
                  <Skeleton variant="circular" width={40} height={40} />
                  <Skeleton sx={{ mt: 1.5 }} height={28} width="60%" />
                  <Skeleton height={20} width="40%" />
                </CardContent>
              </Card>
            </Grid>
          ))}
        </Grid>
      ) : (
        <Grid container spacing={2}>
          {cards.map((cls) => (
            <Grid item xs={12} sm={6} md={4} key={cls.type}>
              <Card
                onClick={() => navigate(cls.path)}
                sx={{
                  height: '100%',
                  cursor: 'pointer',
                  transition: 'border-color 0.2s ease, transform 0.2s ease',
                  '&:hover': {
                    borderColor: theme.palette.primary.main,
                    transform: 'translateY(-2px)',
                    '& .hub-card-icon': { bgcolor: alpha(theme.palette.primary.main, 0.2) }
                  }
                }}
              >
                <CardContent>
                  <Box sx={{ display: 'flex', alignItems: 'flex-start', gap: 1.5 }}>
                    <Box
                      className="hub-card-icon"
                      sx={{
                        width: 40,
                        height: 40,
                        borderRadius: 2,
                        display: 'flex',
                        alignItems: 'center',
                        justifyContent: 'center',
                        bgcolor: alpha(theme.palette.primary.main, 0.12),
                        color: 'primary.main',
                        transition: 'background-color 0.2s ease'
                      }}
                    >
                      <cls.icon fontSize="small" />
                    </Box>
                    <Box sx={{ flex: 1, minWidth: 0 }}>
                      <Typography variant="subtitle1" fontWeight={700}>
                        {cls.label}
                      </Typography>
                      <Typography variant="caption" color="text.secondary">
                        {cls.description}
                      </Typography>
                    </Box>
                    <ChevronRightIcon fontSize="small" sx={{ color: 'text.secondary', mt: 0.5 }} />
                  </Box>

                  <Box sx={{ mt: 2 }}>
                    <Typography variant="h5" sx={{ fontVariantNumeric: 'tabular-nums', fontWeight: 700 }}>
                      {formatTotal(cls.total)}
                    </Typography>
                    <Typography variant="caption" color="text.secondary">
                      {cls.count > 0
                        ? `${cls.count} holding${cls.count === 1 ? '' : 's'} · ${cls.share.toFixed(1)}% of portfolio`
                        : 'No holdings yet'}
                    </Typography>
                  </Box>

                  <Box sx={{ mt: 1.5 }}>
                    <LinearProgress
                      variant="determinate"
                      value={Math.min(cls.share, 100)}
                      sx={{ height: 5, borderRadius: 3 }}
                    />
                  </Box>
                </CardContent>
              </Card>
            </Grid>
          ))}
        </Grid>
      )}

      <InvestmentFormDialog
        open={openDialog}
        onClose={() => setOpenDialog(false)}
        onSuccess={() => {}}
      />
      <ContractNoteImportDialog
        open={cnoteDialogOpen}
        onClose={() => setCnoteDialogOpen(false)}
        onImported={() => {}}
      />
    </Box>
  );
};

/* Sub-screen wrappers inject the shared investments list from context. */
const makeWrapper = (Screen) => {
  const Wrapper = () => {
    const { investments, refresh } = useSharedInvestments();
    return <Screen allInvestments={investments} onRefresh={refresh} />;
  };
  return Wrapper;
};

const EquityScreenWrapper = makeWrapper(EquityScreen);
const BondsScreenWrapper = makeWrapper(BondsScreen);
const CashScreenWrapper = makeWrapper(CashScreen);
const FDScreenWrapper = makeWrapper(FDScreen);
const RDScreenWrapper = makeWrapper(RDScreen);
const NPSScreenWrapper = makeWrapper(NPSScreen);
const PPFScreenWrapper = makeWrapper(PPFScreen);
const RealEstateScreenWrapper = makeWrapper(RealEstateScreen);
const CryptoScreenWrapper = makeWrapper(CryptoScreen);

/** One data fetch shared by the hub and every sub-screen. */
const InvestmentsPage = () => (
  <InvestmentsProvider>
    <Routes>
      <Route index element={<InvestmentsHub />} />
      <Route path="equity" element={<EquityScreenWrapper />} />
      <Route path="bonds" element={<BondsScreenWrapper />} />
      <Route path="cash" element={<CashScreenWrapper />} />
      <Route path="fd" element={<FDScreenWrapper />} />
      <Route path="rd" element={<RDScreenWrapper />} />
      <Route path="nps" element={<NPSScreenWrapper />} />
      <Route path="ppf" element={<PPFScreenWrapper />} />
      <Route path="real-estate" element={<RealEstateScreenWrapper />} />
      <Route path="crypto" element={<CryptoScreenWrapper />} />
    </Routes>
  </InvestmentsProvider>
);

export default InvestmentsPage;
