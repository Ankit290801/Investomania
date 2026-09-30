import React, { useEffect, useState } from 'react';
import { Outlet, useNavigate, useLocation } from 'react-router-dom';
import {
  Box,
  Container,
  Drawer,
  AppBar,
  Toolbar,
  List,
  Typography,
  Divider,
  IconButton,
  Tooltip,
  ListItem,
  ListItemButton,
  ListItemIcon,
  ListItemText,
  Avatar,
  Menu,
  MenuItem,
  useTheme,
  useMediaQuery
} from '@mui/material';
import {
  Menu as MenuIcon,
  ChevronLeft as ChevronLeftIcon,
  Dashboard as DashboardIcon,
  TrendingUp as InvestmentsIcon,
  AccountBalance as TransactionsIcon,
  Receipt as ExpensesIcon,
  Assessment as TaxIcon,
  Settings as SettingsIcon,
  Logout as LogoutIcon,
  History as HistoryIcon,
  DarkMode as DarkModeIcon,
  LightMode as LightModeIcon
} from '@mui/icons-material';
import { useAuth } from '../../hooks/useAuth';
import { useThemePreference } from '../../contexts/ThemeContext';
import { BRAND_GRADIENT } from '../../config/theme';
import { APP_NAME, APP_TAGLINE } from '../../config/branding';
import AppFooter from '../common/AppFooter';

const DRAWER_WIDTH = 232;
const DRAWER_COLLAPSED = 68;

// F1 brand mark: gradient square with monogram. Scales down with the rail.
const BrandMark = ({ collapsed }) => (
  <Box
    sx={{
      width: 34,
      height: 34,
      borderRadius: '10px',
      background: BRAND_GRADIENT,
      display: 'flex',
      alignItems: 'center',
      justifyContent: 'center',
      flexShrink: 0
    }}
  >
    <Typography sx={{ color: '#fff', fontWeight: 800, fontSize: collapsed ? '0.8rem' : '0.95rem' }}>
      IT
    </Typography>
  </Box>
);

const navigationItems = [
  { text: 'Dashboard', icon: <DashboardIcon />, path: '/dashboard' },
  { text: 'Investments', icon: <InvestmentsIcon />, path: '/investments' },
  { text: 'Transactions', icon: <TransactionsIcon />, path: '/transactions' },
  { text: 'Expenses', icon: <ExpensesIcon />, path: '/expenses' },
  { text: 'Tax Reports', icon: <TaxIcon />, path: '/tax-reports' },
  { text: 'Snapshots', icon: <HistoryIcon />, path: '/snapshots' },
  { text: 'Settings', icon: <SettingsIcon />, path: '/settings' }
];

