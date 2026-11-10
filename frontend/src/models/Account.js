import { BaseModel } from './BaseModel';

/**
 * Base Account domain model.
 * Encapsulates authentication identity credentials.
 */
export class Account extends BaseModel {
  constructor(data = {}) {
    super(data);
    this.identityId = data.identityId !== undefined ? Number(data.identityId) : null;
    this.firebaseUid = data.firebaseUid || '';
    this.email = data.email || '';
    this.accountType = data.accountType || null; // BUYER, SHOP_OWNER, DELIVERY_PARTNER, ADMIN
    this.token = data.token || '';
  }

  isAuthenticated() {
    return Boolean(this.token && this.identityId);
  }

  hasRole(role) {
    return this.accountType === role;
  }
}

/**
 * Subclass for Buyer profile
 */
export class BuyerAccount extends Account {
  constructor(data = {}) {
    super({ ...data, accountType: 'BUYER' });
    this.profileId = data.profileId !== undefined ? Number(data.profileId) : null;
    this.displayName = data.displayName || 'Buyer';
    this.phone = data.phone || '';
    this.status = data.status || 'ACTIVE';
  }

  isActive() {
    return this.status === 'ACTIVE';
  }
}

/**
 * Subclass for Shop Owner profile
 */
export class ShopOwnerAccount extends Account {
  constructor(data = {}) {
    super({ ...data, accountType: 'SHOP_OWNER' });
    this.profileId = data.profileId !== undefined ? Number(data.profileId) : null;
    this.displayName = data.displayName || 'Shop Owner';
    this.phone = data.phone || '';
    this.status = data.status || 'ACTIVE';
  }

  isActive() {
    return this.status === 'ACTIVE';
  }
}

/**
 * Subclass for Delivery Partner profile
 */
export class DeliveryPartnerAccount extends Account {
  constructor(data = {}) {
    super({ ...data, accountType: 'DELIVERY_PARTNER' });
    this.profileId = data.profileId !== undefined ? Number(data.profileId) : null;
    this.displayName = data.displayName || 'Delivery Courier';
    this.phone = data.phone || '';
    this.status = data.status || 'ACTIVE';
  }

  isActive() {
    return this.status === 'ACTIVE';
  }
}

/**
 * Subclass for Campus Administrator profile
 */
export class AdminAccount extends Account {
  constructor(data = {}) {
    super({ ...data, accountType: 'ADMIN' });
    this.profileId = data.profileId !== undefined ? Number(data.profileId) : null;
    this.displayName = data.displayName || 'Campus Admin';
    this.status = data.status || 'ACTIVE';
  }

  isActive() {
    return this.status === 'ACTIVE';
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
