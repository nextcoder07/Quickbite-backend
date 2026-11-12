import { BaseModel } from './BaseModel';

/**
 * Domain model representing a Food Establishment at any location.
 * Encapsulates operational status, location & coordinate matching, menu item search, and state helpers.
 */
export class Shop extends BaseModel {
  constructor(data = {}) {
    super(data);
    this.ownerId = data.ownerId !== undefined ? Number(data.ownerId) : null;
    this.name = String(data.name || '').trim();
    this.description = String(data.description || '').trim();
    this.imagePath = data.imagePath || '';
    this.location = String(data.location || data.campusLocation || '').trim();
    this.campusLocation = this.location;
    this.latitude = data.latitude !== undefined && data.latitude !== null ? Number(data.latitude) : null;
    this.longitude = data.longitude !== undefined && data.longitude !== null ? Number(data.longitude) : null;
    this.approvalStatus = data.approvalStatus || 'PENDING'; // PENDING, APPROVED, REJECTED, SUSPENDED
    this.isOpen = Boolean(data.isOpen ?? data.open ?? false);
    this.isDeactivated = Boolean(data.isDeactivated ?? data.deactivated ?? false);
    this.productNames = Array.isArray(data.productNames) ? data.productNames : [];
  }

  hasCoordinates() {
    return Number.isFinite(this.latitude) && Number.isFinite(this.longitude);
  }

  getMapUrl() {
    if (!this.hasCoordinates()) return null;
    return `https://www.google.com/maps?q=${this.latitude},${this.longitude}`;
  }

  isApproved() {
    return this.approvalStatus === 'APPROVED';
  }

  isPending() {
    return this.approvalStatus === 'PENDING';
  }

  isRejected() {
    return this.approvalStatus === 'REJECTED';
  }

  isSuspended() {
    return this.approvalStatus === 'SUSPENDED';
  }

  canAcceptOrders() {
    return this.isApproved() && this.isOpen && !this.isDeactivated;
  }

  /**
   * Evaluates search query against shop name, description, location, AND menu product names.
   * Enables buyers anywhere to search by product name, shop name, or location.
   */
  matchesSearch(keyword = '', locationFilter = 'ALL') {
    const term = keyword.trim().toLowerCase();
    const matchesKeyword = !term ||
      this.name.toLowerCase().includes(term) ||
      this.description.toLowerCase().includes(term) ||
      this.location.toLowerCase().includes(term) ||
      this.productNames.some((p) => p.toLowerCase().includes(term));

    const loc = String(locationFilter || '').trim();
    const matchesLoc = !loc || loc.toUpperCase() === 'ALL' ||
      this.location.toLowerCase().includes(loc.toLowerCase());

    return matchesKeyword && matchesLoc;
  }

  /**
   * Returns list of product names belonging to this shop that match the search keyword.
   */
  getMatchingProducts(keyword = '') {
    const term = keyword.trim().toLowerCase();
    if (!term) return [];
    return this.productNames.filter((p) => p.toLowerCase().includes(term));
  }

  getStatusBadge() {
    if (this.isDeactivated) {
      return { label: 'Deactivated', type: 'error' };
    }
    if (this.approvalStatus === 'PENDING') {
      return { label: 'Pending Approval', type: 'warning' };
    }
    if (this.approvalStatus === 'REJECTED') {
      return { label: 'Rejected', type: 'error' };
    }
    if (this.approvalStatus === 'SUSPENDED') {
      return { label: 'Suspended', type: 'error' };
    }
    if (!this.isOpen) {
      return { label: 'Closed', type: 'default' };
    }
    return { label: 'Open Now', type: 'success' };
  }
}
