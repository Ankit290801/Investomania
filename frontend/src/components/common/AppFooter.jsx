import React from 'react';
import { Box, Typography, useTheme } from '@mui/material';
import { APP_NAME, APP_TAGLINE, APP_VERSION } from '../../config/branding';
import { BRAND_YEAR } from '../../config/branding';

/**
 * E6 — the single, central copyright footer. Rendered once in
 * DashboardLayout (and on the auth screens) so it is uniform across every
 * screen by construction; screens never include it individually.
 *
 * Styling is deliberately quiet: theme tokens only (`text.disabled` grey),
 * hairline divider, small caption type — visible without competing with
 * content. All strings come from config/branding.js, so the copyright
 * line is updated in one place (single owner, zero screen edits).
 */
const AppFooter = () => {
  const theme = useTheme();

  return (
    <Box
      component="footer"
      sx={{
        mt: 'auto',
        px: { xs: 2, md: 3 },
        py: 2.5,
        borderTop: `1px solid ${theme.palette.divider}`,
        textAlign: 'center'
      }}
    >
      <Typography
        variant="caption"
        sx={{
          color: 'text.disabled',
          letterSpacing: '0.02em',
          display: 'block',
          lineHeight: 1.6
        }}
      >
        {`© ${BRAND_YEAR} ${APP_NAME}. All rights reserved.`}
      </Typography>
      <Typography
        variant="caption"
        sx={{ color: 'text.disabled', display: 'block', mt: 0.25 }}
      >
        {`${APP_NAME} — ${APP_TAGLINE} · v${APP_VERSION}`}
      </Typography>
    </Box>
  );
};

export default AppFooter;
