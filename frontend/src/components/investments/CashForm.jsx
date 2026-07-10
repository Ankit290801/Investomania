import React from 'react';
import { useForm, Controller } from 'react-hook-form';
import { yupResolver } from '@hookform/resolvers/yup';
import * as yup from 'yup';
import {
  Box,
  TextField,
  MenuItem,
  Button,
  Grid
} from '@mui/material';

const schema = yup.object().shape({
  accountName: yup.string().required('Account name is required'),
  bankName: yup.string().required('Bank/Institution name is required'),
  accountType: yup.string().required('Account type is required'),
  accountNumber: yup.string(),
  currentBalance: yup.number().required('Current balance is required'),
  currency: yup.string().required('Currency is required'),
  interestRate: yup.number().min(0).max(100),
  ifscCode: yup.string(),
  routingNumber: yup.string(),
  notes: yup.string()
});

const CashForm = ({ onSubmit, onCancel }) => {
  const { control, handleSubmit, formState: { errors } } = useForm({
    resolver: yupResolver(schema),
    defaultValues: {
      accountName: '',
      bankName: '',
      accountType: 'SAVINGS',
      accountNumber: '',
      currentBalance: '',
      currency: 'INR',
      interestRate: '',
      ifscCode: '',
      routingNumber: '',
      notes: ''
    }
  });

  return (
    <Box component="form" onSubmit={handleSubmit(onSubmit)}>
      <Grid container spacing={2}>
        <Grid item xs={12} sm={6}>
          <Controller
            name="accountName"
            control={control}
            render={({ field }) => (
              <TextField
                {...field}
                label="Account Name"
                fullWidth
                required
                error={!!errors.accountName}
                helperText={errors.accountName?.message}
                placeholder="e.g., Primary Savings, Emergency Fund"
              />
            )}
          />
        </Grid>
        <Grid item xs={12} sm={6}>
          <Controller
            name="bankName"
            control={control}
            render={({ field }) => (
              <TextField
                {...field}
                label="Bank/Institution Name"
                fullWidth
                required
                error={!!errors.bankName}
                helperText={errors.bankName?.message}
                placeholder="e.g., HDFC Bank, Chase"
              />
            )}
          />
        </Grid>
        <Grid item xs={12} sm={6}>
          <Controller
            name="accountType"
            control={control}
            render={({ field }) => (
              <TextField
                {...field}
                select
                label="Account Type"
                fullWidth
                required
                error={!!errors.accountType}
                helperText={errors.accountType?.message}
              >
                <MenuItem value="SAVINGS">Savings Account</MenuItem>
                <MenuItem value="CHECKING">Checking Account</MenuItem>
                <MenuItem value="CURRENT">Current Account</MenuItem>
                <MenuItem value="CASH">Cash in Hand</MenuItem>
                <MenuItem value="MONEY_MARKET">Money Market</MenuItem>
                <MenuItem value="OTHER">Other</MenuItem>
              </TextField>
            )}
          />
        </Grid>
        <Grid item xs={12} sm={6}>
          <Controller
            name="accountNumber"
            control={control}
            render={({ field }) => (
              <TextField
                {...field}
                label="Account Number (Last 4 digits)"
                fullWidth
                placeholder="****1234"
                inputProps={{ maxLength: 20 }}
              />
            )}
          />
        </Grid>
        <Grid item xs={12} sm={6}>
          <Controller
            name="currentBalance"
            control={control}
            render={({ field }) => (
              <TextField
                {...field}
                label="Current Balance"
                type="number"
                fullWidth
                required
                error={!!errors.currentBalance}
                helperText={errors.currentBalance?.message}
                inputProps={{ step: 0.01 }}
              />
            )}
          />
        </Grid>
        <Grid item xs={12} sm={6}>
          <Controller
            name="currency"
            control={control}
            render={({ field }) => (
              <TextField
                {...field}
                select
                label="Currency"
                fullWidth
                required
                error={!!errors.currency}
                helperText={errors.currency?.message}
              >
                <MenuItem value="INR">INR (₹)</MenuItem>
                <MenuItem value="USD">USD ($)</MenuItem>
                <MenuItem value="EUR">EUR (€)</MenuItem>
                <MenuItem value="GBP">GBP (£)</MenuItem>
              </TextField>
            )}
          />
        </Grid>
        <Grid item xs={12} sm={6}>
          <Controller
            name="interestRate"
            control={control}
            render={({ field }) => (
              <TextField
                {...field}
                label="Interest Rate (% p.a.)"
                type="number"
                fullWidth
                error={!!errors.interestRate}
                helperText={errors.interestRate?.message || 'Optional for savings accounts'}
                inputProps={{ step: 0.1 }}
              />
            )}
          />
        </Grid>
        <Grid item xs={12} sm={6}>
          <Controller
            name="ifscCode"
            control={control}
            render={({ field }) => (
              <TextField
                {...field}
                label="IFSC Code (India)"
                fullWidth
                placeholder="e.g., HDFC0001234"
                inputProps={{ maxLength: 20 }}
              />
            )}
          />
        </Grid>
        <Grid item xs={12} sm={6}>
          <Controller
            name="routingNumber"
            control={control}
            render={({ field }) => (
              <TextField
                {...field}
                label="Routing Number (USA)"
                fullWidth
                placeholder="e.g., 123456789"
                inputProps={{ maxLength: 20 }}
              />
            )}
          />
        </Grid>
        <Grid item xs={12}>
          <Controller
            name="notes"
            control={control}
            render={({ field }) => (
              <TextField
                {...field}
                label="Notes"
                fullWidth
                multiline
                rows={3}
                placeholder="Additional details about this account"
              />
            )}
          />
        </Grid>
      </Grid>

      <Box sx={{ display: 'flex', justifyContent: 'flex-end', gap: 2, mt: 3 }}>
        <Button onClick={onCancel} color="secondary">
          Cancel
        </Button>
        <Button type="submit" variant="contained" color="primary">
          Save Cash Account
        </Button>
      </Box>
    </Box>
  );
};

export default CashForm;
