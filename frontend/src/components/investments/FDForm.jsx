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
  name: yup.string().required('FD name is required'),
  bank: yup.string().required('Bank name is required'),
  accountNumber: yup.string(),
  principal: yup.number().positive('Principal must be positive').required('Principal is required'),
  interestRate: yup.number().min(0).max(100).required('Interest rate is required'),
  tenure: yup.number().positive('Tenure must be positive').required('Tenure is required'),
  tenureUnit: yup.string().required('Tenure unit is required'),
  startDate: yup.date().required('Start date is required'),
  maturityDate: yup.date().required('Maturity date is required'),
  currency: yup.string().required('Currency is required'),
  notes: yup.string()
});

const FDForm = ({ onSubmit, onCancel }) => {
  const { control, handleSubmit, formState: { errors } } = useForm({
    resolver: yupResolver(schema),
    defaultValues: {
      name: '',
      bank: '',
      accountNumber: '',
      principal: '',
      interestRate: '',
      tenure: '',
      tenureUnit: 'YEARS',
      startDate: new Date().toISOString().split('T')[0],
      maturityDate: '',
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
                label="FD Name"
                fullWidth
                required
                error={!!errors.name}
                helperText={errors.name?.message}
                placeholder="e.g., HDFC 5-Year FD"
              />
            )}
          />
        </Grid>
        <Grid item xs={12} sm={6}>
          <Controller
            name="bank"
            control={control}
            render={({ field }) => (
              <TextField
                {...field}
                label="Bank Name"
                fullWidth
                required
                error={!!errors.bank}
                helperText={errors.bank?.message}
                placeholder="e.g., HDFC Bank"
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
                label="Account/FD Number (Optional)"
                fullWidth
                placeholder="FD Account Number"
              />
            )}
          />
        </Grid>
        <Grid item xs={12} sm={6}>
          <Controller
            name="principal"
            control={control}
            render={({ field }) => (
              <TextField
                {...field}
                label="Principal Amount"
                type="number"
                fullWidth
                error={!!errors.principal}
                helperText={errors.principal?.message}
              />
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
                helperText={errors.interestRate?.message}
                inputProps={{ step: 0.1 }}
              />
            )}
          />
        </Grid>
        <Grid item xs={12} sm={6}>
          <Controller
            name="tenure"
            control={control}
            render={({ field }) => (
              <TextField
                {...field}
                label="Tenure"
                type="number"
                fullWidth
                error={!!errors.tenure}
                helperText={errors.tenure?.message}
              />
            )}
          />
        </Grid>
        <Grid item xs={12} sm={6}>
          <Controller
            name="tenureUnit"
            control={control}
            render={({ field }) => (
              <TextField
                {...field}
                select
                label="Tenure Unit"
                fullWidth
                error={!!errors.tenureUnit}
                helperText={errors.tenureUnit?.message}
              >
                <MenuItem value="MONTHS">Months</MenuItem>
                <MenuItem value="YEARS">Years</MenuItem>
              </TextField>
            )}
          />
        </Grid>
        <Grid item xs={12} sm={6}>
          <Controller
            name="startDate"
            control={control}
            render={({ field }) => (
              <TextField
                {...field}
                label="Start Date"
                type="date"
                fullWidth
                InputLabelProps={{ shrink: true }}
                error={!!errors.startDate}
                helperText={errors.startDate?.message}
              />
            )}
          />
        </Grid>
        <Grid item xs={12} sm={6}>
          <Controller
            name="maturityDate"
            control={control}
            render={({ field }) => (
              <TextField
                {...field}
                label="Maturity Date"
                type="date"
                fullWidth
                InputLabelProps={{ shrink: true }}
                error={!!errors.maturityDate}
                helperText={errors.maturityDate?.message}
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

export default FDForm;
