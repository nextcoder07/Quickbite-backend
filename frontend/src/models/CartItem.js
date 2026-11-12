import { Product } from './Product';

/**
 * Domain model representing a line item in the shopping cart.
 * Encapsulates quantity limits, line total arithmetic, and serialization.
 */
export class CartItem {
  constructor(product, quantity = 1) {
    this.product = product instanceof Product ? product : new Product(product);
    this.quantity = Math.max(1, Number(quantity) || 1);
  }

  getProductId() {
    return this.product.id;
  }

  getProductName() {
    return this.product.name;
  }

  getUnitPrice() {
    return this.product.price;
  }

  getLineTotal() {
    return this.getUnitPrice() * this.quantity;
  }

  getFormattedLineTotal() {
    return `₹${this.getLineTotal().toFixed(2)}`;
  }

  increment(amount = 1) {
    const nextQty = this.quantity + amount;
    if (this.product.stockQuantity && nextQty > this.product.stockQuantity) {
      return false;
    }
    this.quantity = nextQty;
    return true;
  }

  decrement(amount = 1) {
    const nextQty = this.quantity - amount;
    if (nextQty <= 0) return false;
    this.quantity = nextQty;
    return true;
  }

  setQuantity(qty) {
    const validQty = Math.max(1, Number(qty) || 1);
    if (this.product.stockQuantity && validQty > this.product.stockQuantity) {
      this.quantity = this.product.stockQuantity;
    } else {
      this.quantity = validQty;
    }
  }

  toJSON() {
    return {
      product: this.product.toJSON(),
      quantity: this.quantity,
    };
  }

  static fromJSON(json) {
    if (!json || !json.product) return null;
    return new CartItem(new Product(json.product), json.quantity);
  }
}

