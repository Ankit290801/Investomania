import { useCallback, useEffect, useState } from 'react';
import { investmentService } from '../services/investmentService';

/**
 * E2 — shared loader for the investments hub and per-class sub-screens.
 * Returns all investments plus a refresh handle; class filtering and
 * aggregation happen in the screens.
 */
const useInvestments = () => {
  const [investments, setInvestments] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  const fetchInvestments = useCallback(async () => {
    setLoading(true);
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
  }, []);

  useEffect(() => {
    fetchInvestments();
  }, [fetchInvestments]);

  return { investments, loading, error, setError, refresh: fetchInvestments };
};

export default useInvestments;
