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
    return '/login';
  }

  getHomeRoute(account) {
    return this.getDefaultRoute(account);
  }

  canAccessRoute(pathname, account) {
    return Boolean(pathname);
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

  canAccessRoute(pathname) {
    return (
      pathname === '/' ||
      pathname === '/shops' ||
      /^\/shop\/\d+/.test(pathname) ||
      pathname === '/cart' ||
      pathname === '/orders' ||
      pathname === '/profile'
    );
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

  getDefaultRoute(account) {
    return account?.isVerifiedForPlatform() ? '/shop/dashboard' : '/shop/onboarding';
  }

  canAccessRoute(pathname, account) {
    if (!account?.isVerifiedForPlatform()) {
      return pathname === '/shop/onboarding';
    }
    return (
      pathname === '/shop/dashboard' ||
      pathname === '/shop/products' ||
      pathname === '/shop/orders' ||
      pathname === '/shop/settings' ||
      pathname === '/shop/onboarding'
    );
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
    return 'Delivery Partner';
  }

  getBadgeColor() {
    return 'badge-info';
  }

  getDefaultRoute(account) {
    return account?.isVerifiedForPlatform() ? '/delivery/dashboard' : '/delivery/verification';
  }

  canAccessRoute(pathname, account) {
    if (!account?.isVerifiedForPlatform()) {
      return pathname === '/delivery/verification';
    }
    return (
      pathname === '/delivery/dashboard' ||
      pathname === '/delivery/history' ||
      pathname === '/delivery/settings' ||
      pathname === '/delivery/verification'
    );
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

  getDefaultRoute(account) {
    return account?.isVerifiedForPlatform() ? '/admin/dashboard' : '/admin/verification';
  }

  canAccessRoute(pathname, account) {
    if (!account?.isVerifiedForPlatform()) {
      return pathname === '/admin/verification';
    }
    return pathname.startsWith('/admin');
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

