import api from './api';

/**
 * Service for market data operations (prices, symbols, forex)
 */

/**
 * Get current price for a symbol
 */
export const getPrice = async (symbol, market = 'NSE') => {
  const response = await api.get('/market-data/price', {
    params: { symbol, market }
  });
  return response.data;
};

/**
 * Get prices for multiple symbols
 */
export const getBulkPrices = async (symbols) => {
  const response = await api.post('/market-data/prices/bulk', symbols);
  return response.data;
};

/**
 * Update investment value based on latest market price
 */
export const updateInvestmentValue = async (investmentId) => {
  const response = await api.post(`/market-data/investments/${investmentId}/update-value`);
  return response.data;
};

/**
 * Set manual price for a symbol
 */
export const setManualPrice = async (priceData) => {
  const response = await api.post('/market-data/price/manual', priceData);
  return response.data;
};

/**
 * Get price history for a symbol
 */
export const getPriceHistory = async (symbol, limit = 30) => {
  const response = await api.get('/market-data/price/history', {
    params: { symbol, limit }
  });
  return response.data;
};

/**
 * Convert currency amount
 */
export const convertCurrency = async (amount, from, to) => {
  const response = await api.get('/market-data/convert', {
    params: { amount, from, to }
  });
  return response.data;
};

/**
 * Get exchange rate between two currencies
 */
export const getExchangeRate = async (from, to) => {
  const response = await api.get('/market-data/exchange-rate', {
    params: { from, to }
  });
  return response.data;
};

/**
 * Get supported currencies
 */
export const getSupportedCurrencies = async () => {
  const response = await api.get('/market-data/currencies');
  return response.data;
};

/**
 * Get cache statistics
 */
export const getCacheStatistics = async () => {
  const response = await api.get('/market-data/cache/stats');
  return response.data;
};

/**
 * Refresh forex rates for a base currency
 */
export const refreshForexRates = async (baseCurrency = 'USD') => {
  const response = await api.post('/market-data/forex/refresh', null, {
    params: { baseCurrency }
  });
  return response.data;
};

/**
 * Trigger manual price update for all listed investments
 */
export const triggerPriceUpdate = async () => {
  const response = await api.post('/market-data/update/trigger');
  return response.data;
};

/**
 * Get the most recent price update result
 */
export const getLastUpdate = async () => {
  const response = await api.get('/market-data/update/last');
  return response.data;
};

/**
 * Get price update history
 */
export const getUpdateHistory = async (limit = 10) => {
  const response = await api.get('/market-data/update/history', {
    params: { limit }
  });
  return response.data;
};

/**
 * Resolve symbol to API-specific formats
 */
export const resolveSymbol = async (symbol, market) => {
  const response = await api.get('/symbols/resolve', {
    params: { symbol, market }
  });
  return response.data;
};

/**
 * Search symbols (autocomplete)
 */
export const searchSymbols = async (query) => {
  const response = await api.get('/symbols/search', {
    params: { q: query }
  });
  return response.data;
};

/**
 * Update/create symbol mapping manually
 */
export const updateSymbolMapping = async (mappingData) => {
  const response = await api.post('/symbols/mapping', mappingData);
  return response.data;
};

/**
 * Mark symbol as verified
 */
export const verifySymbol = async (symbol) => {
  const response = await api.post(`/symbols/${symbol}/verify`);
  return response.data;
};

export default {
  getPrice,
  getBulkPrices,
  updateInvestmentValue,
  setManualPrice,
  getPriceHistory,
  convertCurrency,
  getExchangeRate,
  getSupportedCurrencies,
  getCacheStatistics,
  refreshForexRates,
  triggerPriceUpdate,
  getLastUpdate,
  getUpdateHistory,
  resolveSymbol,
  searchSymbols,
  updateSymbolMapping,
  verifySymbol
};
