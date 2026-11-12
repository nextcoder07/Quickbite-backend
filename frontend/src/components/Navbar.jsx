import React from 'react';
import { useAuth } from '../context/AuthContext';
import { useCart } from '../context/CartContext';
import { Link, useNavigate } from 'react-router-dom';
import {
  ShoppingBag,
  User,
  LogOut,
  Shield,
  Store,
  Bike,
  Sparkles,
  MapPin,
  Clock,
} from 'lucide-react';

export const Navbar = () => {
  const { user, account, role, policy, logout } = useAuth();
  const { itemCount, subtotal } = useCart();
  const navigate = useNavigate();

  const homeRoute = policy ? policy.getDefaultRoute(account) : '/';
  const isVerified = account ? account.isVerifiedForPlatform() : true;

  const getRoleBadge = () => {
    const badgeClass = policy ? policy.getBadgeColor() : 'badge-success';
    const displayName = policy ? policy.getDisplayName() : 'Buyer';
    let Icon = User;
    if (role === 'ADMIN') Icon = Shield;
    else if (role === 'SHOP_OWNER') Icon = Store;
    else if (role === 'DELIVERY_PARTNER') Icon = Bike;

    return (
      <span className={`badge ${badgeClass}`}>
        <Icon size={12} /> {displayName}
      </span>
    );
  };

  const handleLogout = async () => {
    await logout();
    navigate('/login', { replace: true });
  };

  return (
    <header
      style={{
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
      }}
    >
      {/* Brand */}
      <Link to={homeRoute} style={{ display: 'flex', alignItems: 'center', gap: '10px', textDecoration: 'none' }}>
        <div
          style={{
            width: '38px',
            height: '38px',
            borderRadius: '10px',
            background: 'linear-gradient(135deg, #FF6433 0%, #FF4500 100%)',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            boxShadow: '0 4px 12px rgba(255, 100, 51, 0.3)',
          }}
        >
          <Sparkles size={22} color="#fff" />
        </div>
        <div>
          <span style={{ fontSize: '1.25rem', fontWeight: 800, color: '#fff', letterSpacing: '-0.02em' }}>
            Quick<span style={{ color: '#FF6433' }}>Bite</span>
          </span>
          <span
            style={{
              fontSize: '0.68rem',
              display: 'block',
              color: 'var(--text-muted)',
              fontWeight: 600,
              letterSpacing: '0.05em',
            }}
          >
            ANYWHERE FOOD MARKETPLACE
          </span>
        </div>
      </Link>

      {/* Right Controls */}
      <div style={{ display: 'flex', alignItems: 'center', gap: '14px' }}>
        {role === 'BUYER' && (
          <Link
            to="/profile"
            className="btn btn-secondary btn-sm"
            title="Set your delivery address or GPS location point"
            style={{ display: 'flex', alignItems: 'center', gap: '6px', maxWidth: '240px' }}
          >
            <MapPin size={14} color="#FF6433" />
            <span style={{ overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap', fontSize: '0.78rem' }}>
              {account?.defaultAddress?.trim() || 'Set Delivery Location'}
            </span>
          </Link>
        )}

        <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
          {getRoleBadge()}
          {!isVerified && (
            <span className="badge badge-warning" title="Restricted to verification page until Admin approval">
              <Clock size={11} /> Pending Admin Verification
            </span>
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
              <span
                style={{
                  background: '#fff',
                  color: '#FF6433',
                  borderRadius: '12px',
                  padding: '1px 6px',
                  fontSize: '0.75rem',
                  fontWeight: 800,
                }}
              >
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

        {/* User Info & Sign Out */}
        <div style={{ display: 'flex', alignItems: 'center', gap: '10px', paddingLeft: '8px', borderLeft: '1px solid var(--border-subtle)' }}>
          <div style={{ textAlign: 'right' }}>
            <div style={{ fontSize: '0.84rem', fontWeight: 600 }}>{user?.displayName || 'User'}</div>
            <div style={{ fontSize: '0.7rem', color: 'var(--text-muted)' }}>{user?.email}</div>
          </div>

          <button
            onClick={handleLogout}
            title="Sign out"
            className="btn btn-secondary btn-sm"
            style={{ display: 'flex', alignItems: 'center', gap: '6px', padding: '7px 10px' }}
          >
            <LogOut size={15} color="var(--text-muted)" />
            <span style={{ fontSize: '0.78rem' }}>Logout</span>
          </button>
        </div>
      </div>
    </header>
  );
};
