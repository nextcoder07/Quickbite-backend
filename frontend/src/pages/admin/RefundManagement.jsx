import React, { useState, useEffect } from 'react';
import { api } from '../../api/apiClient';
import { StatusBadge } from '../../components/StatusBadge';
import { RotateCcw, Check, X, ShieldCheck } from 'lucide-react';

export const RefundManagement = () => {
  const [refunds, setRefunds] = useState([]);
  const [loading, setLoading] = useState(true);

  const fetchRefunds = async () => {
    try {
      const data = await api.get('/payments/refunds');
      setRefunds(data);
    } catch (err) {
      console.error('Failed to load refunds:', err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchRefunds();
  }, []);

  const handleReview = async (refundId, approve) => {
    const note = prompt(
      approve ? 'Enter reason for refund approval:' : 'Enter reason for refund rejection:',
      approve ? 'Approved per campus refund policy' : 'Refund not eligible'
    );
    if (note === null) return;

    try {
      await api.post(`/payments/refund/${refundId}/review`, {
        approve,
        note,
      });
      fetchRefunds();
    } catch (err) {
      alert(err.message || 'Refund review failed');
    }
  };

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: '24px' }}>
      <div>
        <h1 style={{ fontSize: '1.8rem', marginBottom: '4px' }}>Refund Authorizations</h1>
        <p style={{ color: 'var(--text-muted)', fontSize: '0.9rem' }}>
          Inspect payment reversals, enforce refund caps, and review transactions
        </p>
      </div>

      {loading ? (
        <div style={{ textAlign: 'center', padding: '60px', color: 'var(--text-muted)' }}>Loading refunds...</div>
      ) : refunds.length === 0 ? (
        <div className="glass-panel" style={{ padding: '40px', textAlign: 'center', color: 'var(--text-muted)' }}>
          No refund requests pending review.
        </div>
      ) : (
        <div className="glass-panel" style={{ overflow: 'hidden' }}>
          <table style={{ width: '100%', borderCollapse: 'collapse', textAlign: 'left' }}>
            <thead>
              <tr style={{ background: 'var(--bg-surface)', borderBottom: '1px solid var(--border-subtle)', color: 'var(--text-muted)', fontSize: '0.8rem' }}>
                <th style={{ padding: '14px 20px' }}>REFUND ID</th>
                <th style={{ padding: '14px 20px' }}>PAYMENT REF</th>
                <th style={{ padding: '14px 20px' }}>AMOUNT</th>
                <th style={{ padding: '14px 20px' }}>REASON</th>
                <th style={{ padding: '14px 20px' }}>STATUS</th>
                <th style={{ padding: '14px 20px', textAlign: 'right' }}>ACTIONS</th>
              </tr>
            </thead>
            <tbody>
              {refunds.map((r) => (
                <tr key={r.id} style={{ borderBottom: '1px solid var(--border-subtle)' }}>
                  <td style={{ padding: '14px 20px', fontWeight: 700 }}>#{r.id}</td>
                  <td style={{ padding: '14px 20px', fontSize: '0.85rem' }}>Payment #{r.paymentId}</td>
                  <td style={{ padding: '14px 20px', fontWeight: 800, color: 'var(--primary)' }}>
                    ₹{Number(r.amount).toFixed(2)}
                  </td>
                  <td style={{ padding: '14px 20px', fontSize: '0.85rem', color: 'var(--text-muted)' }}>
                    {r.reason}
                  </td>
                  <td style={{ padding: '14px 20px' }}>
                    <StatusBadge status={r.status} />
                  </td>
                  <td style={{ padding: '14px 20px', textAlign: 'right' }}>
                    {r.status === 'REQUESTED' && (
                      <div style={{ display: 'inline-flex', gap: '8px' }}>
                        <button onClick={() => handleReview(r.id, true)} className="btn btn-success btn-sm">
                          <Check size={14} /> Approve
                        </button>
                        <button onClick={() => handleReview(r.id, false)} className="btn btn-danger btn-sm">
                          <X size={14} /> Reject
                        </button>
                      </div>
                    )}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </div>
  );
};
