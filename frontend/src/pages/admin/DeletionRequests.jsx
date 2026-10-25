import React, { useState, useEffect } from 'react';
import { api } from '../../api/apiClient';
import { StatusBadge } from '../../components/StatusBadge';
import { ClipboardList, Check, X, AlertTriangle } from 'lucide-react';

export const DeletionRequests = () => {
  const [requests, setRequests] = useState([]);
  const [loading, setLoading] = useState(true);

  const fetchRequests = async () => {
    try {
      const data = await api.get('/admin/deletion-requests');
      setRequests(data);
    } catch (err) {
      console.error('Failed to load deletion requests:', err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchRequests();
  }, []);

  const handleReview = async (requestId, confirm) => {
    const note = prompt(
      confirm ? 'Optional review note for confirming deletion:' : 'Reason for declining deletion request:',
      confirm ? 'Approved by administration' : 'Orders pending fulfillment'
    );
    if (note === null) return;

    try {
      await api.post(`/admin/deletion-requests/${requestId}/review`, {
        confirm,
        note,
      });
      fetchRequests();
    } catch (err) {
      alert(err.message || 'Review action failed');
    }
  };

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: '24px' }}>
      <div>
        <h1 style={{ fontSize: '1.8rem', marginBottom: '4px' }}>Account Deletion Requests</h1>
        <p style={{ color: 'var(--text-muted)', fontSize: '0.9rem' }}>
          Review account closure requests from Shop Owners and Delivery Partners
        </p>
      </div>

      {loading ? (
        <div style={{ textAlign: 'center', padding: '60px', color: 'var(--text-muted)' }}>Loading deletion requests...</div>
      ) : requests.length === 0 ? (
        <div className="glass-panel" style={{ padding: '40px', textAlign: 'center', color: 'var(--text-muted)' }}>
          No account deletion requests filed.
        </div>
      ) : (
        <div style={{ display: 'flex', flexDirection: 'column', gap: '16px' }}>
          {requests.map((req) => (
            <div key={req.id} className="glass-panel" style={{ padding: '20px' }}>
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', flexWrap: 'wrap', gap: '12px' }}>
                <div>
                  <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
                    <span style={{ fontWeight: 700, fontSize: '1rem', color: '#fff' }}>{req.requesterEmail}</span>
                    <span className="badge badge-info">{req.requesterType}</span>
                    <StatusBadge status={req.status} />
                  </div>
                  <div style={{ fontSize: '0.85rem', color: 'var(--text-muted)', marginTop: '8px' }}>
                    <strong>Reason:</strong> {req.reason}
                  </div>
                  {req.reviewNote && (
                    <div style={{ fontSize: '0.8rem', color: '#94a3b8', marginTop: '4px' }}>
                      <strong>Admin Note:</strong> {req.reviewNote}
                    </div>
                  )}
                  <div style={{ fontSize: '0.75rem', color: 'var(--text-faint)', marginTop: '8px' }}>
                    Requested: {new Date(req.requestedAt).toLocaleString()}
                  </div>
                </div>

                {req.status === 'PENDING' && (
                  <div style={{ display: 'flex', gap: '8px' }}>
                    <button onClick={() => handleReview(req.id, true)} className="btn btn-danger btn-sm">
                      <Check size={14} /> Confirm Deletion
                    </button>
                    <button onClick={() => handleReview(req.id, false)} className="btn btn-secondary btn-sm">
                      <X size={14} /> Decline Request
                    </button>
                  </div>
                )}
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  );
};
