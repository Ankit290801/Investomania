// Currency utilities

export const getCurrencySymbol = (currency) => {
  const symbols = {
    INR: '₹',
    USD: '$',
    EUR: '€',
    GBP: '£'
  };
  return symbols[currency] || currency;
};

export const formatCurrency = (value, currency = 'INR') => {
  const symbol = getCurrencySymbol(currency);
  const formattedValue = Math.abs(value).toLocaleString('en-IN', {
    minimumFractionDigits: 0,
    maximumFractionDigits: 0
  });
  return `${symbol}${formattedValue}`;
};

export const formatCurrencyWithSign = (value, currency = 'INR') => {
  const sign = value >= 0 ? '+' : '-';
  return `${sign}${formatCurrency(value, currency)}`;
};
