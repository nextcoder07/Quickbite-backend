import { BaseService } from './BaseService';

/**
 * Service encapsulating campus Delivery Courier operations.
 * Implements delivery task claiming, pickups, deliveries, and failure reporting.
 */
export class DeliveryService extends BaseService {
  constructor() {
    super('/delivery');
  }

  async getAvailableDeliveries() {
    return await this.get('/available');
  }

  async getMyDeliveries() {
    return await this.get('/my');
  }

  async claimAssignment(assignmentId) {
    return await this.post(`/${assignmentId}/claim`);
  }

  async markPickedUp(assignmentId) {
    return await this.post(`/${assignmentId}/pickup`);
  }

  async markDelivered(assignmentId) {
    return await this.post(`/${assignmentId}/deliver`);
  }

  async reportFailure(assignmentId, reason = 'Delivery failure') {
    return await this.post(`/${assignmentId}/fail`, { reason });
  }

  async submitDeletionRequest(reason) {
    return await this.client.post('/account/deletion-request', { reason });
  }

  async getMyDeletionRequests() {
    return await this.client.get('/account/deletion-request');
  }
}

export const deliveryService = new DeliveryService();
