import React, { useState, useEffect } from 'react';
import { api } from '../../api/apiClient';
import { StatusBadge } from '../../components/StatusBadge';
import { AlertCircle, Check, X, MessageSquare } from 'lucide-react';

export const DisputeManagement = () => {
  const [disputes, setDisputes] = useState([]);
  const [loading, setLoading] = useState(true);

  const fetchDisputes = async () => {
    try {
      const data = await api.get('/disputes');
      setDisputes(data);
    } catch (err) {
      console.error('Failed to load disputes:', err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchDisputes();
  }, []);

  const handleResolve = async (disputeId, resolve) => {
    const note = prompt(
      resolve ? 'Enter dispute resolution note for student & shop:' : 'Enter dispute rejection reason:',
      resolve ? 'Refund authorized and shop notified' : 'Dispute claim could not be verified'
    );
    if (note === null) return;

    try {
      await api.post(`/disputes/${disputeId}/resolve`, {
        resolve,
        note,
      });
      fetchDisputes();
    } catch (err) {
      alert(err.message || 'Action failed');
    }
  };

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: '24px' }}>
      <div>
        <h1 style={{ fontSize: '1.8rem', marginBottom: '4px' }}>Order Dispute Cases</h1>
        <p style={{ color: 'var(--text-muted)', fontSize: '0.9rem' }}>
          Moderate student claims, quality reports, and delivery issues
        </p>
      </div>

      {loading ? (
        <div style={{ textAlign: 'center', padding: '60px', color: 'var(--text-muted)' }}>Loading disputes...</div>
      ) : disputes.length === 0 ? (
        <div className="glass-panel" style={{ padding: '40px', textAlign: 'center', color: 'var(--text-muted)' }}>
          No active or historical order disputes found.
        </div>
      ) : (
        <div style={{ display: 'flex', flexDirection: 'column', gap: '16px' }}>
          {disputes.map((d) => (
            <div key={d.id} className="glass-panel" style={{ padding: '22px' }}>
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', flexWrap: 'wrap', gap: '14px' }}>
                <div>
                  <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
                    <span style={{ fontWeight: 800, fontSize: '1.05rem', color: '#fff' }}>Order #{d.orderNumber}</span>
                    <span className="badge badge-warning">{d.category}</span>
                    <StatusBadge status={d.status} />
                  </div>

                  <p style={{ fontSize: '0.9rem', color: '#e2e8f0', marginTop: '10px', background: 'var(--bg-surface)', padding: '12px', borderRadius: '8px', maxWidth: '650px' }}>
                    "{d.description}"
                  </p>

                  {d.resolutionNote && (
                    <div style={{ fontSize: '0.82rem', color: '#94a3b8', marginTop: '8px' }}>
                      <strong>Resolution Note:</strong> {d.resolutionNote}
                    </div>
                  )}

                  <div style={{ fontSize: '0.75rem', color: 'var(--text-faint)', marginTop: '8px' }}>
                    Raised: {new Date(d.createdAt).toLocaleString()}
                  </div>
                </div>

                {d.status === 'OPEN' && (
                  <div style={{ display: 'flex', gap: '8px' }}>
                    <button onClick={() => handleResolve(d.id, true)} className="btn btn-success btn-sm">
                      <Check size={14} /> Resolve Case
                    </button>
                    <button onClick={() => handleResolve(d.id, false)} className="btn btn-danger btn-sm">
                      <X size={14} /> Reject Claim
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
