import api from './api';

/**
 * Service for bank statement PDF import (backlog feature F6).
 */
export const statementImportService = {
  /**
   * Dry-run: detect bank + parse + validate. Nothing is persisted.
   * @param {File} file - PDF statement
   * @param {number|null} cashAccountId - override auto account matching
   * @returns {Promise} StatementImportReportDTO
   */
  async preview(file, cashAccountId = null) {
    const formData = new FormData();
    formData.append('file', file);
    const url = cashAccountId
      ? `/statements/import/preview?cashAccountId=${cashAccountId}`
      : '/statements/import/preview';
    const response = await api.post(url, formData, {
      headers: { 'Content-Type': 'multipart/form-data' }
    });
    return response.data;
  },

  /**
   * Commit: persist new transactions, replay the balance, refresh snapshots.
   * @param {File} file - PDF statement
   * @param {number|null} cashAccountId - override auto account matching
   * @returns {Promise} StatementImportReportDTO
   */
  async commit(file, cashAccountId = null) {
    const formData = new FormData();
    formData.append('file', file);
    const url = cashAccountId
      ? `/statements/import/commit?cashAccountId=${cashAccountId}`
      : '/statements/import/commit';
    const response = await api.post(url, formData, {
      headers: { 'Content-Type': 'multipart/form-data' }
    });
    return response.data;
  }
};
