import React, { useEffect, useState } from 'react';
import {
  Box,
  Typography,
  Tabs,
  Tab,
  Dialog,
  DialogTitle,
  DialogContent,
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
import { todayInputValue, DATE_INPUT_PATTERN } from '../../utils/dateInput';

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

// INV-4: a Date object serializes as a UTC timestamp and shifts the day; the
// API contract is a local 'YYYY-MM-DD' string, so only strings pass through.
const normalizeDateString = (value) =>
  typeof value === 'string' && DATE_INPUT_PATTERN.test(value.trim()) ? value : null;

const InvestmentFormDialog = ({ open, onClose, onSuccess, editInvestment, defaultType }) => {
  const isEditing = Boolean(editInvestment);
  const [selectedType, setSelectedType] = useState(0);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);

  // Reset the dialog state whenever it opens — for create (first tab) or for
  // edit (tab pinned to the row's type; the type itself is not editable).
  useEffect(() => {
    if (!open) return;
    setError(null);
    const typeIndex = investmentTypes.findIndex(
      (t) => t.value === (editInvestment?.type || defaultType)
    );
    setSelectedType(typeIndex >= 0 ? typeIndex : 0);
  }, [open, editInvestment, defaultType]);

  const handleTabChange = (event, newValue) => {
    setSelectedType(newValue);
    setError(null);
  };

  // Inverse of transformFormData: map an API DTO back onto the sub-form's
  // field names so edit mode opens pre-filled (bug INV-2).
  const buildInitialFormData = (inv) => {
    if (!inv) return undefined;
    const tenureFromMonths = (months) => {
      if (months == null) return { tenure: '', tenureUnit: 'MONTHS' };
      return months % 12 === 0
        ? { tenure: String(months / 12), tenureUnit: 'YEARS' }
        : { tenure: String(months), tenureUnit: 'MONTHS' };
    };
    const dates = {
      purchaseDate: inv.purchaseDate || '',
      startDate: inv.purchaseDate || '',
      contributionDate: inv.purchaseDate || ''
    };
    const base = {
      name: inv.name || '',
      currency: inv.currency || '',
      // Preserve the stored current value on edit; create-mode transforms fall
      // back to principal/purchase-price defaults when this is null.
      currentValue: inv.currentValue ?? null,
      ...dates
    };
    switch (inv.type) {
      case 'EQUITY':
        return { ...base, symbol: inv.symbol || '', quantity: inv.quantity ?? '', purchasePrice: inv.avgPrice ?? '', market: inv.market || '', notes: '', isListed: Boolean(inv.isListed) };
      case 'CRYPTO':
        return { ...base, symbol: inv.symbol || '', quantity: inv.quantity ?? '', purchasePrice: inv.avgPrice ?? '', exchange: inv.exchange || '', notes: '', isListed: Boolean(inv.isListed) };
      case 'BOND':
        return { ...base, issuer: inv.issuer || '', faceValue: inv.faceValue ?? '', couponRate: inv.couponRate ?? '', purchasePrice: inv.currentValue ?? '', maturityDate: inv.maturityDate || '', couponFrequency: inv.couponFrequency || '', firstCouponDate: inv.firstCouponDate || '', autoGenerateIncome: inv.autoGenerateIncome !== false, creditRating: inv.creditRating || '', notes: '' };
      case 'FD':
        return { ...base, bank: inv.bankName || '', principal: inv.principal ?? '', interestRate: inv.interestRate ?? '', ...tenureFromMonths(inv.tenureMonths), maturityDate: inv.maturityDate || '', notes: '' };
      case 'RD':
        return { ...base, bank: inv.bankName || '', monthlyAmount: inv.monthlyContribution ?? '', interestRate: inv.interestRate ?? '', ...tenureFromMonths(inv.tenureMonths), notes: '' };
      case 'NPS':
        return { ...base, pranNumber: inv.accountNumber || '', contributionAmount: inv.totalContributed ?? '', contributionFrequency: inv.contributionFrequency || 'MONTHLY', notes: '' };
      case 'PPF':
        return { ...base, accountNumber: inv.accountNumber || '', contributionAmount: inv.totalContributed ?? '', contributionFrequency: inv.contributionFrequency || 'ANNUALLY', notes: '' };
      case 'REAL_ESTATE':
        return { ...base, propertyType: inv.propertyType || 'RESIDENTIAL', location: inv.location || '', purchasePrice: inv.purchasePrice ?? inv.currentValue ?? '', notes: '' };
      case 'CASH':
        return { accountName: inv.name || '', bankName: inv.bankName || '', accountType: inv.accountType || 'SAVINGS', accountNumber: inv.accountNumber || '', currentBalance: inv.currentValue ?? '', currency: inv.currency || '', interestRate: inv.interestRate ?? '', ifscCode: inv.ifscCode || '', routingNumber: inv.routingNumber || '', notes: inv.notes || '' };
      default:
        return base;
    }
  };

  const transformFormData = (formData, type) => {
    // Each sub-form uses a slightly different name for the acquisition date;
    // normalise to the backend's `purchaseDate` field.
    const acquisitionDate =
      normalizeDateString(formData.purchaseDate) ||
      normalizeDateString(formData.startDate) ||
      normalizeDateString(formData.contributionDate) ||
      todayInputValue();

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
          market: formData.market,
          isListed: formData.isListed !== false
          // currentValue intentionally omitted — backend fetches from live price API
        };
      
      case 'BOND':
        return {
          ...baseData,
          issuer: formData.issuer,
          faceValue: parseFloat(formData.faceValue),
          couponRate: parseFloat(formData.couponRate),
          maturityDate: normalizeDateString(formData.maturityDate),
          // F3: coupon auto-recording schedule. Empty frequency = zero-coupon
          // bond (coupons recorded manually); backend derives firstCouponDate
          // from config when a frequency is given without a date.
          couponFrequency: formData.couponFrequency || null,
          firstCouponDate: formData.couponFrequency
            ? normalizeDateString(formData.firstCouponDate)
            : null,
          autoGenerateIncome: formData.couponFrequency
            ? formData.autoGenerateIncome !== false
            : null,
          creditRating: formData.creditRating || null,
          currentValue: formData.currentValue != null
            ? parseFloat(formData.currentValue)
            : parseFloat(formData.purchasePrice)
        };
      
      case 'FD':
        return {
          ...baseData,
          bankName: formData.bank,
          principal: parseFloat(formData.principal),
          interestRate: parseFloat(formData.interestRate),
          tenureMonths: parseInt(formData.tenureMonths),
          maturityDate: normalizeDateString(formData.maturityDate),
          currentValue: formData.currentValue != null
            ? parseFloat(formData.currentValue)
            : parseFloat(formData.principal)
        };
      
      case 'RD':
        return {
          ...baseData,
          bankName: formData.bank,
          monthlyContribution: parseFloat(formData.monthlyAmount),
          interestRate: parseFloat(formData.interestRate),
          tenureMonths: parseInt(formData.tenureMonths),
          maturityDate: normalizeDateString(formData.maturityDate),
          currentValue: formData.currentValue != null ? parseFloat(formData.currentValue) : 0
        };
      
      case 'NPS':
        return {
          ...baseData,
          accountNumber: formData.pranNumber,
          contributionFrequency: formData.contributionFrequency,
          totalContributed: parseFloat(formData.contributionAmount),
          currentValue: formData.currentValue != null
            ? parseFloat(formData.currentValue)
            : parseFloat(formData.contributionAmount)
        };
      
      case 'PPF':
        return {
          ...baseData,
          accountNumber: formData.accountNumber,
          contributionFrequency: formData.contributionFrequency,
          totalContributed: parseFloat(formData.contributionAmount),
          currentValue: formData.currentValue != null
            ? parseFloat(formData.currentValue)
            : parseFloat(formData.contributionAmount)
        };
      
      case 'REAL_ESTATE':
        return {
          ...baseData,
          propertyType: formData.propertyType,
          location: formData.location,
          purchasePrice: parseFloat(formData.purchasePrice),
          currentValue: formData.currentValue != null
            ? parseFloat(formData.currentValue)
            : parseFloat(formData.purchasePrice)
        };
      
      case 'CRYPTO':
        return {
          ...baseData,
          symbol: formData.symbol,
          quantity: parseFloat(formData.quantity),
          avgPrice: parseFloat(formData.purchasePrice),
          exchange: formData.exchange,
          isListed: formData.isListed !== false
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

      if (isEditing) {
        await investmentService.update(editInvestment.id, transformedData);
      } else {
        await investmentService.create(transformedData);
      }

      if (onSuccess) {
        onSuccess();
      }
      onClose();
    } catch (err) {
      console.error(isEditing ? 'Error updating investment:' : 'Error creating investment:', err);
      setError(
        err.response?.data?.error ||
        err.message ||
        (isEditing ? 'Failed to update investment' : 'Failed to create investment')
      );
    } finally {
      setLoading(false);
    }
  };

  const SelectedFormComponent = investmentTypes[selectedType].component;

  return (
    <Dialog open={open} onClose={onClose} maxWidth="md" fullWidth>
      <DialogTitle>
        <Typography variant="h5">
          {isEditing ? `Edit Investment: ${editInvestment.name}` : 'Add New Investment'}
        </Typography>
      </DialogTitle>
      <DialogContent>
        {error && (
          <Alert severity="error" sx={{ mb: 2 }} onClose={() => setError(null)}>
            {error}
          </Alert>
        )}
        
        {isEditing ? (
          <Typography variant="body2" color="text.secondary" sx={{ mb: 3 }}>
            Type: <strong>{investmentTypes[selectedType]?.label}</strong> (cannot be changed)
          </Typography>
        ) : (
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
        )}
        
        {loading ? (
          <Box sx={{ display: 'flex', justifyContent: 'center', p: 4 }}>
            <CircularProgress />
          </Box>
        ) : (
          <SelectedFormComponent
            key={`${selectedType}-${editInvestment?.id ?? 'new'}-${open}`}
            onSubmit={handleSubmit}
            onCancel={onClose}
            initialData={buildInitialFormData(editInvestment)}
          />
        )}
      </DialogContent>
    </Dialog>
  );
};

export default InvestmentFormDialog;
