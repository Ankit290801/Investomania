// Currency conversion utilities
export const convertCurrency = (amount, fromRate, toRate) => {
  return (amount / fromRate) * toRate;
};

export const formatCurrency = (amount, currency = 'INR', locale = 'en-IN') => {
  return new Intl.NumberFormat(locale, {
    style: 'currency',
    currency: currency,
  }).format(amount);
};
