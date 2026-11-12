import { BaseService } from './BaseService';
import { AccountFactory } from '../models/Account';
import { AuthenticationError, ValidationError } from '../errors/AppErrors';
import { firebaseAuthGateway } from '../config/firebase';

/**
 * Service encapsulating Firebase Authentication & QuickBite Supabase Profile synchronization.
 * Returns strongly typed Account domain model instances.
 */
export class AuthService extends BaseService {
  constructor() {
    super('/auth');
    this.USER_STORAGE_KEY = 'qb_user';
    this.TOKEN_STORAGE_KEY = 'qb_token';
    this.client.setTokenProvider(() => firebaseAuthGateway.getActiveIdToken(false));
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
    if (!accountData) return null;
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
    localStorage.removeItem('qb_cart');
    localStorage.removeItem('qb_cart_shop');
    this.client.clearCache();
  }

  async getCurrentUser(forceRefreshToken = false) {
    const firebaseToken = await firebaseAuthGateway.getActiveIdToken(forceRefreshToken);
    if (!firebaseToken) {
      this.clearSession();
      throw new AuthenticationError('No active Firebase session found');
    }

    localStorage.setItem(this.TOKEN_STORAGE_KEY, firebaseToken);
    const rawData = await this.get('/me', { skipCache: true, timeout: 45000 });
    const account = AccountFactory.create({ ...rawData, token: firebaseToken });
    this.storeSession(account);
    return account;
  }

  async loginWithPassword(email, password) {
    const trimmedEmail = String(email || '').trim();
    if (!trimmedEmail || !password) {
      throw new ValidationError('Email and password are required.');
    }

    const { user, idToken } = await firebaseAuthGateway.signIn(trimmedEmail, password);

    try {
      const rawData = await this.post(
        '/login',
        {},
        { Authorization: `Bearer ${idToken}` }
      );
      const refreshedToken = await firebaseAuthGateway.refreshToken(user);
      const account = AccountFactory.create({ ...rawData, token: refreshedToken });
      this.storeSession(account);
      return account;
    } catch (err) {
      await firebaseAuthGateway.signOut();
      this.clearSession();
      throw err;
    }
  }

  async signup(signupData) {
    const email = String(signupData?.email || '').trim();
    const displayName = String(signupData?.displayName || '').trim();
    const password = signupData?.password || '';

    if (!displayName || !email || !password) {
      throw new ValidationError('Full Name, Email, and Password are required.');
    }
    if (password.length < 6) {
      throw new ValidationError('Password must be at least 6 characters.');
    }

    const { user, idToken } = await firebaseAuthGateway.signUp(email, password, displayName);

    try {
      // Never transmit raw password to the REST backend; Firebase holds credentials securely
      const { password: _omittedPassword, ...profilePayload } = signupData;
      const rawData = await this.post(
        '/signup',
        { ...profilePayload, email },
        { Authorization: `Bearer ${idToken}` }
      );
      const refreshedToken = await firebaseAuthGateway.refreshToken(user);
      const account = AccountFactory.create({ ...rawData, token: refreshedToken });
      this.storeSession(account);
      return account;
    } catch (err) {
      await firebaseAuthGateway.rollbackUser(user);
      await firebaseAuthGateway.signOut();
      this.clearSession();
      throw err;
    }
  }

  async logout() {
    try {
      const activeToken = await firebaseAuthGateway.getActiveIdToken(false);
      if (activeToken) {
        await this.post('/logout', {}, { Authorization: `Bearer ${activeToken}` });
      }
    } catch {
      // Best-effort server audit log; always sign out from Firebase & clear local session
    } finally {
      await firebaseAuthGateway.signOut().catch(() => {});
      this.clearSession();
    }
  }
}

export const authService = new AuthService();
