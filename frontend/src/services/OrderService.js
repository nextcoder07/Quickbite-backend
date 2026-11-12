import { BaseService } from './BaseService';
import { Order } from '../models/Order';

/**
 * Service encapsulating customer Order and Checkout operations.
 * Handles checkout dispatch, order lookup, cancellations, and feedback submission.
 */
export class OrderService extends BaseService {
  constructor() {
    super('/orders');
  }

  /**
   * Dispatches a validated checkout payload to backend
   * @param {Object} checkoutPayload
   * @returns {Promise<Order>}
   */
  async checkout(checkoutPayload) {
    const rawData = await this.post('/checkout', checkoutPayload);
    return new Order(rawData);
  }

  /**
   * Validates cart and initiates payment before creating any order
   * @param {Object} checkoutPayload
   */
  async initiatePaymentCheckout(checkoutPayload) {
    return await this.client.post('/payments/checkout/initiate', checkoutPayload);
  }

  /**
   * Verifies payment and atomically creates the order on the server
   * @param {Object} completePayload
   * @returns {Promise<Order>}
   */
  async completePaymentCheckout(completePayload) {
    const rawData = await this.client.post('/payments/checkout/complete', completePayload);
    return new Order(rawData);
  }

  /**
   * Retrieves all orders placed by the current authenticated buyer
   * @returns {Promise<Order[]>}
   */
  async getBuyerOrders() {
    const rawData = await this.get('/buyer', { skipCache: true });
    if (!Array.isArray(rawData)) return [];
    return rawData.map((data) => new Order(data));
  }

  /**
   * Retrieves order details by ID
   * @param {number|string} orderId
   * @returns {Promise<Order>}
   */
  async getOrderById(orderId) {
    const rawData = await this.get(`/${orderId}`, { skipCache: true });
    return new Order(rawData);
  }

  /**
   * Cancels an order before preparation starts
   * @param {number|string} orderId
   * @param {string} reason
   */
  async cancelOrder(orderId, reason = 'Buyer request') {
    const rawData = await this.post(`/${orderId}/cancel`, { reason });
    return new Order(rawData);
  }

  /**
   * Submits buyer feedback for a delivered order
   * @param {number|string} orderId
   * @param {number} rating (1-5)
   * @param {string} comment
   */
  async submitFeedback(orderId, rating, comment = '') {
    return await this.client.post('/feedback', { orderId, rating, comment });
  }

  /**
   * Raises a formal dispute on an order
   * @param {number|string} orderId
   * @param {string} category
   * @param {string} description
   */
  async raiseDispute(orderId, category, description) {
    return await this.client.post('/disputes', { orderId, category, description });
  }
}

export const orderService = new OrderService();
