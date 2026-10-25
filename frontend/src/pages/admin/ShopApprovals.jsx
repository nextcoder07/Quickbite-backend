import React, { useState, useEffect } from 'react';
import { api } from '../../api/apiClient';
import { StatusBadge } from '../../components/StatusBadge';
import { Store, Check, X, Ban, MapPin } from 'lucide-react';

export const ShopApprovals = () => {
  const [shops, setShops] = useState([]);
  const [loading, setLoading] = useState(true);
  const [filter, setFilter] = useState('ALL');

  const fetchShops = async () => {
    try {
      const data = await api.get('/admin/shops');
      setShops(data);
    } catch (err) {
      console.error('Failed to load shops:', err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchShops();
  }, []);

  const handleReview = async (shopId, newStatus) => {
    try {
      await api.patch(`/admin/shops/${shopId}/status`, { status: newStatus });
      fetchShops();
    } catch (err) {
      alert(err.message || 'Status transition failed');
    }
  };

  const filtered = shops.filter((s) => {
    if (filter === 'ALL') return true;
    return s.approvalStatus === filter;
  });

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: '24px' }}>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: '14px' }}>
        <div>
          <h1 style={{ fontSize: '1.8rem', marginBottom: '4px' }}>Campus Shop Moderation</h1>
          <p style={{ color: 'var(--text-muted)', fontSize: '0.9rem' }}>
            Approve, reject, or suspend food shops operating on campus
          </p>
        </div>

        {/* Filter buttons */}
        <div style={{ display: 'flex', gap: '8px' }}>
          {['ALL', 'PENDING', 'APPROVED', 'REJECTED', 'SUSPENDED'].map((st) => (
            <button
              key={st}
              onClick={() => setFilter(st)}
              className={`btn btn-sm ${filter === st ? 'btn-primary' : 'btn-secondary'}`}
            >
              {st}
            </button>
          ))}
        </div>
      </div>

      {loading ? (
        <div style={{ textAlign: 'center', padding: '60px', color: 'var(--text-muted)' }}>Loading shops...</div>
      ) : filtered.length === 0 ? (
        <div className="glass-panel" style={{ padding: '40px', textAlign: 'center', color: 'var(--text-muted)' }}>
          No shops match the selected filter.
        </div>
      ) : (
        <div style={{ display: 'flex', flexDirection: 'column', gap: '18px' }}>
          {filtered.map((shop) => (
            <div key={shop.id} className="glass-panel" style={{ padding: '22px' }}>
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', flexWrap: 'wrap', gap: '14px' }}>
                <div style={{ display: 'flex', gap: '16px' }}>
                  <img
                    src={shop.imagePath || 'https://images.unsplash.com/photo-1555396273-367ea4eb4db5?w=200'}
                    alt={shop.name}
                    style={{ width: '80px', height: '80px', borderRadius: '12px', objectFit: 'cover' }}
                  />
                  <div>
                    <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
                      <h3 style={{ fontSize: '1.2rem', color: '#fff' }}>{shop.name}</h3>
                      <StatusBadge status={shop.approvalStatus} />
                    </div>
                    <div style={{ display: 'flex', alignItems: 'center', gap: '6px', color: 'var(--text-muted)', fontSize: '0.85rem', marginTop: '4px' }}>
                      <MapPin size={14} color="#ff6433" /> {shop.campusLocation}
                    </div>
                    <p style={{ fontSize: '0.85rem', color: 'var(--text-muted)', marginTop: '8px', maxWidth: '600px' }}>
                      {shop.description || 'No description provided.'}
                    </p>
                  </div>
                </div>

                {/* Actions */}
                <div style={{ display: 'flex', gap: '8px', flexWrap: 'wrap' }}>
                  {shop.approvalStatus === 'PENDING' && (
                    <>
                      <button onClick={() => handleReview(shop.id, 'APPROVED')} className="btn btn-success btn-sm">
                        <Check size={14} /> Approve Shop
                      </button>
                      <button onClick={() => handleReview(shop.id, 'REJECTED')} className="btn btn-danger btn-sm">
                        <X size={14} /> Reject Application
                      </button>
                    </>
                  )}

                  {shop.approvalStatus === 'APPROVED' && (
                    <button onClick={() => handleReview(shop.id, 'SUSPENDED')} className="btn btn-secondary btn-sm" style={{ color: 'var(--danger)' }}>
                      <Ban size={14} /> Suspend Shop
                    </button>
                  )}

                  {shop.approvalStatus === 'SUSPENDED' && (
                    <button onClick={() => handleReview(shop.id, 'APPROVED')} className="btn btn-success btn-sm">
                      <Check size={14} /> Re-instate Approval
                    </button>
                  )}
                </div>
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  );
};