const DashboardLayout = () => {
  const [mobileOpen, setMobileOpen] = useState(false);
  const [desktopOpen, setDesktopOpen] = useState(() => {
    try {
      return window.localStorage.getItem('app.sidebarOpen') !== 'false';
    } catch {
      return true;
    }
  });
  const [anchorEl, setAnchorEl] = useState(null);
  const { user, logout } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const theme = useTheme();
  const isMobile = useMediaQuery(theme.breakpoints.down('md'));
  const { mode, setPreference } = useThemePreference();

  useEffect(() => {
    try {
      window.localStorage.setItem('app.sidebarOpen', String(desktopOpen));
    } catch {
      /* non-fatal */
    }
  }, [desktopOpen]);

  const handleDrawerToggle = () => {
    if (isMobile) {
      setMobileOpen(!mobileOpen);
    } else {
      setDesktopOpen(!desktopOpen);
    }
  };

  const handleMenuOpen = (event) => {
    setAnchorEl(event.currentTarget);
  };

  const handleMenuClose = () => {
    setAnchorEl(null);
  };

  const handleLogout = async () => {
    handleMenuClose();
    await logout();
    navigate('/login');
  };

  const handleNavigation = (path) => {
    navigate(path);
    if (isMobile) {
      setMobileOpen(false);
    }
  };

  // E2: sub-screens (/investments/equity, ...) keep the parent's header title
  // and sidebar highlight via prefix match.
  const activeItem = navigationItems.find(
    (item) => location.pathname === item.path || location.pathname.startsWith(`${item.path}/`)
  );

  const drawer = (collapsed) => (
    <Box sx={{ display: 'flex', flexDirection: 'column', height: '100%' }}>
      <Toolbar
        sx={{
          display: 'flex',
          alignItems: 'center',
          gap: 1.25,
          px: collapsed ? 1.5 : 2,
          justifyContent: collapsed ? 'center' : 'flex-start'
        }}
      >
        <BrandMark collapsed={collapsed} />
        {!collapsed && (
          <Box sx={{ minWidth: 0 }}>
            <Typography noWrap sx={{ fontWeight: 700, fontSize: '0.95rem', lineHeight: 1.2 }}>
              {APP_NAME}
            </Typography>
            <Typography variant="caption" color="text.secondary" sx={{ display: 'block' }}>
              {APP_TAGLINE}
            </Typography>
          </Box>
        )}
      </Toolbar>
      <Divider sx={{ mx: 1.5 }} />
      <List sx={{ flexGrow: 1, pt: 1 }}>
        {navigationItems.map((item) => {
          const selected =
            location.pathname === item.path || location.pathname.startsWith(`${item.path}/`);
          const button = (
            <ListItemButton
              selected={selected}
              onClick={() => handleNavigation(item.path)}
              sx={{
                minHeight: 40,
                justifyContent: collapsed ? 'center' : 'flex-start',
                px: collapsed ? 0 : 2
              }}
            >
              <ListItemIcon sx={{ minWidth: collapsed ? 0 : 38, justifyContent: 'center' }}>
                {item.icon}
              </ListItemIcon>
              {!collapsed && <ListItemText primary={item.text} />}
            </ListItemButton>
          );
          return (
            <ListItem key={item.text} disablePadding sx={{ display: 'block' }}>
              {collapsed ? (
                <Tooltip title={item.text} placement="right" arrow>
                  {button}
                </Tooltip>
              ) : (
                button
              )}
            </ListItem>
          );
        })}
      </List>
      {/* Rail footer keeps the collapsed drawer visually anchored */}
      {!collapsed && (
        <Box sx={{ px: 2, pb: 1.5 }}>
          <Typography variant="caption" color="text.secondary">
            {user?.username ? `Signed in as ${user.username}` : ''}
          </Typography>
        </Box>
      )}
    </Box>
  );

  const drawerWidth = desktopOpen ? DRAWER_WIDTH : DRAWER_COLLAPSED;

  return (
    <Box sx={{ display: 'flex', minHeight: '100vh' }}>
      {/* App Bar */}
      <AppBar
        position="fixed"
        sx={{
          width: { xs: '100%', md: `calc(100% - ${drawerWidth}px)` },
          ml: { xs: 0, md: `${drawerWidth}px` },
          transition: theme.transitions.create(['width', 'margin'], {
            easing: theme.transitions.easing.sharp,
            duration: theme.transitions.duration.leavingScreen
          })
        }}
      >
        <Toolbar sx={{ gap: 1 }}>
          <IconButton
            color="inherit"
            aria-label="toggle navigation"
            edge="start"
            onClick={handleDrawerToggle}
            sx={{ mr: 0.5 }}
          >
            {isMobile || !desktopOpen ? <MenuIcon /> : <ChevronLeftIcon />}
          </IconButton>
          <Typography variant="subtitle1" noWrap component="div" sx={{ flexGrow: 1, fontWeight: 700 }}>
            {activeItem?.text || 'Dashboard'}
          </Typography>
          <Tooltip title={mode === 'dark' ? 'Switch to light mode' : 'Switch to dark mode'}>
            <IconButton
              color="inherit"
              aria-label="toggle color scheme"
              onClick={() => setPreference(mode === 'dark' ? 'LIGHT' : 'DARK')}
              size="small"
              sx={{ mr: 0.5 }}
            >
              {mode === 'dark' ? <LightModeIcon /> : <DarkModeIcon />}
            </IconButton>
          </Tooltip>
          <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
            <Typography variant="body2" sx={{ display: { xs: 'none', sm: 'block' }, fontWeight: 500 }}>
              {user?.username}
            </Typography>
            <IconButton color="inherit" onClick={handleMenuOpen} size="small" sx={{ p: 0.5 }}>
              <Avatar
                sx={{
                  width: 32,
                  height: 32,
                  background: BRAND_GRADIENT,
                  fontSize: '0.85rem',
                  fontWeight: 700
                }}
              >
                {user?.username?.charAt(0).toUpperCase() || 'U'}
              </Avatar>
            </IconButton>
          </Box>
        </Toolbar>
      </AppBar>

      {/* User Menu */}
      <Menu
        anchorEl={anchorEl}
        open={Boolean(anchorEl)}
        onClose={handleMenuClose}
        anchorOrigin={{ vertical: 'bottom', horizontal: 'right' }}
        transformOrigin={{ vertical: 'top', horizontal: 'right' }}
      >
        <Box sx={{ px: 2, py: 1 }}>
          <Typography variant="subtitle2">{user?.email}</Typography>
          <Typography variant="caption" color="text.secondary">
            Role: {user?.role}
          </Typography>
        </Box>
        <Divider />
        <MenuItem
          onClick={() => {
            handleMenuClose();
            navigate('/settings');
          }}
        >
          <ListItemIcon>
            <SettingsIcon fontSize="small" />
          </ListItemIcon>
          Settings
        </MenuItem>
        <MenuItem onClick={handleLogout}>
          <ListItemIcon>
            <LogoutIcon fontSize="small" />
          </ListItemIcon>
          Logout
        </MenuItem>
      </Menu>

      {/* Drawer - Mobile */}
      <Drawer
        variant="temporary"
        open={mobileOpen}
        onClose={handleDrawerToggle}
        ModalProps={{ keepMounted: true }}
        sx={{
          display: { xs: 'block', md: 'none' },
          '& .MuiDrawer-paper': { boxSizing: 'border-box', width: DRAWER_WIDTH }
        }}
      >
        {drawer(false)}
      </Drawer>

      {/* Drawer - Desktop (collapsible rail) */}
      <Drawer
        variant="permanent"
        open={desktopOpen}
        sx={{
          display: { xs: 'none', md: 'block' },
          width: drawerWidth,
          flexShrink: 0,
          '& .MuiDrawer-paper': {
            boxSizing: 'border-box',
            width: drawerWidth,
            overflowX: 'hidden',
            transition: theme.transitions.create('width', {
              easing: theme.transitions.easing.sharp,
              duration: theme.transitions.duration.enteringScreen
            })
          }
        }}
      >
        {drawer(!desktopOpen)}
      </Drawer>

      {/* Main Content — E3: the permanent drawer's root already reserves its
          width in the flex row, so the content must NOT also carry
          ml: drawerWidth (that double-counted the sidebar, shrinking the
          content area by the drawer width and pinning it to the right).
          Only the fixed AppBar needs the explicit ml/width, because fixed
          positioning escapes the flex flow. */}
      <Box
        component="main"
        sx={{
          flexGrow: 1,
          minWidth: 0,
          width: { xs: '100%', md: `calc(100% - ${drawerWidth}px)` },
          minHeight: '100vh',
          backgroundColor: 'background.default',
          transition: theme.transitions.create('width', {
            easing: theme.transitions.easing.sharp,
            duration: theme.transitions.duration.enteringScreen
          })
        }}
      >
        <Toolbar />
        <Container
          maxWidth="xl"
          sx={{
            // E6: flex column so the shared footer sits at the bottom of the
            // content area on short pages (mt:auto) and flows naturally on
            // tall ones. One mount point = uniform footer on every screen.
            display: 'flex',
            flexDirection: 'column',
            minHeight: 'calc(100vh - 64px)',
            py: { xs: 2, md: 3 }
          }}
        >
          <Box sx={{ flexGrow: 1 }}>
            <Outlet />
          </Box>
          <AppFooter />
        </Container>
      </Box>
    </Box>
  );
};

export default DashboardLayout;
