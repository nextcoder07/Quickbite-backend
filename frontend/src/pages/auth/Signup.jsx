import React, { useState } from 'react';
import { useAuth } from '../../context/AuthContext';
import { useNavigate, Link } from 'react-router-dom';
import { authService } from '../../services/AuthService';
import { RolePolicyFactory } from '../../policies/RolePolicy';
import { LocationPicker } from '../../components/LocationPicker';
import { FoodInterestSelector } from '../../components/FoodInterestSelector';
import { User, Store, Bike, Shield, Sparkles, ArrowRight, ShieldAlert, KeyRound } from 'lucide-react';

export const Signup = () => {
  const { login } = useAuth();
  const navigate = useNavigate();

  const [accountType, setAccountType] = useState('BUYER');
  const [displayName, setDisplayName] = useState('');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [phone, setPhone] = useState('');
  const [vehicleDetails, setVehicleDetails] = useState('');
  const [serviceArea, setServiceArea] = useState('');
  const [foodInterests, setFoodInterests] = useState('');
  const [adminKey, setAdminKey] = useState('');
  const [adminRequestNote, setAdminRequestNote] = useState('');
  const [buyerLocation, setBuyerLocation] = useState({ address: '', latitude: null, longitude: null });
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  const handleSignup = async (e) => {
    e.preventDefault();
    if (!displayName.trim() || !email.trim() || !password) {
      setError('Full Name, Email, and Password are required.');
      return;
    }
    if (password.length < 6) {
      setError('Password must be at least 6 characters.');
      return;
    }
    if (password !== confirmPassword) {
      setError('Passwords do not match.');
      return;
    }

    setLoading(true);
    setError('');

    try {
      const payload = {
        email: email.trim(),
        password,
        displayName: displayName.trim(),
        phone: phone ? phone.trim() : null,
        accountType,
        defaultAddress: accountType === 'BUYER' ? buyerLocation.address?.trim() || null : null,
        defaultLatitude: accountType === 'BUYER' ? buyerLocation.latitude : null,
        defaultLongitude: accountType === 'BUYER' ? buyerLocation.longitude : null,
        foodInterests: accountType === 'BUYER' && foodInterests.trim() ? foodInterests.trim() : null,
        vehicleDetails: accountType === 'DELIVERY_PARTNER' ? vehicleDetails.trim() || null : null,
        serviceArea: accountType === 'DELIVERY_PARTNER' ? serviceArea.trim() || null : null,
        adminKey: accountType === 'ADMIN' && adminKey.trim() ? adminKey.trim() : null,
        adminRequestNote: accountType === 'ADMIN' && adminRequestNote.trim() ? adminRequestNote.trim() : null,
      };

      const registeredAccount = await authService.signup(payload);
      const storedAccount = login(registeredAccount);

      const policy = RolePolicyFactory.getPolicy(accountType);
      navigate(policy.getDefaultRoute(storedAccount), { replace: true });
    } catch (err) {
      setError(err.message || 'Registration failed');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div
      style={{
        minHeight: '100vh',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        padding: '24px',
        background: 'radial-gradient(circle at top left, #1f1a24 0%, #0c0d12 60%)',
      }}
    >
      <div className="glass-panel" style={{ width: '100%', maxWidth: '560px', padding: '36px' }}>
        <div style={{ textAlign: 'center', marginBottom: '22px' }}>
          <div
            style={{
              width: '44px',
              height: '44px',
              borderRadius: '12px',
              background: 'linear-gradient(135deg, #FF6433 0%, #FF4500 100%)',
              display: 'inline-flex',
              alignItems: 'center',
              justifyContent: 'center',
              marginBottom: '10px',
            }}
          >
            <Sparkles size={24} color="#fff" />
          </div>
          <h2 style={{ fontSize: '1.6rem', color: '#fff', marginBottom: '4px' }}>Create QuickBite Account</h2>
          <p style={{ color: 'var(--text-muted)', fontSize: '0.85rem' }}>
            Open to anyone, anywhere — buyers, restaurants, delivery partners, and platform admins
          </p>
        </div>

        {error && (
          <div
            style={{
              padding: '10px 14px',
              borderRadius: '8px',
              background: 'var(--danger-bg)',
              color: 'var(--danger)',
              fontSize: '0.85rem',
              marginBottom: '16px',
            }}
          >
            {error}
          </div>
        )}

        <form onSubmit={handleSignup}>
          <div style={{ marginBottom: '18px' }}>
            <label className="form-label" style={{ marginBottom: '8px', display: 'block' }}>
              CHOOSE ACCOUNT PROFILE
            </label>
            <div style={{ display: 'grid', gridTemplateColumns: 'repeat(4, 1fr)', gap: '8px' }}>
              {[
                { type: 'BUYER', label: 'Buyer', Icon: User },
                { type: 'SHOP_OWNER', label: 'Shop Owner', Icon: Store },
                { type: 'DELIVERY_PARTNER', label: 'Delivery', Icon: Bike },
                { type: 'ADMIN', label: 'Admin', Icon: Shield },
              ].map(({ type, label, Icon }) => (
                <div
                  key={type}
                  onClick={() => setAccountType(type)}
                  style={{
                    padding: '12px 6px',
                    borderRadius: '10px',
                    textAlign: 'center',
                    border: `1px solid ${accountType === type ? 'var(--primary)' : 'var(--border-subtle)'}`,
                    background: accountType === type ? 'var(--primary-subtle)' : 'var(--bg-surface)',
                    cursor: 'pointer',
                    transition: 'all 0.15s ease',
                  }}
                >
                  <Icon
                    size={18}
                    color={accountType === type ? 'var(--primary)' : 'var(--text-muted)'}
                    style={{ margin: '0 auto 4px' }}
                  />
                  <div style={{ fontSize: '0.78rem', fontWeight: 600 }}>{label}</div>
                </div>
              ))}
            </div>

            {(accountType === 'SHOP_OWNER' || accountType === 'DELIVERY_PARTNER') && (
              <div
                style={{
                  display: 'flex',
                  alignItems: 'center',
                  gap: '8px',
                  marginTop: '10px',
                  padding: '8px 10px',
                  borderRadius: '8px',
                  backgroundColor: 'rgba(245, 158, 11, 0.1)',
                  border: '1px solid rgba(245, 158, 11, 0.25)',
                  color: '#fbbf24',
                  fontSize: '0.78rem',
                }}
              >
                <ShieldAlert size={14} style={{ flexShrink: 0 }} />
                <span>
                  After signing up, you will be taken to the verification page. Other platform pages stay locked until an Admin verifies your account.
                </span>
              </div>
            )}

            {accountType === 'ADMIN' && (
              <div
                style={{
                  display: 'flex',
                  alignItems: 'center',
                  gap: '8px',
                  marginTop: '10px',
                  padding: '8px 10px',
                  borderRadius: '8px',
                  backgroundColor: 'rgba(168, 85, 247, 0.12)',
                  border: '1px solid rgba(168, 85, 247, 0.3)',
                  color: '#d8b4fe',
                  fontSize: '0.78rem',
                }}
              >
                <KeyRound size={14} style={{ flexShrink: 0 }} />
                <span>
                  Enter the environment Admin Security Key for instant Admin activation, or leave it blank to request permission from existing Admins.
                </span>
              </div>
            )}
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
            <label className="form-label">Email Address</label>
            <input
              type="email"
              className="form-input"
              placeholder="e.g. name@example.com"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              required
            />
          </div>

          <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '10px' }}>
            <div className="form-group">
              <label className="form-label">Password</label>
              <input
                type="password"
                className="form-input"
                placeholder="Min 6 characters"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                required
              />
            </div>
            <div className="form-group">
              <label className="form-label">Confirm Password</label>
              <input
                type="password"
                className="form-input"
                placeholder="Re-enter password"
                value={confirmPassword}
                onChange={(e) => setConfirmPassword(e.target.value)}
                required
              />
            </div>
          </div>

          {accountType !== 'ADMIN' && (
            <div className="form-group">
              <label className="form-label">Phone Number (Optional)</label>
              <input
                type="tel"
                className="form-input"
                placeholder="e.g. +91 9876543210"
                value={phone}
                onChange={(e) => setPhone(e.target.value)}
              />
            </div>
          )}

          {accountType === 'BUYER' && (
            <>
              <div className="form-group">
                <LocationPicker
                  label="Default Delivery Address (Optional)"
                  placeholder="Any home, office, street, or city address..."
                  address={buyerLocation.address}
                  latitude={buyerLocation.latitude}
                  longitude={buyerLocation.longitude}
                  onChange={setBuyerLocation}
                  hint="You can order from anywhere and adjust address per order at checkout."
                />
              </div>

              <div className="form-group">
                <FoodInterestSelector
                  label="Food Interests (Optional)"
                  value={foodInterests}
                  onChange={setFoodInterests}
                  hint="Skip or pick cuisines/dishes — if left blank, QuickBite automatically applies our cold-start algorithm for fresh buyers."
                />
              </div>
            </>
          )}

          {accountType === 'DELIVERY_PARTNER' && (
            <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '10px' }}>
              <div className="form-group">
                <label className="form-label">Vehicle Details</label>
                <input
                  type="text"
                  className="form-input"
                  placeholder="e.g. Honda Activa / E-Bike"
                  value={vehicleDetails}
                  onChange={(e) => setVehicleDetails(e.target.value)}
                />
              </div>
              <div className="form-group">
                <label className="form-label">Service City / Area</label>
                <input
                  type="text"
                  className="form-input"
                  placeholder="e.g. Indiranagar / Anywhere"
                  value={serviceArea}
                  onChange={(e) => setServiceArea(e.target.value)}
                />
              </div>
            </div>
          )}

          {accountType === 'ADMIN' && (
            <>
              <div className="form-group">
                <label className="form-label" style={{ display: 'flex', alignItems: 'center', gap: '6px' }}>
                  <KeyRound size={13} /> Admin Security Key (Optional — leave blank to request approval)
                </label>
                <input
                  type="password"
                  className="form-input"
                  placeholder="Enter ADMIN_SIGNUP_KEY for instant access, or leave blank"
                  value={adminKey}
                  onChange={(e) => setAdminKey(e.target.value)}
                />
              </div>
              {!adminKey.trim() && (
                <div className="form-group">
                  <label className="form-label">Why are you requesting Admin permission? (Optional)</label>
                  <input
                    type="text"
                    className="form-input"
                    placeholder="e.g. Operations lead joining moderation team"
                    value={adminRequestNote}
                    onChange={(e) => setAdminRequestNote(e.target.value)}
                  />
                </div>
              )}
            </>
          )}

          <button
            type="submit"
            className="btn btn-primary"
            disabled={loading}
            style={{ width: '100%', padding: '12px', marginTop: '10px' }}
          >
            {loading ? 'Creating Account...' : 'Create Account'} <ArrowRight size={16} />
          </button>
        </form>

        <div style={{ marginTop: '20px', textAlign: 'center', fontSize: '0.85rem', color: 'var(--text-muted)' }}>
          Already have an account?{' '}
          <Link to="/login" style={{ color: 'var(--primary)', fontWeight: 600, textDecoration: 'none' }}>
            Sign in
          </Link>
        </div>
      </div>
    </div>
  );
};
