import React from 'react';
import { Box, Typography, Chip } from '@mui/material';
import AssetClassPage from '../components/investments/AssetClassPage';
import { formatCurrency } from '../utils/currencyFormatter';
import { getEquityMeta, groupByField } from '../utils/equityMetadata';

/**
 * E2 — thin per-class wrappers around the shared AssetClassPage frame.
 * Each defines only its holdings columns and weightage breakdowns; all
 * behaviour (loading, actions, dialogs, summary chips) lives in the frame.
 * Breakdowns are functions of the loaded investments so they recompute
 * after every refresh.
 */

const toNumber = (v) => {
  const n = parseFloat(v);
  return Number.isFinite(n) ? n : 0;
};

const Secondary = ({ children }) => (
  <Typography variant="caption" color="text.secondary" display="block" sx={{ lineHeight: 1.3 }}>
    {children}
  </Typography>
);

const NameCell = ({ primary, secondary }) => (
  <Box sx={{ minWidth: 0 }}>
    <Typography variant="body2" fontWeight={600} sx={{ overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
      {primary}
    </Typography>
    {secondary ? <Secondary>{secondary}</Secondary> : null}
  </Box>
);

const fmtDate = (d) => {
  if (!d) return '—';
  const date = new Date(d);
  return Number.isNaN(date.getTime())
    ? '—'
    : date.toLocaleDateString('en-IN', { day: '2-digit', month: 'short', year: 'numeric' });
};

/** Value cell with strong tabular numerals for money columns. */
const ValueCell = ({ value, currency, strong }) => (
  <Typography
    variant="body2"
    fontWeight={strong ? 700 : 400}
    sx={{ fontVariantNumeric: 'tabular-nums', textAlign: 'right' }}
  >
    {value != null ? formatCurrency(value, currency) : '—'}
  </Typography>
);

/** Absolute + percentage P&L versus quantity × avgPrice. */
const pnlCell = (inv) => {
  const cost = toNumber(inv.quantity) * toNumber(inv.avgPrice);
  const value = toNumber(inv.currentValue);
  const pnl = value - cost;
  const pct = cost > 0 ? (pnl / cost) * 100 : 0;
  const positive = pnl >= 0;
  return (
    <Box sx={{ textAlign: 'right' }}>
      <Typography
        variant="body2"
        sx={{
          fontWeight: 600,
          fontVariantNumeric: 'tabular-nums',
          color: positive ? 'success.main' : 'error.main'
        }}
      >
        {formatCurrency(Math.abs(pnl), inv.currency)} {positive ? '▲' : '▼'}
      </Typography>
      <Typography variant="caption" color="text.secondary" sx={{ fontVariantNumeric: 'tabular-nums' }}>
        {positive ? '+' : ''}{pct.toFixed(2)}%
      </Typography>
    </Box>
  );
};

/** Class-level totals across the whole portfolio (for the weightage donut). */
const portfolioTotals = (allInvestments) => {
  const totals = new Map();
  for (const inv of allInvestments) {
    totals.set(inv.type, (totals.get(inv.type) || 0) + toNumber(inv.currentValue));
  }
  return [...totals.entries()]
    .map(([name, value]) => ({ name: name.replace(/_/g, ' ').toLowerCase(), value }))
    .filter((e) => e.value > 0)
    .sort((a, b) => b.value - a.value);
};

const ofType = (allInvestments, type) => allInvestments.filter((inv) => inv.type === type);

/* ---------------------------------- Equity ---------------------------------- */

const EquityScreen = ({ allInvestments = [] }) => (
  <AssetClassPage
    type="EQUITY"
    allInvestments={allInvestments}
    columns={[
      {
        id: 'name',
        label: 'Stock',
        render: (inv) => <NameCell primary={inv.name} secondary={`${inv.symbol || ''} · ${inv.market || ''}`} />
      },
      {
        id: 'quantity',
        label: 'Qty',
        align: 'right',
        render: (inv) => (
          <Typography variant="body2" sx={{ fontVariantNumeric: 'tabular-nums' }}>
            {toNumber(inv.quantity)}
          </Typography>
        )
      },
      {
        id: 'avgPrice',
        label: 'Avg Price',
        align: 'right',
        render: (inv) => <ValueCell value={inv.avgPrice} currency={inv.currency} />
      },
      {
        id: 'currentValue',
        label: 'Current Value',
        align: 'right',
        render: (inv) => <ValueCell value={inv.currentValue} currency={inv.currency} strong />
      },
      { id: 'pnl', label: 'P&L', align: 'right', render: (inv) => pnlCell(inv) },
      {
        id: 'isListed',
        label: 'Status',
        render: (inv) => (
          <Chip
            size="small"
            label={inv.isListed ? 'Listed' : 'Unlisted'}
            color={inv.isListed ? 'primary' : 'default'}
            variant={inv.isListed ? 'outlined' : 'filled'}
          />
        )
      }
    ]}
    breakdowns={(classInvestments, all) => [
      { title: 'Portfolio Weightage', entries: portfolioTotals(all) },
      {
        title: 'Sectoral Weightage',
        entries: groupByField(classInvestments, (inv) => getEquityMeta(inv.symbol, inv.market).sector)
      },
      {
        title: 'Market Cap Weightage',
        entries: groupByField(classInvestments, (inv) => getEquityMeta(inv.symbol, inv.market).marketCap)
      }
    ]}
    emptyHint="Add a stock manually or import a broker contract note (CNOTE)."
  />
);

/* ---------------------------------- Crypto ---------------------------------- */

const CryptoScreen = ({ allInvestments = [] }) => (
  <AssetClassPage
    type="CRYPTO"
    allInvestments={allInvestments}
    columns={[
      {
        id: 'name',
        label: 'Asset',
        render: (inv) => <NameCell primary={inv.name} secondary={`${inv.symbol || ''} · ${inv.exchange || ''}`} />
      },
      {
        id: 'quantity',
        label: 'Qty',
        align: 'right',
        render: (inv) => (
          <Typography variant="body2" sx={{ fontVariantNumeric: 'tabular-nums' }}>
            {toNumber(inv.quantity)}
          </Typography>
        )
      },
      {
        id: 'avgPrice',
        label: 'Avg Price',
        align: 'right',
        render: (inv) => <ValueCell value={inv.avgPrice} currency={inv.currency} />
      },
      {
        id: 'currentValue',
        label: 'Current Value',
        align: 'right',
        render: (inv) => <ValueCell value={inv.currentValue} currency={inv.currency} strong />
      },
      { id: 'pnl', label: 'P&L', align: 'right', render: (inv) => pnlCell(inv) }
    ]}
    breakdowns={(classInvestments, all) => [
      { title: 'Portfolio Weightage', entries: portfolioTotals(all) },
      {
        title: 'By Exchange',
        entries: groupByField(classInvestments, (inv) => inv.exchange || 'Unclassified')
      }
    ]}
  />
);

/* ----------------------------------- Bonds ----------------------------------- */

const BondsScreen = ({ allInvestments = [] }) => (
  <AssetClassPage
    type="BOND"
    allInvestments={allInvestments}
    columns={[
      {
        id: 'name',
        label: 'Bond',
        render: (inv) => (
          <NameCell
            primary={inv.name}
            secondary={`${inv.issuer || ''}${inv.creditRating ? ` · ${inv.creditRating}` : ''}`}
          />
        )
      },
      {
        id: 'couponRate',
        label: 'Coupon',
        align: 'right',
        render: (inv) => (
          <Typography variant="body2" sx={{ fontVariantNumeric: 'tabular-nums' }}>
            {inv.couponRate != null ? `${toNumber(inv.couponRate)}%` : '—'}
          </Typography>
        )
      },
      {
        id: 'faceValue',
        label: 'Face Value',
        align: 'right',
        render: (inv) => <ValueCell value={inv.faceValue} currency={inv.currency} />
      },
      {
        id: 'maturityDate',
        label: 'Maturity',
        align: 'right',
        render: (inv) => (
          <Typography variant="body2" sx={{ fontVariantNumeric: 'tabular-nums' }}>
            {fmtDate(inv.maturityDate)}
          </Typography>
        )
      },
      {
        id: 'currentValue',
        label: 'Current Value',
        align: 'right',
        render: (inv) => <ValueCell value={inv.currentValue} currency={inv.currency} strong />
      }
    ]}
    breakdowns={(classInvestments, all) => [
      { title: 'Portfolio Weightage', entries: portfolioTotals(all) },
      {
        title: 'Weightage by Issuer',
        entries: groupByField(classInvestments, (inv) => inv.issuer || 'Unclassified')
      }
    ]}
  />
);

/* ----------------------------------- Cash ----------------------------------- */

const CashScreen = ({ allInvestments = [] }) => (
  <AssetClassPage
    type="CASH"
    allInvestments={allInvestments}
    columns={[
      {
        id: 'name',
        label: 'Account',
        render: (inv) => (
          <NameCell primary={inv.name} secondary={`${inv.bankName || ''} · ${inv.accountType || ''}`} />
        )
      },
      {
        id: 'accountNumber',
        label: 'Account No.',
        render: (inv) => (
          <Typography variant="body2" sx={{ fontVariantNumeric: 'tabular-nums' }}>
            {inv.accountNumber ? `•••• ${String(inv.accountNumber).slice(-4)}` : '—'}
          </Typography>
        )
      },
      {
        id: 'interestRate',
        label: 'Rate',
        align: 'right',
        render: (inv) => (
          <Typography variant="body2" sx={{ fontVariantNumeric: 'tabular-nums' }}>
            {inv.interestRate != null ? `${toNumber(inv.interestRate)}%` : '—'}
          </Typography>
        )
      },
      {
        id: 'currentValue',
        label: 'Balance',
        align: 'right',
        render: (inv) => <ValueCell value={inv.currentValue} currency={inv.currency} strong />
      }
    ]}
    breakdowns={(classInvestments, all) => [
      { title: 'Portfolio Weightage', entries: portfolioTotals(all) },
      {
        title: 'Cash by Bank',
        entries: groupByField(classInvestments, (inv) => inv.bankName || 'Unclassified')
      }
    ]}
  />
);

/* ------------------------------- Fixed Deposits ------------------------------ */

const FDScreen = ({ allInvestments = [] }) => (
  <AssetClassPage
    type="FD"
    allInvestments={allInvestments}
    columns={[
      {
        id: 'name',
        label: 'Deposit',
        render: (inv) => <NameCell primary={inv.name} secondary={inv.bankName} />
      },
      {
        id: 'principal',
        label: 'Principal',
        align: 'right',
        render: (inv) => <ValueCell value={inv.principal} currency={inv.currency} />
      },
      {
        id: 'interestRate',
        label: 'Rate',
        align: 'right',
        render: (inv) => (
          <Typography variant="body2" sx={{ fontVariantNumeric: 'tabular-nums' }}>
            {inv.interestRate != null ? `${toNumber(inv.interestRate)}%` : '—'}
          </Typography>
        )
      },
      {
        id: 'maturityDate',
        label: 'Maturity',
        align: 'right',
        render: (inv) => (
          <Typography variant="body2" sx={{ fontVariantNumeric: 'tabular-nums' }}>
            {fmtDate(inv.maturityDate)}
          </Typography>
        )
      },
      {
        id: 'currentValue',
        label: 'Current Value',
        align: 'right',
        render: (inv) => <ValueCell value={inv.currentValue} currency={inv.currency} strong />
      }
    ]}
    breakdowns={(classInvestments, all) => [
      { title: 'Portfolio Weightage', entries: portfolioTotals(all) },
      {
        title: 'FDs by Bank',
        entries: groupByField(classInvestments, (inv) => inv.bankName || 'Unclassified')
      }
    ]}
  />
);

/* ----------------------------- Recurring Deposits ---------------------------- */

const RDScreen = ({ allInvestments = [] }) => (
  <AssetClassPage
    type="RD"
    allInvestments={allInvestments}
    columns={[
      {
        id: 'name',
        label: 'Plan',
        render: (inv) => <NameCell primary={inv.name} secondary={inv.bankName} />
      },
      {
        id: 'monthlyContribution',
        label: 'Monthly',
        align: 'right',
        render: (inv) => <ValueCell value={inv.monthlyContribution} currency={inv.currency} />
      },
      {
        id: 'interestRate',
        label: 'Rate',
        align: 'right',
        render: (inv) => (
          <Typography variant="body2" sx={{ fontVariantNumeric: 'tabular-nums' }}>
            {inv.interestRate != null ? `${toNumber(inv.interestRate)}%` : '—'}
          </Typography>
        )
      },
      {
        id: 'currentValue',
        label: 'Current Value',
        align: 'right',
        render: (inv) => <ValueCell value={inv.currentValue} currency={inv.currency} strong />
      }
    ]}
    breakdowns={(classInvestments, all) => [
      { title: 'Portfolio Weightage', entries: portfolioTotals(all) }
    ]}
  />
);

/* ------------------------------- NPS and PPF -------------------------------- */

const contributionColumns = [
  {
    id: 'accountNumber',
    label: 'Account',
    render: (inv) => (
      <Typography variant="body2" sx={{ fontVariantNumeric: 'tabular-nums' }}>
        {inv.accountNumber || '—'}
      </Typography>
    )
  },
  {
    id: 'contributionFrequency',
    label: 'Frequency',
    render: (inv) => (
      <Typography variant="body2">
        {(inv.contributionFrequency || '—').toLowerCase()}
      </Typography>
    )
  },
  {
    id: 'totalContributed',
    label: 'Total Contributed',
    align: 'right',
    render: (inv) => <ValueCell value={inv.totalContributed} currency={inv.currency} />
  },
  {
    id: 'currentValue',
    label: 'Current Value',
    align: 'right',
    render: (inv) => <ValueCell value={inv.currentValue} currency={inv.currency} strong />
  }
];

const NPSScreen = ({ allInvestments = [] }) => (
  <AssetClassPage
    type="NPS"
    allInvestments={allInvestments}
    columns={contributionColumns}
    breakdowns={(classInvestments, all) => [
      { title: 'Portfolio Weightage', entries: portfolioTotals(all) }
    ]}
  />
);

const PPFScreen = ({ allInvestments = [] }) => (
  <AssetClassPage
    type="PPF"
    allInvestments={allInvestments}
    columns={contributionColumns}
    breakdowns={(classInvestments, all) => [
      { title: 'Portfolio Weightage', entries: portfolioTotals(all) }
    ]}
  />
);

/* -------------------------------- Real Estate -------------------------------- */

const RealEstateScreen = ({ allInvestments = [] }) => (
  <AssetClassPage
    type="REAL_ESTATE"
    allInvestments={allInvestments}
    columns={[
      {
        id: 'name',
        label: 'Property',
        render: (inv) => <NameCell primary={inv.name} secondary={inv.location || undefined} />
      },
      {
        id: 'propertyType',
        label: 'Type',
        render: (inv) => (
          <Typography variant="body2">
            {(inv.propertyType || '—').replace(/_/g, ' ').toLowerCase()}
          </Typography>
        )
      },
      {
        id: 'purchasePrice',
        label: 'Purchase Price',
        align: 'right',
        render: (inv) => <ValueCell value={inv.purchasePrice} currency={inv.currency} />
      },
      {
        id: 'currentValue',
        label: 'Current Value',
        align: 'right',
        render: (inv) => <ValueCell value={inv.currentValue} currency={inv.currency} strong />
      }
    ]}
    breakdowns={(classInvestments, all) => [
      { title: 'Portfolio Weightage', entries: portfolioTotals(all) }
    ]}
  />
);

export {
  EquityScreen,
  BondsScreen,
  CashScreen,
  FDScreen,
  RDScreen,
  NPSScreen,
  PPFScreen,
  RealEstateScreen,
  CryptoScreen
};
