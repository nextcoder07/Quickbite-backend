import { BaseService } from './BaseService';

/**
 * Service encapsulating campus Delivery Courier operations.
 */
export class DeliveryService extends BaseService {
  constructor() {
    super('/delivery');
  }

  async getAvailableTasks() {
    return await this.get('/available-orders');
  }

  async getMyActiveDelivery() {
    return await this.get('/my-active-delivery');
  }

  async acceptDelivery(orderId) {
    return await this.post(`/orders/${orderId}/accept`);
  }

  async updateDeliveryStatus(orderId, status) {
    return await this.patch(`/orders/${orderId}/status`, { status });
  }

  async getDeliveryHistory() {
    return await this.get('/history');
  }
}

export const deliveryService = new DeliveryService();
