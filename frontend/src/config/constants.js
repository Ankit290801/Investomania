// Application constants
// NOTE (E5-lite): chart/asset colours used to live here as raw hexes and
// drifted from the design system. All colours are now owned by
// `config/theme.js` (CHART_COLORS, BRAND_GRADIENT, semantic) — do not
// reintroduce colour constants in this file.

export const DATE_FORMATS = {
  display: 'MMM dd, yyyy',
  api: 'yyyy-MM-dd',
  full: 'MMM dd, yyyy HH:mm:ss',
};

export const INVESTMENT_TYPES = [
  'EQUITY',
  'BOND',
  'FD',
  'RD',
  'NPS',
  'PPF',
  'REAL_ESTATE',
  'CRYPTO',
  'CASH',
];

export const TRANSACTION_TYPES = [
  'BUY',
  'SELL',
  'DIVIDEND',
  'INTEREST',
  'CONTRIBUTION',
  'WITHDRAWAL',
  'BONUS',
  'SPLIT',
  'MERGER',
  'SALARY',
  'DEPOSIT',
  'TRANSFER_IN',
  'TRANSFER_OUT',
];

export const EXPENSE_CATEGORIES = [
  'HOUSING',
  'FOOD',
  'TRANSPORT',
  'HEALTHCARE',
  'ENTERTAINMENT',
  'EDUCATION',
  'UTILITIES',
  'INSURANCE',
  'OTHER',
];
