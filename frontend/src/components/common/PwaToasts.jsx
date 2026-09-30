import React from 'react';
import { Snackbar, Alert, Button } from '@mui/material';
import { usePwa } from '../../contexts/PwaContext';

/**
 * E9 P3 — global PWA UX surfaces, mounted once in App:
 *  - OfflineBanner: shows whenever the browser reports offline; data views
 *    are serving last-known cache (as-of behaviour from the E9 plan).
 *  - Update toast: appears when a new service-worker version is waiting;
 *    reload only happens on the user's explicit choice (prompt mode).
 */
const PwaToasts = () => {
  const { offline, updateReady, applyUpdate } = usePwa();

  return (
    <>
      <Snackbar
        open={offline}
        anchorOrigin={{ vertical: 'bottom', horizontal: 'center' }}
        message="You are offline — showing your last synced data"
      />
      <Snackbar
        open={updateReady}
        anchorOrigin={{ vertical: 'bottom', horizontal: 'center' }}
        message="A new version is available"
        action={
          <Button color="inherit" size="small" onClick={applyUpdate}>
            Reload
          </Button>
        }
      />
    </>
  );
};

export default PwaToasts;
