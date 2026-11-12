import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../../context/AuthContext';
import { api } from '../../api/apiClient';
import { AlertTriangle, CheckCircle2, Shield } from 'lucide-react';
import { StatusBadge } from '../../components/StatusBadge';
import { LocationPicker } from '../../components/LocationPicker';
import { FoodInterestSelector } from '../../components/FoodInterestSelector';
import { useDialog } from '../../context/DialogContext';

export const Profile = () => {
  const dialog = useDialog();
  const { user, account, updateProfile, refreshAccount, logout } = useAuth();
  const navigate = useNavigate();
  const [displayName, setDisplayName] = useState(user?.displayName || '');
  const [phone, setPhone] = useState(user?.phone || '');
  const [foodInterests, setFoodInterests] = useState(account?.foodInterests || user?.foodInterests || '');
  const [defaultLocation, setDefaultLocation] = useState({
    address: account?.defaultAddress || '',
    latitude: account?.defaultLatitude ?? null,
    longitude: account?.defaultLongitude ?? null,
  });
  const [loading, setLoading] = useState(false);
  const [success, setSuccess] = useState('');
  const [error, setError] = useState('');
  const [adminRequests, setAdminRequests] = useState([]);
  const [adminReason, setAdminReason] = useState('');
  const [requestingAdmin, setRequestingAdmin] = useState(false);

  useEffect(() => {
    api.get('/account/admin-request')
      .then((data) => setAdminRequests(Array.isArray(data) ? data : []))
      .catch((err) => setError(err.message || 'Failed to load admin permission requests'));
  }, []);

  const handleUpdate = async (e) => {
    e.preventDefault();
    setLoading(true);
    setError('');
    setSuccess('');

    try {
      const res = await api.patch('/account/profile', {
        displayName,
        phone,
        defaultAddress: defaultLocation.address ? defaultLocation.address.trim() : null,
        defaultLatitude: defaultLocation.latitude,
        defaultLongitude: defaultLocation.longitude,
        foodInterests: foodInterests.trim(),
      });
      updateProfile({
        displayName: res.displayName,
        phone: res.phone,
        defaultAddress: res.defaultAddress,
        defaultLatitude: res.defaultLatitude,
        defaultLongitude: res.defaultLongitude,
        foodInterests: res.foodInterests,
      });
      setSuccess('Profile, delivery location, and food interests updated successfully');
    } catch (err) {
      setError(err.message || 'Failed to update profile');
    } finally {
      setLoading(false);
    }
  };

  const handleDeactivate = async () => {
    const confirmDeactivate = await dialog.confirm({
      title: 'Deactivate Buyer Account?',
      message:
        'Are you sure you want to deactivate your buyer account? Your order, payment, and refund records will be safely preserved, but you will not be able to place new orders.',
      confirmText: 'Deactivate Account',
      danger: true,
    });
    if (!confirmDeactivate) return;

    try {
      await api.post('/account/deactivate');
      await dialog.alert({ title: 'Account Deactivated', message: 'Your account has been deactivated. Logging out now.', tone: 'success' });
      await logout();
      navigate('/login', { replace: true });
    } catch (err) {
      await dialog.alert({ title: 'Deactivation Failed', message: err.message || 'Deactivation failed' });
    }
  };

  const handleRequestAdminRole = async (e) => {
    e.preventDefault();
    setRequestingAdmin(true);
    setError('');
    setSuccess('');
    try {
      const created = await api.post('/account/admin-request', { reason: adminReason.trim() });
      setAdminRequests((prev) => [created, ...prev]);
      setAdminReason('');
      await refreshAccount();
      setSuccess('Admin permission request sent. An active Admin can approve your request.');
    } catch (err) {
      setError(err.message || 'Failed to submit Admin role request');
    } finally {
      setRequestingAdmin(false);
    }
  };

  const hasPendingAdminRequest = adminRequests.some((r) => r.status === 'PENDING');

  return (
    <div style={{ maxWidth: '640px', margin: '0 auto', display: 'flex', flexDirection: 'column', gap: '24px' }}>
      <div>
        <h1 style={{ fontSize: '1.8rem', marginBottom: '4px' }}>Account Settings</h1>
        <p style={{ color: 'var(--text-muted)', fontSize: '0.9rem' }}>
          Manage your personal details, saved delivery location, and account preferences
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
            <LocationPicker
              label="Default Delivery Location & GPS Point"
              placeholder="House/Flat, Street, Area, Landmark, City..."
              address={defaultLocation.address}
              latitude={defaultLocation.latitude}
              longitude={defaultLocation.longitude}
              onChange={setDefaultLocation}
              hint="Pre-filled automatically during checkout; you can still change it per order."
            />
          </div>

          <div className="form-group">
            <FoodInterestSelector
              label="Food Interests & Preferred Dishes"
              value={foodInterests}
              onChange={setFoodInterests}
              hint="Used alongside your past orders to rank dishes on 'For You • Top Items'. Leave blank to receive global cold-start top-rated picks."
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

      {/* Request Admin Permission Card */}
      <div className="glass-panel" style={{
        padding: '24px 28px',
        border: '1px solid rgba(168, 85, 247, 0.25)',
        background: 'rgba(168, 85, 247, 0.05)'
      }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: '10px', color: '#c084fc', marginBottom: '8px' }}>
          <Shield size={20} />
          <h3 style={{ fontSize: '1.1rem' }}>Request Admin Permission</h3>
        </div>
        <p style={{ color: 'var(--text-muted)', fontSize: '0.85rem', lineHeight: '1.5', marginBottom: '14px' }}>
          Ask existing Platform Admins for permission to become an Admin on QuickBite.
        </p>

        {hasPendingAdminRequest ? (
          <div style={{ padding: '10px 14px', borderRadius: '8px', background: 'rgba(245, 158, 11, 0.12)', color: '#fbbf24', fontSize: '0.85rem' }}>
            Your Admin permission request is currently pending review.
          </div>
        ) : (
          <form onSubmit={handleRequestAdminRole} style={{ display: 'flex', gap: '10px', flexWrap: 'wrap' }}>
            <input
              type="text"
              className="form-input"
              style={{ flex: '1 1 260px' }}
              placeholder="Reason or role on team (optional)..."
              value={adminReason}
              onChange={(e) => setAdminReason(e.target.value)}
            />
            <button type="submit" className="btn btn-secondary" disabled={requestingAdmin}>
              {requestingAdmin ? 'Submitting...' : 'Ask Admin Permission'}
            </button>
          </form>
        )}

        {adminRequests.length > 0 && (
          <div style={{ marginTop: '14px', display: 'flex', flexDirection: 'column', gap: '8px' }}>
            {adminRequests.map((req, idx) => (
              <div
                key={req.id || idx}
                style={{
                  display: 'flex',
                  justifyContent: 'space-between',
                  alignItems: 'center',
                  padding: '8px 12px',
                  borderRadius: '8px',
                  background: 'rgba(15, 23, 42, 0.45)',
                  fontSize: '0.82rem',
                }}
              >
                <span>{req.reason || 'Admin permission request'}</span>
                <StatusBadge status={req.status} />
              </div>
            ))}
          </div>
        )}
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
