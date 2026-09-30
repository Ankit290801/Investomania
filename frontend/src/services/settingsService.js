import api from './api';

/**
 * Service for per-user settings (backlog feature F5 — Settings Page).
 */
export const settingsService = {
  /**
   * Get the effective settings for the current user.
   * @returns {Promise} UserSettingsDTO
   */
  async getSettings() {
    const response = await api.get('/settings');
    return response.data;
  },

  /**
   * Apply a partial settings update; null/omitted fields are left unchanged.
   * @param {Object} update - partial UserSettingsDTO
   * @returns {Promise} updated effective UserSettingsDTO
   */
  async updateSettings(update) {
    const response = await api.put('/settings', update);
    return response.data;
  },

  /**
   * Get option lists for rendering the settings UI.
   * @returns {Promise} { currencies, defaultCurrency, themes, dateFormats }
   */
  async getOptions() {
    const response = await api.get('/settings/options');
    return response.data;
  },

  /**
   * Change the current user's password.
   * @param {string} currentPassword
   * @param {string} newPassword
   * @param {string} confirmPassword
   * @returns {Promise} success message
   */
  async changePassword(currentPassword, newPassword, confirmPassword) {
    const response = await api.post('/settings/change-password', {
      currentPassword,
      newPassword,
      confirmPassword
    });
    return response.data;
  }
};
