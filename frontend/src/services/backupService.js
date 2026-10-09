import api from './api';

/**
 * Phase 9 Task 8.4 — full-data JSON backup/restore client.
 */
export const backupService = {
  /**
   * Download the signed-in user's full data as a versioned JSON backup.
   * Triggers a browser download of investment-backup-YYYY-MM-DD.json.
   */
  async exportBackup() {
    const response = await api.get('/backup/export', { responseType: 'blob' });
    const blob = new Blob([response.data], { type: 'application/json' });
    const disposition = response.headers['content-disposition'] || '';
    const match = disposition.match(/filename="?([^"]+)"?/);
    const filename = match ? match[1] : 'investment-backup.json';

    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = filename;
    document.body.appendChild(a);
    a.click();
    a.remove();
    URL.revokeObjectURL(url);
    return filename;
  },

  /**
   * Restore the user's data from a previously exported backup file.
   * @param {File} file - the backup JSON file
   * @returns {Promise} { message, restored: { investments, transactions, ... } }
   */
  async restoreBackup(file) {
    const formData = new FormData();
    formData.append('file', file);
    const response = await api.post('/backup/restore', formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
    });
    return response.data;
  },
};
