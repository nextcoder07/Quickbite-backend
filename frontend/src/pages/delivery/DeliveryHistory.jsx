import React, { useState, useEffect } from 'react';
import { api } from '../../api/apiClient';
import { StatusBadge } from '../../components/StatusBadge';
import { Clock, Store, CheckCircle2, TrendingUp } from 'lucide-react';

export const DeliveryHistory = () => {
  const [deliveries, setDeliveries] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  useEffect(() => {
    const fetchHistory = async () => {
      try {
        const data = await api.get('/delivery/my');
        setDeliveries(data);
      } catch (err) {
        setError(err.message || 'Failed to load delivery history');
      } finally {
        setLoading(false);
      }
    };

    fetchHistory();
  }, []);

  const completed = deliveries.filter((d) => d.status === 'DELIVERED');
  const estimatedEarnings = completed.length * 30; // standard delivery fee

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: '24px' }}>
      <div>
        <h1 style={{ fontSize: '1.8rem', marginBottom: '4px' }}>Delivery History & Earnings</h1>
        <p style={{ color: 'var(--text-muted)', fontSize: '0.9rem' }}>
          Record of fulfilled deliveries and delivery earnings
        </p>
      </div>

      {/* Stats */}
      <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '20px', maxWidth: '500px' }}>
        <div className="glass-panel" style={{ padding: '20px' }}>
          <div style={{ fontSize: '0.8rem', color: 'var(--text-muted)', fontWeight: 600 }}>COMPLETED TRIPS</div>
          <div style={{ fontSize: '1.8rem', fontWeight: 800, marginTop: '4px' }}>{completed.length}</div>
        </div>

        <div className="glass-panel" style={{ padding: '20px' }}>
          <div style={{ fontSize: '0.8rem', color: 'var(--text-muted)', fontWeight: 600 }}>ESTIMATED EARNINGS</div>
          <div style={{ fontSize: '1.8rem', fontWeight: 800, color: 'var(--success)', marginTop: '4px' }}>
            ₹{estimatedEarnings.toFixed(2)}
          </div>
        </div>
      </div>

      {loading ? (
        <div style={{ textAlign: 'center', padding: '60px', color: 'var(--text-muted)' }}>Loading history...</div>
      ) : deliveries.length === 0 ? (
        <div className="glass-panel" style={{ padding: '40px', textAlign: 'center', color: 'var(--text-muted)' }}>
          No delivery history recorded yet.
        </div>
      ) : (
        <div className="glass-panel" style={{ overflow: 'hidden' }}>
          <table style={{ width: '100%', borderCollapse: 'collapse', textAlign: 'left' }}>
            <thead>
              <tr style={{ background: 'var(--bg-surface)', borderBottom: '1px solid var(--border-subtle)', color: 'var(--text-muted)', fontSize: '0.8rem' }}>
                <th style={{ padding: '14px 20px' }}>ORDER</th>
                <th style={{ padding: '14px 20px' }}>SHOP & LOCATION</th>
                <th style={{ padding: '14px 20px' }}>STUDENT</th>
                <th style={{ padding: '14px 20px' }}>STATUS</th>
                <th style={{ padding: '14px 20px', textAlign: 'right' }}>DATE</th>
              </tr>
            </thead>
            <tbody>
              {deliveries.map((d) => (
                <tr key={d.id} style={{ borderBottom: '1px solid var(--border-subtle)' }}>
                  <td style={{ padding: '14px 20px', fontWeight: 700 }}>#{d.orderNumber}</td>
                  <td style={{ padding: '14px 20px' }}>
                    <div style={{ fontWeight: 600 }}>{d.shopName}</div>
                    <div style={{ fontSize: '0.78rem', color: 'var(--text-muted)' }}>{d.shopLocation}</div>
                  </td>
                  <td style={{ padding: '14px 20px' }}>{d.buyerName}</td>
                  <td style={{ padding: '14px 20px' }}>
                    <StatusBadge status={d.status} />
                  </td>
                  <td style={{ padding: '14px 20px', textAlign: 'right', fontSize: '0.82rem', color: 'var(--text-muted)' }}>
                    {d.deliveredAt ? new Date(d.deliveredAt).toLocaleString() : new Date(d.assignedAt).toLocaleString()}
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
