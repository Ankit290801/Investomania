import React, { createContext, useState, useEffect } from 'react';
import { authService } from '../services/authService';

/**
 * Authentication context for managing user state across the application.
 */
export const AuthContext = createContext(null);

export const AuthProvider = ({ children }) => {
  const [user, setUser] = useState(null);
  const [isAuthenticated, setIsAuthenticated] = useState(false);
  const [isLoading, setIsLoading] = useState(true);

  // Load user from session storage on mount
  useEffect(() => {
    const loadUser = () => {
      try {
        const currentUser = authService.getCurrentUser();
        if (currentUser) {
          setUser(currentUser);
          setIsAuthenticated(true);
        }
      } catch (error) {
        console.error('Error loading user:', error);
      } finally {
        setIsLoading(false);
      }
    };

    loadUser();
  }, []);

  /**
   * Login user with username and password.
   * @param {string} username - User's username
   * @param {string} password - User's password
   * @returns {Promise} User data on success
   */
  const login = async (username, password) => {
    const userData = await authService.login(username, password);
    setUser(userData);
    setIsAuthenticated(true);
    return userData;
  };

  /**
   * Register a new user.
   * @param {string} username - User's username
   * @param {string} email - User's email
   * @param {string} password - User's password
   * @returns {Promise} User data on success
   */
  const register = async (username, email, password) => {
    const userData = await authService.register(username, email, password);
    setUser(userData);
    setIsAuthenticated(true);
    return userData;
  };

  /**
   * Logout current user.
   */
  const logout = async () => {
    await authService.logout();
    setUser(null);
    setIsAuthenticated(false);
  };

  const value = {
    user,
    isAuthenticated,
    isLoading,
    login,
    register,
    logout
  };

  return (
    <AuthContext.Provider value={value}>
      {children}
    </AuthContext.Provider>
  );
};
