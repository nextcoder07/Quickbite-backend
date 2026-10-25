import React, { createContext, useContext, useState, useEffect } from 'react';
import { api } from '../api/apiClient';

const AuthContext = createContext(null);

export const AuthProvider = ({ children }) => {
  const [user, setUser] = useState(() => {
    const saved = localStorage.getItem('qb_user');
    return saved ? JSON.parse(saved) : null;
  });
  const [token, setToken] = useState(() => localStorage.getItem('qb_token'));
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const checkAuth = async () => {
      const storedToken = localStorage.getItem('qb_token');
      if (storedToken) {
        try {
          const profile = await api.get('/auth/me');
          setUser(profile);
          localStorage.setItem('qb_user', JSON.stringify(profile));
        } catch (err) {
          console.warn('Session restoration failed:', err.message);
          // Don't forcefully wipe immediately if server is still starting
        }
      }
      setLoading(false);
    };

    checkAuth();
  }, []);

  const login = (authData) => {
    setUser(authData);
    setToken(authData.token);
    localStorage.setItem('qb_token', authData.token);
    localStorage.setItem('qb_user', JSON.stringify(authData));
  };

  const logout = () => {
    setUser(null);
    setToken(null);
    localStorage.removeItem('qb_token');
    localStorage.removeItem('qb_user');
  };

  const devQuickLogin = async (targetRole) => {
    let email = 'buyer@quickbite.campus';
    if (targetRole === 'SHOP_OWNER') email = 'shop@quickbite.campus';
    if (targetRole === 'DELIVERY_PARTNER') email = 'delivery@quickbite.campus';
    if (targetRole === 'ADMIN') email = 'admin@quickbite.campus';

    try {
      const res = await api.post('/auth/dev-login', { email });
      login(res);
      return res;
    } catch (err) {
      console.error('Dev login failed:', err);
      throw err;
    }
  };

  const updateProfile = (updatedFields) => {
    const updated = { ...user, ...updatedFields };
    setUser(updated);
    localStorage.setItem('qb_user', JSON.stringify(updated));
  };

  const role = user?.accountType;

  return (
    <AuthContext.Provider
      value={{
        user,
        token,
        role,
        isAuthenticated: !!user && !!token,
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
