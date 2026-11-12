import React, { useEffect, useState } from 'react';
import { adminService } from '../../services/AdminService';
import { useAuth } from '../../context/AuthContext';
import { StatusBadge } from '../../components/StatusBadge';
import { PasswordInput } from '../../components/PasswordInput';
import { Shield, Check, X, KeyRound, Ban, RotateCcw, UserPlus } from 'lucide-react';

export const AdminManagement = () => {
  const { account } = useAuth();
  const [admins, setAdmins] = useState([]);
  const [roleRequests, setRoleRequests] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [feedback, setFeedback] = useState('');
  const [processingKey, setProcessingKey] = useState(null);

  // Security key modal state for suspending or reactivating an admin
  const [keyTarget, setKeyTarget] = useState(null); // { admin, targetStatus }
  const [securityKeyInput, setSecurityKeyInput] = useState('');

  const loadData = async () => {
    setLoading(true);
    setError('');
    try {
      const [adminsData, requestsData] = await Promise.all([
        adminService.getAdmins(),
        adminService.getAdminRoleRequests(),
      ]);
      setAdmins(Array.isArray(adminsData) ? adminsData : []);
      setRoleRequests(Array.isArray(requestsData) ? requestsData : []);
    } catch (err) {
      setError(err.message || 'Failed to load admin management data');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadData();
  }, []);

  const handleAdminStatus = async (adminId, status, adminKey = null) => {
    setProcessingKey(`admin-${adminId}`);
    setError('');
    setFeedback('');
    try {
      await adminService.reviewAdminStatus(adminId, status, adminKey);
      setKeyTarget(null);
      setSecurityKeyInput('');
      await loadData();
      setFeedback(`Admin account updated to ${status}.`);
    } catch (err) {
      setError(err.message || 'Failed to update admin status');
    } finally {
      setProcessingKey(null);
    }
  };

  const handleRoleRequestReview = async (requestId, approve) => {
    setProcessingKey(`req-${requestId}`);
    setError('');
    setFeedback('');
    try {
      await adminService.reviewAdminRoleRequest(requestId, approve, '');
      await loadData();
      setFeedback(approve ? 'User promoted to active Admin.' : 'Admin permission request declined.');
    } catch (err) {
      setError(err.message || 'Failed to review admin permission request');
    } finally {
      setProcessingKey(null);
    }
  };

  const pendingAdminSignups = admins.filter((a) => a.verificationStatus === 'PENDING');
  const otherAdmins = admins.filter((a) => a.verificationStatus !== 'PENDING');
  const pendingRoleRequests = roleRequests.filter((r) => r.status === 'PENDING');

  if (loading) {
    return <div style={{ textAlign: 'center', padding: '60px', color: 'var(--text-muted)' }}>Loading admin roster & requests...</div>;
  }

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: '24px' }}>
      <div>
        <h1 style={{ fontSize: '1.8rem', marginBottom: '4px' }}>Admin Team & Permission Requests</h1>
        <p style={{ color: 'var(--text-muted)', fontSize: '0.9rem' }}>
          Approve or decline Admin signup/promotion requests. Suspending or reactivating an Admin requires the environment Admin Security Key.
        </p>
      </div>

      {error && (
        <div style={{ padding: '12px 16px', borderRadius: '10px', background: 'var(--danger-bg)', color: 'var(--danger)', fontSize: '0.88rem' }}>
          {error}
        </div>
      )}
      {feedback && (
        <div style={{ padding: '12px 16px', borderRadius: '10px', background: 'var(--success-bg)', color: 'var(--success)', fontSize: '0.88rem' }}>
          {feedback}
        </div>
      )}

      {/* Security Key Prompt for Suspending / Reactivating an Admin */}
      {keyTarget && (
        <div className="glass-panel" style={{ padding: '20px', border: '1px solid rgba(239, 68, 68, 0.35)', background: 'rgba(239, 68, 68, 0.06)' }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: '8px', marginBottom: '8px', fontWeight: 700 }}>
            <KeyRound size={18} color="#f87171" />
            <span>
              {keyTarget.targetStatus === 'SUSPENDED'
                ? `Enter Admin Security Key to Suspend ${keyTarget.admin.displayName}`
                : `Enter Admin Security Key to Reactivate ${keyTarget.admin.displayName}`}
            </span>
          </div>
          <p style={{ fontSize: '0.82rem', color: 'var(--text-muted)', marginBottom: '12px' }}>
            Target account: {keyTarget.admin.email}
          </p>
          <form
            onSubmit={(e) => {
              e.preventDefault();
              handleAdminStatus(keyTarget.admin.id, keyTarget.targetStatus, securityKeyInput);
            }}
            style={{ display: 'flex', gap: '10px', flexWrap: 'wrap' }}
          >
            <PasswordInput
              name="admin-security-key"
              autoComplete="off"
              style={{ flex: '1 1 260px' }}
              placeholder="Enter ADMIN_SIGNUP_KEY..."
              value={securityKeyInput}
              onChange={(e) => setSecurityKeyInput(e.target.value)}
              required
            />
            <button
              type="submit"
              className={keyTarget.targetStatus === 'SUSPENDED' ? 'btn btn-danger' : 'btn btn-primary'}
              disabled={processingKey === `admin-${keyTarget.admin.id}`}
            >
              {processingKey === `admin-${keyTarget.admin.id}` ? 'Verifying...' : 'Confirm with Key'}
            </button>
            <button
              type="button"
              className="btn btn-secondary"
              onClick={() => {
                setKeyTarget(null);
                setSecurityKeyInput('');
              }}
            >
              Cancel
            </button>
          </form>
        </div>
      )}

      {/* Pending Admin Signups (signed up as ADMIN without key) */}
      <div className="glass-panel" style={{ padding: '24px' }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: '10px', marginBottom: '14px' }}>
          <Shield size={20} color="#fbbf24" />
          <h2 style={{ fontSize: '1.15rem' }}>Pending Admin Signups ({pendingAdminSignups.length})</h2>
        </div>

        {pendingAdminSignups.length === 0 ? (
          <p style={{ color: 'var(--text-muted)', fontSize: '0.88rem' }}>No pending Admin signup requests.</p>
        ) : (
          <div style={{ display: 'flex', flexDirection: 'column', gap: '12px' }}>
            {pendingAdminSignups.map((a) => (
              <div
                key={a.id}
                style={{
                  display: 'flex',
                  justifyContent: 'space-between',
                  alignItems: 'center',
                  flexWrap: 'wrap',
                  gap: '12px',
                  padding: '14px 16px',
                  borderRadius: '10px',
                  background: 'rgba(15, 23, 42, 0.45)',
                  border: '1px solid var(--border-subtle)',
                }}
              >
                <div>
                  <div style={{ fontWeight: 700 }}>{a.displayName}</div>
                  <div style={{ fontSize: '0.84rem', color: 'var(--text-muted)' }}>{a.email}</div>
                  {a.requestNote && (
                    <div style={{ fontSize: '0.82rem', color: 'var(--text-secondary)', marginTop: '4px' }}>
                      Note: {a.requestNote}
                    </div>
                  )}
                </div>
                <div style={{ display: 'flex', gap: '8px', alignItems: 'center' }}>
                  <StatusBadge status={a.verificationStatus} />
                  <button
                    type="button"
                    className="btn btn-primary btn-sm"
                    disabled={processingKey === `admin-${a.id}`}
                    onClick={() => handleAdminStatus(a.id, 'APPROVED')}
                  >
                    <Check size={14} /> Approve
                  </button>
                  <button
                    type="button"
                    className="btn btn-danger btn-sm"
                    disabled={processingKey === `admin-${a.id}`}
                    onClick={() => handleAdminStatus(a.id, 'REJECTED')}
                  >
                    <X size={14} /> Decline
                  </button>
                </div>
              </div>
            ))}
          </div>
        )}
      </div>

      {/* Existing User Admin Promotion Requests */}
      <div className="glass-panel" style={{ padding: '24px' }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: '10px', marginBottom: '14px' }}>
          <UserPlus size={20} color="#c084fc" />
          <h2 style={{ fontSize: '1.15rem' }}>Existing User Admin Permission Requests ({pendingRoleRequests.length} Pending)</h2>
        </div>

        {roleRequests.length === 0 ? (
          <p style={{ color: 'var(--text-muted)', fontSize: '0.88rem' }}>No permission requests from existing users yet.</p>
        ) : (
          <div style={{ display: 'flex', flexDirection: 'column', gap: '12px' }}>
            {roleRequests.map((req) => (
              <div
                key={req.id}
                style={{
                  display: 'flex',
                  justifyContent: 'space-between',
                  alignItems: 'center',
                  flexWrap: 'wrap',
                  gap: '12px',
                  padding: '14px 16px',
                  borderRadius: '10px',
                  background: 'rgba(15, 23, 42, 0.45)',
                  border: '1px solid var(--border-subtle)',
                }}
              >
                <div>
                  <div style={{ fontWeight: 700 }}>
                    {req.displayName} <span style={{ fontSize: '0.78rem', color: 'var(--text-muted)' }}>({req.currentRole})</span>
                  </div>
                  <div style={{ fontSize: '0.84rem', color: 'var(--text-muted)' }}>{req.email}</div>
                  {req.reason && (
                    <div style={{ fontSize: '0.82rem', color: 'var(--text-secondary)', marginTop: '4px' }}>
                      Reason: {req.reason}
                    </div>
                  )}
                </div>
                <div style={{ display: 'flex', gap: '8px', alignItems: 'center' }}>
                  <StatusBadge status={req.status} />
                  {req.status === 'PENDING' && (
                    <>
                      <button
                        type="button"
                        className="btn btn-primary btn-sm"
                        disabled={processingKey === `req-${req.id}`}
                        onClick={() => handleRoleRequestReview(req.id, true)}
                      >
                        <Check size={14} /> Grant Admin
                      </button>
                      <button
                        type="button"
                        className="btn btn-danger btn-sm"
                        disabled={processingKey === `req-${req.id}`}
                        onClick={() => handleRoleRequestReview(req.id, false)}
                      >
                        <X size={14} /> Decline
                      </button>
                    </>
                  )}
                </div>
              </div>
            ))}
          </div>
        )}
      </div>

      {/* Active / Suspended / Rejected Admin Roster */}
      <div className="glass-panel" style={{ padding: '24px' }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: '10px', marginBottom: '14px' }}>
          <Shield size={20} color="#34d399" />
          <h2 style={{ fontSize: '1.15rem' }}>Platform Admins ({otherAdmins.length})</h2>
        </div>

        <div style={{ display: 'flex', flexDirection: 'column', gap: '12px' }}>
          {otherAdmins.map((a) => {
            const isSelf = account?.identityId && Number(account.identityId) === Number(a.identityId);
            const isSuspended = a.accountStatus === 'SUSPENDED' || a.accountStatus === 'DEACTIVATED';
            const isApproved = a.verificationStatus === 'APPROVED' && a.accountStatus === 'ACTIVE';

            return (
              <div
                key={a.id}
                style={{
                  display: 'flex',
                  justifyContent: 'space-between',
                  alignItems: 'center',
                  flexWrap: 'wrap',
                  gap: '12px',
                  padding: '14px 16px',
                  borderRadius: '10px',
                  background: 'rgba(15, 23, 42, 0.45)',
                  border: '1px solid var(--border-subtle)',
                }}
              >
                <div>
                  <div style={{ fontWeight: 700 }}>
                    {a.displayName} {isSelf && <span style={{ fontSize: '0.75rem', color: 'var(--primary)' }}>(You)</span>}
                  </div>
                  <div style={{ fontSize: '0.84rem', color: 'var(--text-muted)' }}>{a.email}</div>
                </div>
                <div style={{ display: 'flex', gap: '8px', alignItems: 'center' }}>
                  <StatusBadge status={isSuspended ? a.accountStatus : a.verificationStatus} />
                  {!isSelf && isApproved && (
                    <button
                      type="button"
                      className="btn btn-danger btn-sm"
                      onClick={() => {
                        setSecurityKeyInput('');
                        setKeyTarget({ admin: a, targetStatus: 'SUSPENDED' });
                      }}
                    >
                      <Ban size={14} /> Suspend (Key Required)
                    </button>
                  )}
                  {!isSelf && isSuspended && (
                    <button
                      type="button"
                      className="btn btn-secondary btn-sm"
                      onClick={() => {
                        setSecurityKeyInput('');
                        setKeyTarget({ admin: a, targetStatus: 'APPROVED' });
                      }}
                    >
                      <RotateCcw size={14} /> Reactivate (Key Required)
                    </button>
                  )}
                  {!isSelf && a.verificationStatus === 'REJECTED' && !isSuspended && (
                    <button
                      type="button"
                      className="btn btn-primary btn-sm"
                      disabled={processingKey === `admin-${a.id}`}
                      onClick={() => handleAdminStatus(a.id, 'APPROVED')}
                    >
                      <Check size={14} /> Approve
                    </button>
                  )}
                </div>
              </div>
            );
          })}
        </div>
      </div>
    </div>
  );
};

