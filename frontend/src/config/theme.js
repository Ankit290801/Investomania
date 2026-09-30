import { alpha, createTheme } from '@mui/material/styles';

/**
 * F1 Modern UI Revamp — central design system.
 *
 * Two design intents live side by side (backlog F1 goal):
 *  - Youth / modern: flat surfaces, rounded corners, vibrant accents,
 *    no heavy shadows, clean sans-serif.
 *  - Seasoned finance: dense information layout, tabular numerals so
 *    money columns align, restrained chrome, high-contrast dark mode.
 *
 * Everything reads from here — screens must never hardcode colours;
 * they consume theme tokens or the exported constants below.
 */

// System stack — prefers Inter when available. No bundled webfont, so the
// app ships with zero font payload. Numeric UI gets `fontVariantNumeric:
// 'tabular-nums'` (below), which every listed family supports.
const FONT_STACK =
  '"Inter", "Segoe UI Variable", "Segoe UI", system-ui, -apple-system, "Roboto", "Helvetica Neue", Arial, sans-serif';

/** Categorical palette for multi-series charts (F4 charts consume this). */
export const CHART_COLORS = [
  '#0F766E', // teal
  '#4F46E5', // indigo
  '#D97706', // amber
  '#DC2626', // red
  '#0284C7', // sky
  '#7C3AED', // violet
  '#059669', // emerald
  '#DB2777' // pink
];

/** Brand gradient used for the logo mark and accents. */
export const BRAND_GRADIENT = 'linear-gradient(135deg, #0F766E 0%, #4F46E5 100%)';

/** Consistent semantic colours for gains/losses across light and dark. */
export const semantic = {
  positive: { light: '#15803D', dark: '#4ADE80' },
  negative: { light: '#B91C1C', dark: '#F87171' }
};

const lightPalette = {
  mode: 'light',
  primary: { main: '#0F766E', light: '#14B8A6', dark: '#115E59', contrastText: '#FFFFFF' },
  secondary: { main: '#4F46E5', light: '#818CF8', dark: '#3730A3', contrastText: '#FFFFFF' },
  success: { main: '#15803D' },
  warning: { main: '#B45309' },
  error: { main: '#B91C1C' },
  info: { main: '#0369A1' },
  background: { default: '#F4F6F8', paper: '#FFFFFF' },
  divider: '#E2E8F0',
  text: { primary: '#0F172A', secondary: '#5B6B7C' }
};

const darkPalette = {
  mode: 'dark',
  primary: { main: '#2DD4BF', light: '#5EEAD4', dark: '#14B8A6', contrastText: '#06281F' },
  secondary: { main: '#818CF8', light: '#A5B4FC', dark: '#6366F1', contrastText: '#0B1023' },
  success: { main: '#4ADE80' },
  warning: { main: '#FBBF24' },
  error: { main: '#F87171' },
  info: { main: '#38BDF8' },
  background: { default: '#0B1220', paper: '#111A2B' },
  divider: 'rgba(148, 163, 184, 0.16)',
  text: { primary: '#E6EDF3', secondary: '#8FA1B3' }
};

