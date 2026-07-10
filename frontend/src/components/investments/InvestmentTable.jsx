import React, { useMemo, useState } from 'react';
import {
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  Paper,
  IconButton,
  Chip,
  TableSortLabel,
  Box,
  MenuItem,
  Select,
  FormControl,
  InputLabel,
  Tooltip
} from '@mui/material';
import {
  Edit as EditIcon,
  Delete as DeleteIcon,
  Visibility as VisibilityIcon
} from '@mui/icons-material';

const CURRENCY_SYMBOLS = { INR: '₹', USD: '$', EUR: '€', GBP: '£' };

const formatDate = (dateStr) => {
  if (!dateStr) return '\u2014';
  const d = new Date(dateStr);
  return d.toLocaleDateString('en-IN', { day: '2-digit', month: 'short', year: 'numeric' });
};

const formatCurrency = (value, currency) => {
  if (value === null || value === undefined || Number.isNaN(value)) return '\u2014';
  const symbol = CURRENCY_SYMBOLS[currency] || currency || '';
  return `${symbol} ${Number(value).toLocaleString(undefined, { maximumFractionDigits: 2 })}`;
};

const formatNumber = (value, fractionDigits = 4) => {
  if (value === null || value === undefined || Number.isNaN(value)) return '\u2014';
  return Number(value).toLocaleString(undefined, {
    maximumFractionDigits: fractionDigits,
    minimumFractionDigits: 0
  });
};

const TYPE_COLORS = {
  EQUITY: 'primary',
  BOND: 'secondary',
  FD: 'success',
  RD: 'info',
  NPS: 'warning',
  PPF: 'success',
  REAL_ESTATE: 'error',
  CRYPTO: 'warning',
  CASH: 'default'
};

// Cost basis used to compute P&L. EQUITY/CRYPTO use weighted lot cost; other
// types fall back to their type-specific principal so the column stays useful.
const getCostBasis = (inv) => {
  switch (inv.type) {
    case 'EQUITY':
    case 'CRYPTO':
      if (inv.quantity != null && inv.avgPrice != null) {
        return Number(inv.quantity) * Number(inv.avgPrice);
      }
      return null;
    case 'FD':
      return inv.principal != null ? Number(inv.principal) : null;
    case 'RD':
      if (inv.monthlyContribution != null && inv.tenureMonths != null) {
        return Number(inv.monthlyContribution) * Number(inv.tenureMonths);
      }
      return null;
    case 'PPF':
    case 'NPS':
      return inv.totalContributed != null ? Number(inv.totalContributed) : null;
    case 'BOND':
      return inv.faceValue != null ? Number(inv.faceValue) : null;
    case 'REAL_ESTATE':
      return inv.purchasePrice != null ? Number(inv.purchasePrice) : null;
    case 'CASH':
      return inv.currentValue != null ? Number(inv.currentValue) : null;
    default:
      return null;
  }
};

// Aggregate EQUITY/CRYPTO lots by symbol+venue into one security row.
// Weighted avg price = Σ(qty × price) / Σ(qty). Other types pass through.
const buildRows = (investments) => {
  const groups = new Map();
  const others = [];

  investments.forEach((inv) => {
    if (inv.type !== 'EQUITY' && inv.type !== 'CRYPTO') {
      others.push(inv);
      return;
    }
    const venue = inv.market || inv.exchange || '';
    const key = `${inv.type}|${(inv.symbol || inv.name || '').toUpperCase()}|${venue}|${inv.currency || ''}`;
    if (!groups.has(key)) {
      groups.set(key, {
        type: inv.type,
        symbol: inv.symbol,
        name: inv.name,
        market: inv.market,
        exchange: inv.exchange,
        currency: inv.currency,
        purchaseDate: inv.purchaseDate,
        totalQuantity: 0,
        weightedCost: 0,
        currentValue: 0,
        lots: []
      });
    }
    const grp = groups.get(key);
    const qty = inv.quantity != null ? Number(inv.quantity) : 0;
    const avg = inv.avgPrice != null ? Number(inv.avgPrice) : 0;
    grp.totalQuantity += qty;
    grp.weightedCost += qty * avg;
    grp.currentValue += inv.currentValue != null ? Number(inv.currentValue) : 0;
    grp.lots.push(inv);
    if (inv.purchaseDate && (!grp.purchaseDate || new Date(inv.purchaseDate) < new Date(grp.purchaseDate))) {
      grp.purchaseDate = inv.purchaseDate;
    }
  });

  const grouped = Array.from(groups.values()).map((grp) => {
    const avgPrice = grp.totalQuantity > 0 ? grp.weightedCost / grp.totalQuantity : null;
    const cost = grp.weightedCost;
    const pnl = grp.currentValue - cost;
    const pnlPct = cost > 0 ? (pnl / cost) * 100 : null;
    return {
      id: grp.lots[0].id,
      type: grp.type,
      name: grp.symbol || grp.name,
      displayName: grp.name,
      currency: grp.currency,
      purchaseDate: grp.purchaseDate,
      quantity: grp.totalQuantity,
      avgPrice,
      currentValue: grp.currentValue,
      cost,
      pnl,
      pnlPct,
      lotCount: grp.lots.length,
      _lots: grp.lots,
      _grouped: grp.lots.length > 1
    };
  });

  const otherRows = others.map((inv) => {
    const cost = getCostBasis(inv);
    const cv = inv.currentValue != null ? Number(inv.currentValue) : null;
    const pnl = cost != null && cv != null ? cv - cost : null;
    const pnlPct = cost != null && cost > 0 && pnl != null ? (pnl / cost) * 100 : null;
    return {
      id: inv.id,
      type: inv.type,
      name: inv.name,
      displayName: inv.name,
      currency: inv.currency,
      purchaseDate: inv.purchaseDate,
      quantity: inv.quantity ?? null,
      avgPrice: inv.avgPrice ?? null,
      currentValue: cv,
      cost,
      pnl,
      pnlPct,
      lotCount: 1,
      _lots: [inv],
      _grouped: false
    };
  });

  return [...grouped, ...otherRows];
};

