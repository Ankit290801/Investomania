import React, { useMemo } from 'react';
import { Routes, Route, Navigate } from 'react-router-dom';
import { ThemeProvider as MuiThemeProvider, createTheme } from '@mui/material/styles';
import { CssBaseline } from '@mui/material';
import { AuthProvider } from './contexts/AuthContext';
import { PwaProvider } from './contexts/PwaContext';
import PwaToasts from './components/common/PwaToasts';
import { SiteTourProvider } from './contexts/SiteTourContext';
import { ThemeProvider, useThemePreference } from './contexts/ThemeContext';
import { buildAppTheme } from './config/theme';
import Login from './components/auth/Login';
import Register from './components/auth/Register';
import ProtectedRoute from './components/auth/ProtectedRoute';
import DashboardLayout from './components/layout/DashboardLayout';
import SettingsWithTourAnchors from './components/tour/SettingsWithTourAnchors';
import DashboardPage from './pages/DashboardPage';
import InvestmentsPage from './pages/InvestmentsPage';
import TransactionsPage from './pages/TransactionsPage';
import ExpensesPage from './pages/ExpensesPage';
import TaxReportsPage from './pages/TaxReportsPage';
import SettingsPage from './pages/SettingsPage';
import SnapshotPage from './pages/SnapshotPage';

/**
 * F1: theme is resolved from the user's saved preference (F5 `theme` field,
 * mirrored in localStorage) and rebuilt only when the resolved mode changes.
 */
const ThemedApp = () => {
  const { mode } = useThemePreference();
  const theme = useMemo(() => buildAppTheme(mode), [mode]);
  return (
    <MuiThemeProvider theme={theme}>
      <CssBaseline />
      {/* E8: app-wide tour host — the provider renders the Joyride engine
          once, above the routes, so the tour survives Dashboard →
          Investments → Expenses → Settings page changes mid-run. */}
      <SiteTourProvider theme={theme}>
        <PwaProvider>
        <Routes>
          {/* Public routes */}
          <Route path="/login" element={<Login />} />
          <Route path="/register" element={<Register />} />
          
          {/* Protected routes with layout */}
          <Route 
            element={
              <ProtectedRoute>
                <DashboardLayout />
              </ProtectedRoute>
            }
          >
            <Route path="/dashboard" element={<DashboardPage />} />
            {/* E2: descendant <Routes> inside InvestmentsPage needs the trailing wildcard to match /investments/:type sub-screens. */}
            <Route path="/investments/*" element={<InvestmentsPage />} />
            <Route path="/transactions" element={<TransactionsPage />} />
            <Route path="/expenses" element={<ExpensesPage />} />
            <Route path="/tax-reports" element={<TaxReportsPage />} />
            {/* E8: renders SettingsPage untouched — the wrapper only stamps
                data-tour attributes on the page's cards from outside
                (SettingsPage.jsx is stakeholder-owned). */}
            <Route path="/settings" element={<SettingsWithTourAnchors />} />
            <Route path="/snapshots" element={<SnapshotPage />} />
          </Route>
          
          {/* Redirect root to dashboard */}
          <Route path="/" element={<Navigate to="/dashboard" replace />} />
          
          {/* Catch all - redirect to dashboard */}
          <Route path="*" element={<Navigate to="/dashboard" replace />} />
        </Routes>
      <PwaToasts />
      </PwaProvider>
      </SiteTourProvider>
    </MuiThemeProvider>
  );
}

function App() {
  // E11: AuthProvider above ThemeProvider — the theme context syncs the
  // preference to backend settings only while signed in.
  return (
    <AuthProvider>
      <ThemeProvider>
        <ThemedApp />
      </ThemeProvider>
    </AuthProvider>
  );
}

export default App;
