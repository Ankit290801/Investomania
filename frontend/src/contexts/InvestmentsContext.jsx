import { createContext, useContext, useEffect, useMemo, useState } from 'react';
import { investmentService } from '../services/investmentService';

/**
 * E2 — shares one investments fetch between the hub (/investments) and the
 * active asset-class sub-screen, so navigating between them never refetches
 * and mutations trigger a single coordinated refresh.
 */
const InvestmentsContext = createContext(null);

export const InvestmentsProvider = ({ children }) => {
  const [investments, setInvestments] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  const fetchInvestments = async () => {
    setError(null);
    try {
      const data = await investmentService.getAll();
      setInvestments(Array.isArray(data) ? data : []);
    } catch (err) {
      console.error('Error fetching investments:', err);
      setError(err.response?.data?.error || err.message || 'Failed to load investments');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchInvestments();
  }, []);

  const value = useMemo(
    () => ({ investments, loading, error, setError, refresh: fetchInvestments }),
    [investments, loading, error]
  );

  return <InvestmentsContext.Provider value={value}>{children}</InvestmentsContext.Provider>;
};

export const useSharedInvestments = () => {
  const ctx = useContext(InvestmentsContext);
  if (!ctx) {
    throw new Error('useSharedInvestments must be used within an InvestmentsProvider');
  }
  return ctx;
};
