import api from './api';

export const analyticsService = {
  async getNetWorth(currency = 'INR') {
    const response = await api.get(`/portfolio/net-worth?currency=${currency}`);
    return response.data;
  },

  async getSegmentBreakdown(currency = 'INR') {
    const response = await api.get(`/portfolio/segments?currency=${currency}`);
    return response.data;
  },

  async getGrowthMetrics(years = 5, currency = 'INR') {
    const response = await api.get(`/portfolio/growth?years=${years}&currency=${currency}`);
    return response.data;
  },

  async getSegmentImpact(currency = 'INR') {
    const response = await api.get(`/portfolio/segment-impact?currency=${currency}`);
    return response.data;
  },
};

