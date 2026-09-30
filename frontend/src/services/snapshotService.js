import api from './api';

/**
 * Client for the /api/snapshots endpoints.
 *
 * All paths are relative to the axios baseURL (which already ends in /api) —
 * an earlier version prefixed every path with '/api/' again, producing
 * /api/api/snapshots and 404s on every call (bug SNAP-1).
 */
const snapshotService = {

  /**
   * Generate historical FY-end snapshots for the current user.
   * Pass force=true to regenerate already-existing snapshots.
   */
  generateSnapshots: (force = false) =>
    api.post(`/snapshots/generate?force=${force}`).then(r => r.data),

  /**
   * Recalculate a snapshot for a specific date (YYYY-MM-DD).
   */
  recalculateSnapshot: (date) =>
    api.post(`/snapshots/recalculate?date=${date}`).then(r => r.data),

  /**
   * Fetch all snapshots for the current user (newest first).
   */
  getSnapshots: () =>
    api.get('/snapshots').then(r => r.data),

  /**
   * Get the missing-data report.
   */
  getMissingDataReport: () =>
    api.get('/snapshots/missing-data').then(r => r.data),

  /**
   * Delete the snapshot for a specific date (YYYY-MM-DD).
   */
  deleteSnapshot: (date) =>
    api.delete(`/snapshots?date=${date}`).then(r => r.data),
};

export default snapshotService;
