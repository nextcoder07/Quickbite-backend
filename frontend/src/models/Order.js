import { BaseModel } from './BaseModel';

/**
 * Domain model representing a customer Food Order.
 * Encapsulates order state transitions, status badges, and business invariants.
 */
export class Order extends BaseModel {
  constructor(data = {}) {
    super(data);
    this.shopId = data.shopId !== undefined ? Number(data.shopId) : null;
    this.shopName = data.shopName || '';
    this.shopLocation = data.shopLocation || '';
    this.shopLatitude = data.shopLatitude !== undefined && data.shopLatitude !== null ? Number(data.shopLatitude) : null;
    this.shopLongitude = data.shopLongitude !== undefined && data.shopLongitude !== null ? Number(data.shopLongitude) : null;
    this.orderNumber = data.orderNumber || '';
    this.buyerId = data.buyerId !== undefined ? Number(data.buyerId) : null;
    this.buyerName = data.buyerName || '';
    this.status = data.status || 'PLACED'; // PLACED, ACCEPTED, PREPARING, READY, OUT_FOR_DELIVERY, DELIVERED, CANCELLED, REJECTED
    this.subtotal = Number(data.subtotal) || 0;
    this.deliveryFee = Number(data.deliveryFee) || 0;
    this.discountAmount = Number(data.discountAmount) || 0;
    this.totalAmount = Number(data.totalAmount) || 0;
    this.deliveryAddress = data.deliveryAddress || '';
    this.deliveryLatitude = data.deliveryLatitude !== undefined && data.deliveryLatitude !== null ? Number(data.deliveryLatitude) : null;
    this.deliveryLongitude = data.deliveryLongitude !== undefined && data.deliveryLongitude !== null ? Number(data.deliveryLongitude) : null;
    this.buyerNote = data.buyerNote || data.notes || '';
    this.notes = this.buyerNote;
    this.placedAt = data.placedAt || data.createdAt || null;
    this.items = Array.isArray(data.items) ? data.items : [];
    this.estimatedDeliveryMinutes = data.estimatedDeliveryMinutes || 25;
    this.hasFeedback = Boolean(data.hasFeedback);
  }

  hasDeliveryCoordinates() {
    return Number.isFinite(this.deliveryLatitude) && Number.isFinite(this.deliveryLongitude);
  }

  getDeliveryMapUrl() {
    if (!this.hasDeliveryCoordinates()) return null;
    return `https://www.google.com/maps?q=${this.deliveryLatitude},${this.deliveryLongitude}`;
  }

  isTerminal() {
    return ['DELIVERED', 'CANCELLED', 'REJECTED'].includes(this.status);
  }

  isInProgress() {
    return ['ACCEPTED', 'PREPARING', 'READY', 'OUT_FOR_DELIVERY'].includes(this.status);
  }

  isDelivered() {
    return this.status === 'DELIVERED';
  }

  canBeCancelled() {
    return this.status === 'PLACED';
  }

  canSubmitFeedback() {
    return this.isDelivered() && !this.hasFeedback;
  }

  canRaiseDispute() {
    return this.status !== 'CANCELLED';
  }

  getFormattedTotal() {
    return `₹${this.totalAmount.toFixed(2)}`;
  }

  getFormattedDate() {
    try {
      return this.createdAt.toLocaleDateString('en-IN', {
        day: 'numeric',
        month: 'short',
        hour: '2-digit',
        minute: '2-digit',
      });
    } catch {
      return '';
    }
  }

  getStatusBadge() {
    switch (this.status) {
      case 'PLACED':
        return { label: 'Order Placed', type: 'info', description: 'Waiting for shop to accept' };
      case 'ACCEPTED':
        return { label: 'Accepted', type: 'warning', description: 'Shop confirmed your order' };
      case 'PREPARING':
        return { label: 'Preparing', type: 'warning', description: 'Chef is preparing your meal' };
      case 'READY':
        return { label: 'Ready for Pickup', type: 'info', description: 'Packaged & waiting for courier' };
      case 'OUT_FOR_DELIVERY':
        return { label: 'Out for Delivery', type: 'purple', description: 'Courier is on the way' };
      case 'DELIVERED':
        return { label: 'Delivered', type: 'success', description: 'Successfully handed over' };
      case 'CANCELLED':
        return { label: 'Cancelled', type: 'error', description: 'Order was cancelled' };
      case 'REJECTED':
        return { label: 'Declined', type: 'error', description: 'Shop was unable to accept' };
      default:
        return { label: this.status, type: 'default', description: '' };
    }
  }
}

