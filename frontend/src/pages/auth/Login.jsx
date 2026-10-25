import React, { useState } from 'react';
import { useAuth } from '../../context/AuthContext';
import { useNavigate, Link } from 'react-router-dom';
import { Sparkles, User, Store, Bike, Shield, ArrowRight } from 'lucide-react';

export const Login = () => {
  const { devQuickLogin } = useAuth();
  const navigate = useNavigate();
  const [email, setEmail] = useState('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  const handleLogin = async (e) => {
    e.preventDefault();
    if (!email) {
      setError('Please enter your campus email');
      return;
    }
    setLoading(true);
    setError('');
    try {
      await devQuickLogin(email);
      navigate('/');
    } catch (err) {
      setError(err.message || 'Login failed. Please verify credentials.');
    } finally {
      setLoading(false);
    }
  };

  const handleQuickRole = async (targetRole) => {
    setLoading(true);
    try {
      await devQuickLogin(targetRole);
      if (targetRole === 'SHOP_OWNER') navigate('/shop/dashboard');
      else if (targetRole === 'DELIVERY_PARTNER') navigate('/delivery/dashboard');
      else if (targetRole === 'ADMIN') navigate('/admin/dashboard');
      else navigate('/');
    } catch (err) {
      setError(err.message || 'Quick login failed');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div style={{
      minHeight: '100vh',
      display: 'flex',
      alignItems: 'center',
      justifyContent: 'center',
      padding: '24px',
      background: 'radial-gradient(circle at top right, #1f1a24 0%, #0c0d12 60%)'
    }}>
      <div className="glass-panel" style={{ width: '100%', maxWidth: '440px', padding: '36px' }}>
        {/* Brand header */}
        <div style={{ textAlign: 'center', marginBottom: '28px' }}>
          <div style={{
            width: '48px',
            height: '48px',
            borderRadius: '14px',
            background: 'linear-gradient(135deg, #FF6433 0%, #FF4500 100%)',
            display: 'inline-flex',
            alignItems: 'center',
            justifyContent: 'center',
            marginBottom: '12px',
            boxShadow: '0 6px 18px rgba(255, 100, 51, 0.35)'
          }}>
            <Sparkles size={28} color="#fff" />
          </div>
          <h2 style={{ fontSize: '1.7rem', color: '#fff', marginBottom: '6px' }}>Welcome to QuickBite</h2>
          <p style={{ color: 'var(--text-muted)', fontSize: '0.9rem' }}>Campus Food Marketplace & Ordering System</p>
        </div>

        {error && (
          <div style={{
            padding: '10px 14px',
            borderRadius: '8px',
            background: 'var(--danger-bg)',
            color: 'var(--danger)',
            fontSize: '0.85rem',
            marginBottom: '16px'
          }}>
            {error}
          </div>
        )}

        <form onSubmit={handleLogin}>
          <div className="form-group">
            <label className="form-label">Campus Email Address</label>
            <input
              type="email"
              className="form-input"
              placeholder="e.g. buyer@quickbite.campus"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
            />
          </div>

          <button
            type="submit"
            className="btn btn-primary"
            disabled={loading}
            style={{ width: '100%', padding: '12px', marginTop: '8px' }}
          >
            {loading ? 'Authenticating...' : 'Sign In'} <ArrowRight size={16} />
          </button>
        </form>

        <div style={{
          marginTop: '28px',
          paddingTop: '20px',
          borderTop: '1px solid var(--border-subtle)'
        }}>
          <p style={{ fontSize: '0.78rem', color: 'var(--text-faint)', fontWeight: 700, textAlign: 'center', marginBottom: '12px' }}>
            DEMO ACCOUNT QUICK ACCESS
          </p>
          <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '8px' }}>
            <button
              onClick={() => handleQuickRole('BUYER')}
              type="button"
              className="btn btn-secondary btn-sm"
              style={{ justifyContent: 'center' }}
            >
              <User size={14} color="#10B981" /> Buyer
            </button>
            <button
              onClick={() => handleQuickRole('SHOP_OWNER')}
              type="button"
              className="btn btn-secondary btn-sm"
              style={{ justifyContent: 'center' }}
            >
              <Store size={14} color="#F59E0B" /> Shop Owner
            </button>
            <button
              onClick={() => handleQuickRole('DELIVERY_PARTNER')}
              type="button"
              className="btn btn-secondary btn-sm"
              style={{ justifyContent: 'center' }}
            >
              <Bike size={14} color="#3B82F6" /> Delivery
            </button>
            <button
              onClick={() => handleQuickRole('ADMIN')}
              type="button"
              className="btn btn-secondary btn-sm"
              style={{ justifyContent: 'center' }}
            >
              <Shield size={14} color="#A855F7" /> Admin
            </button>
          </div>
        </div>

        <div style={{ marginTop: '24px', textAlign: 'center', fontSize: '0.85rem', color: 'var(--text-muted)' }}>
          Don't have an account yet?{' '}
          <Link to="/signup" style={{ color: 'var(--primary)', fontWeight: 600, textDecoration: 'none' }}>
            Sign up here
          </Link>
        </div>
      </div>
    </div>
  );
};
