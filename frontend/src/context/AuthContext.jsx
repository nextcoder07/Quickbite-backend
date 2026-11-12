import React, { createContext, useContext, useState, useEffect, useMemo } from 'react';
import { authService } from '../services/AuthService';
import { RolePolicyFactory } from '../policies/RolePolicy';

const AuthContext = createContext(null);

export const AuthProvider = ({ children }) => {
  const [account, setAccount] = useState(() => authService.getStoredAccount());
  const [token, setToken] = useState(() => authService.getStoredToken());
  const [loading, setLoading] = useState(() => {
    const storedToken = authService.getStoredToken();
    const storedAccount = authService.getStoredAccount();
    return !!storedToken && !storedAccount;
  });

  useEffect(() => {
    const verifySession = async () => {
      const storedToken = authService.getStoredToken();
      if (storedToken) {
        try {
          const freshAccount = await authService.getCurrentUser();
          setAccount(freshAccount);
        } catch (err) {
          console.warn('Session verification notice:', err.message);
          if (err.status === 401) {
            authService.logout();
            setAccount(null);
            setToken(null);
          }
        }
      }
      setLoading(false);
    };

    verifySession();
  }, []);

  const login = (accountData) => {
    const stored = authService.storeSession(accountData);
    setAccount(stored);
    setToken(stored.token);
    return stored;
  };

  const loginWithPassword = async (email, password) => {
    const loggedInAccount = await authService.loginWithPassword(email, password);
    setAccount(loggedInAccount);
    setToken(loggedInAccount.token);
    return loggedInAccount;
  };

  const refreshAccount = async () => {
    const freshAccount = await authService.getCurrentUser();
    setAccount(freshAccount);
    setToken(freshAccount.token || authService.getStoredToken());
    return freshAccount;
  };

  const logout = async () => {
    await authService.logout();
    setAccount(null);
    setToken(null);
  };

  const updateProfile = (updatedFields) => {
    if (!account) return;
    const updated = { ...(account.toJSON ? account.toJSON() : account), ...updatedFields };
    login(updated);
  };

  const role = account?.accountType || null;

  // Polymorphic Role Policy
  const policy = useMemo(() => RolePolicyFactory.getPolicy(role), [role]);

  return (
    <AuthContext.Provider
      value={{
        user: account,
        account,
        token,
        role,
        policy,
        isAuthenticated: !!account && !!token,
        loading,
        login,
        loginWithPassword,
        refreshAccount,
        logout,
        updateProfile,
      }}
    >
      {children}
    </AuthContext.Provider>
  );
};

export const useAuth = () => {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return context;
};
