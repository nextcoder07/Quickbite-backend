import { BaseService } from './BaseService';
import { Shop } from '../models/Shop';
import { Product } from '../models/Product';

/**
 * Service encapsulating campus Marketplace operations.
 * Maps raw backend responses into strongly typed Shop and Product domain models.
 */
export class MarketplaceService extends BaseService {
  constructor() {
    super('/marketplace');
  }

  /**
   * Retrieves all campus food shops
   * @param {boolean} includeClosed - whether to include currently closed shops
   * @returns {Promise<Shop[]>}
   */
  async getShops(includeClosed = true) {
    const rawData = await this.get(`/shops?includeClosed=${includeClosed}`);
    if (!Array.isArray(rawData)) return [];
    return rawData.map((data) => new Shop(data));
  }

  /**
   * Retrieves a specific shop by ID
   * @param {number|string} shopId
   * @returns {Promise<Shop>}
   */
  async getShopById(shopId) {
    const rawData = await this.get(`/shops/${shopId}`);
    return new Shop(rawData);
  }

  /**
   * Retrieves available menu items for a shop
   * @param {number|string} shopId
   * @returns {Promise<Product[]>}
   */
  async getProductsByShop(shopId) {
    const rawData = await this.get(`/shops/${shopId}/products`);
    if (!Array.isArray(rawData)) return [];
    return rawData.map((data) => new Product(data));
  }
}

export const marketplaceService = new MarketplaceService();
