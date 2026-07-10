import React from 'react';
import {
  Card,
  CardContent,
  CardActions,
  Typography,
  Chip,
  IconButton,
  Box,
  Divider
} from '@mui/material';
import { Edit as EditIcon, Delete as DeleteIcon, Visibility as VisibilityIcon } from '@mui/icons-material';

const InvestmentCard = ({ investment, onView, onEdit, onDelete }) => {
  const getTypeColor = (type) => {
    const colors = {
      EQUITY: 'primary',
      BOND: 'secondary',
      FD: 'success',
      RD: 'info',
      NPS: 'warning',
      PPF: 'success',
      REAL_ESTATE: 'error',
      CRYPTO: 'warning'
    };
    return colors[type] || 'default';
  };

  const formatCurrency = (value, currency) => {
    const symbols = { INR: '₹', USD: '$', EUR: '€', GBP: '£' };
    return `${symbols[currency] || currency} ${value.toLocaleString()}`;
  };

  const formatGrowth = (growth) => {
    const color = growth >= 0 ? 'success.main' : 'error.main';
    const sign = growth >= 0 ? '+' : '';
    return <Box component="span" sx={{ color, fontWeight: 'bold' }}>{sign}{growth}%</Box>;
  };

  return (
    <Card sx={{ mb: 2 }}>
      <CardContent>
        <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 1 }}>
          <Typography variant="h6" component="div">
            {investment.name}
          </Typography>
          <Chip
            label={investment.type}
            color={getTypeColor(investment.type)}
            size="small"
          />
        </Box>
        <Divider sx={{ my: 1 }} />
        <Box sx={{ display: 'flex', justifyContent: 'space-between', mt: 2 }}>
          <Box>
            <Typography variant="body2" color="text.secondary">
              Current Value
            </Typography>
            <Typography variant="h6">
              {formatCurrency(investment.currentValue, investment.currency)}
            </Typography>
          </Box>
          <Box sx={{ textAlign: 'right' }}>
            <Typography variant="body2" color="text.secondary">
              Growth
            </Typography>
            <Typography variant="h6">
              {formatGrowth(investment.growth)}
            </Typography>
          </Box>
        </Box>
      </CardContent>
      <CardActions sx={{ justifyContent: 'flex-end', px: 2, pb: 2 }}>
        <IconButton size="small" onClick={() => onView(investment)} color="primary">
          <VisibilityIcon />
        </IconButton>
        <IconButton size="small" onClick={() => onEdit(investment)} color="primary">
          <EditIcon />
        </IconButton>
        <IconButton size="small" onClick={() => onDelete(investment)} color="error">
          <DeleteIcon />
        </IconButton>
      </CardActions>
    </Card>
  );
};

export default InvestmentCard;
