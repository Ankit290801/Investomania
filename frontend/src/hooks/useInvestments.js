import { useState, useEffect } from 'react';
import { investmentService } from '../services/investmentService';

export const useInvestments = () => {
  const [investments, setInvestments] = useState([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);

  const fetchInvestments = async () => {
    setLoading(true);
    try {
      const data = await investmentService.getAll();
      setInvestments(data);
      setError(null);
    } catch (err) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchInvestments();
  }, []);

  return { investments, loading, error, refetch: fetchInvestments };
};
