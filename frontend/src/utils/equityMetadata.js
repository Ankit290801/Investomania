/**
 * E2 — Equity sub-screen classification metadata.
 *
 * The backend DTOs carry no sector / market-cap fields today (recorded in
 * the tracker as a deferred enhancement: persist `sector` + `marketCap`
 * on EQUITY investments and seed them from live-market lookups). Until
 * then, weightage breakdowns use this curated map keyed by symbol, with
 * per-market fallbacks so unknown symbols still aggregate somewhere.
 */

const SECTORS = {
  // IT / Technology
  TCS: 'IT', INFY: 'IT', WIPRO: 'IT', HCLTECH: 'IT', TECHM: 'IT',
  LTIM: 'IT', PERSISTENT: 'IT', COFORGE: 'IT', MPHASIS: 'IT',
  // Banking & Financials
  HDFCBANK: 'Financials', ICICIBANK: 'Financials', SBIN: 'Financials',
  KOTAKBANK: 'Financials', AXISBANK: 'Financials', INDUSINDBK: 'Financials',
  BAJFINANCE: 'Financials', BAJAJFINSV: 'Financials', SBILIFE: 'Financials',
  HDFCLIFE: 'Financials', ICICIPRULI: 'Financials', PFC: 'Financials',
  RECLTD: 'Financials', HDFCAMC: 'Financials',
  // Energy & Utilities
  RELIANCE: 'Energy', ONGC: 'Energy', NTPC: 'Utilities', POWERGRID: 'Utilities',
  ADANIGREEN: 'Utilities', TATAPOWER: 'Utilities', COALINDIA: 'Energy',
  // FMCG & Consumer
  HINDUNILVR: 'FMCG', ITC: 'FMCG', NESTLEIND: 'FMCG', BRITANNIA: 'FMCG',
  DABUR: 'FMCG', MARICO: 'FMCG', TATACONSUM: 'FMCG', UNITDSPR: 'FMCG',
  // Auto
  MARUTI: 'Auto', TATAMOTORS: 'Auto', 'M&M': 'Auto', MOTHERSON: 'Auto',
  'BAJAJ-AUTO': 'Auto', EICHERMOT: 'Auto', HEROMOTOCO: 'Auto', TVSMOTOR: 'Auto',
  // Pharma & Healthcare
  SUNPHARMA: 'Pharma', DRREDDY: 'Pharma', CIPLA: 'Pharma', DIVISLAB: 'Pharma',
  APOLLOHOSP: 'Healthcare', LUPIN: 'Pharma', AUROPHARMA: 'Pharma',
  // Metals & Mining
  TATASTEEL: 'Metals', JSWSTEEL: 'Metals', HINDALCO: 'Metals',
  VEDL: 'Metals', SAIL: 'Metals', NATIONALUM: 'Metals', HINDZINC: 'Metals',
  // Infrastructure & Cement
  LT: 'Infrastructure', ULTRACEMCO: 'Cement', SHREECEM: 'Cement',
  ADANIPORTS: 'Infrastructure', ADANIENT: 'Infrastructure', GRASIM: 'Cement',
  // Telecom & Media
  BHARTIARTL: 'Telecom', IDEA: 'Telecom', ZEEL: 'Media', PVRINOX: 'Media',
  // Others commonly held
  DMART: 'Retail', TITAN: 'Consumer', ASIANPAINT: 'Consumer',
  DLF: 'Real Estate', GODREJPROP: 'Real Estate'
};

