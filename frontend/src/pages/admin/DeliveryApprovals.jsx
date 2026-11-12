import React, { useState, useEffect } from 'react';
import { adminService } from '../../services/AdminService';
import { StatusBadge } from '../../components/StatusBadge';
import { Bike, Check, X, Ban, Phone, Mail, Award, Clock, MapPin } from 'lucide-react';

export const DeliveryApprovals = () => {
  const [partners, setPartners] = useState([]);
  const [loading, setLoading] = useState(true);
  const [filter, setFilter] = useState('ALL');
  const [error, setError] = useState('');

  const fetchPartners = async () => {
    try {
      const data = await adminService.getDeliveryPartners();
      setPartners(data);
    } catch (err) {
      console.error('Failed to load delivery partners:', err);
      setError(err.message || 'Failed to load delivery partners');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchPartners();
  }, []);

  const handleReview = async (partnerId, newStatus) => {
    const labelMap = {
      APPROVED: 'verify & approve',
      ACTIVE: 'verify & approve',
      REJECTED: 'reject',
      SUSPENDED: 'suspend',
      DEACTIVATED: 'remove / deactivate from platform',
    };
    const actionName = labelMap[newStatus] || newStatus;
    if (!window.confirm(`Are you sure you want to ${actionName} this delivery partner?`)) return;

    try {
      await adminService.reviewDeliveryPartnerStatus(partnerId, newStatus);
      fetchPartners();
    } catch (err) {
      alert(err.message || 'Status transition failed');
    }
  };

  const filtered = partners.filter((p) => {
    if (filter === 'ALL') return true;
    if (filter === 'DEACTIVATED') return p.accountStatus === 'DEACTIVATED';
    if (filter === 'SUSPENDED') return p.accountStatus === 'SUSPENDED' || p.verificationStatus === 'SUSPENDED';
    return p.verificationStatus === filter && p.accountStatus !== 'DEACTIVATED';
  });

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: '24px' }}>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: '14px' }}>
        <div>
          <h1 style={{ fontSize: '1.8rem', marginBottom: '4px' }}>Delivery Partner Verification & Removal</h1>
          <p style={{ color: 'var(--text-muted)', fontSize: '0.9rem' }}>
            Verify newly registered delivery partners, review rejected/suspended accounts, or confirm removal
          </p>
        </div>

        {/* Filter buttons */}
        <div style={{ display: 'flex', gap: '8px', flexWrap: 'wrap' }}>
          {['ALL', 'PENDING', 'APPROVED', 'REJECTED', 'SUSPENDED', 'DEACTIVATED'].map((st) => (
            <button
              key={st}
              onClick={() => setFilter(st)}
              className={`btn btn-sm ${filter === st ? 'btn-primary' : 'btn-secondary'}`}
            >
              {st === 'ALL' ? 'All Partners' : st}
            </button>
          ))}
        </div>
      </div>

      {error && (
        <div style={{ padding: '12px 16px', borderRadius: '10px', background: 'var(--danger-bg)', color: 'var(--danger)' }}>
          {error}
        </div>
      )}

      {loading ? (
        <div style={{ textAlign: 'center', padding: '60px', color: 'var(--text-muted)' }}>Loading delivery partners...</div>
      ) : filtered.length === 0 ? (
        <div className="glass-panel" style={{ padding: '40px', textAlign: 'center', color: 'var(--text-muted)' }}>
          <Bike size={44} color="var(--text-faint)" style={{ margin: '0 auto 12px' }} />
          <h3>No delivery partners in this view</h3>
        </div>
      ) : (
        <div style={{ display: 'flex', flexDirection: 'column', gap: '16px' }}>
          {filtered.map((partner) => {
            const verStatus = partner.verificationStatus || 'PENDING';
            const accStatus = partner.accountStatus || 'ACTIVE';
            const isDeactivated = accStatus === 'DEACTIVATED';
            const isVerifiedActive = verStatus === 'APPROVED' && accStatus === 'ACTIVE';
            const isSuspended = accStatus === 'SUSPENDED' || verStatus === 'SUSPENDED';

            return (
              <div
                key={partner.id}
                className="glass-panel"
                style={{
                  padding: '22px',
                  display: 'flex',
                  justifyContent: 'space-between',
                  alignItems: 'center',
                  flexWrap: 'wrap',
                  gap: '16px',
                  borderLeft: isVerifiedActive
                    ? '4px solid var(--success)'
                    : verStatus === 'PENDING'
                    ? '4px solid var(--warning)'
                    : '4px solid var(--danger)',
                }}
              >
                <div style={{ display: 'flex', gap: '18px', alignItems: 'center' }}>
                  <div
                    style={{
                      width: '48px',
                      height: '48px',
                      borderRadius: '12px',
                      background: 'var(--bg-surface)',
                      display: 'flex',
                      alignItems: 'center',
                      justifyContent: 'center',
                      border: '1px solid var(--border-subtle)',
                    }}
                  >
                    <Bike size={24} color="#ff6433" />
                  </div>

                  <div>
                    <div style={{ display: 'flex', alignItems: 'center', gap: '10px', marginBottom: '4px', flexWrap: 'wrap' }}>
                      <h3 style={{ fontSize: '1.15rem', color: '#fff' }}>{partner.displayName}</h3>
                      <StatusBadge status={verStatus} />
                      {accStatus !== 'ACTIVE' && <StatusBadge status={accStatus} />}
                    </div>

                    <div style={{ display: 'flex', alignItems: 'center', gap: '14px', color: 'var(--text-muted)', fontSize: '0.82rem', flexWrap: 'wrap' }}>
                      <span style={{ display: 'inline-flex', alignItems: 'center', gap: '4px' }}>
                        <Mail size={13} /> {partner.email}
                      </span>
                      {partner.phone && (
                        <span style={{ display: 'inline-flex', alignItems: 'center', gap: '4px' }}>
                          <Phone size={13} /> {partner.phone}
                        </span>
                      )}
                      {partner.vehicleDetails && (
                        <span style={{ display: 'inline-flex', alignItems: 'center', gap: '4px', color: '#cbd5e1' }}>
                          <Bike size={13} /> {partner.vehicleDetails}
                        </span>
                      )}
                      {partner.serviceArea && (
                        <span style={{ display: 'inline-flex', alignItems: 'center', gap: '4px' }}>
                          <MapPin size={13} /> {partner.serviceArea}
                        </span>
                      )}
                      <span style={{ display: 'inline-flex', alignItems: 'center', gap: '4px' }}>
                        <Award size={13} color="var(--success)" /> {partner.completedDeliveriesCount} trips completed
                      </span>
                      {partner.activeDeliveriesCount > 0 && (
                        <span style={{ display: 'inline-flex', alignItems: 'center', gap: '4px', color: '#38bdf8' }}>
                          <Clock size={13} /> {partner.activeDeliveriesCount} in transit
                        </span>
                      )}
                    </div>
                  </div>
                </div>

                {/* Moderation & Verification Controls */}
                <div style={{ display: 'flex', gap: '8px', flexWrap: 'wrap' }}>
                  {!isDeactivated && !isVerifiedActive && (
                    <button
                      onClick={() => handleReview(partner.id, 'APPROVED')}
                      className="btn btn-sm btn-primary"
                      style={{ background: 'var(--success)', borderColor: 'var(--success)' }}
                    >
                      <Check size={14} /> Verify & Approve
                    </button>
                  )}

                  {!isDeactivated && verStatus === 'PENDING' && (
                    <button
                      onClick={() => handleReview(partner.id, 'REJECTED')}
                      className="btn btn-sm btn-secondary"
                      style={{ color: '#f87171', borderColor: 'rgba(239, 68, 68, 0.35)' }}
                    >
                      <X size={14} /> Reject
                    </button>
                  )}

                  {!isDeactivated && !isSuspended && (
                    <button
                      onClick={() => handleReview(partner.id, 'SUSPENDED')}
                      className="btn btn-sm btn-secondary"
                      style={{ color: 'var(--warning)', borderColor: 'var(--warning)' }}
                    >
                      <Ban size={14} /> Suspend
                    </button>
                  )}

                  {!isDeactivated && (
                    <button
                      onClick={() => handleReview(partner.id, 'DEACTIVATED')}
                      className="btn btn-sm btn-danger"
                    >
                      <X size={14} /> Remove from Platform
                    </button>
                  )}
                </div>
              </div>
            );
          })}
        </div>
      )}
    </div>
  );
};
