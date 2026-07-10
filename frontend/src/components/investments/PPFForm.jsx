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
  name: yup.string().required('PPF account name is required'),
  accountNumber: yup.string().required('Account number is required'),
  contributionAmount: yup.number().positive('Amount must be positive').required('Contribution amount is required'),
  contributionFrequency: yup.string().required('Frequency is required'),
  contributionDate: yup.date().required('Contribution date is required'),
  currency: yup.string().required('Currency is required'),
  notes: yup.string()
});

const PPFForm = ({ onSubmit, onCancel }) => {
  const { control, handleSubmit, formState: { errors } } = useForm({
    resolver: yupResolver(schema),
    defaultValues: {
      name: '',
      accountNumber: '',
      contributionAmount: '',
      contributionFrequency: 'ANNUALLY',
      contributionDate: new Date().toISOString().split('T')[0],
      currency: 'INR',
      notes: ''
    }
  });

  return (
    <Box component="form" onSubmit={handleSubmit(onSubmit)}>
      <Grid container spacing={2}>
        <Grid item xs={12}>
          <Controller
            name="name"
            control={control}
            render={({ field }) => (
              <TextField
                {...field}
                label="PPF Account Name"
                fullWidth
                required
                error={!!errors.name}
                helperText={errors.name?.message}
                placeholder="e.g., My PPF Account"
              />
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
                label="PPF Account Number"
                fullWidth
                required
                error={!!errors.accountNumber}
                helperText={errors.accountNumber?.message}
              />
            )}
          />
        </Grid>
        <Grid item xs={12} sm={6}>
          <Controller
            name="contributionAmount"
            control={control}
            render={({ field }) => (
              <TextField
                {...field}
                label="Contribution Amount"
                type="number"
                fullWidth
                error={!!errors.contributionAmount}
                helperText={errors.contributionAmount?.message}
              />
            )}
          />
        </Grid>
        <Grid item xs={12} sm={6}>
          <Controller
            name="contributionFrequency"
            control={control}
            render={({ field }) => (
              <TextField
                {...field}
                select
                label="Contribution Frequency"
                fullWidth
                error={!!errors.contributionFrequency}
                helperText={errors.contributionFrequency?.message}
              >
                <MenuItem value="MONTHLY">Monthly</MenuItem>
                <MenuItem value="QUARTERLY">Quarterly</MenuItem>
                <MenuItem value="ANNUALLY">Annually</MenuItem>
                <MenuItem value="ONE_TIME">One Time</MenuItem>
              </TextField>
            )}
          />
        </Grid>
        <Grid item xs={12} sm={6}>
          <Controller
            name="contributionDate"
            control={control}
            render={({ field }) => (
              <TextField
                {...field}
                label="Contribution Date"
                type="date"
                fullWidth
                InputLabelProps={{ shrink: true }}
                error={!!errors.contributionDate}
                helperText={errors.contributionDate?.message}
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
        <Grid item xs={12}>
          <Controller
            name="notes"
            control={control}
            render={({ field }) => (
              <TextField
                {...field}
                label="Notes (Optional)"
                fullWidth
                multiline
                rows={2}
                placeholder="Any additional notes..."
              />
            )}
          />
        </Grid>
      </Grid>
      <Box sx={{ mt: 3, display: 'flex', gap: 2, justifyContent: 'flex-end' }}>
        <Button onClick={onCancel}>Cancel</Button>
        <Button type="submit" variant="contained">
          Add Investment
        </Button>
      </Box>
    </Box>
  );
};

export default PPFForm;
