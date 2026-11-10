import { BaseService } from './BaseService';
import { Shop } from '../models/Shop';

/**
 * Service encapsulating campus Administrator operations: metrics, shop review, audits, disputes.
 */
export class AdminService extends BaseService {
  constructor() {
    super('/admin');
  }

  async getDashboardSummary() {
    return await this.get('/dashboard-summary');
  }

  async getPendingShops() {
    const rawData = await this.get('/shops/pending');
    if (!Array.isArray(rawData)) return [];
    return rawData.map((data) => new Shop(data));
  }

  async reviewShop(shopId, approved, reason = '') {
    return await this.post(`/shops/${shopId}/review`, { approved, reason });
  }

  async getAuditLogs() {
    return await this.get('/audit-logs');
  }

  async getDeletionRequests() {
    return await this.get('/deletion-requests');
  }

  async reviewDeletionRequest(requestId, status, note = '') {
    return await this.post(`/deletion-requests/${requestId}/review`, { status, note });
  }

  async getDisputes() {
    return await this.get('/disputes');
  }

  async resolveDispute(disputeId, status, note = '') {
    return await this.post(`/disputes/${disputeId}/resolve`, { status, note });
  }

  async getRefunds() {
    return await this.get('/refunds');
  }

  async processRefund(refundId, status) {
    return await this.post(`/refunds/${refundId}/process`, { status });
  }
}

export const adminService = new AdminService();
