import React, { useEffect } from 'react';
import { useForm, Controller } from 'react-hook-form';
import { yupResolver } from '@hookform/resolvers/yup';
import * as yup from 'yup';
import {
  Box,
  TextField,
  MenuItem,
  Button,
  Grid,
  Typography
} from '@mui/material';

const schema = yup.object().shape({
  type: yup.string().required('Transaction type is required'),
  date: yup.date().required('Date is required'),
  quantity: yup.number().positive('Quantity must be positive'),
  price: yup.number().positive('Price must be positive'),
  amount: yup.number().positive('Amount must be positive').required('Amount is required'),
  notes: yup.string()
});

const TransactionForm = ({ onSubmit, onCancel, investmentType }) => {
  const { control, handleSubmit, watch, setValue, formState: { errors } } = useForm({
    resolver: yupResolver(schema),
    defaultValues: {
      type: 'BUY',
      date: new Date().toISOString().split('T')[0],
      quantity: '',
      price: '',
      amount: '',
      notes: ''
    }
  });

  const quantity = watch('quantity');
  const price = watch('price');

  // Auto-calculate amount when quantity or price changes
  useEffect(() => {
    if (quantity && price) {
      const calculatedAmount = parseFloat(quantity) * parseFloat(price);
      setValue('amount', calculatedAmount.toFixed(2));
    }
  }, [quantity, price, setValue]);

  const getTransactionTypes = () => {
    const baseTypes = ['BUY', 'SELL'];
    
    if (['FD', 'RD', 'NPS', 'PPF'].includes(investmentType)) {
      return ['CONTRIBUTION', 'MATURITY', 'INTEREST'];
    }
    
    if (investmentType === 'EQUITY') {
      return [...baseTypes, 'DIVIDEND'];
    }
    
    if (investmentType === 'BOND') {
      return [...baseTypes, 'INTEREST', 'MATURITY'];
    }
    
    return baseTypes;
  };

  const showQuantityPrice = ['EQUITY', 'CRYPTO', 'BOND'].includes(investmentType);

  return (
    <Box component="form" onSubmit={handleSubmit(onSubmit)}>
      <Grid container spacing={2}>
        <Grid item xs={12} sm={6}>
          <Controller
            name="type"
            control={control}
            render={({ field }) => (
              <TextField
                {...field}
                select
                label="Transaction Type"
                fullWidth
                error={!!errors.type}
                helperText={errors.type?.message}
              >
                {getTransactionTypes().map((type) => (
                  <MenuItem key={type} value={type}>
                    {type}
                  </MenuItem>
                ))}
              </TextField>
            )}
          />
        </Grid>
        <Grid item xs={12} sm={6}>
          <Controller
            name="date"
            control={control}
            render={({ field }) => (
              <TextField
                {...field}
                label="Date"
                type="date"
                fullWidth
                InputLabelProps={{ shrink: true }}
                error={!!errors.date}
                helperText={errors.date?.message}
              />
            )}
          />
        </Grid>

        {showQuantityPrice && (
          <>
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
                    inputProps={{ step: 0.00001 }}
                  />
                )}
              />
            </Grid>
            <Grid item xs={12} sm={6}>
              <Controller
                name="price"
                control={control}
                render={({ field }) => (
                  <TextField
                    {...field}
                    label="Price per Unit"
                    type="number"
                    fullWidth
                    error={!!errors.price}
                    helperText={errors.price?.message}
                    inputProps={{ step: 0.01 }}
                  />
                )}
              />
            </Grid>
          </>
        )}

        <Grid item xs={12}>
          <Controller
            name="amount"
            control={control}
            render={({ field }) => (
              <TextField
                {...field}
                label="Amount"
                type="number"
                fullWidth
                error={!!errors.amount}
                helperText={errors.amount?.message || (showQuantityPrice ? 'Auto-calculated from quantity × price' : '')}
                InputProps={{
                  readOnly: showQuantityPrice && quantity && price
                }}
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
          Add Transaction
        </Button>
      </Box>
    </Box>
  );
};

export default TransactionForm;
