import { BaseModel } from './BaseModel';

/**
 * Base Account domain model.
 * Encapsulates authentication identity credentials & account lifecycle state.
 */
export class Account extends BaseModel {
  constructor(data = {}) {
    super(data);
    this.identityId = data.identityId !== undefined && data.identityId !== null ? Number(data.identityId) : null;
    this.profileId = data.profileId !== undefined && data.profileId !== null ? Number(data.profileId) : null;
    this.firebaseUid = data.firebaseUid || '';
    this.email = data.email || '';
    this.accountType = data.accountType || null; // BUYER, SHOP_OWNER, DELIVERY_PARTNER, ADMIN
    this.displayName = data.displayName || 'User';
    this.status = data.status || 'ACTIVE';
    this.deletionPending = Boolean(data.deletionPending);
    this.token = data.token || '';
  }

  isAuthenticated() {
    return Boolean(this.token && this.identityId);
  }

  hasRole(role) {
    return this.accountType === role;
  }

  isActive() {
    return this.status === 'ACTIVE';
  }

  isDeactivated() {
    return this.status === 'DEACTIVATED';
  }

  hasPendingDeletion() {
    return this.deletionPending;
  }

  isVerifiedForPlatform() {
    return this.isActive();
  }
}

/**
 * Contactable intermediate class in the OOP account hierarchy
 * shared by BuyerAccount, ShopOwnerAccount, and DeliveryPartnerAccount.
 */
export class ContactableAccount extends Account {
  constructor(data = {}) {
    super(data);
    this.phone = data.phone || '';
  }
}

/**
 * Subclass for Buyer profile (can order from anywhere)
 */
export class BuyerAccount extends ContactableAccount {
  constructor(data = {}) {
    super({ ...data, accountType: 'BUYER', displayName: data.displayName || 'Buyer' });
    this.defaultAddress = data.defaultAddress || '';
    this.defaultLatitude = data.defaultLatitude !== undefined && data.defaultLatitude !== null ? Number(data.defaultLatitude) : null;
    this.defaultLongitude = data.defaultLongitude !== undefined && data.defaultLongitude !== null ? Number(data.defaultLongitude) : null;
    this.foodInterests = data.foodInterests || '';
  }

  hasDefaultLocation() {
    return Boolean(this.defaultAddress && this.defaultAddress.trim());
  }

  getFoodInterestTags() {
    if (!this.foodInterests) return [];
    return this.foodInterests
      .split(/[,;|]+/)
      .map((t) => t.trim())
      .filter(Boolean);
  }
}

/**
 * Subclass for Shop Owner profile (requires Admin-approved shop to operate)
 */
export class ShopOwnerAccount extends ContactableAccount {
  constructor(data = {}) {
    super({ ...data, accountType: 'SHOP_OWNER', displayName: data.displayName || 'Shop Owner' });
    this.hasShop = Boolean(data.hasShop);
    this.shopId = data.shopId !== undefined && data.shopId !== null ? Number(data.shopId) : null;
    this.shopApprovalStatus = data.shopApprovalStatus || null; // PENDING, APPROVED, REJECTED, SUSPENDED
  }

  isVerifiedForPlatform() {
    return this.isActive() && this.hasShop && this.shopApprovalStatus === 'APPROVED';
  }
}

/**
 * Subclass for Delivery Partner profile (requires Admin verification to accept deliveries)
 */
export class DeliveryPartnerAccount extends ContactableAccount {
  constructor(data = {}) {
    super({ ...data, accountType: 'DELIVERY_PARTNER', displayName: data.displayName || 'Delivery Partner' });
    this.verificationStatus = data.verificationStatus || 'PENDING'; // PENDING, APPROVED, REJECTED, SUSPENDED
    this.vehicleDetails = data.vehicleDetails || '';
    this.serviceArea = data.serviceArea || '';
  }

  isVerifiedForPlatform() {
    return this.isActive() && this.verificationStatus === 'APPROVED';
  }
}

/**
 * Subclass for Platform Administrator profile
 */
export class AdminAccount extends Account {
  constructor(data = {}) {
    super({ ...data, accountType: 'ADMIN', displayName: data.displayName || 'Platform Admin' });
    this.verificationStatus = data.verificationStatus || 'PENDING';
  }

  isVerifiedForPlatform() {
    return this.isActive() && this.verificationStatus === 'APPROVED';
  }
}

/**
 * Factory for creating specific Account instances based on accountType
 */
export class AccountFactory {
  static create(data) {
    if (!data) return null;
    switch (data.accountType) {
      case 'BUYER':
        return new BuyerAccount(data);
      case 'SHOP_OWNER':
        return new ShopOwnerAccount(data);
      case 'DELIVERY_PARTNER':
        return new DeliveryPartnerAccount(data);
      case 'ADMIN':
        return new AdminAccount(data);
      default:
        return new Account(data);
    }
  }
}

