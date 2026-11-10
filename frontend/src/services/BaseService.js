import { api } from '../api/apiClient';
import { HttpError, AppError } from '../errors/AppErrors';

/**
 * Base Service class for API communication.
 * Provides abstraction over HTTP client, error mapping, and consistent request lifecycle.
 */
export class BaseService {
  constructor(baseEndpoint = '') {
    this.baseEndpoint = baseEndpoint;
    this.client = api;
  }

  async get(endpoint = '', options = {}) {
    try {
      return await this.client.get(`${this.baseEndpoint}${endpoint}`, {}, options);
    } catch (err) {
      throw this._normalizeError(err);
    }
  }

  async post(endpoint = '', body = {}, headers = {}) {
    try {
      return await this.client.post(`${this.baseEndpoint}${endpoint}`, body, headers);
    } catch (err) {
      throw this._normalizeError(err);
    }
  }

  async put(endpoint = '', body = {}, headers = {}) {
    try {
      return await this.client.put(`${this.baseEndpoint}${endpoint}`, body, headers);
    } catch (err) {
      throw this._normalizeError(err);
    }
  }

  async patch(endpoint = '', body = {}, headers = {}) {
    try {
      return await this.client.patch(`${this.baseEndpoint}${endpoint}`, body, headers);
    } catch (err) {
      throw this._normalizeError(err);
    }
  }

  async delete(endpoint = '', headers = {}) {
    try {
      return await this.client.delete(`${this.baseEndpoint}${endpoint}`, headers);
    } catch (err) {
      throw this._normalizeError(err);
    }
  }

  async upload(endpoint = '', formData, headers = {}) {
    try {
      return await this.client.uploadFile(`${this.baseEndpoint}${endpoint}`, formData, headers);
    } catch (err) {
      throw this._normalizeError(err);
    }
  }

  _normalizeError(err) {
    if (err instanceof AppError) return err;
    return new HttpError(
      err.message || 'An unexpected server error occurred',
      err.status || 500,
      err.data || null
    );
  }
}
