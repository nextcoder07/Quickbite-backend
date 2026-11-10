import { BaseModel } from './BaseModel';

/**
 * Domain model representing a Shop's food menu item.
 * Encapsulates pricing formatting, stock management rules, and availability invariants.
 */
export class Product extends BaseModel {
  constructor(data = {}) {
    super(data);
    this.shopId = data.shopId !== undefined ? Number(data.shopId) : null;
    this.name = String(data.name || '').trim();
    this.description = String(data.description || '').trim();
    this.price = Number(data.price) || 0;
    this.stockQuantity = data.stockQuantity !== undefined ? Number(data.stockQuantity) : 0;
    this.imagePath = data.imagePath || '';
    this.isAvailable = data.isAvailable ?? data.available ?? true;
  }

  get formattedPrice() {
    return `₹${this.price.toFixed(2)}`;
  }

  isInStock() {
    return this.isAvailable && this.stockQuantity > 0;
  }

  canOrderQuantity(requestedQty) {
    if (!this.isInStock()) return false;
    return requestedQty > 0 && requestedQty <= this.stockQuantity;
  }

  getStockStatusLabel() {
    if (!this.isAvailable) return 'Currently Unavailable';
    if (this.stockQuantity <= 0) return 'Out of Stock';
    if (this.stockQuantity <= 5) return `Only ${this.stockQuantity} left!`;
    return 'In Stock';
  }

  cloneWith(overrides = {}) {
    return new Product({ ...this.toJSON(), ...overrides });
  }
}