const buildComponents = (palette) => {
  const dark = palette.mode === 'dark';
  return {
    MuiCssBaseline: {
      styleOverrides: {
        body: {
          backgroundColor: palette.background.default,
          // Smooth theme switch without janky white flash
          transition: 'background-color 0.25s ease'
        },
        '::selection': { backgroundColor: alpha(palette.primary.main, 0.24) }
      }
    },
    MuiPaper: {
      styleOverrides: {
        root: {
          backgroundImage: 'none' // kill MUI dark-mode elevation tint; we are flat
        },
        elevation1: {
          border: `1px solid ${palette.divider}`,
          boxShadow: 'none'
        }
      }
    },
    MuiCard: {
      defaultProps: { elevation: 0 },
      styleOverrides: {
        root: {
          backgroundImage: 'none',
          border: `1px solid ${palette.divider}`,
          borderRadius: 14,
          boxShadow: 'none'
        }
      }
    },
    MuiButton: {
      defaultProps: { disableElevation: true },
      styleOverrides: {
        root: {
          textTransform: 'none',
          fontWeight: 600,
          borderRadius: 9,
          paddingInline: 16
        },
        sizeSmall: { paddingInline: 12 }
      }
    },
    MuiToggleButton: {
      styleOverrides: {
        root: {
          textTransform: 'none',
          fontWeight: 500,
          borderRadius: 8,
          paddingInline: 10,
          paddingBlock: 3,
          fontSize: '0.75rem',
          '&.Mui-selected': {
            backgroundColor: alpha(palette.primary.main, dark ? 0.22 : 0.1),
            color: palette.primary.main
          }
        }
      }
    },
    MuiChip: {
      styleOverrides: {
        root: { fontWeight: 500, borderRadius: 7 },
        sizeSmall: { height: 24 }
      }
    },
    MuiTableCell: {
      styleOverrides: {
        root: { borderColor: palette.divider, padding: '10px 14px' },
        head: {
          fontWeight: 600,
          fontSize: '0.7rem',
          letterSpacing: '0.06em',
          textTransform: 'uppercase',
          color: palette.text.secondary,
          backgroundColor: dark ? alpha('#FFFFFF', 0.03) : alpha('#0F172A', 0.03),
          whiteSpace: 'nowrap'
        }
      }
    },
    MuiTableRow: {
      styleOverrides: {
        root: { '&:last-child td, &:last-child th': { borderBottom: 0 } },
        hover: { backgroundColor: dark ? alpha('#FFFFFF', 0.04) : alpha('#0F172A', 0.03) }
      }
    },
    MuiOutlinedInput: {
      styleOverrides: { root: { borderRadius: 9 } }
    },
    MuiInputLabel: {
      styleOverrides: { root: { fontSize: '0.875rem' } }
    },
    MuiDialog: {
      styleOverrides: {
        paper: {
          borderRadius: 16,
          backgroundImage: 'none',
          border: `1px solid ${palette.divider}`
        }
      }
    },
    MuiDialogTitle: {
      styleOverrides: { root: { fontWeight: 700, fontSize: '1.05rem', paddingBottom: 6 } }
    },
    MuiAppBar: {
      defaultProps: { elevation: 0, enableColorOnDark: true },
      styleOverrides: {
        root: {
          backgroundColor: palette.background.paper,
          color: palette.text.primary,
          borderBottom: `1px solid ${palette.divider}`
        }
      }
    },
    MuiDrawer: {
      styleOverrides: {
        paper: {
          backgroundColor: dark ? '#0D1626' : '#FFFFFF',
          borderRight: `1px solid ${palette.divider}`,
          backgroundImage: 'none'
        }
      }
    },
    MuiListItemButton: {
      styleOverrides: {
        root: {
          borderRadius: 10,
          marginInline: 8,
          paddingBlock: 7,
          '&.Mui-selected': {
            backgroundColor: alpha(palette.primary.main, dark ? 0.18 : 0.1),
            color: palette.primary.main,
            '& .MuiListItemIcon-root': { color: palette.primary.main },
            '&:hover': { backgroundColor: alpha(palette.primary.main, dark ? 0.24 : 0.14) }
          }
        }
      }
    },
    MuiListItemIcon: {
      styleOverrides: { root: { minWidth: 38 } }
    },
    MuiListItemText: {
      styleOverrides: {
        primary: { fontSize: '0.875rem', fontWeight: 500 }
      }
    },
    MuiTooltip: {
      styleOverrides: {
        tooltip: {
          backgroundColor: dark ? '#1E293B' : '#0F172A',
          fontSize: '0.72rem',
          borderRadius: 7,
          padding: '6px 10px'
        }
      }
    },
    MuiAlert: {
      styleOverrides: { root: { borderRadius: 10, fontSize: '0.875rem' } }
    },
    MuiLinearProgress: {
      styleOverrides: { root: { borderRadius: 6 } }
    },
    MuiSnackbarContent: {
      styleOverrides: { root: { borderRadius: 10 } }
    },
    MuiDivider: {
      styleOverrides: { root: { borderColor: palette.divider } }
    },
    MuiSkeleton: {
      defaultProps: { animation: 'wave' }
    }
  };
};

const buildTypography = () => ({
  fontFamily: FONT_STACK,
  h1: { fontWeight: 700, letterSpacing: '-0.02em' },
  h2: { fontWeight: 700, letterSpacing: '-0.02em' },
  h3: { fontWeight: 700, letterSpacing: '-0.01em' },
  h4: { fontWeight: 700, letterSpacing: '-0.01em', fontSize: '1.75rem' },
  h5: { fontWeight: 600 },
  h6: { fontWeight: 600, fontSize: '1rem' },
  subtitle1: { fontWeight: 600, fontSize: '0.95rem' },
  subtitle2: { fontWeight: 600, fontSize: '0.8125rem' },
  body1: { fontSize: '0.9rem', lineHeight: 1.55 },
  body2: { fontSize: '0.8125rem', lineHeight: 1.5 },
  button: { textTransform: 'none', fontWeight: 600 },
  caption: { fontSize: '0.72rem', lineHeight: 1.4 },
  overline: {
    fontSize: '0.68rem',
    fontWeight: 700,
    letterSpacing: '0.09em',
    textTransform: 'uppercase'
  },
  // Money-safe numerals: identical digit widths keep ₹-columns aligned.
  numeric: {
    fontFamily: FONT_STACK,
    fontVariantNumeric: 'tabular-nums',
    letterSpacing: 0
  }
});

/**
 * Build the app theme for the requested mode ('light' | 'dark').
 * MUST go through createTheme — a plain object lacks the generated
 * fields (palette.common, transitions, breakpoints, augmentColor) and
 * crashes CssBaseline at runtime.
 */
export const buildAppTheme = (mode = 'light') => {
  const palette = mode === 'dark' ? darkPalette : lightPalette;
  return createTheme({
    palette,
    typography: buildTypography(),
    shape: { borderRadius: 10 },
    components: buildComponents(palette)
  });
};

/** Helper for screen-level semantic (gain/loss) colours from the active theme. */
export const semanticColor = (theme, positive) => {
  const key = positive ? 'positive' : 'negative';
  return semantic[key][theme.palette.mode];
};
