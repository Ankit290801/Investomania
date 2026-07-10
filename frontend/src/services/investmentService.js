import api from './api';

export const investmentService = {
  async getAll() {
    const response = await api.get('/investments');
    return response.data;
  },

  async getById(id) {
    const response = await api.get(`/investments/${id}`);
    return response.data;
  },

  async create(investment) {
    const response = await api.post('/investments', investment);
    return response.data;
  },

  async update(id, investment) {
    const response = await api.put(`/investments/${id}`, investment);
    return response.data;
  },

  async delete(id) {
    const response = await api.delete(`/investments/${id}`);
    return response.data;
  },
};
