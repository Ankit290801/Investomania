import React from 'react';
import { useForm, Controller } from 'react-hook-form';
import { yupResolver } from '@hookform/resolvers/yup';
import * as yup from 'yup';
import {
  TextField,
  MenuItem,
  Button,
  Box,
  FormControl,
  InputLabel,
  Select,
  FormHelperText,
  InputAdornment,
} from '@mui/material';
import { DatePicker } from '@mui/x-date-pickers/DatePicker';
import { LocalizationProvider } from '@mui/x-date-pickers/LocalizationProvider';
import { AdapterDateFns } from '@mui/x-date-pickers/AdapterDateFns';

const expenseCategories = [
  'HOUSING',
  'FOOD',
  'TRANSPORT',
  'HEALTHCARE',
  'ENTERTAINMENT',
  'EDUCATION',
  'UTILITIES',
  'INSURANCE',
  'OTHER',
];

const currencies = ['INR', 'USD', 'EUR', 'GBP'];

const schema = yup.object().shape({
  description: yup.string().required('Description is required'),
  category: yup.string().required('Category is required'),
  amount: yup.number().positive('Amount must be positive').required('Amount is required'),
  currency: yup.string().required('Currency is required'),
  expenseDate: yup.date().required('Expense date is required'),
  notes: yup.string(),
});

const ExpenseForm = ({ expense, onSubmit, onCancel }) => {
  const {
    control,
    handleSubmit,
    formState: { errors },
  } = useForm({
    resolver: yupResolver(schema),
    defaultValues: expense || {
      description: '',
      category: 'OTHER',
      amount: '',
      currency: 'INR',
      expenseDate: new Date(),
      notes: '',
    },
  });

  return (
    <Box component="form" onSubmit={handleSubmit(onSubmit)} noValidate>
      <Controller
        name="description"
        control={control}
        render={({ field }) => (
          <TextField
            {...field}
            label="Description"
            fullWidth
            margin="normal"
            error={!!errors.description}
            helperText={errors.description?.message}
          />
        )}
      />

      <Controller
        name="category"
        control={control}
        render={({ field }) => (
          <FormControl fullWidth margin="normal" error={!!errors.category}>
            <InputLabel>Category</InputLabel>
            <Select {...field} label="Category">
              {expenseCategories.map((cat) => (
                <MenuItem key={cat} value={cat}>
                  {cat.replace('_', ' ')}
                </MenuItem>
              ))}
            </Select>
            {errors.category && (
              <FormHelperText>{errors.category.message}</FormHelperText>
            )}
          </FormControl>
        )}
      />

      <Box sx={{ display: 'flex', gap: 2 }}>
        <Controller
          name="amount"
          control={control}
          render={({ field }) => (
            <TextField
              {...field}
              label="Amount"
              type="number"
              fullWidth
              margin="normal"
              error={!!errors.amount}
              helperText={errors.amount?.message}
              InputProps={{
                startAdornment: <InputAdornment position="start">₹</InputAdornment>,
              }}
            />
          )}
        />

        <Controller
          name="currency"
          control={control}
          render={({ field }) => (
            <FormControl fullWidth margin="normal" error={!!errors.currency}>
              <InputLabel>Currency</InputLabel>
              <Select {...field} label="Currency">
                {currencies.map((curr) => (
                  <MenuItem key={curr} value={curr}>
                    {curr}
                  </MenuItem>
                ))}
              </Select>
              {errors.currency && (
                <FormHelperText>{errors.currency.message}</FormHelperText>
              )}
            </FormControl>
          )}
        />
      </Box>

      <LocalizationProvider dateAdapter={AdapterDateFns}>
        <Controller
          name="expenseDate"
          control={control}
          render={({ field }) => (
            <DatePicker
              {...field}
              label="Expense Date"
              slotProps={{
                textField: {
                  fullWidth: true,
                  margin: 'normal',
                  error: !!errors.expenseDate,
                  helperText: errors.expenseDate?.message,
                },
              }}
            />
          )}
        />
      </LocalizationProvider>

      <Controller
        name="notes"
        control={control}
        render={({ field }) => (
          <TextField
            {...field}
            label="Notes (Optional)"
            fullWidth
            margin="normal"
            multiline
            rows={3}
          />
        )}
      />

      <Box sx={{ display: 'flex', gap: 2, justifyContent: 'flex-end', mt: 3 }}>
        <Button onClick={onCancel} color="inherit">
          Cancel
        </Button>
        <Button type="submit" variant="contained">
          {expense ? 'Update' : 'Create'} Expense
        </Button>
      </Box>
    </Box>
  );
};

export default ExpenseForm;
