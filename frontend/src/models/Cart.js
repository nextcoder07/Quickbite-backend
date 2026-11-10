import { CartItem } from './CartItem';
import { Shop } from './Shop';
import { Product } from './Product';
import { BusinessRuleViolationError } from '../errors/AppErrors';

/**
 * Cart Aggregate Root
 * Enforces business invariants:
 * 1. Single-Shop Ordering: Cannot mix items from different shops.
 * 2. Non-negative quantities.
 * 3. Free delivery threshold calculation (aligned with campus policy: ₹200).
 * 4. Total calculation: subtotal + deliveryFee.
 */
export class Cart {
  constructor(items = [], shop = null) {
    this.shop = shop ? (shop instanceof Shop ? shop : new Shop(shop)) : null;
    this.items = items
      .map((item) => (item instanceof CartItem ? item : CartItem.fromJSON(item)))
      .filter(Boolean);
  }

  getShop() {
    return this.shop;
  }

  getItems() {
    return [...this.items];
  }

  getItemCount() {
    return this.items.reduce((sum, item) => sum + item.quantity, 0);
  }

  isEmpty() {
    return this.items.length === 0;
  }

  /**
   * Adds a product to the cart.
   * If cart has items from another shop, returns { conflict: true, existingShop, newShop }
   * so the caller can prompt the user to replace or abort.
   */
  addItem(productInput, shopInput, forceReset = false) {
    const product = productInput instanceof Product ? productInput : new Product(productInput);
    const targetShop = shopInput instanceof Shop ? shopInput : new Shop(shopInput);

    if (this.shop && this.shop.id !== targetShop.id && !this.isEmpty()) {
      if (!forceReset) {
        return {
          conflict: true,
          existingShop: this.shop,
          newShop: targetShop,
        };
      }
      this.clear();
    }

    if (!this.shop || this.isEmpty()) {
      this.shop = targetShop;
    }

    const existingItem = this.items.find((i) => i.getProductId() === product.id);
    if (existingItem) {
      existingItem.increment(1);
    } else {
      this.items.push(new CartItem(product, 1));
    }

    return { conflict: false, success: true };
  }

  updateQuantity(productId, delta) {
    const numId = Number(productId);
    const itemIndex = this.items.findIndex((i) => i.getProductId() === numId);
    if (itemIndex === -1) return;

    const item = this.items[itemIndex];
    if (delta > 0) {
      item.increment(delta);
    } else if (delta < 0) {
      const couldDecrement = item.decrement(Math.abs(delta));
      if (!couldDecrement) {
        this.items.splice(itemIndex, 1);
      }
    }

    if (this.items.length === 0) {
      this.shop = null;
    }
  }

  removeItem(productId) {
    const numId = Number(productId);
    this.items = this.items.filter((i) => i.getProductId() !== numId);
    if (this.items.length === 0) {
      this.shop = null;
    }
  }

  clear() {
    this.items = [];
    this.shop = null;
  }

  getSubtotal() {
    return this.items.reduce((sum, item) => sum + item.getLineTotal(), 0);
  }

  /**
   * Delivery fee calculation rule:
   * Orders below ₹200 incur ₹30 delivery fee.
   * Orders ₹200 and above get FREE delivery.
   */
  getDeliveryFee() {
    const sub = this.getSubtotal();
    if (sub <= 0) return 0;
    return sub < 200 ? 30 : 0;
  }

  getTotalAmount() {
    return this.getSubtotal() + this.getDeliveryFee();
  }

  getFreeDeliveryRemaining() {
    const sub = this.getSubtotal();
    return Math.max(0, 200 - sub);
  }

  /**
   * Generates immutable payload for backend /orders/checkout endpoint
   */
  toCheckoutPayload(deliveryAddress, notes = '') {
    if (this.isEmpty()) {
      throw new BusinessRuleViolationError('Cannot checkout an empty cart');
    }
    if (!this.shop) {
      throw new BusinessRuleViolationError('Cart is missing target shop information');
    }

    return {
      shopId: this.shop.id,
      deliveryAddress: String(deliveryAddress || '').trim(),
      notes: String(notes || '').trim(),
      items: this.items.map((i) => ({
        productId: i.getProductId(),
        quantity: i.quantity,
      })),
    };
  }

  toJSON() {
    return {
      shop: this.shop ? this.shop.toJSON() : null,
      items: this.items.map((i) => i.toJSON()),
    };
  }

  static fromJSON(data) {
    if (!data) return new Cart();
    const shop = data.shop ? new Shop(data.shop) : null;
    const items = Array.isArray(data.items)
      ? data.items.map((i) => CartItem.fromJSON(i)).filter(Boolean)
      : [];
    return new Cart(items, shop);
  }
}
