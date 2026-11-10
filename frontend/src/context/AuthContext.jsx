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
  };

  const logout = () => {
    authService.logout();
    setAccount(null);
    setToken(null);
  };

  const devQuickLogin = async (targetRoleOrEmail) => {
    try {
      const loggedInAccount = await authService.devQuickLogin(targetRoleOrEmail);
      setAccount(loggedInAccount);
      setToken(loggedInAccount.token);
      return loggedInAccount;
    } catch (err) {
      console.error('Dev login failed:', err);
      throw err;
    }
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
        logout,
        devQuickLogin,
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
