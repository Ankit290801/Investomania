# Investment Tracker - Frontend

React-based frontend application for the Investment Tracker platform, built with Vite and Material-UI.

## Technology Stack

- **React 18.3.1** - UI library
- **Vite 5.4.21** - Build tool and dev server
- **Material-UI 5.18.0** - Component library
- **React Router 6.30.3** - Client-side routing
- **Axios 1.16.0** - HTTP client
- **React Hook Form 7.75.0** - Form management
- **Yup 1.7.1** - Form validation
- **Recharts 2.15.4** - Data visualization
- **date-fns 3.6.0** - Date utilities
- **pnpm 10.33.2** - Package manager

## Project Structure

```
frontend/
├── src/
│   ├── components/          # Reusable UI components
│   │   ├── auth/           # Authentication components (login, register)
│   │   ├── common/         # Common components (Loading, ErrorMessage)
│   │   ├── dashboard/      # Dashboard widgets and cards
│   │   ├── investments/    # Investment-related components
│   │   ├── expenses/       # Expense tracking components
│   │   └── tax/            # Tax calculation components
│   ├── pages/              # Page-level components
│   │   ├── DashboardPage.jsx
│   │   └── InvestmentsPage.jsx
│   ├── services/           # API service layer
│   │   ├── api.js         # Axios instance with interceptors
│   │   ├── authService.js # Authentication API calls
│   │   ├── investmentService.js
│   │   ├── expenseService.js
│   │   └── analyticsService.js
│   ├── hooks/              # Custom React hooks
│   ├── contexts/           # React Context providers
│   ├── utils/              # Utility functions
│   ├── config/             # Configuration constants
│   ├── App.jsx            # Main application component
│   └── main.jsx           # Application entry point
├── public/                 # Static assets
├── .env.development       # Development environment variables
├── .env.production        # Production environment variables
├── vite.config.js         # Vite configuration
├── package.json           # Dependencies and scripts
└── pnpm-lock.yaml         # Lock file for pnpm

```

## Environment Variables

### Development (.env.development)
```env
VITE_API_BASE_URL=http://localhost:8080/api
VITE_ENABLE_DEBUG=true
```

### Production (.env.production)
```env
VITE_API_BASE_URL=https://api.yourdomain.com/api
VITE_ENABLE_DEBUG=false
```

## Available Scripts

### Development
```bash
pnpm install        # Install dependencies
pnpm run dev        # Start dev server (http://localhost:5173)
pnpm run build      # Build for production
pnpm run preview    # Preview production build
pnpm run lint       # Run ESLint
```

## Development Setup

1. **Install Dependencies**
   ```bash
   pnpm install
   ```

2. **Start Development Server**
   ```bash
   pnpm run dev
   ```
   The application will be available at `http://localhost:5173`

3. **Ensure Backend is Running**
   The frontend expects the backend API to be running at `http://localhost:8080/api`

## API Integration

### Axios Configuration (`src/services/api.js`)

The Axios instance is pre-configured with:
- Base URL from environment variables
- JWT token interceptor (adds `Authorization: Bearer <token>`)
- Error interceptor (handles 401 unauthorized responses)
- Automatic redirect to login on authentication failure

### Usage Example
```javascript
import api from '../services/api';

// GET request
const response = await api.get('/investments');

// POST request
const response = await api.post('/investments', data);

// With authentication token (automatic)
localStorage.setItem('token', 'your-jwt-token');
const response = await api.get('/user/profile'); // Token added automatically
```

## Key Features

### CORS Configuration
- Configured to work with backend on `localhost:8080`
- Vite proxy forwards `/api/*` requests to backend
- CORS handled by Spring Boot backend

### Material-UI Theming
- Custom theme configuration in `App.jsx`
- Primary color: #1976d2 (blue)
- Secondary color: #dc004e (pink)
- Responsive design with Material-UI breakpoints

### Routing
- React Router v6 for client-side routing
- Protected routes (authentication required)
- Route-based code splitting (lazy loading)

## Component Guidelines

### Common Components
- **Loading** - Centered loading spinner
- **ErrorMessage** - Styled error alert with title
- **DashboardCard** - Reusable card for dashboard metrics

### Page Structure
Pages are located in `src/pages/` and follow this structure:
```javascript
import React, { useState, useEffect } from 'react';
import { Container, Box, Typography } from '@mui/material';

const ExamplePage = () => {
  // Page logic here
  return (
    <Container maxWidth="lg">
      <Box sx={{ py: 4 }}>
        <Typography variant="h4">Page Title</Typography>
        {/* Page content */}
      </Box>
    </Container>
  );
};

export default ExamplePage;
```

## Build for Production

```bash
pnpm run build
```

Output will be in the `dist/` directory. The build is optimized with:
- Code splitting
- Tree shaking
- Minification
- Asset optimization

Preview the production build:
```bash
pnpm run preview
```

## Troubleshooting

### Port Already in Use
If port 5173 is already in use, Vite will automatically try the next available port (5174, 5175, etc.)

### Backend Connection Issues
1. Verify backend is running on port 8080
2. Check CORS configuration in backend `CorsConfig.java`
3. Verify proxy configuration in `vite.config.js`

### Module Not Found Errors
```bash
# Remove node_modules and reinstall
rm -rf node_modules pnpm-lock.yaml
pnpm install
```

## Next Steps (Phase 2)

- [ ] Implement authentication pages (Login, Register)
- [ ] Create auth context with JWT management
- [ ] Implement protected routes
- [ ] Add authentication forms with validation
- [ ] Connect to backend auth endpoints

## Development Notes

- Using pnpm for faster, more efficient package management
- Vite provides fast HMR (Hot Module Replacement)
- Material-UI provides consistent, accessible components
- All API calls go through the centralized `api.js` service
- Environment variables must be prefixed with `VITE_` to be exposed