const InvestmentTable = ({ investments, onView, onEdit, onDelete }) => {
  const [orderBy, setOrderBy] = useState('name');
  const [order, setOrder] = useState('asc');
  const [filterType, setFilterType] = useState('ALL');

  const rows = useMemo(() => buildRows(investments || []), [investments]);

  const filtered = filterType === 'ALL' ? rows : rows.filter((r) => r.type === filterType);

  const sorted = useMemo(() => {
    const dir = order === 'asc' ? 1 : -1;
    const cmp = (a, b) => {
      const av = a[orderBy];
      const bv = b[orderBy];
      if (av == null && bv == null) return 0;
      if (av == null) return 1;
      if (bv == null) return -1;
      if (typeof av === 'string' && typeof bv === 'string') return av.localeCompare(bv) * dir;
      if (orderBy === 'purchaseDate') return (new Date(av) - new Date(bv)) * dir;
      return (av - bv) * dir;
    };
    return [...filtered].sort(cmp);
  }, [filtered, orderBy, order]);

  const handleSort = (property) => {
    const isAsc = orderBy === property && order === 'asc';
    setOrder(isAsc ? 'desc' : 'asc');
    setOrderBy(property);
  };

  const renderPnl = (row) => {
    if (row.pnl == null) return '\u2014';
    const color = row.pnl >= 0 ? 'success.main' : 'error.main';
    const sign = row.pnl >= 0 ? '+' : '';
    const pctPart = row.pnlPct != null ? ` (${sign}${row.pnlPct.toFixed(2)}%)` : '';
    return (
      <Box component="span" sx={{ color, fontWeight: 500 }}>
        {sign}{formatCurrency(row.pnl, row.currency)}{pctPart}
      </Box>
    );
  };

  const dispatchAction = (cb, row) => () => {
    // For grouped securities, act on the first lot for now;
    // full lot management can be added later from the details view.
    cb(row._lots[0]);
  };

  return (
    <Box>
      <Box sx={{ mb: 2, display: 'flex', justifyContent: 'flex-end' }}>
        <FormControl size="small" sx={{ minWidth: 150 }}>
          <InputLabel>Filter by Type</InputLabel>
          <Select
            value={filterType}
            label="Filter by Type"
            onChange={(e) => setFilterType(e.target.value)}
          >
            <MenuItem value="ALL">All Types</MenuItem>
            <MenuItem value="EQUITY">Equity</MenuItem>
            <MenuItem value="BOND">Bonds</MenuItem>
            <MenuItem value="FD">Fixed Deposit</MenuItem>
            <MenuItem value="RD">Recurring Deposit</MenuItem>
            <MenuItem value="NPS">NPS</MenuItem>
            <MenuItem value="PPF">PPF</MenuItem>
            <MenuItem value="REAL_ESTATE">Real Estate</MenuItem>
            <MenuItem value="CRYPTO">Crypto</MenuItem>
            <MenuItem value="CASH">Cash</MenuItem>
          </Select>
        </FormControl>
      </Box>

      <TableContainer component={Paper}>
        <Table size="small">
          <TableHead>
            <TableRow>
              <TableCell>
                <TableSortLabel
                  active={orderBy === 'type'}
                  direction={orderBy === 'type' ? order : 'asc'}
                  onClick={() => handleSort('type')}
                >
                  Type
                </TableSortLabel>
              </TableCell>
              <TableCell>
                <TableSortLabel
                  active={orderBy === 'name'}
                  direction={orderBy === 'name' ? order : 'asc'}
                  onClick={() => handleSort('name')}
                >
                  Name
                </TableSortLabel>
              </TableCell>
              <TableCell align="right">
                <TableSortLabel
                  active={orderBy === 'quantity'}
                  direction={orderBy === 'quantity' ? order : 'asc'}
                  onClick={() => handleSort('quantity')}
                >
                  Quantity
                </TableSortLabel>
              </TableCell>
              <TableCell align="right">
                <TableSortLabel
                  active={orderBy === 'avgPrice'}
                  direction={orderBy === 'avgPrice' ? order : 'asc'}
                  onClick={() => handleSort('avgPrice')}
                >
                  Avg Price
                </TableSortLabel>
              </TableCell>
              <TableCell align="right">
                <TableSortLabel
                  active={orderBy === 'currentValue'}
                  direction={orderBy === 'currentValue' ? order : 'asc'}
                  onClick={() => handleSort('currentValue')}
                >
                  Current Value
                </TableSortLabel>
              </TableCell>
              <TableCell align="right">
                <TableSortLabel
                  active={orderBy === 'pnl'}
                  direction={orderBy === 'pnl' ? order : 'asc'}
                  onClick={() => handleSort('pnl')}
                >
                  P&amp;L
                </TableSortLabel>
              </TableCell>
              <TableCell>
                <TableSortLabel
                  active={orderBy === 'purchaseDate'}
                  direction={orderBy === 'purchaseDate' ? order : 'asc'}
                  onClick={() => handleSort('purchaseDate')}
                >
                  Buy Date
                </TableSortLabel>
              </TableCell>
              <TableCell align="center">Actions</TableCell>
            </TableRow>
          </TableHead>
          <TableBody>
            {sorted.map((row) => (
              <TableRow key={`${row.type}-${row.id}-${row.name}`} hover>
                <TableCell>
                  <Chip label={row.type} color={TYPE_COLORS[row.type] || 'default'} size="small" />
                </TableCell>
                <TableCell>
                  <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                    <Box>{row.name}</Box>
                    {row._grouped && (
                      <Chip
                        label={`${row.lotCount} lots`}
                        size="small"
                        variant="outlined"
                        sx={{ height: 20 }}
                      />
                    )}
                  </Box>
                </TableCell>
                <TableCell align="right">{formatNumber(row.quantity)}</TableCell>
                <TableCell align="right">{formatCurrency(row.avgPrice, row.currency)}</TableCell>
                <TableCell align="right">{formatCurrency(row.currentValue, row.currency)}</TableCell>
                <TableCell align="right">{renderPnl(row)}</TableCell>
                <TableCell>{formatDate(row.purchaseDate)}</TableCell>
                <TableCell align="center">
                  <Tooltip title={row._grouped ? 'View first lot' : 'View'}>
                    <span>
                      <IconButton size="small" onClick={dispatchAction(onView, row)} color="primary">
                        <VisibilityIcon fontSize="small" />
                      </IconButton>
                    </span>
                  </Tooltip>
                  <Tooltip title={row._grouped ? 'Edit individual lots from details view' : 'Edit'}>
                    <span>
                      <IconButton
                        size="small"
                        onClick={dispatchAction(onEdit, row)}
                        color="primary"
                        disabled={row._grouped}
                      >
                        <EditIcon fontSize="small" />
                      </IconButton>
                    </span>
                  </Tooltip>
                  <Tooltip title={row._grouped ? 'Delete individual lots from details view' : 'Delete'}>
                    <span>
                      <IconButton
                        size="small"
                        onClick={dispatchAction(onDelete, row)}
                        color="error"
                        disabled={row._grouped}
                      >
                        <DeleteIcon fontSize="small" />
                      </IconButton>
                    </span>
                  </Tooltip>
                </TableCell>
              </TableRow>
            ))}
          </TableBody>
        </Table>
      </TableContainer>
    </Box>
  );
};

export default InvestmentTable;
