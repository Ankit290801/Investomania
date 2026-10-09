import React, { useEffect, useMemo } from 'react';
import { useForm, Controller } from 'react-hook-form';
import { yupResolver } from '@hookform/resolvers/yup';
import * as yup from 'yup';
import { requiredDateString, todayInputValue } from '../../utils/dateInput';
import {
  Box,
  TextField,
  MenuItem,
  Button,
  Grid,
  Typography
} from '@mui/material';

// Quantity/price only make sense for trade-shaped rows (BUY/SELL) on
// investments that actually hold units. Every other type — bond interest,
// dividends, contributions, withdrawals, ... — is an amount-only row.
const TRADE_TYPES = ['BUY', 'SELL'];
const UNIT_INVESTMENT_TYPES = ['EQUITY', 'CRYPTO', 'BOND'];

// Yup casts an empty string to NaN for number fields, which turns an optional
// field into an effective "must be a number" error. Normalise '' / null to
// undefined so blank stays blank.
const emptyToUndefined = (value, originalValue) =>
  originalValue === '' || originalValue === null ? undefined : value;

/**
 * Unit trades must carry quantity and price: the backend's equity/crypto
 * recalculation sums `txn.getQuantity()` for BUY/SELL without null guards,
 * and the trade amount is derived from quantity × price. Every other
 * transaction type keeps both fields optional (and hidden).
 */
const buildSchema = (unitTradesNeedQuantityPrice) =>
  yup.object().shape({
    type: yup.string().required('Transaction type is required'),
    date: requiredDateString('Date'),
    quantity: yup
      .number()
      .transform(emptyToUndefined)
      .when('type', {
        is: (type) => unitTradesNeedQuantityPrice && TRADE_TYPES.includes(type),
        then: (field) => field.required('Quantity is required').positive('Quantity must be positive'),
        otherwise: (field) => field.notRequired().positive('Quantity must be positive')
      }),
    price: yup
      .number()
      .transform(emptyToUndefined)
      .when('type', {
        is: (type) => unitTradesNeedQuantityPrice && TRADE_TYPES.includes(type),
        then: (field) => field.required('Price is required').positive('Price must be positive'),
        otherwise: (field) => field.notRequired().positive('Price must be positive')
      }),
    amount: yup
      .number()
      .transform(emptyToUndefined)
      .typeError('Amount is required')
      .required('Amount is required')
      .positive('Amount must be positive'),
    notes: yup.string()
  });

const TransactionForm = ({ onSubmit, onCancel, investmentType }) => {
  const supportsQuantityPrice = UNIT_INVESTMENT_TYPES.includes(investmentType);

  const schema = useMemo(
    () => buildSchema(supportsQuantityPrice),
    [supportsQuantityPrice]
  );

  const { control, handleSubmit, watch, setValue, formState: { errors } } = useForm({
    resolver: yupResolver(schema),
    defaultValues: {
      type: 'BUY',
      date: todayInputValue(),
      quantity: '',
      price: '',
      amount: '',
      notes: ''
    }
  });

  const transactionType = watch('type');
  const quantity = watch('quantity');
  const price = watch('price');

  const showQuantityPrice = supportsQuantityPrice && TRADE_TYPES.includes(transactionType);

  // Auto-calculate the amount for unit trades only — never for amount-only rows.
  useEffect(() => {
    if (showQuantityPrice && quantity && price) {
      const calculatedAmount = parseFloat(quantity) * parseFloat(price);
      setValue('amount', calculatedAmount.toFixed(2));
    }
  }, [showQuantityPrice, quantity, price, setValue]);

  // Never carry a stale quantity/price across a type change into a row that
  // doesn't show them (e.g. BOND BUY → INTEREST).
  useEffect(() => {
    if (!showQuantityPrice) {
      setValue('quantity', '');
      setValue('price', '');
    }
  }, [showQuantityPrice, setValue]);

  const getTransactionTypes = () => {
    const baseTypes = ['BUY', 'SELL'];

    if (['FD', 'RD', 'NPS', 'PPF'].includes(investmentType)) {
      // MATURITY was offered here but is not a backend TransactionType and
      // every submission failed; WITHDRAWAL covers the maturity payout.
      return ['CONTRIBUTION', 'INTEREST', 'WITHDRAWAL'];
    }
    
    if (investmentType === 'EQUITY') {
      return [...baseTypes, 'DIVIDEND'];
    }
    
    if (investmentType === 'BOND') {
      return [...baseTypes, 'INTEREST', 'WITHDRAWAL'];
    }
    
    return baseTypes;
  };

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
                  readOnly: Boolean(showQuantityPrice && quantity && price)
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
