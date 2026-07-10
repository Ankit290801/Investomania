import api from './api';

/**
 * Client for the /api/snapshots endpoints.
 */
const snapshotService = {

  /**
   * Generate historical FY-end snapshots for the current user.
   * Pass force=true to regenerate already-existing snapshots.
   */
  generateSnapshots: (force = false) =>
    api.post(`/api/snapshots/generate?force=${force}`).then(r => r.data),

  /**
   * Recalculate a snapshot for a specific date (YYYY-MM-DD).
   */
  recalculateSnapshot: (date) =>
    api.post(`/api/snapshots/recalculate?date=${date}`).then(r => r.data),

  /**
   * Fetch all snapshots for the current user (newest first).
   */
  getSnapshots: () =>
    api.get('/api/snapshots').then(r => r.data),

  /**
   * Get the missing-data report.
   */
  getMissingDataReport: () =>
    api.get('/api/snapshots/missing-data').then(r => r.data),

  /**
   * Delete the snapshot for a specific date (YYYY-MM-DD).
   */
  deleteSnapshot: (date) =>
    api.delete(`/api/snapshots?date=${date}`).then(r => r.data),
};

export default snapshotService;
