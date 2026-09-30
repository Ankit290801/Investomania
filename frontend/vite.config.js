import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'
import { VitePWA } from 'vite-plugin-pwa'

// E9 PWA — workbox strategies per tracker plan:
//  - precache: full app shell (hashed bundles + icons), auto rebuilt each build
//  - navigations: NetworkFirst → precached index.html (all SPA routes render offline)
//  - /api GET reads: NetworkFirst with per-class freshness (analytics 5 min,
//    lists 60 s, snapshots 24 h); /api/auth and mutations NEVER cached
//  - manifest hexes duplicate theme.js constants by necessity (static file) —
//    keep in sync when re-theming (sanctioned exception, colour-lint scans src/ only)
export default defineConfig({
  plugins: [
    react(),
    VitePWA({
      registerType: 'prompt',
      injectRegister: 'auto',
      // Plan: SW disabled in dev — test on `vite preview` / packaged build
      devOptions: { enabled: false },
      includeAssets: ['favicon-32.png', 'icons/apple-touch-icon.png'],
      manifest: {
        name: 'Investment Tracker',
        short_name: 'Investments',
        description: 'Personal finance console — portfolio, expenses and snapshots',
        lang: 'en',
        display: 'standalone',
        start_url: '/dashboard',
        scope: '/',
        theme_color: '#0F766E',
        background_color: '#0B1220',
        icons: [
          { src: '/icons/icon-192.png', sizes: '192x192', type: 'image/png' },
          { src: '/icons/icon-512.png', sizes: '512x512', type: 'image/png' },
          { src: '/icons/maskable-192.png', sizes: '192x192', type: 'image/png', purpose: 'maskable' },
          { src: '/icons/maskable-512.png', sizes: '512x512', type: 'image/png', purpose: 'maskable' }
        ]
      },
      workbox: {
        globPatterns: ['**/*.{js,css,html,png,svg,woff2}'],
        navigateFallback: '/index.html',
        // Never serve the SPA shell for these — API/console/tool endpoints
        navigateFallbackDenylist: [/^\/api\//, /^\/h2-console/, /^\/actuator/],
        runtimeCaching: [
          // --- analytics/aggregates (freshness 5 min) ---
          {
            urlPattern: ({ url, request }) =>
              request.method === 'GET' && url.pathname.startsWith('/api/portfolio/'),
            handler: 'NetworkFirst',
            options: {
              cacheName: 'api-analytics',
              networkTimeoutSeconds: 3,
              expiration: { maxEntries: 60, maxAgeSeconds: 300 },
              cacheableResponse: { statuses: [200] }
            }
          },
          // --- snapshots / FY history (freshness 24 h) ---
          {
            urlPattern: ({ url, request }) =>
              request.method === 'GET' && url.pathname.startsWith('/api/snapshots/'),
            handler: 'NetworkFirst',
            options: {
              cacheName: 'api-snapshots',
              networkTimeoutSeconds: 3,
              expiration: { maxEntries: 60, maxAgeSeconds: 86400 },
              cacheableResponse: { statuses: [200] }
            }
          },
          // --- entity lists (freshness 60 s) ---
          {
            urlPattern: ({ url, request }) =>
              request.method === 'GET' &&
              ['/api/investments', '/api/expenses', '/api/transactions', '/api/market-data', '/api/symbols', '/api/settings']
                .some((p) => url.pathname.startsWith(p)),
            handler: 'NetworkFirst',
            options: {
              cacheName: 'api-lists',
              networkTimeoutSeconds: 3,
              expiration: { maxEntries: 120, maxAgeSeconds: 60 },
              cacheableResponse: { statuses: [200] }
            }
          },
          // --- generic /api reads (NOT /api/auth) — safety net, 5 min ---
          {
            urlPattern: ({ url, request }) =>
              request.method === 'GET' &&
              url.pathname.startsWith('/api/') &&
              !url.pathname.startsWith('/api/auth/'),
            handler: 'NetworkFirst',
            options: {
              cacheName: 'api-misc',
              networkTimeoutSeconds: 3,
              expiration: { maxEntries: 100, maxAgeSeconds: 300 },
              cacheableResponse: { statuses: [200] }
            }
          }
        ]
      }
    })
  ],
  server: {
    port: 5173,
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      }
    }
  },
  preview: {
    port: 4173,
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      }
    }
  },
  build: {
    outDir: 'dist',
    sourcemap: false,
  }
})
