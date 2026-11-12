import React, { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../../context/AuthContext';
import { api } from '../../api/apiClient';
import { shopOwnerService } from '../../services/ShopOwnerService';
import { deliveryService } from '../../services/DeliveryService';
import { StatusBadge } from '../../components/StatusBadge';
import { LocationPicker } from '../../components/LocationPicker';
import {
  ShieldAlert,
  Store,
  Truck,
  Shield,
  RefreshCw,
  LogOut,
  CheckCircle2,
  Trash2,
  Clock,
} from 'lucide-react';

/**
 * Mandatory Onboarding & Admin Verification Gate Page.
 * Shop Owners, Delivery Partners, and unapproved Admins are confined strictly to this page until
 * an Administrator approves their profile.
 * Platform removal requests also require explicit Admin confirmation.
 */
export const VerificationGate = () => {
  const { account, refreshAccount, logout, policy } = useAuth();
  const navigate = useNavigate();

  const isShopOwner = account?.accountType === 'SHOP_OWNER';
  const isDeliveryPartner = account?.accountType === 'DELIVERY_PARTNER';
  const isAdmin = account?.accountType === 'ADMIN';

  const [shop, setShop] = useState(null);
  const [deletionRequests, setDeletionRequests] = useState([]);
  const [loading, setLoading] = useState(true);
  const [checking, setChecking] = useState(false);
  const [error, setError] = useState('');
  const [feedback, setFeedback] = useState('');
  const [adminNote, setAdminNote] = useState('');
  const [requestingAdmin, setRequestingAdmin] = useState(false);

  // Shop creation form (for newly registered shop owners without a shop)
  const [shopName, setShopName] = useState('');
  const [shopDescription, setShopDescription] = useState('');
  const [shopLocation, setShopLocation] = useState({
    address: '',
    latitude: null,
    longitude: null,
  });
  const [creatingShop, setCreatingShop] = useState(false);

  // Removal request form (requires admin confirmation)
  const [removalReason, setRemovalReason] = useState('');
  const [submittingRemoval, setSubmittingRemoval] = useState(false);

  const loadGateData = async () => {
    setLoading(true);
    setError('');
    try {
      const [reqs, fetchedShop] = await Promise.all([
        deliveryService.getMyDeletionRequests().catch(() => []),
        isShopOwner ? shopOwnerService.getMyShop().catch(() => null) : Promise.resolve(null),
      ]);
      setDeletionRequests(Array.isArray(reqs) ? reqs : []);
      setShop(fetchedShop);
    } catch (err) {
      setError(err.message || 'Could not load verification status');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadGateData();
  }, [account?.accountType]);

  const handleRefreshVerification = async () => {
    setChecking(true);
    setError('');
    setFeedback('');
    try {
      const updated = await refreshAccount();
      await loadGateData();
      if (updated?.isVerifiedForPlatform()) {
        setFeedback('Your account is verified by Admin! Redirecting to your workspace...');
        navigate(policy.getDefaultRoute(updated), { replace: true });
      } else {
        setFeedback('Status checked. Still waiting for Admin verification.');
      }
    } catch (err) {
      setError(err.message || 'Unable to refresh verification status');
    } finally {
      setChecking(false);
    }
  };

  const handleCreateShop = async (e) => {
    e.preventDefault();
    if (!shopName.trim() || !shopLocation.address.trim()) {
      setError('Shop name and location address are required.');
      return;
    }

    setCreatingShop(true);
    setError('');
    setFeedback('');
    try {
      const created = await shopOwnerService.createShop({
        name: shopName.trim(),
        description: shopDescription.trim(),
        location: shopLocation.address.trim(),
        latitude: shopLocation.latitude,
        longitude: shopLocation.longitude,
      });
      setShop(created);
      await refreshAccount();
      setFeedback('Shop submitted! It is now awaiting Admin verification before going live.');
    } catch (err) {
      setError(err.message || 'Failed to submit shop registration');
    } finally {
      setCreatingShop(false);
    }
  };

  const handleSubmitRemoval = async (e) => {
    e.preventDefault();
    if (!removalReason.trim()) return;
    setSubmittingRemoval(true);
    setError('');
    setFeedback('');
    try {
      await deliveryService.submitDeletionRequest(removalReason.trim());
      setRemovalReason('');
      await refreshAccount();
      await loadGateData();
      setFeedback('Removal request sent. An Administrator must confirm before your account is removed.');
    } catch (err) {
      setError(err.message || 'Failed to submit removal request');
    } finally {
      setSubmittingRemoval(false);
    }
  };

  const handleLogout = async () => {
    await logout();
    navigate('/login', { replace: true });
  };

  const handleRequestAdminApproval = async (e) => {
    e.preventDefault();
    setRequestingAdmin(true);
    setError('');
    setFeedback('');
    try {
      await api.post('/account/admin-request', { reason: adminNote.trim() });
      setAdminNote('');
      await refreshAccount();
      setFeedback('Admin permission request sent. Existing Admins can now review and approve your access.');
    } catch (err) {
      setError(err.message || 'Could not submit admin permission request');
    } finally {
      setRequestingAdmin(false);
    }
  };

  const verified = account?.isVerifiedForPlatform();
  const hasPendingDeletion =
    account?.hasPendingDeletion() || deletionRequests.some((r) => r.status === 'PENDING');

  return (
    <div style={{ maxWidth: '820px', margin: '0 auto', display: 'flex', flexDirection: 'column', gap: '20px' }}>
      <div className="glass-panel" style={{ padding: '24px' }}>
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', gap: '16px', flexWrap: 'wrap' }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: '14px' }}>
            <div
              style={{
                width: '48px',
                height: '48px',
                borderRadius: '14px',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                background: verified ? 'rgba(16, 185, 129, 0.15)' : 'rgba(245, 158, 11, 0.15)',
                color: verified ? '#34d399' : '#fbbf24',
              }}
            >
              {verified ? <CheckCircle2 size={26} /> : <ShieldAlert size={26} />}
            </div>
            <div>
              <h1 style={{ fontSize: '1.5rem', marginBottom: '4px' }}>
                {isShopOwner
                  ? 'Shop Onboarding & Verification'
                  : isAdmin
                    ? 'Admin Permission & Verification'
                    : 'Delivery Partner Verification'}
              </h1>
              <p style={{ color: 'var(--text-secondary)', fontSize: '0.9rem' }}>
                Signed in as <strong>{account?.displayName}</strong> ({account?.email})
              </p>
            </div>
          </div>

          <div style={{ display: 'flex', gap: '10px', flexWrap: 'wrap' }}>
            <button
              type="button"
              className="btn btn-secondary"
              onClick={handleRefreshVerification}
              disabled={checking}
            >
              <RefreshCw size={15} />
              {checking ? 'Checking...' : 'Check Status'}
            </button>
            <button type="button" className="btn btn-danger" onClick={handleLogout}>
              <LogOut size={15} />
              Sign Out
            </button>
          </div>
        </div>

        {error && (
          <div style={{ marginTop: '14px', padding: '12px', borderRadius: '10px', backgroundColor: 'rgba(239, 68, 68, 0.12)', border: '1px solid rgba(239, 68, 68, 0.3)', color: '#f87171', fontSize: '0.88rem' }}>
            {error}
          </div>
        )}
        {feedback && (
          <div style={{ marginTop: '14px', padding: '12px', borderRadius: '10px', backgroundColor: 'rgba(16, 185, 129, 0.12)', border: '1px solid rgba(16, 185, 129, 0.3)', color: '#34d399', fontSize: '0.88rem' }}>
            {feedback}
          </div>
        )}

        {verified && (
          <div style={{ marginTop: '18px', padding: '16px', borderRadius: '12px', backgroundColor: 'rgba(16, 185, 129, 0.1)', border: '1px solid rgba(16, 185, 129, 0.3)', display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: '12px' }}>
            <div>
              <div style={{ fontWeight: 700, color: '#34d399' }}>Verified & Active on QuickBite</div>
              <div style={{ fontSize: '0.85rem', color: 'var(--text-secondary)' }}>
                Your account has full platform access.
              </div>
            </div>
            <button
              type="button"
              className="btn btn-primary"
              onClick={() => navigate(policy.getDefaultRoute(account))}
            >
              Open Workspace
            </button>
          </div>
        )}
      </div>

      {loading ? (
        <div className="glass-panel" style={{ padding: '24px', color: 'var(--text-secondary)' }}>
          Loading verification details...
        </div>
      ) : isShopOwner ? (
        <div className="glass-panel" style={{ padding: '24px' }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: '10px', marginBottom: '14px' }}>
            <Store size={20} color="var(--brand-primary)" />
            <h2 style={{ fontSize: '1.15rem' }}>
              {shop ? 'Your Shop Verification Status' : 'Register Your Shop for Admin Review'}
            </h2>
          </div>

          {!shop ? (
            <form onSubmit={handleCreateShop} style={{ display: 'flex', flexDirection: 'column', gap: '14px' }}>
              <p style={{ fontSize: '0.88rem', color: 'var(--text-secondary)' }}>
                Complete your shop profile with its real-world location and optional GPS coordinates. Other platform tabs remain locked until an Admin approves your shop.
              </p>

              <div className="form-group">
                <label className="form-label">Shop / Kitchen Name</label>
                <input
                  type="text"
                  className="form-input"
                  placeholder="e.g. Bella Spice Kitchen"
                  value={shopName}
                  onChange={(e) => setShopName(e.target.value)}
                  required
                />
              </div>

              <div className="form-group">
                <label className="form-label">Cuisine / Description</label>
                <textarea
                  className="form-input"
                  rows="2"
                  placeholder="Specialty dishes, hours, notes..."
                  value={shopDescription}
                  onChange={(e) => setShopDescription(e.target.value)}
                />
              </div>

              <LocationPicker
                label="Shop Location & Optional GPS Point"
                placeholder="e.g. 42 MG Road, Indiranagar, Bengaluru"
                address={shopLocation.address}
                latitude={shopLocation.latitude}
                longitude={shopLocation.longitude}
                onChange={setShopLocation}
                required
              />

              <button type="submit" className="btn btn-primary" disabled={creatingShop} style={{ width: 'fit-content' }}>
                {creatingShop ? 'Submitting...' : 'Submit Shop for Verification'}
              </button>
            </form>
          ) : (
            <div style={{ display: 'flex', flexDirection: 'column', gap: '12px' }}>
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: '10px' }}>
                <div>
                  <div style={{ fontSize: '1.1rem', fontWeight: 700 }}>{shop.name}</div>
                  <div style={{ fontSize: '0.85rem', color: 'var(--text-secondary)' }}>{shop.location || shop.campusLocation}</div>
                  {shop.hasCoordinates() && (
                    <div style={{ fontSize: '0.78rem', color: 'var(--text-muted)' }}>
                      GPS Point: {shop.latitude.toFixed(5)}, {shop.longitude.toFixed(5)}
                    </div>
                  )}
                </div>
                <StatusBadge status={shop.approvalStatus} />
              </div>

              {shop.approvalStatus !== 'APPROVED' && (
                <div style={{ padding: '12px', borderRadius: '10px', backgroundColor: 'rgba(245, 158, 11, 0.1)', border: '1px solid rgba(245, 158, 11, 0.25)', fontSize: '0.85rem', color: '#fbbf24', display: 'flex', alignItems: 'center', gap: '8px' }}>
                  <Clock size={16} />
                  <span>
                    {shop.approvalStatus === 'PENDING'
                      ? 'Your shop is pending Administrator approval. All other platform pages remain locked until verified.'
                      : `Your shop status is currently ${shop.approvalStatus}. Please contact Admin or submit a removal request below.`}
                  </span>
                </div>
              )}
            </div>
          )}
        </div>
      ) : isDeliveryPartner ? (
        <div className="glass-panel" style={{ padding: '24px' }}>
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: '12px', marginBottom: '12px' }}>
            <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
              <Truck size={20} color="var(--brand-primary)" />
              <h2 style={{ fontSize: '1.15rem' }}>Courier Verification Status</h2>
            </div>
            <StatusBadge status={account?.verificationStatus || 'PENDING'} />
          </div>

          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(220px, 1fr))', gap: '12px', marginBottom: '14px' }}>
            <div style={{ padding: '12px', borderRadius: '10px', backgroundColor: 'rgba(15, 23, 42, 0.45)', border: '1px solid var(--border-subtle)' }}>
              <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)' }}>Vehicle Details</div>
              <div style={{ fontWeight: 600 }}>{account?.vehicleDetails || 'Not specified'}</div>
            </div>
            <div style={{ padding: '12px', borderRadius: '10px', backgroundColor: 'rgba(15, 23, 42, 0.45)', border: '1px solid var(--border-subtle)' }}>
              <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)' }}>Preferred Service Area</div>
              <div style={{ fontWeight: 600 }}>{account?.serviceArea || 'Anywhere / Flexible'}</div>
            </div>
          </div>

          {!verified && (
            <div style={{ padding: '12px', borderRadius: '10px', backgroundColor: 'rgba(245, 158, 11, 0.1)', border: '1px solid rgba(245, 158, 11, 0.25)', fontSize: '0.86rem', color: '#fbbf24', display: 'flex', alignItems: 'center', gap: '8px' }}>
              <Clock size={16} />
              <span>
                {account?.verificationStatus === 'PENDING'
                  ? 'Your delivery partner profile is awaiting Admin verification. Delivery feeds and claiming actions unlock only once approved.'
                  : `Your delivery partner verification is ${account?.verificationStatus}. Only Admin can restore or approve access.`}
              </span>
            </div>
          )}
        </div>
      ) : isAdmin ? (
        <div className="glass-panel" style={{ padding: '24px' }}>
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: '12px', marginBottom: '12px' }}>
            <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
              <Shield size={20} color="#c084fc" />
              <h2 style={{ fontSize: '1.15rem' }}>Admin Role Approval Status</h2>
            </div>
            <StatusBadge status={account?.status === 'SUSPENDED' ? 'SUSPENDED' : account?.verificationStatus || 'PENDING'} />
          </div>

          {!verified && (
            <div style={{ padding: '12px', borderRadius: '10px', backgroundColor: 'rgba(168, 85, 247, 0.12)', border: '1px solid rgba(168, 85, 247, 0.3)', fontSize: '0.86rem', color: '#e9d5ff', display: 'flex', alignItems: 'center', gap: '8px', marginBottom: '16px' }}>
              <Clock size={16} />
              <span>
                {account?.status === 'SUSPENDED'
                  ? 'Your Admin access is suspended. Reactivating a suspended Admin requires the Admin Security Key.'
                  : account?.verificationStatus === 'REJECTED'
                    ? 'Your Admin request was declined. You can submit an updated permission request to existing Admins below.'
                    : 'Your Admin signup is pending approval from an active platform Admin. Admin tools stay locked until approved.'}
              </span>
            </div>
          )}

          {!verified && account?.status !== 'SUSPENDED' && (
            <form onSubmit={handleRequestAdminApproval} style={{ display: 'flex', gap: '10px', flexWrap: 'wrap' }}>
              <input
                type="text"
                className="form-input"
                style={{ flex: '1 1 280px' }}
                placeholder="Optional note explaining why you need Admin permission..."
                value={adminNote}
                onChange={(e) => setAdminNote(e.target.value)}
              />
              <button type="submit" className="btn btn-primary" disabled={requestingAdmin}>
                {requestingAdmin ? 'Submitting...' : 'Ask Admins for Permission'}
              </button>
            </form>
          )}
        </div>
      ) : null}

      {!isAdmin && (
      <div className="glass-panel" style={{ padding: '24px' }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: '10px', marginBottom: '10px' }}>
          <Trash2 size={18} color="#f87171" />
          <h3 style={{ fontSize: '1.05rem' }}>Remove Presence From Platform</h3>
        </div>
        <p style={{ fontSize: '0.85rem', color: 'var(--text-secondary)', marginBottom: '14px' }}>
          Leaving QuickBite requires explicit Administrator confirmation to settle pending orders and audit records.
        </p>

        {hasPendingDeletion ? (
          <div style={{ padding: '12px', borderRadius: '10px', backgroundColor: 'rgba(245, 158, 11, 0.1)', border: '1px solid rgba(245, 158, 11, 0.3)', color: '#fbbf24', fontSize: '0.85rem' }}>
            You already have a pending removal request awaiting Admin confirmation.
          </div>
        ) : (
          <form onSubmit={handleSubmitRemoval} style={{ display: 'flex', gap: '10px', flexWrap: 'wrap' }}>
            <input
              type="text"
              className="form-input"
              style={{ flex: '1 1 280px' }}
              placeholder="Reason for removing your shop / partner account from the platform..."
              value={removalReason}
              onChange={(e) => setRemovalReason(e.target.value)}
              required
            />
            <button type="submit" className="btn btn-danger" disabled={submittingRemoval}>
              {submittingRemoval ? 'Submitting...' : 'Request Removal'}
            </button>
          </form>
        )}

        {deletionRequests.length > 0 && (
          <div style={{ marginTop: '14px', display: 'flex', flexDirection: 'column', gap: '8px' }}>
            {deletionRequests.map((req) => (
              <div
                key={req.id}
                style={{
                  display: 'flex',
                  justifyContent: 'space-between',
                  alignItems: 'center',
                  padding: '10px 12px',
                  borderRadius: '8px',
                  backgroundColor: 'rgba(15, 23, 42, 0.45)',
                  fontSize: '0.83rem',
                }}
              >
                <span>{req.reason}</span>
                <StatusBadge status={req.status} />
              </div>
            ))}
          </div>
        )}
      </div>
      )}
    </div>
  );
};

