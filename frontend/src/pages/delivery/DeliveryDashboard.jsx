import React, { useState, useEffect } from 'react';
import { api } from '../../api/apiClient';
import { StatusBadge } from '../../components/StatusBadge';
import { 
  Bike, 
  MapPin, 
  Store, 
  CheckCircle2, 
  PackageCheck, 
  AlertTriangle, 
  X,
  Navigation
} from 'lucide-react';

export const DeliveryDashboard = () => {
  const [availableDeliveries, setAvailableDeliveries] = useState([]);
  const [myDeliveries, setMyDeliveries] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  // Failure reporting modal
  const [failedAssignment, setFailedAssignment] = useState(null);
  const [failureReason, setFailureReason] = useState('');
  const [failing, setFailing] = useState(false);

  const fetchDeliveries = async () => {
    try {
      const [avail, my] = await Promise.all([
        api.get('/delivery/available'),
        api.get('/delivery/my'),
      ]);
      setAvailableDeliveries(avail);
      setMyDeliveries(my);
    } catch (err) {
      setError(err.message || 'Failed to fetch deliveries');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchDeliveries();
    const interval = setInterval(fetchDeliveries, 5000); // 5s delivery polling
    return () => clearInterval(interval);
  }, []);

  const handleClaim = async (assignmentId) => {
    try {
      await api.post(`/delivery/${assignmentId}/claim`);
      fetchDeliveries();
    } catch (err) {
      alert(err.message || 'Failed to claim delivery');
    }
  };

  const handlePickedUp = async (assignmentId) => {
    try {
      await api.post(`/delivery/${assignmentId}/pickup`);
      fetchDeliveries();
    } catch (err) {
      alert(err.message || 'Failed to mark picked up');
    }
  };

  const handleDelivered = async (assignmentId) => {
    try {
      await api.post(`/delivery/${assignmentId}/deliver`);
      fetchDeliveries();
    } catch (err) {
      alert(err.message || 'Failed to mark delivered');
    }
  };

  const handleReportFailure = async (e) => {
    e.preventDefault();
    if (!failedAssignment || !failureReason) return;
    setFailing(true);
    try {
      await api.post(`/delivery/${failedAssignment.id}/fail`, {
        reason: failureReason.trim(),
      });
      setFailedAssignment(null);
      setFailureReason('');
      fetchDeliveries();
    } catch (err) {
      alert(err.message || 'Failed to report failure');
    } finally {
      setFailing(false);
    }
  };

  const activeMyDeliveries = myDeliveries.filter((d) =>
    ['ASSIGNED', 'ACCEPTED', 'PICKED_UP'].includes(d.status)
  );

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: '28px' }}>
      <div>
        <h1 style={{ fontSize: '1.8rem', marginBottom: '4px' }}>Campus Delivery Fulfillment</h1>
        <p style={{ color: 'var(--text-muted)', fontSize: '0.9rem' }}>
          Claim available campus orders, pick up from food shops, and deliver directly to students
        </p>
      </div>

      {error && (
        <div style={{ padding: '12px 16px', borderRadius: '10px', background: 'var(--danger-bg)', color: 'var(--danger)' }}>
          {error}
        </div>
      )}

      {/* Active Deliveries Section */}
      <div>
        <div style={{ display: 'flex', alignItems: 'center', gap: '8px', marginBottom: '14px' }}>
          <h2 style={{ fontSize: '1.3rem' }}>My Active Deliveries ({activeMyDeliveries.length})</h2>
        </div>

        {activeMyDeliveries.length === 0 ? (
          <div className="glass-panel" style={{ padding: '32px', textAlign: 'center', color: 'var(--text-muted)' }}>
            You have no active orders in transit. Claim an available order below to start delivering.
          </div>
        ) : (
          <div style={{ display: 'flex', flexDirection: 'column', gap: '16px' }}>
            {activeMyDeliveries.map((d) => (
              <div key={d.id} className="glass-panel" style={{ padding: '22px', border: '1px solid rgba(59, 130, 246, 0.3)' }}>
                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', marginBottom: '14px', flexWrap: 'wrap', gap: '10px' }}>
                  <div>
                    <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
                      <span style={{ fontSize: '1.15rem', fontWeight: 800 }}>Order #{d.orderNumber}</span>
                      <StatusBadge status={d.status} />
                    </div>
                    <div style={{ fontSize: '0.85rem', color: 'var(--text-muted)', marginTop: '4px' }}>
                      Buyer: <strong style={{ color: '#fff' }}>{d.buyerName}</strong>
                    </div>
                  </div>

                  <div style={{ fontSize: '1.3rem', fontWeight: 800, color: 'var(--primary)' }}>
                    ₹{Number(d.orderTotal).toFixed(2)}
                  </div>
                </div>

                {/* Pickup & Drop Details */}
                <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '12px', padding: '14px', background: 'var(--bg-surface)', borderRadius: '10px', marginBottom: '16px' }}>
                  <div>
                    <div style={{ fontSize: '0.72rem', color: 'var(--text-faint)', fontWeight: 700 }}>PICKUP LOCATION</div>
                    <div style={{ fontWeight: 600, fontSize: '0.9rem', color: '#fff', display: 'flex', alignItems: 'center', gap: '6px', marginTop: '2px' }}>
                      <Store size={14} color="#ff6433" /> {d.shopName}
                    </div>
                    <div style={{ fontSize: '0.8rem', color: 'var(--text-muted)' }}>{d.shopLocation}</div>
                  </div>

                  <div>
                    <div style={{ fontSize: '0.72rem', color: 'var(--text-faint)', fontWeight: 700 }}>DROP DESTINATION</div>
                    <div style={{ fontWeight: 600, fontSize: '0.9rem', color: '#fff', display: 'flex', alignItems: 'center', gap: '6px', marginTop: '2px' }}>
                      <Navigation size={14} color="#3b82f6" /> Campus Delivery
                    </div>
                    <div style={{ fontSize: '0.8rem', color: 'var(--text-muted)' }}>Direct handover to student</div>
                  </div>
                </div>

                {/* Progress Actions */}
                <div style={{ display: 'flex', gap: '10px', justifyContent: 'flex-end', flexWrap: 'wrap' }}>
                  {(d.status === 'ASSIGNED' || d.status === 'ACCEPTED') && (
                    <button onClick={() => handlePickedUp(d.id)} className="btn btn-primary btn-sm">
                      <PackageCheck size={14} /> Confirm Kitchen Pickup
                    </button>
                  )}

                  {d.status === 'PICKED_UP' && (
                    <button onClick={() => handleDelivered(d.id)} className="btn btn-success btn-sm">
                      <CheckCircle2 size={14} /> Confirm Student Handover
                    </button>
                  )}

                  <button
                    onClick={() => setFailedAssignment(d)}
                    className="btn btn-danger btn-sm"
                  >
                    <AlertTriangle size={14} /> Report Issue
                  </button>
                </div>
              </div>
            ))}
          </div>
        )}
      </div>

      {/* Available Campus Deliveries */}
      <div>
        <div style={{ display: 'flex', alignItems: 'center', gap: '8px', marginBottom: '14px' }}>
          <h2 style={{ fontSize: '1.3rem' }}>Available Orders to Claim ({availableDeliveries.length})</h2>
        </div>

        {availableDeliveries.length === 0 ? (
          <div className="glass-panel" style={{ padding: '36px', textAlign: 'center', color: 'var(--text-muted)' }}>
            No unassigned deliveries currently waiting for pickup.
          </div>
        ) : (
          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(320px, 1fr))', gap: '18px' }}>
            {availableDeliveries.map((d) => (
              <div key={d.id} className="glass-panel" style={{ padding: '20px', display: 'flex', flexDirection: 'column', justifyContent: 'space-between' }}>
                <div>
                  <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '8px' }}>
                    <span style={{ fontWeight: 800, fontSize: '1rem' }}>#{d.orderNumber}</span>
                    <span style={{ fontWeight: 700, color: 'var(--primary)', fontSize: '1.1rem' }}>₹{Number(d.orderTotal).toFixed(2)}</span>
                  </div>

                  <div style={{ display: 'flex', alignItems: 'center', gap: '6px', fontSize: '0.88rem', color: '#fff', marginBottom: '4px' }}>
                    <Store size={14} color="#ff6433" /> {d.shopName}
                  </div>
                  <div style={{ fontSize: '0.8rem', color: 'var(--text-muted)', marginBottom: '14px' }}>
                    {d.shopLocation}
                  </div>
                </div>

                <button
                  onClick={() => handleClaim(d.id)}
                  className="btn btn-primary"
                  style={{ width: '100%', justifyContent: 'center' }}
                >
                  <Bike size={16} /> Accept Delivery Assignment
                </button>
              </div>
            ))}
          </div>
        )}
      </div>

      {/* Failure Reporting Modal */}
      {failedAssignment && (
        <div style={{
          position: 'fixed',
          inset: 0,
          background: 'rgba(0,0,0,0.7)',
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
          zIndex: 9999,
          padding: '20px'
        }}>
          <div className="glass-panel" style={{ width: '100%', maxWidth: '420px', padding: '26px', background: '#161922' }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '14px' }}>
              <h3 style={{ fontSize: '1.2rem', color: '#f87171' }}>Report Delivery Issue</h3>
              <button onClick={() => setFailedAssignment(null)} style={{ background: 'transparent', border: 'none', color: '#fff', cursor: 'pointer' }}>
                <X size={18} />
              </button>
            </div>

            <form onSubmit={handleReportFailure}>
              <div className="form-group">
                <label className="form-label">Failure / Exception Reason</label>
                <textarea
                  className="form-textarea"
                  rows={3}
                  placeholder="e.g. Student unreachable at location, food spilled..."
                  value={failureReason}
                  onChange={(e) => setFailureReason(e.target.value)}
                  required
                />
              </div>

              <button type="submit" disabled={failing} className="btn btn-danger" style={{ width: '100%', marginTop: '8px' }}>
                {failing ? 'Submitting...' : 'Submit Delivery Exception'}
              </button>
            </form>
          </div>
        </div>
      )}
    </div>
  );
};
