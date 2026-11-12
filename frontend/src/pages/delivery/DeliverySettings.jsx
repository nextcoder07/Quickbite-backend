import React, { useState, useEffect } from 'react';
import { useAuth } from '../../context/AuthContext';
import { deliveryService } from '../../services/DeliveryService';
import { StatusBadge } from '../../components/StatusBadge';
import { Bike, ShieldCheck, AlertTriangle, Send, CheckCircle2, Phone, Mail, Clock } from 'lucide-react';
import { useDialog } from '../../context/DialogContext';

export const DeliverySettings = () => {
  const dialog = useDialog();
  const { user } = useAuth();
  const [reason, setReason] = useState('');
  const [submitting, setSubmitting] = useState(false);
  const [requests, setRequests] = useState([]);
  const [loadingRequests, setLoadingRequests] = useState(true);
  const [successMsg, setSuccessMsg] = useState('');
  const [errorMsg, setErrorMsg] = useState('');

  const fetchMyRequests = async () => {
    try {
      const data = await deliveryService.getMyDeletionRequests();
      setRequests(data);
    } catch (err) {
      console.warn('Could not fetch deletion requests', err);
    } finally {
      setLoadingRequests(false);
    }
  };

  useEffect(() => {
    fetchMyRequests();
  }, []);

  const handleRequestDeletion = async (e) => {
    e.preventDefault();
    if (!reason.trim()) {
      setErrorMsg('Please state a reason for account closure or offboarding.');
      return;
    }

    const confirmed = await dialog.confirm({
      title: 'Submit Account Removal Request?',
      message: 'Are you sure you want to submit an account removal request? An Administrator must confirm it before your profile is removed.',
      confirmText: 'Submit Request',
      danger: true,
    });
    if (!confirmed) {
      return;
    }

    setSubmitting(true);
    setErrorMsg('');
    setSuccessMsg('');

    try {
      await deliveryService.submitDeletionRequest(reason.trim());
      setSuccessMsg('Removal request submitted successfully. Platform administration confirmation is required.');
      setReason('');
      fetchMyRequests();
    } catch (err) {
      setErrorMsg(err.message || 'Failed to submit request.');
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div style={{ maxWidth: '800px', margin: '0 auto', display: 'flex', flexDirection: 'column', gap: '28px' }}>
      <div>
        <h1 style={{ fontSize: '1.8rem', marginBottom: '4px' }}>Courier Profile & Offboarding</h1>
        <p style={{ color: 'var(--text-muted)', fontSize: '0.9rem' }}>
          Verification credentials, fulfillment status, and administrative account closure
        </p>
      </div>

      {/* Courier Identity & Verification Status Card */}
      <div className="glass-panel" style={{ padding: '28px' }}>
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', flexWrap: 'wrap', gap: '16px' }}>
          <div style={{ display: 'flex', gap: '16px', alignItems: 'center' }}>
            <div style={{
              width: '56px',
              height: '56px',
              borderRadius: '16px',
              background: 'linear-gradient(135deg, rgba(255, 100, 51, 0.2) 0%, rgba(255, 69, 0, 0.1) 100%)',
              border: '1px solid rgba(255, 100, 51, 0.3)',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center'
            }}>
              <Bike size={28} color="#ff6433" />
            </div>

            <div>
              <h2 style={{ fontSize: '1.3rem', color: '#fff' }}>{user?.displayName || 'Delivery Partner'}</h2>
              <div style={{ display: 'flex', alignItems: 'center', gap: '14px', marginTop: '4px', color: 'var(--text-muted)', fontSize: '0.85rem' }}>
                <span style={{ display: 'inline-flex', alignItems: 'center', gap: '4px' }}>
                  <Mail size={14} /> {user?.email}
                </span>
                {user?.phone && (
                  <span style={{ display: 'inline-flex', alignItems: 'center', gap: '4px' }}>
                    <Phone size={14} /> {user.phone}
                  </span>
                )}
              </div>
            </div>
          </div>

          <div style={{ display: 'flex', gap: '8px', alignItems: 'center' }}>
            <StatusBadge status={user?.verificationStatus || 'APPROVED'} />
            <StatusBadge status={user?.status || 'ACTIVE'} />
          </div>
        </div>

        <div style={{ marginTop: '20px', paddingTop: '18px', borderTop: '1px solid var(--border-subtle)', display: 'flex', gap: '24px', flexWrap: 'wrap', fontSize: '0.88rem' }}>
          <div>
            <span style={{ color: 'var(--text-muted)', display: 'block', fontSize: '0.75rem', fontWeight: 600 }}>PLATFORM ROLE</span>
            <span style={{ color: '#fff', fontWeight: 700 }}>Verified Delivery Partner</span>
          </div>
          <div>
            <span style={{ color: 'var(--text-muted)', display: 'block', fontSize: '0.75rem', fontWeight: 600 }}>VEHICLE & SERVICE AREA</span>
            <span style={{ color: '#fff', fontWeight: 600 }}>
              {user?.vehicleDetails || 'Flexible'} • {user?.serviceArea || 'Anywhere'}
            </span>
          </div>
          <div>
            <span style={{ color: 'var(--text-muted)', display: 'block', fontSize: '0.75rem', fontWeight: 600 }}>VERIFICATION BADGE</span>
            <span style={{ color: 'var(--success)', fontWeight: 700, display: 'inline-flex', alignItems: 'center', gap: '4px' }}>
              <ShieldCheck size={14} /> Admin Verified
            </span>
          </div>
        </div>
      </div>

      {/* Account Deletion / Offboarding Section */}
      <div className="glass-panel" style={{ padding: '28px', border: '1px solid rgba(239, 68, 68, 0.25)' }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: '10px', marginBottom: '8px' }}>
          <AlertTriangle size={20} color="var(--danger)" />
          <h2 style={{ fontSize: '1.25rem', color: '#fff' }}>Request Platform Removal</h2>
        </div>
        <p style={{ color: 'var(--text-muted)', fontSize: '0.88rem', marginBottom: '20px', lineHeight: '1.5' }}>
          No longer delivering with QuickBite? Submitting a removal request sends your request to platform administration for confirmation after settling active deliveries.
        </p>

        {successMsg && (
          <div style={{ padding: '12px 16px', borderRadius: '10px', background: 'rgba(16, 185, 129, 0.15)', color: 'var(--success)', marginBottom: '16px', display: 'flex', alignItems: 'center', gap: '8px', fontSize: '0.88rem' }}>
            <CheckCircle2 size={16} /> {successMsg}
          </div>
        )}

        {errorMsg && (
          <div style={{ padding: '12px 16px', borderRadius: '10px', background: 'var(--danger-bg)', color: 'var(--danger)', marginBottom: '16px', fontSize: '0.88rem' }}>
            {errorMsg}
          </div>
        )}

        <form onSubmit={handleRequestDeletion} style={{ display: 'flex', flexDirection: 'column', gap: '14px' }}>
          <div>
            <label style={{ display: 'block', marginBottom: '6px', fontSize: '0.82rem', fontWeight: 600, color: 'var(--text-muted)' }}>
              Reason for Offboarding
            </label>
            <textarea
              className="form-input"
              rows={3}
              placeholder="e.g., Graduating this semester, schedule conflict, or no longer available for deliveries..."
              value={reason}
              onChange={(e) => setReason(e.target.value)}
              style={{ width: '100%', resize: 'vertical' }}
              disabled={submitting}
            />
          </div>

          <div>
            <button
              type="submit"
              className="btn btn-danger"
              disabled={submitting || !reason.trim()}
              style={{ display: 'inline-flex', alignItems: 'center', gap: '8px' }}
            >
              <Send size={15} /> {submitting ? 'Submitting...' : 'Submit Offboarding Request'}
            </button>
          </div>
        </form>

        {/* Existing submitted requests history */}
        {requests.length > 0 && (
          <div style={{ marginTop: '24px', paddingTop: '18px', borderTop: '1px solid var(--border-subtle)' }}>
            <h3 style={{ fontSize: '0.95rem', marginBottom: '12px' }}>Your Offboarding Requests ({requests.length})</h3>
            <div style={{ display: 'flex', flexDirection: 'column', gap: '10px' }}>
              {requests.map((req) => (
                <div key={req.id} style={{
                  padding: '12px 16px',
                  borderRadius: '10px',
                  background: 'var(--bg-surface)',
                  display: 'flex',
                  justifyContent: 'space-between',
                  alignItems: 'center',
                  fontSize: '0.85rem'
                }}>
                  <div>
                    <span style={{ color: '#fff', fontWeight: 600 }}>{req.reason}</span>
                    <span style={{ display: 'block', color: 'var(--text-faint)', fontSize: '0.75rem', marginTop: '2px' }}>
                      Requested: {new Date(req.requestedAt).toLocaleDateString()}
                    </span>
                  </div>
                  <div>
                    <StatusBadge status={req.status} />
                  </div>
                </div>
              ))}
            </div>
          </div>
        )}
      </div>
    </div>
  );
};

