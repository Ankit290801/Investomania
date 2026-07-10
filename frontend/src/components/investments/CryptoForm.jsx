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
  symbol: yup.string().required('Crypto symbol is required'),
  name: yup.string().required('Crypto name is required'),
  quantity: yup.number().positive('Quantity must be positive').required('Quantity is required'),
  purchasePrice: yup.number().positive('Price must be positive').required('Purchase price is required'),
  purchaseDate: yup.date().required('Purchase date is required'),
  exchange: yup.string().required('Exchange is required'),
  currency: yup.string().required('Currency is required'),
  walletAddress: yup.string(),
  notes: yup.string(),
  isListed: yup.boolean()
});

const CryptoForm = ({ onSubmit, onCancel }) => {
  const { control, handleSubmit, formState: { errors } } = useForm({
    resolver: yupResolver(schema),
    defaultValues: {
      symbol: '',
      name: '',
      quantity: '',
      purchasePrice: '',
      purchaseDate: new Date().toISOString().split('T')[0],
      exchange: '',
      currency: 'USD',
      walletAddress: '',
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
                label="Crypto Symbol"
                fullWidth
                error={!!errors.symbol}
                helperText={errors.symbol?.message}
                placeholder="e.g., BTC"
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
                label="Crypto Name"
                fullWidth
                error={!!errors.name}
                helperText={errors.name?.message}
                placeholder="e.g., Bitcoin"
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
                inputProps={{ step: 0.00000001 }}
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
                label="Purchase Price per Unit"
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
            name="exchange"
            control={control}
            render={({ field }) => (
              <TextField
                {...field}
                label="Exchange"
                fullWidth
                error={!!errors.exchange}
                helperText={errors.exchange?.message}
                placeholder="e.g., WazirX, Binance"
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
        <Grid item xs={12} sm={6}>
          <Controller
            name="walletAddress"
            control={control}
            render={({ field }) => (
              <TextField
                {...field}
                label="Wallet Address (Optional)"
                fullWidth
                placeholder="Crypto wallet address"
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
                label="Listed on Exchange (Enable automatic price updates)"
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

export default CryptoForm;
