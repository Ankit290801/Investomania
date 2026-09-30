import React, { createContext, useCallback, useContext, useEffect, useState } from 'react';
import { useRegisterSW } from 'virtual:pwa-register/react';

/**
 * E9 P3 — PWA runtime integration.
 *
 *  - Registers the workbox service worker (vite-plugin-pwa, `prompt` mode):
 *    when a new version is precached and waiting, exposes `updateReady`;
 *    the update toast calls `updateServiceWorker(true)` on the user's
 *    explicit "Reload" choice (never auto-reloads mid-entry).
 *  - Tracks online/offline so the shell can show an OfflineBanner instead of
 *    failing requests blindly.
 *  - Captures `beforeinstallprompt` for the Settings install button
 *    (Chromium only; iOS installs via "Add to Home Screen" manually).
 */
const PwaContext = createContext(null);

export const PwaProvider = ({ children }) => {
  const [offline, setOffline] = useState(!navigator.onLine);
  const [installPrompt, setInstallPrompt] = useState(
    // Adopt a prompt captured before React mounted (index.html stashes
    // it — Chromium fires beforeinstallprompt very early in page load).
    () => window.__stashedInstallPrompt || null
  );
  const [installed, setInstalled] = useState(
    () => window.matchMedia('(display-mode: standalone)').matches
  );

  const {
    needRefresh: [updateReady],
    updateServiceWorker
  } = useRegisterSW({
    onRegisteredSW(url, registration) {
      // Check for updates on each load + hourly (prompt mode: user acts).
      if (registration) {
        setInterval(() => registration.update(), 60 * 60 * 1000);
      }
    }
  });

  const applyUpdate = useCallback(() => {
    updateServiceWorker(true);
  }, [updateServiceWorker]);

  useEffect(() => {
    const goOffline = () => setOffline(true);
    const goOnline = () => setOffline(false);
    window.addEventListener('offline', goOffline);
    window.addEventListener('online', goOnline);
    return () => {
      window.removeEventListener('offline', goOffline);
      window.removeEventListener('online', goOnline);
    };
  }, []);

  useEffect(() => {
    const onBeforeInstall = (event) => {
      event.preventDefault(); // take over from the mini-infobar
      setInstallPrompt(event);
    };
    const onInstalled = () => {
      setInstalled(true);
      setInstallPrompt(null);
    };
    window.addEventListener('beforeinstallprompt', onBeforeInstall);
    window.addEventListener('appinstalled', onInstalled);
    return () => {
      window.removeEventListener('beforeinstallprompt', onBeforeInstall);
      window.removeEventListener('appinstalled', onInstalled);
    };
  }, []);

  const promptInstall = useCallback(async () => {
    if (!installPrompt) return false;
    installPrompt.prompt();
    const { outcome } = await installPrompt.userChoice;
    setInstallPrompt(null);
    return outcome === 'accepted';
  }, [installPrompt]);

  const value = {
    offline,
    updateReady,
    applyUpdate,
    canInstall: Boolean(installPrompt) && !installed,
    installed,
    promptInstall
  };

  return <PwaContext.Provider value={value}>{children}</PwaContext.Provider>;
};

export const usePwa = () => {
  const ctx = useContext(PwaContext);
  if (!ctx) throw new Error('usePwa must be used within PwaProvider');
  return ctx;
};
