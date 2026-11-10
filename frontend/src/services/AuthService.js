import { BaseService } from './BaseService';
import { AccountFactory } from '../models/Account';
import { AuthenticationError } from '../errors/AppErrors';

/**
 * Service encapsulating Authentication & Identity operations.
 * Returns strongly typed Account domain model instances.
 */
export class AuthService extends BaseService {
  constructor() {
    super('/auth');
    this.USER_STORAGE_KEY = 'qb_user';
    this.TOKEN_STORAGE_KEY = 'qb_token';
  }

  getStoredToken() {
    return localStorage.getItem(this.TOKEN_STORAGE_KEY);
  }

  getStoredAccount() {
    try {
      const raw = localStorage.getItem(this.USER_STORAGE_KEY);
      if (!raw) return null;
      return AccountFactory.create(JSON.parse(raw));
    } catch {
      return null;
    }
  }

  storeSession(accountData) {
    if (!accountData) return;
    const account = accountData.accountType ? AccountFactory.create(accountData) : accountData;
    if (account.token) {
      localStorage.setItem(this.TOKEN_STORAGE_KEY, account.token);
    }
    localStorage.setItem(this.USER_STORAGE_KEY, JSON.stringify(account.toJSON ? account.toJSON() : account));
    return account;
  }

  clearSession() {
    localStorage.removeItem(this.TOKEN_STORAGE_KEY);
    localStorage.removeItem(this.USER_STORAGE_KEY);
    this.client.clearCache();
  }

  async getCurrentUser() {
    const token = this.getStoredToken();
    if (!token) {
      throw new AuthenticationError('No stored token found');
    }
    const rawData = await this.get('/me', { timeout: 4000 });
    const account = AccountFactory.create({ ...rawData, token });
    this.storeSession(account);
    return account;
  }

  async devQuickLogin(targetRoleOrEmail) {
    let email = 'buyer@quickbite.campus';
    if (targetRoleOrEmail === 'SHOP_OWNER') email = 'shop@quickbite.campus';
    else if (targetRoleOrEmail === 'DELIVERY_PARTNER') email = 'delivery@quickbite.campus';
    else if (targetRoleOrEmail === 'ADMIN') email = 'admin@quickbite.campus';
    else if (targetRoleOrEmail && targetRoleOrEmail.includes('@')) email = targetRoleOrEmail.trim();

    const rawData = await this.post('/dev-login', { email });
    const account = AccountFactory.create(rawData);
    this.storeSession(account);
    return account;
  }

  async signup(signupData) {
    const rawData = await this.post('/signup', signupData);
    const account = AccountFactory.create(rawData);
    this.storeSession(account);
    return account;
  }

  logout() {
    this.clearSession();
  }
}

export const authService = new AuthService();
