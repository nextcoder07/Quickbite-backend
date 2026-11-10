/**
 * Base Role Policy (Strategy Pattern / Polymorphism)
 * Defines the contract for role-specific privileges, routes, and UI details.
 */
export class BaseRolePolicy {
  getRole() {
    return 'GUEST';
  }

  getDisplayName() {
    return 'Guest';
  }

  getBadgeColor() {
    return 'badge-info';
  }

  getDefaultRoute() {
    return '/';
  }

  canBrowseMarketplace() {
    return true;
  }

  canPlaceOrder() {
    return false;
  }

  canManageShop() {
    return false;
  }

  canAcceptDeliveries() {
    return false;
  }

  canAccessAdmin() {
    return false;
  }
}

export class BuyerPolicy extends BaseRolePolicy {
  getRole() {
    return 'BUYER';
  }

  getDisplayName() {
    return 'Buyer';
  }

  getBadgeColor() {
    return 'badge-success';
  }

  getDefaultRoute() {
    return '/';
  }

  canPlaceOrder() {
    return true;
  }
}

export class ShopOwnerPolicy extends BaseRolePolicy {
  getRole() {
    return 'SHOP_OWNER';
  }

  getDisplayName() {
    return 'Shop Owner';
  }

  getBadgeColor() {
    return 'badge-warning';
  }

  getDefaultRoute() {
    return '/shop/dashboard';
  }

  canManageShop() {
    return true;
  }
}

export class DeliveryPartnerPolicy extends BaseRolePolicy {
  getRole() {
    return 'DELIVERY_PARTNER';
  }

  getDisplayName() {
    return 'Delivery';
  }

  getBadgeColor() {
    return 'badge-info';
  }

  getDefaultRoute() {
    return '/delivery/dashboard';
  }

  canAcceptDeliveries() {
    return true;
  }
}

export class AdminPolicy extends BaseRolePolicy {
  getRole() {
    return 'ADMIN';
  }

  getDisplayName() {
    return 'Admin';
  }

  getBadgeColor() {
    return 'badge-purple';
  }

  getDefaultRoute() {
    return '/admin/dashboard';
  }

  canAccessAdmin() {
    return true;
  }
}

/**
 * Factory to instantiate the appropriate polymorphic RolePolicy instance.
 */
export class RolePolicyFactory {
  static #cache = new Map();

  static getPolicy(role) {
    const key = role || 'GUEST';
    if (this.#cache.has(key)) {
      return this.#cache.get(key);
    }

    let policy;
    switch (key) {
      case 'BUYER':
        policy = new BuyerPolicy();
        break;
      case 'SHOP_OWNER':
        policy = new ShopOwnerPolicy();
        break;
      case 'DELIVERY_PARTNER':
        policy = new DeliveryPartnerPolicy();
        break;
      case 'ADMIN':
        policy = new AdminPolicy();
        break;
      default:
        policy = new BaseRolePolicy();
    }

    this.#cache.set(key, policy);
    return policy;
  }
}
