import React, { useState } from 'react';
import { useAuth } from '../../context/AuthContext';
import { api } from '../../api/apiClient';
import { User, Mail, Phone, Shield, AlertTriangle, CheckCircle2 } from 'lucide-react';
import { StatusBadge } from '../../components/StatusBadge';

export const Profile = () => {
  const { user, updateProfile, logout } = useAuth();
  const [displayName, setDisplayName] = useState(user?.displayName || '');
  const [phone, setPhone] = useState(user?.phone || '');
  const [loading, setLoading] = useState(false);
  const [success, setSuccess] = useState('');
  const [error, setError] = useState('');

  const handleUpdate = async (e) => {
    e.preventDefault();
    setLoading(true);
    setError('');
    setSuccess('');

    try {
      const res = await api.patch('/account/profile', {
        displayName,
        phone,
      });
      updateProfile({ displayName: res.displayName, phone: res.phone });
      setSuccess('Profile updated successfully');
    } catch (err) {
      setError(err.message || 'Failed to update profile');
    } finally {
      setLoading(false);
    }
  };

  const handleDeactivate = async () => {
    const confirmDeactivate = window.confirm(
      'Are you sure you want to deactivate your buyer account? Your order, payment, and refund records will be safely preserved, but you will not be able to place new orders.'
    );
    if (!confirmDeactivate) return;

    try {
      await api.post('/account/deactivate');
      alert('Your account has been deactivated. Logging out now.');
      logout();
    } catch (err) {
      alert(err.message || 'Deactivation failed');
    }
  };

  return (
    <div style={{ maxWidth: '640px', margin: '0 auto', display: 'flex', flexDirection: 'column', gap: '24px' }}>
      <div>
        <h1 style={{ fontSize: '1.8rem', marginBottom: '4px' }}>Account Settings</h1>
        <p style={{ color: 'var(--text-muted)', fontSize: '0.9rem' }}>
          Manage your personal details and campus account preferences
        </p>
      </div>

      <div className="glass-panel" style={{ padding: '28px' }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: '16px', marginBottom: '24px', paddingBottom: '20px', borderBottom: '1px solid var(--border-subtle)' }}>
          <div style={{
            width: '60px',
            height: '60px',
            borderRadius: '16px',
            background: 'linear-gradient(135deg, #FF6433 0%, #FF4500 100%)',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            fontSize: '1.5rem',
            fontWeight: 800,
            color: '#fff'
          }}>
            {user?.displayName ? user.displayName.charAt(0).toUpperCase() : 'U'}
          </div>
          <div>
            <h2 style={{ fontSize: '1.3rem', color: '#fff' }}>{user?.displayName}</h2>
            <div style={{ display: 'flex', alignItems: 'center', gap: '10px', marginTop: '4px' }}>
              <span style={{ fontSize: '0.85rem', color: 'var(--text-muted)' }}>{user?.email}</span>
              <StatusBadge status={user?.status || 'ACTIVE'} />
            </div>
          </div>
        </div>

        {success && (
          <div style={{
            display: 'flex',
            alignItems: 'center',
            gap: '8px',
            padding: '10px 14px',
            borderRadius: '8px',
            background: 'var(--success-bg)',
            color: 'var(--success)',
            fontSize: '0.85rem',
            marginBottom: '16px'
          }}>
            <CheckCircle2 size={16} />
            <span>{success}</span>
          </div>
        )}

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

        <form onSubmit={handleUpdate}>
          <div className="form-group">
            <label className="form-label">Display Name</label>
            <input
              type="text"
              className="form-input"
              value={displayName}
              onChange={(e) => setDisplayName(e.target.value)}
              required
            />
          </div>

          <div className="form-group">
            <label className="form-label">Phone Number</label>
            <input
              type="tel"
              className="form-input"
              value={phone}
              onChange={(e) => setPhone(e.target.value)}
              placeholder="e.g. 9876543210"
            />
          </div>

          <div className="form-group">
            <label className="form-label">Account Identity Email (Non-editable)</label>
            <input
              type="email"
              className="form-input"
              value={user?.email || ''}
              disabled
              style={{ opacity: 0.6, cursor: 'not-allowed' }}
            />
          </div>

          <button
            type="submit"
            className="btn btn-primary"
            disabled={loading}
            style={{ marginTop: '8px' }}
          >
            {loading ? 'Saving...' : 'Save Changes'}
          </button>
        </form>
      </div>

      {/* Account Deactivation Card */}
      <div className="glass-panel" style={{
        padding: '24px 28px',
        border: '1px solid rgba(239, 68, 68, 0.2)',
        background: 'rgba(239, 68, 68, 0.04)'
      }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: '10px', color: '#f87171', marginBottom: '8px' }}>
          <AlertTriangle size={20} />
          <h3 style={{ fontSize: '1.1rem' }}>Danger Zone: Account Deactivation</h3>
        </div>
        <p style={{ color: 'var(--text-muted)', fontSize: '0.85rem', lineHeight: '1.5', marginBottom: '16px' }}>
          Under our data preservation policy, your financial, order, and dispute history will be retained, but your account will become inactive and you will not be able to place new orders.
        </p>
        <button
          onClick={handleDeactivate}
          className="btn btn-danger"
          style={{ fontSize: '0.85rem' }}
        >
          Deactivate My Account
        </button>
      </div>
    </div>
  );
};
