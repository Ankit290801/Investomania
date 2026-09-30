import React, { createContext, useCallback, useContext, useEffect, useMemo, useRef, useState } from 'react';
import { buildAppTheme } from '../config/theme';
import { settingsService } from '../services/settingsService';
import { useAuth } from '../hooks/useAuth';

/**
 * F1/F5 bridge: resolves the user's theme preference
 * ('LIGHT' | 'DARK' | 'SYSTEM') to a concrete mode and exposes
 * { mode, preference, setPreference }.
 *
 * E11: ThemeContext is the SINGLE runtime source of truth. Every setter —
 * header toggle and Settings dropdown alike — updates this context and
 * write-through-syncs to backend UserSettings (fire-and-forget; failure
 * only means the preference won't roam to another device). Loading the
 * Settings page never overrides the live choice again: the backend value
 * only seeds a first-ever visit when no local preference exists yet.
 *
 * SYSTEM tracks the OS `prefers-color-scheme` live. The explicit choice is
 * mirrored to localStorage so the theme is applied before the first paint
 * on the next visit (and in login/register screens, where no backend
 * settings exist yet).
 */

const STORAGE_KEY = 'app.themePreference';

/** True when the user has expressed any explicit local preference. */
const hasStoredPreference = () => {
  try {
    return window.localStorage.getItem(STORAGE_KEY) != null;
  } catch {
    return false;
  }
};

const readStoredPreference = () => {
  try {
    const v = window.localStorage.getItem(STORAGE_KEY);
    return v === 'LIGHT' || v === 'DARK' || v === 'SYSTEM' ? v : 'SYSTEM';
  } catch {
    return 'SYSTEM';
  }
};

const systemPrefersDark = () =>
  typeof window !== 'undefined' &&
  window.matchMedia &&
  window.matchMedia('(prefers-color-scheme: dark)').matches;

const resolveMode = (preference) => {
  if (preference === 'LIGHT') return 'light';
  if (preference === 'DARK') return 'dark';
  return systemPrefersDark() ? 'dark' : 'light';
};

const ThemeContext = createContext(null);

export const ThemeProvider = ({ children }) => {
  const [preference, setPreference] = useState(() => {
    // Seed once from localStorage; absence = SYSTEM and the backend may
    // seed its stored choice on first Settings visit (E11.1.3).
    return readStoredPreference();
  });
  const [mode, setMode] = useState(() => resolveMode(readStoredPreference()));
  const { user } = useAuth();
  const authed = Boolean(user);
  const seededRef = useRef(false);

  useEffect(() => {
    setMode(resolveMode(preference));
    try {
      window.localStorage.setItem(STORAGE_KEY, preference);
    } catch {
      /* storage unavailable — preference stays in-memory */
    }
  }, [preference]);

  useEffect(() => {
    if (preference !== 'SYSTEM' || !window.matchMedia) return undefined;
    const mq = window.matchMedia('(prefers-color-scheme: dark)');
    const onChange = () => setMode(mq.matches ? 'dark' : 'light');
    mq.addEventListener('change', onChange);
    return () => mq.removeEventListener('change', onChange);
  }, [preference]);

  const setPreferenceSafe = useCallback((next) => {
    setPreference(next === 'LIGHT' || next === 'DARK' || next === 'SYSTEM' ? next : 'SYSTEM');
  }, []);

  /**
   * E11: the one public setter. Updates context (→ localStorage via the
   * effect above) and write-through-syncs to backend settings when signed
   * in. Fire-and-forget: a failed sync must never block or log out the UI.
   */
  const updatePreference = useCallback((next) => {
    setPreferenceSafe(next);
    if (!authed) return;
    settingsService.updateSettings({ theme: next }).catch(() => {
      /* preference stays local; retried on the next explicit change */
    });
  }, [authed, setPreferenceSafe]);

  // E11.1.3: first-authenticated load with no local preference — adopt the
  // backend's stored theme (if any) exactly once. Afterwards the local
  // choice always wins; Settings loads never override it again.
  useEffect(() => {
    if (!authed || seededRef.current || hasStoredPreference()) return;
    seededRef.current = true;
    let cancelled = false;
    settingsService.getSettings().then((s) => {
      if (!cancelled && s?.theme) setPreferenceSafe(s.theme);
    }).catch(() => { /* backend unreachable — keep SYSTEM */ });
    return () => { cancelled = true; };
  }, [authed, setPreferenceSafe]);

  const value = useMemo(
    () => ({ mode, preference, setPreference: updatePreference }),
    [mode, preference, updatePreference]
  );

  return <ThemeContext.Provider value={value}>{children}</ThemeContext.Provider>;
};

export const useThemePreference = () => {
  const ctx = useContext(ThemeContext);
  if (!ctx) {
    throw new Error('useThemePreference must be used within ThemeProvider');
  }
  return ctx;
};

export default ThemeContext;
