import api from './api';

/**
 * Service for broker contract note (CNOTE) PDF import (Phase 9).
 */
export const contractNoteService = {
  /**
   * Dry-run: detect broker + parse trades + resolve portfolio actions.
   * Nothing is persisted.
   * @param {File} file - contract note PDF
   * @returns {Promise} ContractNoteImportReportDTO
   */
  async preview(file) {
    const formData = new FormData();
    formData.append('file', file);
    const response = await api.post('/contract-notes/import/preview', formData, {
      headers: { 'Content-Type': 'multipart/form-data' }
    });
    return response.data;
  },

  /**
   * Commit: apply all trades (holdings, avg prices, settlement cash legs).
   * @param {File} file - contract note PDF
   * @returns {Promise} ContractNoteImportReportDTO
   */
  async commit(file) {
    const formData = new FormData();
    formData.append('file', file);
    const response = await api.post('/contract-notes/import/commit', formData, {
      headers: { 'Content-Type': 'multipart/form-data' }
    });
    return response.data;
  }
};
