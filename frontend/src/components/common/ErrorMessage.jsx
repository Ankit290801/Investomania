import React from 'react';
import { Alert, AlertTitle } from '@mui/material';

const ErrorMessage = ({ title = 'Error', message }) => {
  if (!message) return null;

  return (
    <Alert severity="error" sx={{ mb: 2 }}>
      <AlertTitle>{title}</AlertTitle>
      {message}
    </Alert>
  );
};

export default ErrorMessage;
