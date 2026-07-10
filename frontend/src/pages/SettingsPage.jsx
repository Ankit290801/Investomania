import React from 'react';
import { Box, Typography, Paper } from '@mui/material';

const SettingsPage = () => {
  return (
    <Box>
      <Typography variant="h4" gutterBottom>
        Settings
      </Typography>
      <Paper sx={{ p: 3, mt: 2 }}>
        <Typography variant="body1">
          Configure your application preferences.
        </Typography>
        <Typography variant="body2" color="text.secondary" sx={{ mt: 2 }}>
          Profile settings, currency preferences, notification settings, and more.
        </Typography>
      </Paper>
    </Box>
  );
};

export default SettingsPage;
