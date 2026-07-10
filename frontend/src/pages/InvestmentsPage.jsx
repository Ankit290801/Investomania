import React, { useState, useEffect } from 'react';
import {
  Box,
  Typography,
  Button,
  ToggleButton,
  ToggleButtonGroup,
  useMediaQuery,
  useTheme,
  Alert,
  CircularProgress
} from '@mui/material';
import {
  Add as AddIcon,
  ViewList as ViewListIcon,
  ViewModule as ViewModuleIcon
} from '@mui/icons-material';
import InvestmentFormDialog from '../components/investments/InvestmentFormDialog';
import InvestmentTable from '../components/investments/InvestmentTable';
import InvestmentCard from '../components/investments/InvestmentCard';
import InvestmentDetails from '../components/investments/InvestmentDetails';
import { investmentService } from '../services/investmentService';
import Loading from '../components/common/Loading';

const InvestmentsPage = () => {
  const theme = useTheme();
  const isMobile = useMediaQuery(theme.breakpoints.down('sm'));
  
  const [openDialog, setOpenDialog] = useState(false);
  const [viewMode, setViewMode] = useState(isMobile ? 'card' : 'table');
  const [selectedInvestment, setSelectedInvestment] = useState(null);
  const [openDetails, setOpenDetails] = useState(false);
  const [investments, setInvestments] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  const fetchInvestments = async () => {
    setLoading(true);
    setError(null);
    try {
      const data = await investmentService.getAll();
      setInvestments(data);
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

  const handleView = (investment) => {
    setSelectedInvestment(investment);
    setOpenDetails(true);
  };

  const handleEdit = (investment) => {
    console.log('Edit investment:', investment);
    // TODO: Open edit dialog
  };

  const handleDelete = async (investment) => {
    if (window.confirm(`Are you sure you want to delete ${investment.name}?`)) {
      try {
        await investmentService.delete(investment.id);
        await fetchInvestments(); // Refresh the list
      } catch (err) {
        console.error('Error deleting investment:', err);
        setError(err.response?.data?.error || err.message || 'Failed to delete investment');
      }
    }
  };

  const handleInvestmentCreated = () => {
    fetchInvestments(); // Refresh the list after creating an investment
  };

  if (loading) {
    return <Loading />;
  }

  return (
    <Box>
      <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 3, flexWrap: 'wrap', gap: 2 }}>
        <Typography variant="h4">
          Investments
        </Typography>
        <Box sx={{ display: 'flex', gap: 2, alignItems: 'center' }}>
          {!isMobile && (
            <ToggleButtonGroup
              value={viewMode}
              exclusive
              onChange={(e, newMode) => newMode && setViewMode(newMode)}
              size="small"
            >
              <ToggleButton value="table">
                <ViewListIcon />
              </ToggleButton>
              <ToggleButton value="card">
                <ViewModuleIcon />
              </ToggleButton>
            </ToggleButtonGroup>
          )}
          <Button
            variant="contained"
            startIcon={<AddIcon />}
            onClick={() => setOpenDialog(true)}
          >
            Add Investment
          </Button>
        </Box>
      </Box>

      {error && (
        <Alert severity="error" sx={{ mb: 2 }} onClose={() => setError(null)}>
          {error}
        </Alert>
      )}

      {investments.length === 0 ? (
        <Box sx={{ textAlign: 'center', py: 8 }}>
          <Typography variant="h6" color="text.secondary" gutterBottom>
            No investments yet
          </Typography>
          <Typography color="text.secondary" paragraph>
            Start by adding your first investment
          </Typography>
          <Button
            variant="contained"
            startIcon={<AddIcon />}
            onClick={() => setOpenDialog(true)}
          >
            Add Investment
          </Button>
        </Box>
      ) : (
        <>
          {viewMode === 'table' ? (
            <InvestmentTable
              investments={investments}
              onView={handleView}
              onEdit={handleEdit}
              onDelete={handleDelete}
            />
          ) : (
            <Box>
              {investments.map((investment) => (
                <InvestmentCard
                  key={investment.id}
                  investment={investment}
                  onView={handleView}
                  onEdit={handleEdit}
                  onDelete={handleDelete}
                />
              ))}
            </Box>
          )}
        </>
      )}

      <InvestmentFormDialog
        open={openDialog}
        onClose={() => setOpenDialog(false)}
        onSuccess={handleInvestmentCreated}
      />

      <InvestmentDetails
        investment={selectedInvestment}
        open={openDetails}
        onClose={() => setOpenDetails(false)}
      />
    </Box>
  );
};

export default InvestmentsPage;
