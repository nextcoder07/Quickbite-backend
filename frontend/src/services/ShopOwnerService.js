import { BaseService } from './BaseService';
import { Shop } from '../models/Shop';
import { Product } from '../models/Product';
import { Order } from '../models/Order';

/**
 * Service encapsulating Shop Owner actions: shop settings, menu catalog, incoming orders.
 */
export class ShopOwnerService extends BaseService {
  constructor() {
    super('/marketplace/owner');
  }

  async getMyShop() {
    const rawData = await this.get('/shop');
    return rawData ? new Shop(rawData) : null;
  }

  async createShop(shopData) {
    const rawData = await this.post('/shop', shopData);
    return new Shop(rawData);
  }

  async updateShop(shopData) {
    const rawData = await this.put('/shop', shopData);
    return new Shop(rawData);
  }

  async getMyProducts() {
    const rawData = await this.get('/products');
    if (!Array.isArray(rawData)) return [];
    return rawData.map((data) => new Product(data));
  }

  async createProduct(productData) {
    const rawData = await this.post('/products', productData);
    return new Product(rawData);
  }

  async updateProduct(productId, productData) {
    const rawData = await this.put(`/products/${productId}`, productData);
    return new Product(rawData);
  }

  async deleteProduct(productId) {
    return await this.delete(`/products/${productId}`);
  }

  async getIncomingOrders() {
    const rawData = await this.client.get('/orders/shop');
    if (!Array.isArray(rawData)) return [];
    return rawData.map((data) => new Order(data));
  }

  async updateOrderStatus(orderId, status) {
    const rawData = await this.client.patch(`/orders/${orderId}/status`, { status });
    return new Order(rawData);
  }
}

export const shopOwnerService = new ShopOwnerService();
