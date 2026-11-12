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
    return await this.get('/available', { skipCache: true });
  }

  async getMyDeliveries() {
    return await this.get('/my', { skipCache: true });
  }

  async claimAssignment(assignmentId) {
    return await this.post(`/${assignmentId}/claim`);
  }

  async markPickedUp(assignmentId) {
    return await this.post(`/${assignmentId}/pickup`);
  }

  async markDelivered(assignmentId, otp) {
    return await this.post(`/${assignmentId}/deliver`, { otp });
  }

  async reportFailure(assignmentId, reason = 'Delivery failure') {
    return await this.post(`/${assignmentId}/fail`, { reason });
  }

  async submitDeletionRequest(reason) {
    return await this.client.post('/account/deletion-request', { reason });
  }

  async getMyDeletionRequests(options = {}) {
    return await this.client.get('/account/deletion-request', {}, options);
  }
}

export const deliveryService = new DeliveryService();
