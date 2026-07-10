import api from './api';

/**
 * Service for managing transactions (buy, sell, dividend, interest, etc.)
 */
export const transactionService = {
  /**
   * Add a transaction to an investment
   * @param {number} investmentId - Investment ID
   * @param {object} transactionData - Transaction details
   * @returns {Promise<object>} Created transaction
   */
  async addTransaction(investmentId, transactionData) {
    const response = await api.post(`/investments/${investmentId}/transactions`, transactionData);
    return response.data;
  },

  /**
   * Get transaction history for a specific investment
   * @param {number} investmentId - Investment ID
   * @returns {Promise<array>} List of transactions
   */
  async getTransactionHistory(investmentId) {
    const response = await api.get(`/investments/${investmentId}/transactions`);
    return response.data;
  },

  /**
   * Get all transactions for the current user
   * @param {object} filters - Optional filters (startDate, endDate)
   * @returns {Promise<array>} List of transactions
   */
  async getAllTransactions(filters = {}) {
    const params = {};
    if (filters.startDate) params.startDate = filters.startDate;
    if (filters.endDate) params.endDate = filters.endDate;
    
    const response = await api.get('/transactions', { params });
    return response.data;
  },

  /**
   * Delete a transaction
   * @param {number} transactionId - Transaction ID
   * @returns {Promise<object>} Success message
   */
  async deleteTransaction(transactionId) {
    const response = await api.delete(`/transactions/${transactionId}`);
    return response.data;
  },
};
