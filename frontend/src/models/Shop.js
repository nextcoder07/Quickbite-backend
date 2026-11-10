import { BaseModel } from './BaseModel';

/**
 * Domain model representing a campus Food Establishment.
 * Encapsulates operational status, campus location matching, and state helpers.
 */
export class Shop extends BaseModel {
  constructor(data = {}) {
    super(data);
    this.ownerId = data.ownerId !== undefined ? Number(data.ownerId) : null;
    this.name = String(data.name || '').trim();
    this.description = String(data.description || '').trim();
    this.imagePath = data.imagePath || '';
    this.campusLocation = String(data.campusLocation || '').trim();
    this.approvalStatus = data.approvalStatus || 'PENDING'; // PENDING, APPROVED, REJECTED, SUSPENDED
    this.isOpen = Boolean(data.isOpen ?? data.open ?? false);
    this.isDeactivated = Boolean(data.isDeactivated ?? data.deactivated ?? false);
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

  matchesSearch(keyword = '', locationFilter = 'ALL') {
    const term = keyword.trim().toLowerCase();
    const matchesKeyword = !term ||
      this.name.toLowerCase().includes(term) ||
      this.description.toLowerCase().includes(term);

    const matchesLoc = !locationFilter || locationFilter === 'ALL' ||
      this.campusLocation.toLowerCase().includes(locationFilter.toLowerCase());

    return matchesKeyword && matchesLoc;
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
