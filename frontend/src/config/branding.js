/**
 * E6 — single owner of product strings used by the shared footer (and
 * anywhere else that needs them). Editing here updates every screen at
 * once; screens must not inline these strings.
 */
export const APP_NAME = 'Investment Tracker';
export const APP_TAGLINE = 'Personal finance console';
export const APP_VERSION = '1.1.0';
export const BRAND_YEAR = new Date().getFullYear();
