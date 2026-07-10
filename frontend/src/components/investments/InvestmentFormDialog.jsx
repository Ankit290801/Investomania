import React, { useState } from 'react';
import {
  Box,
  Paper,
  Typography,
  Tabs,
  Tab,
  Button,
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  Alert,
  CircularProgress
} from '@mui/material';
import {
  TrendingUp,
  AccountBalance,
  Savings,
  CalendarMonth,
  Shield,
  Home,
  CurrencyBitcoin,
  LocalAtm,
  AccountBalanceWallet
} from '@mui/icons-material';
import EquityForm from './EquityForm';
import BondForm from './BondForm';
import FDForm from './FDForm';
import RDForm from './RDForm';
import NPSForm from './NPSForm';
import PPFForm from './PPFForm';
import RealEstateForm from './RealEstateForm';
import CryptoForm from './CryptoForm';
import CashForm from './CashForm';
import { investmentService } from '../../services/investmentService';

const investmentTypes = [
  { value: 'EQUITY', label: 'Equity', icon: <TrendingUp />, component: EquityForm },
  { value: 'BOND', label: 'Bonds', icon: <AccountBalance />, component: BondForm },
  { value: 'FD', label: 'Fixed Deposit', icon: <Savings />, component: FDForm },
  { value: 'RD', label: 'Recurring Deposit', icon: <CalendarMonth />, component: RDForm },
  { value: 'NPS', label: 'NPS', icon: <Shield />, component: NPSForm },
  { value: 'PPF', label: 'PPF', icon: <LocalAtm />, component: PPFForm },
  { value: 'REAL_ESTATE', label: 'Real Estate', icon: <Home />, component: RealEstateForm },
  { value: 'CRYPTO', label: 'Crypto', icon: <CurrencyBitcoin />, component: CryptoForm },
  { value: 'CASH', label: 'Cash/Bank', icon: <AccountBalanceWallet />, component: CashForm }
];

const InvestmentFormDialog = ({ open, onClose, onSuccess }) => {
  const [selectedType, setSelectedType] = useState(0);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);

  const handleTabChange = (event, newValue) => {
    setSelectedType(newValue);
    setError(null);
  };

  const transformFormData = (formData, type) => {
    // Each sub-form uses a slightly different name for the acquisition date;
    // normalise to the backend's `purchaseDate` field.
    const acquisitionDate =
      formData.purchaseDate ||
      formData.startDate ||
      formData.contributionDate ||
      new Date().toISOString().split('T')[0];

    // Transform form data to match backend DTO structure
    const baseData = {
      type,
      name: formData.name,
      currency: formData.currency,
      purchaseDate: acquisitionDate
    };

    switch (type) {
      case 'EQUITY':
        return {
          ...baseData,
          symbol: formData.symbol,
          quantity: parseFloat(formData.quantity),
          avgPrice: parseFloat(formData.purchasePrice),
          market: formData.market
          // currentValue intentionally omitted — backend fetches from live price API
        };
      
      case 'BOND':
        return {
          ...baseData,
          issuer: formData.issuer,
          faceValue: parseFloat(formData.faceValue),
          couponRate: parseFloat(formData.couponRate),
          maturityDate: formData.maturityDate,
          creditRating: formData.creditRating || null,
          currentValue: parseFloat(formData.purchasePrice)
        };
      
      case 'FD':
        return {
          ...baseData,
          bankName: formData.bank,
          principal: parseFloat(formData.principal),
          interestRate: parseFloat(formData.interestRate),
          tenureMonths: parseInt(formData.tenureMonths),
          maturityDate: formData.maturityDate,
          currentValue: parseFloat(formData.principal)
        };
      
      case 'RD':
        return {
          ...baseData,
          bankName: formData.bank,
          monthlyContribution: parseFloat(formData.monthlyAmount),
          interestRate: parseFloat(formData.interestRate),
          tenureMonths: parseInt(formData.tenureMonths),
          maturityDate: formData.maturityDate,
          currentValue: 0
        };
      
      case 'NPS':
        return {
          ...baseData,
          accountNumber: formData.pranNumber,
          contributionFrequency: formData.contributionFrequency,
          totalContributed: parseFloat(formData.contributionAmount),
          currentValue: parseFloat(formData.contributionAmount)
        };
      
      case 'PPF':
        return {
          ...baseData,
          accountNumber: formData.accountNumber,
          contributionFrequency: formData.contributionFrequency,
          totalContributed: parseFloat(formData.contributionAmount),
          currentValue: parseFloat(formData.contributionAmount)
        };
      
      case 'REAL_ESTATE':
        return {
          ...baseData,
          propertyType: formData.propertyType,
          location: formData.location,
          purchasePrice: parseFloat(formData.purchasePrice),
          currentValue: parseFloat(formData.purchasePrice)
        };
      
      case 'CRYPTO':
        return {
          ...baseData,
          symbol: formData.symbol,
          quantity: parseFloat(formData.quantity),
          avgPrice: parseFloat(formData.purchasePrice),
          exchange: formData.exchange
          // currentValue intentionally omitted — backend fetches from live price API
        };
      
      case 'CASH':
        return {
          ...baseData,
          name: formData.accountName,
          bankName: formData.bankName,
          accountType: formData.accountType,
          accountNumber: formData.accountNumber,
          currentValue: parseFloat(formData.currentBalance),
          interestRate: formData.interestRate ? parseFloat(formData.interestRate) : null,
          ifscCode: formData.ifscCode,
          routingNumber: formData.routingNumber,
          notes: formData.notes
        };
      
      default:
        return baseData;
    }
  };

  const handleSubmit = async (formData) => {
    setLoading(true);
    setError(null);

    try {
      const investmentType = investmentTypes[selectedType].value;
      const transformedData = transformFormData(formData, investmentType);
      
      await investmentService.create(transformedData);
      
      if (onSuccess) {
        onSuccess();
      }
      onClose();
    } catch (err) {
      console.error('Error creating investment:', err);
      setError(err.response?.data?.error || err.message || 'Failed to create investment');
    } finally {
      setLoading(false);
    }
  };

  const SelectedFormComponent = investmentTypes[selectedType].component;

  return (
    <Dialog open={open} onClose={onClose} maxWidth="md" fullWidth>
      <DialogTitle>
        <Typography variant="h5">Add New Investment</Typography>
      </DialogTitle>
      <DialogContent>
        {error && (
          <Alert severity="error" sx={{ mb: 2 }} onClose={() => setError(null)}>
            {error}
          </Alert>
        )}
        
        <Box sx={{ borderBottom: 1, borderColor: 'divider', mb: 3 }}>
          <Tabs
            value={selectedType}
            onChange={handleTabChange}
            variant="scrollable"
            scrollButtons="auto"
          >
            {investmentTypes.map((type, index) => (
              <Tab
                key={type.value}
                icon={type.icon}
                label={type.label}
                iconPosition="start"
              />
            ))}
          </Tabs>
        </Box>
        
        {loading ? (
          <Box sx={{ display: 'flex', justifyContent: 'center', p: 4 }}>
            <CircularProgress />
          </Box>
        ) : (
          <SelectedFormComponent onSubmit={handleSubmit} onCancel={onClose} />
        )}
      </DialogContent>
    </Dialog>
  );
};

export default InvestmentFormDialog;
