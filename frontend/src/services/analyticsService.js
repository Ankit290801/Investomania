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

  // E13: real (inflation-adjusted) growth in today's rupees — always INR,
  // independent of the dashboard currency selector.
  async getRealGrowthMetrics(years = 5) {
    const response = await api.get(`/portfolio/real-growth?years=${years}`);
    return response.data;
  },

  async getSegmentImpact(currency = 'INR') {
    const response = await api.get(`/portfolio/segment-impact?currency=${currency}`);
    return response.data;
  },
};