const MARKET_CAP = {
  // Mega / large caps (Nifty 50 constituents and peers)
  RELIANCE: 'Large Cap', TCS: 'Large Cap', HDFCBANK: 'Large Cap',
  ICICIBANK: 'Large Cap', INFY: 'Large Cap', SBIN: 'Large Cap',
  BHARTIARTL: 'Large Cap', ITC: 'Large Cap', HINDUNILVR: 'Large Cap',
  LT: 'Large Cap', KOTAKBANK: 'Large Cap', AXISBANK: 'Large Cap',
  MARUTI: 'Large Cap', SUNPHARMA: 'Large Cap', TITAN: 'Large Cap',
  ULTRACEMCO: 'Large Cap', NTPC: 'Large Cap', POWERGRID: 'Large Cap',
  ONGC: 'Large Cap', TATAMOTORS: 'Large Cap', TATASTEEL: 'Large Cap',
  JSWSTEEL: 'Large Cap', 'M&M': 'Large Cap', BAJFINANCE: 'Large Cap',
  HINDALCO: 'Large Cap', COALINDIA: 'Large Cap', ADANIPORTS: 'Large Cap',
  NESTLEIND: 'Large Cap', BAJAJFINSV: 'Large Cap', TECHM: 'Large Cap',
  WIPRO: 'Large Cap', HCLTECH: 'Large Cap', DRREDDY: 'Large Cap',
  CIPLA: 'Large Cap', EICHERMOT: 'Large Cap', HEROMOTOCO: 'Large Cap',
  BRITANNIA: 'Large Cap', GRASIM: 'Large Cap', SBILIFE: 'Large Cap',
  HDFCLIFE: 'Large Cap', INDUSINDBK: 'Large Cap', APOLLOHOSP: 'Large Cap',
  DIVISLAB: 'Large Cap', TATACONSUM: 'Large Cap', SHREECEM: 'Large Cap',
  // Mid caps
  DLF: 'Mid Cap', PFC: 'Mid Cap', RECLTD: 'Mid Cap', VEDL: 'Mid Cap',
  LUPIN: 'Mid Cap', AUROPHARMA: 'Mid Cap', TVSMOTOR: 'Mid Cap',
  FEDERALBNK: 'Mid Cap', BANKBARODA: 'Mid Cap', CANBK: 'Mid Cap',
  PERSISTENT: 'Mid Cap', COFORGE: 'Mid Cap', MPHASIS: 'Mid Cap',
  LTIM: 'Mid Cap', SAIL: 'Mid Cap', NATIONALUM: 'Mid Cap',
  HINDZINC: 'Mid Cap', IDEA: 'Mid Cap', PVRINOX: 'Mid Cap',
  GODREJPROP: 'Mid Cap', TATAPOWER: 'Mid Cap', ADANIGREEN: 'Mid Cap',
  ADANIENT: 'Mid Cap', DMART: 'Mid Cap', MARICO: 'Mid Cap', DABUR: 'Mid Cap',
  // Small caps (fallback for anything unclassified)
  _DEFAULT: 'Small Cap'
};

const MARKET_FALLBACK_SECTOR = {
  NSE: 'Diversified',
  BSE: 'Diversified',
  NASDAQ: 'Technology',
  NYSE: 'Diversified'
};

/**
 * Classification for one equity holding: sector + market-cap bucket.
 * Unknown symbols fall back to per-market defaults so every holding
 * aggregates into the donuts.
 */
export const getEquityMeta = (symbol, market) => {
  const key = String(symbol || '').toUpperCase().trim();
  const isIndian = !market || market.toUpperCase() === 'NSE' || market.toUpperCase() === 'BSE';
  return {
    sector: SECTORS[key] || (isIndian ? 'Other' : MARKET_FALLBACK_SECTOR[market?.toUpperCase()] || 'Other'),
    marketCap: MARKET_CAP[key] || (isIndian ? MARKET_CAP._DEFAULT : 'Unclassified')
  };
};

/**
 * Group a list of investments by a field accessor and return
 * chart-ready entries sorted by value (desc).
 */
export const groupByField = (investments, accessor) => {
  const totals = new Map();
  for (const inv of investments) {
    const key = accessor(inv) || 'Unclassified';
    totals.set(key, (totals.get(key) || 0) + parseFloat(inv.currentValue || 0));
  }
  return [...totals.entries()]
    .map(([name, value]) => ({ name, value }))
    .filter((e) => e.value > 0)
    .sort((a, b) => b.value - a.value);
};
