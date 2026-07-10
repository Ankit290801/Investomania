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
  name: yup.string().required('Bond name is required'),
  issuer: yup.string().required('Issuer is required'),
  bondType: yup.string().required('Bond type is required'),
  faceValue: yup.number().positive('Face value must be positive').required('Face value is required'),
  couponRate: yup.number().min(0).max(100).required('Coupon rate is required'),
  purchasePrice: yup.number().positive('Purchase price must be positive').required('Purchase price is required'),
  maturityDate: yup.date().required('Maturity date is required'),
  purchaseDate: yup.date().required('Purchase date is required'),
  currency: yup.string().required('Currency is required'),
  creditRating: yup.string(),
  notes: yup.string()
});

const BondForm = ({ onSubmit, onCancel }) => {
  const { control, handleSubmit, formState: { errors } } = useForm({
    resolver: yupResolver(schema),
    defaultValues: {
      name: '',
      issuer: '',
      bondType: 'GOVERNMENT',
      faceValue: '',
      couponRate: '',
      purchasePrice: '',
      maturityDate: '',
      purchaseDate: new Date().toISOString().split('T')[0],
      currency: 'INR',
      creditRating: '',
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
                label="Bond Name"
                fullWidth
                required
                error={!!errors.name}
                helperText={errors.name?.message}
                placeholder="e.g., GOI 7.5% 2026 Bond"
              />
            )}
          />
        </Grid>
        <Grid item xs={12} sm={6}>
          <Controller
            name="issuer"
            control={control}
            render={({ field }) => (
              <TextField
                {...field}
                label="Issuer"
                fullWidth
                error={!!errors.issuer}
                helperText={errors.issuer?.message}
                placeholder="e.g., Government of India"
              />
            )}
          />
        </Grid>
        <Grid item xs={12} sm={6}>
          <Controller
            name="bondType"
            control={control}
            render={({ field }) => (
              <TextField
                {...field}
                select
                label="Bond Type"
                fullWidth
                error={!!errors.bondType}
                helperText={errors.bondType?.message}
              >
                <MenuItem value="GOVERNMENT">Government Bond</MenuItem>
                <MenuItem value="CORPORATE">Corporate Bond</MenuItem>
                <MenuItem value="MUNICIPAL">Municipal Bond</MenuItem>
                <MenuItem value="TREASURY">Treasury Bond</MenuItem>
              </TextField>
            )}
          />
        </Grid>
        <Grid item xs={12} sm={6}>
          <Controller
            name="faceValue"
            control={control}
            render={({ field }) => (
              <TextField
                {...field}
                label="Face Value"
                type="number"
                fullWidth
                error={!!errors.faceValue}
                helperText={errors.faceValue?.message}
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
                label="Purchase Price"
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
            name="couponRate"
            control={control}
            render={({ field }) => (
              <TextField
                {...field}
                label="Coupon Rate (%)"
                type="number"
                fullWidth
                error={!!errors.couponRate}
                helperText={errors.couponRate?.message}
                inputProps={{ step: 0.1 }}
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
            name="creditRating"
            control={control}
            render={({ field }) => (
              <TextField
                {...field}
                select
                label="Credit Rating (Optional)"
                fullWidth
                error={!!errors.creditRating}
                helperText={errors.creditRating?.message}
              >
                <MenuItem value="">None</MenuItem>
                <MenuItem value="AAA">AAA</MenuItem>
                <MenuItem value="AA">AA</MenuItem>
                <MenuItem value="A">A</MenuItem>
                <MenuItem value="BBB">BBB</MenuItem>
                <MenuItem value="BB">BB</MenuItem>
                <MenuItem value="B">B</MenuItem>
                <MenuItem value="CCC">CCC</MenuItem>
                <MenuItem value="CC">CC</MenuItem>
                <MenuItem value="C">C</MenuItem>
                <MenuItem value="D">D</MenuItem>
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

export default BondForm;
