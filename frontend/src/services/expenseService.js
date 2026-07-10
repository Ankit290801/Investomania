import api from './api';

export const expenseService = {
  async getAll(category = null, startDate = null, endDate = null) {
    let url = '/expenses';
    const params = new URLSearchParams();
    
    if (category) params.append('category', category);
    if (startDate) params.append('startDate', startDate);
    if (endDate) params.append('endDate', endDate);
    
    const queryString = params.toString();
    if (queryString) url += `?${queryString}`;
    
    const response = await api.get(url);
    return response.data;
  },

  async getById(id) {
    const response = await api.get(`/expenses/${id}`);
    return response.data;
  },

  async create(expense) {
    const response = await api.post('/expenses', expense);
    return response.data;
  },

  async update(id, expense) {
    const response = await api.put(`/expenses/${id}`, expense);
    return response.data;
  },

  async delete(id) {
    await api.delete(`/expenses/${id}`);
  },

  // Mappings
  async addMapping(mapping) {
    const response = await api.post('/expenses/mappings', mapping);
    return response.data;
  },

  async getMappings(expenseId) {
    const response = await api.get(`/expenses/${expenseId}/mappings`);
    return response.data;
  },

  async deleteMapping(mappingId) {
    await api.delete(`/expenses/mappings/${mappingId}`);
  },

  // Analytics
  async getCoverage(startDate = null, endDate = null, currency = 'INR') {
    let url = `/expenses/coverage?currency=${currency}`;
    if (startDate) url += `&startDate=${startDate}`;
    if (endDate) url += `&endDate=${endDate}`;
    
    const response = await api.get(url);
    return response.data;
  },

  async getCategoryBreakdown(startDate = null, endDate = null) {
    let url = '/expenses/category-breakdown';
    const params = new URLSearchParams();
    
    if (startDate) params.append('startDate', startDate);
    if (endDate) params.append('endDate', endDate);
    
    const queryString = params.toString();
    if (queryString) url += `?${queryString}`;
    
    const response = await api.get(url);
    return response.data;
  },

  async getMonthlyTrend(months = 12) {
    const response = await api.get(`/expenses/monthly-trend?months=${months}`);
    return response.data;
  },
};

