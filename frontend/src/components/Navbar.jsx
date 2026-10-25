import React, { useState } from 'react';
import { useAuth } from '../context/AuthContext';
import { useCart } from '../context/CartContext';
import { Link, useNavigate } from 'react-router-dom';
import { 
  ShoppingBag, 
  User, 
  LogOut, 
  ChevronDown, 
  Shield, 
  Store, 
  Bike, 
  Sparkles,
  Layers
} from 'lucide-react';

export const Navbar = () => {
  const { user, role, logout, devQuickLogin } = useAuth();
  const { itemCount, subtotal } = useCart();
  const navigate = useNavigate();
  const [showRoleMenu, setShowRoleMenu] = useState(false);

  const handleRoleSwitch = async (targetRole) => {
    setShowRoleMenu(false);
    await devQuickLogin(targetRole);
    if (targetRole === 'BUYER') navigate('/');
    if (targetRole === 'SHOP_OWNER') navigate('/shop/dashboard');
    if (targetRole === 'DELIVERY_PARTNER') navigate('/delivery/dashboard');
    if (targetRole === 'ADMIN') navigate('/admin/dashboard');
  };

  const getRoleBadge = () => {
    switch (role) {
      case 'ADMIN':
        return <span className="badge badge-purple"><Shield size={12} /> Admin</span>;
      case 'SHOP_OWNER':
        return <span className="badge badge-warning"><Store size={12} /> Shop Owner</span>;
      case 'DELIVERY_PARTNER':
        return <span className="badge badge-info"><Bike size={12} /> Delivery</span>;
      default:
        return <span className="badge badge-success"><User size={12} /> Buyer</span>;
    }
  };

  return (
    <header style={{
      height: '68px',
      background: 'rgba(18, 21, 28, 0.85)',
      backdropFilter: 'blur(16px)',
      borderBottom: '1px solid var(--border-subtle)',
      padding: '0 28px',
      display: 'flex',
      alignItems: 'center',
      justifyContent: 'space-between',
      position: 'sticky',
      top: 0,
      zIndex: 100,
    }}>
      {/* Brand */}
      <Link to="/" style={{ display: 'flex', alignItems: 'center', gap: '10px', textDecoration: 'none' }}>
        <div style={{
          width: '38px',
          height: '38px',
          borderRadius: '10px',
          background: 'linear-gradient(135deg, #FF6433 0%, #FF4500 100%)',
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
          boxShadow: '0 4px 12px rgba(255, 100, 51, 0.3)'
        }}>
          <Sparkles size={22} color="#fff" />
        </div>
        <div>
          <span style={{ fontSize: '1.25rem', fontWeight: 800, color: '#fff', letterSpacing: '-0.02em' }}>Quick<span style={{ color: '#FF6433' }}>Bite</span></span>
          <span style={{ fontSize: '0.68rem', display: 'block', color: 'var(--text-muted)', fontWeight: 600, letterSpacing: '0.05em' }}>CAMPUS FOOD MARKETPLACE</span>
        </div>
      </Link>

      {/* Right Controls */}
      <div style={{ display: 'flex', alignItems: 'center', gap: '18px' }}>
        {/* Role Switcher (College Project Fast-Testing Modal) */}
        <div style={{ position: 'relative' }}>
          <button
            onClick={() => setShowRoleMenu(!showRoleMenu)}
            className="btn btn-secondary btn-sm"
            style={{ display: 'flex', alignItems: 'center', gap: '8px', padding: '6px 12px' }}
          >
            <Layers size={14} color="#FF6433" />
            <span>Role:</span>
            {getRoleBadge()}
            <ChevronDown size={14} color="var(--text-muted)" />
          </button>

          {showRoleMenu && (
            <div className="glass-panel" style={{
              position: 'absolute',
              top: '42px',
              right: 0,
              width: '230px',
              padding: '8px',
              background: '#191D24',
              zIndex: 200,
              boxShadow: '0 10px 30px rgba(0,0,0,0.6)'
            }}>
              <div style={{ padding: '6px 10px', fontSize: '0.72rem', color: 'var(--text-faint)', fontWeight: 700 }}>
                TEST ACCOUNT SWITCHER
              </div>
              <button
                onClick={() => handleRoleSwitch('BUYER')}
                className="btn btn-secondary btn-sm"
                style={{ width: '100%', justifyContent: 'flex-start', marginBottom: '4px' }}
              >
                <User size={14} color="#10B981" /> Buyer (Aarav)
              </button>
              <button
                onClick={() => handleRoleSwitch('SHOP_OWNER')}
                className="btn btn-secondary btn-sm"
                style={{ width: '100%', justifyContent: 'flex-start', marginBottom: '4px' }}
              >
                <Store size={14} color="#F59E0B" /> Shop Owner (Vikram)
              </button>
              <button
                onClick={() => handleRoleSwitch('DELIVERY_PARTNER')}
                className="btn btn-secondary btn-sm"
                style={{ width: '100%', justifyContent: 'flex-start', marginBottom: '4px' }}
              >
                <Bike size={14} color="#3B82F6" /> Delivery Partner (Rohan)
              </button>
              <button
                onClick={() => handleRoleSwitch('ADMIN')}
                className="btn btn-secondary btn-sm"
                style={{ width: '100%', justifyContent: 'flex-start' }}
              >
                <Shield size={14} color="#A855F7" /> Campus Admin
              </button>
            </div>
          )}
        </div>

        {/* Buyer Cart Button */}
        {role === 'BUYER' && (
          <Link
            to="/cart"
            className="btn btn-primary btn-sm"
            style={{ display: 'flex', alignItems: 'center', gap: '8px' }}
          >
            <ShoppingBag size={16} />
            <span>Cart</span>
            {itemCount > 0 && (
              <span style={{
                background: '#fff',
                color: '#FF6433',
                borderRadius: '12px',
                padding: '1px 6px',
                fontSize: '0.75rem',
                fontWeight: 800
              }}>
                {itemCount}
              </span>
            )}
            {subtotal > 0 && (
              <span style={{ borderLeft: '1px solid rgba(255,255,255,0.3)', paddingLeft: '6px' }}>
                ₹{subtotal.toFixed(0)}
              </span>
            )}
          </Link>
        )}

        {/* User Info & Logout */}
        <div style={{ display: 'flex', alignItems: 'center', gap: '12px' }}>
          <div style={{ textAlign: 'right', display: 'none', md: 'block' }}>
            <div style={{ fontSize: '0.86rem', fontWeight: 600 }}>{user?.displayName || 'User'}</div>
            <div style={{ fontSize: '0.72rem', color: 'var(--text-muted)' }}>{user?.email}</div>
          </div>

          <button
            onClick={() => {
              logout();
              navigate('/login');
            }}
            title="Sign out"
            className="btn btn-secondary btn-sm"
            style={{ padding: '8px' }}
          >
            <LogOut size={16} color="var(--text-muted)" />
          </button>
        </div>
      </div>
    </header>
  );
};
