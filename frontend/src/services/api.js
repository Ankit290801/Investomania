import axios from 'axios';

// E9 P0: relative /api by default — in production the backend serves the
// SPA itself (SpaWebConfig), so shell and API share one origin and the PWA
// service worker can cache API reads. Vite dev serves /api via the proxy;
// VITE_API_BASE_URL still overrides for standalone API deployments.
const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || '/api';

const api = axios.create({
  baseURL: API_BASE_URL,
  headers: {
    'Content-Type': 'application/json',
  },
  withCredentials: true, // Enable sending cookies with requests
});

// Response interceptor for error handling
api.interceptors.response.use(
  (response) => response,
  (error) => {
    // E9 P3: distinguish offline from logged-out. A network-level failure
    // (no response at all — includes the SW returning a cache fallback)
    // must NOT clear the session; only a real HTTP 401 logs out.
    if (error.response?.status === 401) {
      // Session expired or invalid - clear session storage and redirect
      sessionStorage.removeItem('user');
      window.location.href = '/login';
    }
    return Promise.reject(error);
  }
);

export default api;
