import React, { useEffect, useState } from 'react';
import {
  Box,
  Typography,
  Paper,
  TextField,
  MenuItem,
  Button,
  Grid,
  Alert,
  Snackbar,
  Divider,
  InputAdornment,
  IconButton,
  CircularProgress
} from '@mui/material';
import Visibility from '@mui/icons-material/Visibility';
import VisibilityOff from '@mui/icons-material/VisibilityOff';
import { settingsService } from '../services/settingsService';
import { useThemePreference } from '../contexts/ThemeContext';
import { usePwa } from '../contexts/PwaContext';

const DATE_FORMAT_LABELS = {
  DD_MMM_YYYY: '24 Sep 2026',
  DD_MM_YYYY: '24/09/2026',
  YYYY_MM_DD: '2026-09-24'
};

const THEME_LABELS = {
  LIGHT: 'Light',
  DARK: 'Dark',
  SYSTEM: 'System default'
};

/**
 * Settings page (backlog feature F5): profile password change plus
 * per-user preferences (base currency, theme, date format, market,
 * market-data refresh interval). Unset values fall back to application
 * defaults served by the backend.
 */
const SettingsPage = () => {
  const { setPreference: setThemePreference } = useThemePreference();
  const { canInstall, promptInstall } = usePwa();
  const [settings, setSettings] = useState(null);
  const [options, setOptions] = useState(null);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [saveError, setSaveError] = useState(null);
  const [toast, setToast] = useState('');

  // Password change form state
  const [passwordForm, setPasswordForm] = useState({
    currentPassword: '',
    newPassword: '',
    confirmPassword: ''
  });
  const [passwordError, setPasswordError] = useState(null);
  const [showPasswords, setShowPasswords] = useState(false);

  useEffect(() => {
    let cancelled = false;
    const load = async () => {
      try {
        const [settingsData, optionsData] = await Promise.all([
          settingsService.getSettings(),
          settingsService.getOptions()
        ]);
        if (!cancelled) {
          setSettings(settingsData);
          setOptions(optionsData);
        }
        // E11: the backend theme is NOT applied on load any more — that
        // was resetting the app theme on every visit to this page.
        // ThemeContext seeds it once on first authenticated visit when no
        // local preference exists; after that the local choice wins.
      } catch (err) {
        if (!cancelled) {
          setSaveError(err.response?.data?.error || err.message || 'Failed to load settings');
        }
      } finally {
        if (!cancelled) setLoading(false);
      }
    };
    load();
    return () => {
      cancelled = true;
    };
  }, []);

  const handlePreferenceChange = (field, value) => {
    setSettings((prev) => ({ ...prev, [field]: value }));
  };

  const handleSavePreferences = async () => {
    setSaving(true);
    setSaveError(null);
    try {
      const updated = await settingsService.updateSettings({
        baseCurrency: settings.baseCurrency,
        theme: settings.theme,
        dateFormat: settings.dateFormat,
        defaultMarket: settings.defaultMarket,
        marketDataRefreshSeconds: Number(settings.marketDataRefreshSeconds)
      });
      setSettings(updated);
      setToast('Preferences saved');
    } catch (err) {
      setSaveError(err.response?.data?.error || err.message || 'Failed to save preferences');
    } finally {
      setSaving(false);
    }
  };

  const handlePasswordChangeField = (field, value) => {
    setPasswordForm((prev) => ({ ...prev, [field]: value }));
  };

  const handleChangePassword = async () => {
    setPasswordError(null);
    if (passwordForm.newPassword !== passwordForm.confirmPassword) {
      setPasswordError('New password and confirmation do not match');
      return;
    }
    if (passwordForm.newPassword.length < 6) {
      setPasswordError('New password must be at least 6 characters');
      return;
    }
    try {
      await settingsService.changePassword(
        passwordForm.currentPassword,
        passwordForm.newPassword,
        passwordForm.confirmPassword
      );
      setPasswordForm({ currentPassword: '', newPassword: '', confirmPassword: '' });
      setToast('Password changed successfully');
    } catch (err) {
      setPasswordError(err.response?.data?.error || err.message || 'Failed to change password');
    }
  };

  if (loading) {
    return (
      <Box sx={{ display: 'flex', justifyContent: 'center', p: 6 }}>
        <CircularProgress />
      </Box>
    );
  }

  return (
    <Box>
      <Typography variant="h4" gutterBottom>
        Settings
      </Typography>

      {saveError && (
        <Alert severity="error" sx={{ mb: 2 }} onClose={() => setSaveError(null)}>
          {saveError}
        </Alert>
      )}

      <Grid container spacing={3}>
        <Grid item xs={12} md={6}>
          <Paper sx={{ p: 3 }}>
            <Typography variant="h6" gutterBottom>
              Preferences
            </Typography>
            <Grid container spacing={2}>
              <Grid item xs={12} sm={6}>
                <TextField
                  select
                  fullWidth
                  label="Base Currency"
                  value={settings?.baseCurrency || ''}
                  onChange={(e) => handlePreferenceChange('baseCurrency', e.target.value)}
                  helperText="Used for portfolio totals and snapshots"
                >
                  {(options?.currencies || []).map((code) => (
                    <MenuItem key={code} value={code}>{code}</MenuItem>
                  ))}
                </TextField>
              </Grid>
              <Grid item xs={12} sm={6}>
                <TextField
                  select
                  fullWidth
                  label="Theme"
                  value={settings?.theme || 'SYSTEM'}
                  onChange={(e) => {
                    handlePreferenceChange('theme', e.target.value);
                    // E11: one setter — applies immediately app-wide and
                    // write-through-syncs to backend settings (ThemeContext).
                    setThemePreference(e.target.value);
                  }}
                >
                  {Object.keys(THEME_LABELS).map((key) => (
                    <MenuItem key={key} value={key}>{THEME_LABELS[key]}</MenuItem>
                  ))}
                </TextField>
              </Grid>
              <Grid item xs={12} sm={6}>
                <TextField
                  select
                  fullWidth
                  label="Date Format"
                  value={settings?.dateFormat || 'DD_MMM_YYYY'}
                  onChange={(e) => handlePreferenceChange('dateFormat', e.target.value)}
                >
                  {Object.keys(DATE_FORMAT_LABELS).map((key) => (
                    <MenuItem key={key} value={key}>{DATE_FORMAT_LABELS[key]}</MenuItem>
                  ))}
                </TextField>
              </Grid>
              <Grid item xs={12} sm={6}>
                <TextField
                  fullWidth
                  label="Default Market"
                  value={settings?.defaultMarket || ''}
                  onChange={(e) => handlePreferenceChange('defaultMarket', e.target.value)}
                  placeholder="e.g., NSE"
                />
              </Grid>
              <Grid item xs={12} sm={6}>
                <TextField
                  fullWidth
                  type="number"
                  label="Market Data Refresh"
                  value={settings?.marketDataRefreshSeconds ?? 60}
                  onChange={(e) => handlePreferenceChange('marketDataRefreshSeconds', e.target.value)}
                  InputProps={{ endAdornment: <InputAdornment position="end">sec</InputAdornment> }}
                  helperText="0 disables auto-refresh (max 86400)"
                />
              </Grid>
            </Grid>
            <Box sx={{ mt: 3, display: 'flex', justifyContent: 'flex-end' }}>
              <Button
                variant="contained"
                onClick={handleSavePreferences}
                disabled={saving}
              >
                {saving ? 'Saving…' : 'Save Preferences'}
              </Button>
            </Box>
          </Paper>
        </Grid>

        <Grid item xs={12} md={6}>
          <Paper sx={{ p: 3 }}>
            <Typography variant="h6" gutterBottom>
              Change Password
            </Typography>
            {passwordError && (
              <Alert severity="error" sx={{ mb: 2 }} onClose={() => setPasswordError(null)}>
                {passwordError}
              </Alert>
            )}
            <Grid container spacing={2}>
              <Grid item xs={12}>
                <TextField
                  fullWidth
                  type={showPasswords ? 'text' : 'password'}
                  label="Current Password"
                  value={passwordForm.currentPassword}
                  onChange={(e) => handlePasswordChangeField('currentPassword', e.target.value)}
                />
              </Grid>
              <Grid item xs={12}>
                <TextField
                  fullWidth
                  type={showPasswords ? 'text' : 'password'}
                  label="New Password"
                  value={passwordForm.newPassword}
                  onChange={(e) => handlePasswordChangeField('newPassword', e.target.value)}
                  helperText="Minimum 6 characters"
                />
              </Grid>
              <Grid item xs={12}>
                <TextField
                  fullWidth
                  type={showPasswords ? 'text' : 'password'}
                  label="Confirm New Password"
                  value={passwordForm.confirmPassword}
                  onChange={(e) => handlePasswordChangeField('confirmPassword', e.target.value)}
                  InputProps={{
                    endAdornment: (
                      <InputAdornment position="end">
                        <IconButton
                          onClick={() => setShowPasswords((v) => !v)}
                          edge="end"
                          size="small"
                        >
                          {showPasswords ? <VisibilityOff /> : <Visibility />}
                        </IconButton>
                      </InputAdornment>
                    )
                  }}
                />
              </Grid>
            </Grid>
            <Box sx={{ mt: 3, display: 'flex', justifyContent: 'flex-end' }}>
              <Button
                variant="contained"
                onClick={handleChangePassword}
                disabled={!passwordForm.currentPassword || !passwordForm.newPassword
                  || !passwordForm.confirmPassword}
              >
                Change Password
              </Button>
            </Box>
          </Paper>
        </Grid>

        {/* E9 P1: install surface — Chromium shows the custom prompt; iOS
            installs via Safari's "Add to Home Screen" (documented below). */}
        {canInstall && (
          <Grid item xs={12}>
            <Paper sx={{ p: 3 }}>
              <Typography variant="h6" gutterBottom>
                Install App
              </Typography>
              <Typography variant="body2" color="text.secondary" sx={{ mb: 2 }}>
                Install Investment Tracker as an app for offline access to your
                last synced data. On iPhone/iPad, use Safari's Share menu →
                "Add to Home Screen".
              </Typography>
              <Button variant="contained" onClick={promptInstall}>
                Install
              </Button>
            </Paper>
          </Grid>
        )}

        <Grid item xs={12}>
          <Paper sx={{ p: 3 }}>
            <Typography variant="h6" gutterBottom>
              About Preferences
            </Typography>
            <Divider sx={{ mb: 2 }} />
            <Typography variant="body2" color="text.secondary">
              Preferences apply per user and fall back to application defaults when
              unset. Base currency drives portfolio totals and snapshot aggregation;
              the default market preselects the market field in equity forms; the
              refresh interval controls how often listed holdings re-price (set 0 to
              disable). Theme and date-format support ties into the upcoming UI revamp.
            </Typography>
          </Paper>
        </Grid>
      </Grid>

      <Snackbar
        open={Boolean(toast)}
        autoHideDuration={4000}
        onClose={() => setToast('')}
        message={toast}
      />
    </Box>
  );
};

export default SettingsPage;
