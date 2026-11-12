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
    return await this.get('/summary');
  }

  async getAllShops() {
    const rawData = await this.get('/shops');
    if (!Array.isArray(rawData)) return [];
    return rawData.map((data) => new Shop(data));
  }

  async reviewShopStatus(shopId, status) {
    const rawData = await this.patch(`/shops/${shopId}/status`, { status });
    return new Shop(rawData);
  }

  async deactivateShop(shopId) {
    return await this.post(`/shops/${shopId}/deactivate`, {});
  }

  async getDeliveryPartners() {
    return await this.get('/delivery-partners');
  }

  async reviewDeliveryPartnerStatus(partnerId, status) {
    return await this.patch(`/delivery-partners/${partnerId}/status`, { status });
  }

  async getAuditLogs() {
    return await this.get('/audit-logs');
  }

  async getDeletionRequests() {
    return await this.get('/deletion-requests');
  }

  async reviewDeletionRequest(requestId, confirm, note = '') {
    return await this.post(`/deletion-requests/${requestId}/review`, { confirm, note });
  }

  async getDisputes() {
    return await this.client.get('/disputes');
  }

  async resolveDispute(disputeId, resolve, note = '') {
    return await this.client.post(`/disputes/${disputeId}/resolve`, { resolve, note });
  }

  async getRefunds() {
    return await this.client.get('/payments/refunds');
  }

  async reviewRefund(refundId, approve, note = '') {
    return await this.client.post(`/payments/refund/${refundId}/review`, { approve, note });
  }

  async getAdmins() {
    return await this.get('/admins');
  }

  async reviewAdminStatus(adminId, status, adminKey = null) {
    return await this.patch(`/admins/${adminId}/status`, { status, adminKey });
  }

  async getAdminRoleRequests() {
    return await this.get('/admin-requests');
  }

  async reviewAdminRoleRequest(requestId, approve, note = '') {
    return await this.post(`/admin-requests/${requestId}/review`, { approve, note });
  }
}

export const adminService = new AdminService();
