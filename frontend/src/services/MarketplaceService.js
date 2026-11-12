import { BaseService } from './BaseService';
import { Shop } from '../models/Shop';
import { Product } from '../models/Product';

/**
 * Service encapsulating Marketplace operations across all locations.
 * Maps raw backend responses into strongly typed Shop and Product domain models.
 */
export class MarketplaceService extends BaseService {
  constructor() {
    super('/marketplace');
  }

  /**
   * Retrieves food shops with optional keyword (shop/product) and location filtering.
   * @param {boolean} includeClosed - whether to include currently closed shops
   * @param {string} query - optional search keyword
   * @param {string} location - optional location filter
   * @returns {Promise<Shop[]>}
   */
  async getShops(includeClosed = true, query = '', location = '') {
    const params = new URLSearchParams();
    params.set('includeClosed', String(includeClosed));
    if (query && query.trim()) params.set('q', query.trim());
    if (location && location.trim() && location !== 'ALL') params.set('location', location.trim());
    const rawData = await this.get(`/shops?${params.toString()}`);
    if (!Array.isArray(rawData)) return [];
    return rawData.map((data) => new Shop(data));
  }

  /**
   * Searches menu products directly by product name across all shops and locations.
   * @param {string} query
   * @param {string} location
   * @returns {Promise<Array>}
   */
  async searchProducts(query, location = '') {
    if (!query || !query.trim()) return [];
    const params = new URLSearchParams();
    params.set('q', query.trim());
    if (location && location.trim() && location !== 'ALL') params.set('location', location.trim());
    const rawData = await this.get(`/products/search?${params.toString()}`);
    return Array.isArray(rawData) ? rawData : [];
  }

  /**
   * Fetches recommended food items ranked by top ratings, buyer profile interests,
   * previous buys, or cold-start discovery algorithm for fresh accounts.
   * @param {string} query
   * @param {string} location
   */
  async getRecommendedProducts(query = '', location = '') {
    const params = new URLSearchParams();
    if (query && query.trim()) params.set('q', query.trim());
    if (location && location.trim() && location !== 'ALL') params.set('location', location.trim());
    const qs = params.toString();
    return await this.get(`/products/recommended${qs ? `?${qs}` : ''}`);
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

