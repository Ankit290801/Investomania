import api from './api';

/**
 * Authentication service for handling login/logout operations.
 */
export const authService = {
  /**
   * Login with username and password.
   * @param {string} username - User's username
   * @param {string} password - User's password
   * @returns {Promise} User data on successful login
   */
  async login(username, password) {
    const response = await api.post('/auth/login', { username, password });
    if (response.data) {
      // Store user info in session storage
      sessionStorage.setItem('user', JSON.stringify(response.data));
    }
    return response.data;
  },

  /**
   * Register a new user.
   * @param {string} username - User's username
   * @param {string} email - User's email
   * @param {string} password - User's password
   * @returns {Promise} User data on successful registration
   */
  async register(username, email, password) {
    const response = await api.post('/auth/register', { username, email, password });
    if (response.data) {
      // Store user info in session storage (auto-login after registration)
      sessionStorage.setItem('user', JSON.stringify(response.data));
    }
    return response.data;
  },

  /**
   * Logout current user.
   */
  async logout() {
    try {
      await api.post('/auth/logout');
    } catch (error) {
      console.error('Logout error:', error);
    } finally {
      // Clear session storage
      sessionStorage.removeItem('user');
    }
  },

  /**
   * Get current user from session storage.
   * @returns {Object|null} User object or null if not authenticated
   */
  getCurrentUser() {
    const userStr = sessionStorage.getItem('user');
    return userStr ? JSON.parse(userStr) : null;
  },

  /**
   * Check if user is authenticated.
   * @returns {boolean} True if user is logged in
   */
  isAuthenticated() {
    return !!this.getCurrentUser();
  }
};
