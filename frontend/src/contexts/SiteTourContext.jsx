import React, { createContext, useContext, useMemo, useRef, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useJoyride } from 'react-joyride';
import { TOUR_OVERLAY, buildAppTheme } from '../config/theme';
import ThemeContext from './ThemeContext';

/**
 * E8 — First-Time User Site Guide.
 *
 * A guided tour that spans four real screens (Dashboard → Investments →
 * Expenses → Settings). react-joyride (v3 — the industry-standard React
 * tour library) drives the spotlight/tooltip engine; the multi-route hops
 * follow the library's own recipe: each step declares its route in `data`,
 * a `before` hook navigates with React Router before the step renders, and
 * Joyride's target wait keeps polling until the page's targets have mounted.
 *
 * Storage note: auth state lives in sessionStorage, but "has seen the tour"
 * is a durable, per-user preference — so it is remembered in localStorage
 * (`app.siteTour.seen.<username>`) per the E8 record.
 *
 * Colours (E5): the tour never hardcodes hex literals — every style reads
 * from the live MUI theme so light/dark follow the app exactly.
 */

/** Per-user localStorage keys (`seen.<username>`). Session-scoped run-gate flag. */
export const SITE_TOUR_KEYS = {
  seenFor: (username) => `app.siteTour.seen.${username || 'guest'}`,
  RUN_GATE: 'app.siteTour.runGate'
};

/**
 * The full tour, Dashboard → Investments → Expenses → Settings. Plain data —
 * targets are `[data-tour]` attributes; route changes happen through the
 * `before` hooks (inline below). Steps without a `before` hook stay on the
 * current page.
 *
 * Every step carries `skipBeacon: true` (behaviour options are documented as
 * per-step fields) so the tour opens tooltips directly instead of waiting on
 * a pulsing beacon click per stop.
 */
const buildSteps = (navigate) => [
  {
    id: 'welcome',
    data: { route: '/dashboard' },
    hideOverlay: true,
    target: '[data-tour="tour-welcome"]',
    title: 'Welcome to Investment Tracker 👋',
    content:
      'A quick 4-page tour: Dashboard, Investments, Expenses and Settings. Use Next to move on — or Skip to look around on your own.'
  },
  {
    id: 'sidebar',
    target: '[data-tour="nav-sidebar"]',
    title: 'Navigation sidebar',
    content:
      'Switch between Dashboard, Investments, Transactions, Expenses, Tax Reports, Snapshots and Settings from here. On small screens it collapses behind the ☰ menu button.'
  },
  {
    id: 'dashboard-actions',
    target: '[data-tour="dashboard-actions"]',
    title: 'Update Prices & Currency',
    content:
      '"Update Prices" refreshes your listed holdings with the latest market data. The currency selector re-renders the whole portfolio in INR, USD, EUR or GBP.'
  },
  {
    id: 'dashboard-cards',
    target: '[data-tour="dashboard-cards"]',
    title: 'Your portfolio at a glance',
    content:
      'Net worth, how many months your cash covers your expenses, portfolio mix and growth history. The Growth chart also has a Nominal/Real toggle that adjusts for inflation.'
  },
  {
    id: 'investments-hub',
    data: { route: '/investments' },
    before: () => {
      navigate('/investments');
      return new Promise((resolve) => setTimeout(resolve, 350)); // page transition
    },
    target: '[data-tour="investments-header"]',
    title: 'Investments hub',
    content:
      'Every asset class gets its own card — Equity, Bonds, Cash, FDs and more. Open a card for that asset class’s holdings, dividends and coupons, or use "Add Investment" to record one.'
  },
  {
    id: 'expenses-header',
    data: { route: '/expenses' },
    before: () => {
      navigate('/expenses');
      return new Promise((resolve) => setTimeout(resolve, 350)); // page transition
    },
    target: '[data-tour="expenses-header"]',
    title: 'Expenses',
    content:
      'Track spending in nine categories, then see month-over-month and year-over-year analytics right above the table. "Import Excel" bulk-loads statements, and expenses can be linked to the investments they were paid from.'
  },
  {
    id: 'settings-preferences',
    data: { route: '/settings' },
    before: () => {
      navigate('/settings');
      return new Promise((resolve) => setTimeout(resolve, 350)); // page transition
    },
    target: '[data-tour="settings-preferences"]',
    title: 'Settings — Preferences',
    content:
      'Base currency, theme, date format, default market and how often listed holdings re-price. Preferences apply per user.'
  },
  {
    id: 'settings-backup',
    target: '[data-tour="settings-backup"]',
    title: 'Backup & Restore',
    content:
      'Download a complete JSON backup of your portfolio — or restore one. That’s the tour! Replay it anytime from the avatar menu (top right).'
  }
];

