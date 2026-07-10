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
  name: yup.string().required('Property name is required'),
  propertyType: yup.string().required('Property type is required'),
  location: yup.string().required('Location is required'),
  address: yup.string(),
  purchasePrice: yup.number().positive('Price must be positive').required('Purchase price is required'),
  purchaseDate: yup.date().required('Purchase date is required'),
  area: yup.number().positive('Area must be positive'),
  areaUnit: yup.string(),
  currency: yup.string().required('Currency is required'),
  notes: yup.string()
});

const RealEstateForm = ({ onSubmit, onCancel }) => {
  const { control, handleSubmit, formState: { errors } } = useForm({
    resolver: yupResolver(schema),
    defaultValues: {
      name: '',
      propertyType: 'RESIDENTIAL',
      location: '',
      address: '',
      purchasePrice: '',
      purchaseDate: new Date().toISOString().split('T')[0],
      area: '',
      areaUnit: 'SQ_FT',
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
                label="Property Name"
                fullWidth
                required
                error={!!errors.name}
                helperText={errors.name?.message}
                placeholder="e.g., Mumbai Apartment"
              />
            )}
          />
        </Grid>
        <Grid item xs={12} sm={6}>
          <Controller
            name="propertyType"
            control={control}
            render={({ field }) => (
              <TextField
                {...field}
                select
                label="Property Type"
                fullWidth
                required
                error={!!errors.propertyType}
                helperText={errors.propertyType?.message}
              >
                <MenuItem value="RESIDENTIAL">Residential</MenuItem>
                <MenuItem value="COMMERCIAL">Commercial</MenuItem>
                <MenuItem value="LAND">Land/Plot</MenuItem>
                <MenuItem value="AGRICULTURAL">Agricultural</MenuItem>
              </TextField>
            )}
          />
        </Grid>
        <Grid item xs={12} sm={6}>
          <Controller
            name="location"
            control={control}
            render={({ field }) => (
              <TextField
                {...field}
                label="Location (City)"
                fullWidth
                error={!!errors.location}
                helperText={errors.location?.message}
                placeholder="e.g., Mumbai"
              />
            )}
          />
        </Grid>
        <Grid item xs={12}>
          <Controller
            name="address"
            control={control}
            render={({ field }) => (
              <TextField
                {...field}
                label="Full Address (Optional)"
                fullWidth
                placeholder="Property address"
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
            name="area"
            control={control}
            render={({ field }) => (
              <TextField
                {...field}
                label="Area (Optional)"
                type="number"
                fullWidth
                placeholder="Property area"
              />
            )}
          />
        </Grid>
        <Grid item xs={12} sm={6}>
          <Controller
            name="areaUnit"
            control={control}
            render={({ field }) => (
              <TextField
                {...field}
                select
                label="Area Unit"
                fullWidth
              >
                <MenuItem value="SQ_FT">Square Feet</MenuItem>
                <MenuItem value="SQ_M">Square Meters</MenuItem>
                <MenuItem value="ACRES">Acres</MenuItem>
                <MenuItem value="HECTARES">Hectares</MenuItem>
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

export default RealEstateForm;
