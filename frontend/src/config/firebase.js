import { initializeApp } from 'firebase/app';
import {
  getAuth,
  createUserWithEmailAndPassword,
  signInWithEmailAndPassword,
  updateProfile,
  deleteUser,
  signOut,
  onAuthStateChanged,
} from 'firebase/auth';
import { AuthenticationError } from '../errors/AppErrors';

const firebaseConfig = {
  apiKey: import.meta.env.VITE_FIREBASE_API_KEY,
  authDomain: import.meta.env.VITE_FIREBASE_AUTH_DOMAIN,
  projectId: import.meta.env.VITE_FIREBASE_PROJECT_ID,
  storageBucket: import.meta.env.VITE_FIREBASE_STORAGE_BUCKET,
  messagingSenderId: import.meta.env.VITE_FIREBASE_MESSAGING_SENDER_ID,
  appId: import.meta.env.VITE_FIREBASE_APP_ID,
};

const missingKeys = Object.entries({
  VITE_FIREBASE_API_KEY: firebaseConfig.apiKey,
  VITE_FIREBASE_AUTH_DOMAIN: firebaseConfig.authDomain,
  VITE_FIREBASE_PROJECT_ID: firebaseConfig.projectId,
  VITE_FIREBASE_APP_ID: firebaseConfig.appId,
})
  .filter(([, value]) => !value || !String(value).trim())
  .map(([key]) => key);

if (missingKeys.length > 0) {
  throw new Error(
    `Missing required Firebase frontend configuration: ${missingKeys.join(', ')}. Check frontend/.env files.`
  );
}

export const firebaseApp = initializeApp(firebaseConfig);
export const firebaseAuth = getAuth(firebaseApp);

/**
 * OOP Gateway encapsulating Firebase Client Authentication operations.
 */
export class FirebaseAuthGateway {
  constructor(authInstance) {
    this.auth = authInstance;
    this._readyPromise = null;
  }

  waitForAuthReady() {
    if (!this._readyPromise) {
      this._readyPromise = new Promise((resolve) => {
        const unsubscribe = onAuthStateChanged(this.auth, (user) => {
          unsubscribe();
          resolve(user || null);
        });
      });
    }
    return this._readyPromise;
  }

  async getActiveIdToken(forceRefresh = false) {
    const user = this.auth.currentUser || (await this.waitForAuthReady());
    if (!user) return null;
    return user.getIdToken(forceRefresh);
  }

  async signUp(email, password, displayName) {
    try {
      const credential = await createUserWithEmailAndPassword(this.auth, email, password);
      if (displayName && displayName.trim()) {
        await updateProfile(credential.user, { displayName: displayName.trim() });
      }
      const idToken = await credential.user.getIdToken();
      return { user: credential.user, idToken };
    } catch (err) {
      throw this._mapError(err);
    }
  }

  async signIn(email, password) {
    try {
      const credential = await signInWithEmailAndPassword(this.auth, email, password);
      const idToken = await credential.user.getIdToken(true);
      return { user: credential.user, idToken };
    } catch (err) {
      throw this._mapError(err);
    }
  }

  async refreshToken(user) {
    const target = user || this.auth.currentUser;
    if (!target) {
      throw new AuthenticationError('No active Firebase user session.');
    }
    return target.getIdToken(true);
  }

  async rollbackUser(user) {
    if (!user) return;
    try {
      await deleteUser(user);
    } catch (rollbackErr) {
      console.error('Failed to rollback Firebase user after backend signup failure:', rollbackErr);
    }
  }

  async signOut() {
    await signOut(this.auth);
  }

  _mapError(err) {
    const code = err?.code || '';
    switch (code) {
      case 'auth/email-already-in-use':
        return new AuthenticationError(
          'An account with this email already exists in Firebase Authentication. Please sign in instead.'
        );
      case 'auth/invalid-credential':
      case 'auth/wrong-password':
      case 'auth/user-not-found':
        return new AuthenticationError('Invalid email or password.');
      case 'auth/user-disabled':
        return new AuthenticationError('This account has been deactivated and removed from the platform.');
      case 'auth/weak-password':
        return new AuthenticationError('Password must be at least 6 characters.');
      case 'auth/invalid-email':
        return new AuthenticationError('Invalid email address format.');
      case 'auth/too-many-requests':
        return new AuthenticationError('Too many failed attempts. Please wait a moment and try again.');
      case 'auth/network-request-failed':
        return new AuthenticationError(
          'Cannot reach Firebase Authentication servers. Please check your internet connection.'
        );
      default:
        return new AuthenticationError(err?.message || 'Firebase authentication failed.');
    }
  }
}

export const firebaseAuthGateway = new FirebaseAuthGateway(firebaseAuth);

