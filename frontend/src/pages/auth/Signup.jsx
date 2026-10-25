import React, { useState } from 'react';
import { useAuth } from '../../context/AuthContext';
import { useNavigate, Link } from 'react-router-dom';
import { api } from '../../api/apiClient';
import { User, Store, Bike, Sparkles, ArrowRight, ShieldAlert } from 'lucide-react';

export const Signup = () => {
  const { login } = useAuth();
  const navigate = useNavigate();

  const [accountType, setAccountType] = useState('BUYER');
  const [displayName, setDisplayName] = useState('');
  const [email, setEmail] = useState('');
  const [phone, setPhone] = useState('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  const handleSignup = async (e) => {
    e.preventDefault();
    if (!displayName || !email) {
      setError('Name and Email are required');
      return;
    }

    setLoading(true);
    setError('');

    try {
      const payload = {
        firebaseUid: `uid-${Date.now().toString(36)}`,
        email: email.trim(),
        displayName: displayName.trim(),
        phone: phone ? phone.trim() : null,
        accountType: accountType,
      };

      const res = await api.post('/auth/signup', payload);
      login(res);

      if (accountType === 'SHOP_OWNER') navigate('/shop/dashboard');
      else if (accountType === 'DELIVERY_PARTNER') navigate('/delivery/dashboard');
      else navigate('/');
    } catch (err) {
      setError(err.message || 'Registration failed');
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
      background: 'radial-gradient(circle at top left, #1f1a24 0%, #0c0d12 60%)'
    }}>
      <div className="glass-panel" style={{ width: '100%', maxWidth: '480px', padding: '36px' }}>
        <div style={{ textAlign: 'center', marginBottom: '24px' }}>
          <div style={{
            width: '44px',
            height: '44px',
            borderRadius: '12px',
            background: 'linear-gradient(135deg, #FF6433 0%, #FF4500 100%)',
            display: 'inline-flex',
            alignItems: 'center',
            justifyContent: 'center',
            marginBottom: '10px'
          }}>
            <Sparkles size={24} color="#fff" />
          </div>
          <h2 style={{ fontSize: '1.6rem', color: '#fff', marginBottom: '4px' }}>Create Campus Account</h2>
          <p style={{ color: 'var(--text-muted)', fontSize: '0.85rem' }}>Join the QuickBite campus food marketplace</p>
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

        <form onSubmit={handleSignup}>
          {/* Account Profile Selection */}
          <div style={{ marginBottom: '18px' }}>
            <label className="form-label" style={{ marginBottom: '8px', display: 'block' }}>
              CHOOSE ACCOUNT PROFILE
            </label>
            <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr 1fr', gap: '8px' }}>
              <div
                onClick={() => setAccountType('BUYER')}
                style={{
                  padding: '12px 8px',
                  borderRadius: '10px',
                  textAlign: 'center',
                  border: `1px solid ${accountType === 'BUYER' ? 'var(--primary)' : 'var(--border-subtle)'}`,
                  background: accountType === 'BUYER' ? 'var(--primary-subtle)' : 'var(--bg-surface)',
                  cursor: 'pointer',
                  transition: 'all 0.15s ease'
                }}
              >
                <User size={18} color={accountType === 'BUYER' ? 'var(--primary)' : 'var(--text-muted)'} style={{ margin: '0 auto 4px' }} />
                <div style={{ fontSize: '0.8rem', fontWeight: 600 }}>Buyer</div>
              </div>

              <div
                onClick={() => setAccountType('SHOP_OWNER')}
                style={{
                  padding: '12px 8px',
                  borderRadius: '10px',
                  textAlign: 'center',
                  border: `1px solid ${accountType === 'SHOP_OWNER' ? 'var(--primary)' : 'var(--border-subtle)'}`,
                  background: accountType === 'SHOP_OWNER' ? 'var(--primary-subtle)' : 'var(--bg-surface)',
                  cursor: 'pointer',
                  transition: 'all 0.15s ease'
                }}
              >
                <Store size={18} color={accountType === 'SHOP_OWNER' ? 'var(--primary)' : 'var(--text-muted)'} style={{ margin: '0 auto 4px' }} />
                <div style={{ fontSize: '0.8rem', fontWeight: 600 }}>Shop Owner</div>
              </div>

              <div
                onClick={() => setAccountType('DELIVERY_PARTNER')}
                style={{
                  padding: '12px 8px',
                  borderRadius: '10px',
                  textAlign: 'center',
                  border: `1px solid ${accountType === 'DELIVERY_PARTNER' ? 'var(--primary)' : 'var(--border-subtle)'}`,
                  background: accountType === 'DELIVERY_PARTNER' ? 'var(--primary-subtle)' : 'var(--bg-surface)',
                  cursor: 'pointer',
                  transition: 'all 0.15s ease'
                }}
              >
                <Bike size={18} color={accountType === 'DELIVERY_PARTNER' ? 'var(--primary)' : 'var(--text-muted)'} style={{ margin: '0 auto 4px' }} />
                <div style={{ fontSize: '0.8rem', fontWeight: 600 }}>Delivery</div>
              </div>
            </div>
            <div style={{ display: 'flex', alignItems: 'center', gap: '6px', marginTop: '8px', color: 'var(--text-faint)', fontSize: '0.74rem' }}>
              <ShieldAlert size={12} />
              <span>Admin accounts are strictly provisioned by campus operations.</span>
            </div>
          </div>

          <div className="form-group">
            <label className="form-label">Full Name</label>
            <input
              type="text"
              className="form-input"
              placeholder="e.g. Aarav Sharma"
              value={displayName}
              onChange={(e) => setDisplayName(e.target.value)}
              required
            />
          </div>

          <div className="form-group">
            <label className="form-label">Campus Email</label>
            <input
              type="email"
              className="form-input"
              placeholder="e.g. aarav@campus.edu"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              required
            />
          </div>

          <div className="form-group">
            <label className="form-label">Phone Number (Optional)</label>
            <input
              type="tel"
              className="form-input"
              placeholder="e.g. 9876543210"
              value={phone}
              onChange={(e) => setPhone(e.target.value)}
            />
          </div>

          <button
            type="submit"
            className="btn btn-primary"
            disabled={loading}
            style={{ width: '100%', padding: '12px', marginTop: '12px' }}
          >
            {loading ? 'Creating Profile...' : 'Complete Registration'} <ArrowRight size={16} />
          </button>
        </form>

        <div style={{ marginTop: '22px', textAlign: 'center', fontSize: '0.85rem', color: 'var(--text-muted)' }}>
          Already have an account?{' '}
          <Link to="/login" style={{ color: 'var(--primary)', fontWeight: 600, textDecoration: 'none' }}>
            Sign in
          </Link>
        </div>
      </div>
    </div>
  );
};
