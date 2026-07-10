import React from 'react';
import { useForm, Controller } from 'react-hook-form';
import { yupResolver } from '@hookform/resolvers/yup';
import * as yup from 'yup';
import {
  Box,
  TextField,
  MenuItem,
  Button,
  Grid,
  FormControlLabel,
  Checkbox
} from '@mui/material';

const schema = yup.object().shape({
  symbol: yup.string().required('Stock symbol is required'),
  name: yup.string().required('Company name is required'),
  quantity: yup.number().positive('Quantity must be positive').required('Quantity is required'),
  purchasePrice: yup.number().positive('Price must be positive').required('Purchase price is required'),
  purchaseDate: yup.date().required('Purchase date is required'),
  market: yup.string().required('Market is required'),
  currency: yup.string().required('Currency is required'),
  broker: yup.string(),
  notes: yup.string(),
  isListed: yup.boolean()
});

const EquityForm = ({ onSubmit, onCancel }) => {
  const { control, handleSubmit, formState: { errors } } = useForm({
    resolver: yupResolver(schema),
    defaultValues: {
      symbol: '',
      name: '',
      quantity: '',
      purchasePrice: '',
      purchaseDate: new Date().toISOString().split('T')[0],
      market: 'NSE',
      currency: 'INR',
      broker: '',
      notes: '',
      isListed: true
    }
  });

  return (
    <Box component="form" onSubmit={handleSubmit(onSubmit)}>
      <Grid container spacing={2}>
        <Grid item xs={12} sm={6}>
          <Controller
            name="symbol"
            control={control}
            render={({ field }) => (
              <TextField
                {...field}
                label="Stock Symbol"
                fullWidth
                error={!!errors.symbol}
                helperText={errors.symbol?.message}
                placeholder="e.g., RELIANCE"
              />
            )}
          />
        </Grid>
        <Grid item xs={12} sm={6}>
          <Controller
            name="name"
            control={control}
            render={({ field }) => (
              <TextField
                {...field}
                label="Company Name"
                fullWidth
                error={!!errors.name}
                helperText={errors.name?.message}
                placeholder="e.g., Reliance Industries"
              />
            )}
          />
        </Grid>
        <Grid item xs={12} sm={6}>
          <Controller
            name="quantity"
            control={control}
            render={({ field }) => (
              <TextField
                {...field}
                label="Quantity"
                type="number"
                fullWidth
                error={!!errors.quantity}
                helperText={errors.quantity?.message}
              />
            )}
          />
        </Grid>
        <Grid item xs={12} sm={6}>
          <Controller
            name="purchasePrice"
            control={control}
            render={({ field }) => (
              <TextField
                {...field}
                label="Purchase Price per Share"
                type="number"
                fullWidth
                error={!!errors.purchasePrice}
                helperText={errors.purchasePrice?.message}
              />
            )}
          />
        </Grid>
        <Grid item xs={12} sm={6}>
          <Controller
            name="purchaseDate"
            control={control}
            render={({ field }) => (
              <TextField
                {...field}
                label="Purchase Date"
                type="date"
                fullWidth
                InputLabelProps={{ shrink: true }}
                error={!!errors.purchaseDate}
                helperText={errors.purchaseDate?.message}
              />
            )}
          />
        </Grid>
        <Grid item xs={12} sm={6}>
          <Controller
            name="market"
            control={control}
            render={({ field }) => (
              <TextField
                {...field}
                select
                label="Market"
                fullWidth
                error={!!errors.market}
                helperText={errors.market?.message}
              >
                <MenuItem value="NSE">NSE</MenuItem>
                <MenuItem value="BSE">BSE</MenuItem>
                <MenuItem value="NYSE">NYSE</MenuItem>
                <MenuItem value="NASDAQ">NASDAQ</MenuItem>
              </TextField>
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
        <Grid item xs={12} sm={6}>
          <Controller
            name="broker"
            control={control}
            render={({ field }) => (
              <TextField
                {...field}
                label="Broker (Optional)"
                fullWidth
                placeholder="e.g., Zerodha"
              />
            )}
          />
        </Grid>
        <Grid item xs={12} sm={6}>
          <Controller
            name="isListed"
            control={control}
            render={({ field }) => (
              <FormControlLabel
                control={
                  <Checkbox
                    checked={field.value}
                    onChange={(e) => field.onChange(e.target.checked)}
                  />
                }
                label="Listed Security (Enable automatic price updates)"
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

export default EquityForm;