/**
 * Theme-driven appearance (v3 API): colour tokens go in the `options` prop,
 * element-level CSS in `styles` (buttonPrimary — not v2's buttonNext — is
 * the Next/Last button). See E5 note at top: no hardcoded colours.
 */
const buildTourOptions = (theme) => ({
  backgroundColor: theme.palette.background.paper,
  textColor: theme.palette.text.primary,
  primaryColor: theme.palette.primary.main,
  arrowColor: theme.palette.background.paper,
  overlayColor: TOUR_OVERLAY[theme.palette.mode] || TOUR_OVERLAY.light,
  zIndex: 1400,
  skipBeacon: true, // tooltips open straight away — no pulsing beacon stop
  spotlightRadius: 12
});

const buildTourStyles = (theme) => ({
  tooltip: {
    borderRadius: 16,
    padding: 18,
    border: `1px solid ${theme.palette.divider}`
  },
  tooltipTitle: {
    fontWeight: 700,
    fontSize: '0.95rem',
    marginBottom: 6,
    color: theme.palette.text.primary
  },
  tooltipContent: {
    fontSize: '0.84rem',
    lineHeight: 1.55,
    color: theme.palette.text.primary
  },
  buttonBack: {
    color: theme.palette.text.secondary,
    marginRight: 10
  },
  buttonSkip: {
    color: theme.palette.text.secondary,
    marginRight: 10
  },
  buttonPrimary: {
    borderRadius: 9,
    fontSize: '0.84rem',
    fontWeight: 600
  }
});

const SiteTourContext = createContext(null);

/**
 * App-wide tour host. Mounts ONCE above the routes (first line of ThemedApp,
 * App.jsx — before PwaToasts) so it survives every route change; targets
 * mount and unmount on pages around it. Steps resolve per-page through the
 * before-hook navigation, never by re-hosting the tour per page.
 *
 * Public API (context value):
 *  - startTour()      — launch the full tour from the sidebar route copy
 *  - isActive         — whether the tour is currently running
 */
export const SiteTourProvider = ({ theme, children }) => {
  const navigate = useNavigate();

  // `run` toggles Joyride's engine on/off. The engine is uncontrolled —
  // stepIndex handled internally — and every `before` hook navigates with
  // React Router before its step renders.
  const [run, setRun] = useState(false);
  const runRef = useRef(false);

  const themeSource = useContext(ThemeContext);
  // App.jsx always passes the active theme; the context-mode fallback covers
  // standalone use (tests, other mounts) where no prop is given.
  const resolvedTheme = theme || buildAppTheme(themeSource?.mode || 'light');

  const startTour = () => {
    // Back to the tour's first screen so step 1's target exists wherever
    // the user launched from (e.g. replay from the Settings page).
    navigate('/dashboard');
    runRef.current = true;
    setRun(true);
  };

  const handleEvent = (eventData) => {
    const { status } = eventData;
    if (status === 'finished' || status === 'skipped') {
      runRef.current = false;
      setRun(false);
    }
  };

  // navigate is stable in react-router v6, so the steps array keeps a
  // stable identity across renders (the engine re-merges changing props).
  const steps = useMemo(() => buildSteps(navigate), [navigate]);

  const { Tour } = useJoyride({
    steps,
    run,
    continuous: true, // Next/Prev navigation across steps
    targetWaitTimeout: 2500,
    scrollDuration: 300,
    options: buildTourOptions(resolvedTheme),
    styles: buildTourStyles(resolvedTheme),
    onEvent: handleEvent
  });

  const value = {
    startTour,
    isActive: runRef.current,
    Tour
  };

  return (
    <SiteTourContext.Provider value={value}>
      {children}
      {value.Tour}
    </SiteTourContext.Provider>
  );
};

export const useSiteTour = () => useContext(SiteTourContext);
