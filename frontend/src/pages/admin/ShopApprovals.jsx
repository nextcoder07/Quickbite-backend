import React, { useState, useEffect } from 'react';
import { adminService } from '../../services/AdminService';
import { StatusBadge } from '../../components/StatusBadge';
import { Check, X, Ban, MapPin, Trash2, ExternalLink } from 'lucide-react';

export const ShopApprovals = () => {
  const [shops, setShops] = useState([]);
  const [loading, setLoading] = useState(true);
  const [filter, setFilter] = useState('ALL');

  const fetchShops = async () => {
    try {
      const data = await adminService.getAllShops();
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
    if (!window.confirm(`Confirm setting shop verification status to ${newStatus}?`)) return;
    try {
      await adminService.reviewShopStatus(shopId, newStatus);
      fetchShops();
    } catch (err) {
      alert(err.message || 'Status transition failed');
    }
  };

  const handleDeactivateShop = async (shopId) => {
    if (!window.confirm('Confirm removing/deactivating this shop from the platform? Active orders must already be closed.')) return;
    try {
      await adminService.deactivateShop(shopId);
      fetchShops();
    } catch (err) {
      alert(err.message || 'Failed to remove shop');
    }
  };

  const filtered = shops.filter((s) => {
    if (filter === 'ALL') return true;
    if (filter === 'DEACTIVATED') return s.isDeactivated;
    return s.approvalStatus === filter && !s.isDeactivated;
  });

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: '24px' }}>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: '14px' }}>
        <div>
          <h1 style={{ fontSize: '1.8rem', marginBottom: '4px' }}>Shop Verification & Removal</h1>
          <p style={{ color: 'var(--text-muted)', fontSize: '0.9rem' }}>
            Verify new restaurants, suspend non-compliant shops, or confirm shop removal from the platform
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
                    <div style={{ display: 'flex', alignItems: 'center', gap: '10px', flexWrap: 'wrap' }}>
                      <h3 style={{ fontSize: '1.2rem', color: '#fff' }}>{shop.name}</h3>
                      <StatusBadge status={shop.isDeactivated ? 'DEACTIVATED' : shop.approvalStatus} />
                    </div>
                    <div style={{ display: 'flex', alignItems: 'center', gap: '10px', color: 'var(--text-muted)', fontSize: '0.85rem', marginTop: '4px', flexWrap: 'wrap' }}>
                      <span style={{ display: 'inline-flex', alignItems: 'center', gap: '4px' }}>
                        <MapPin size={14} color="#ff6433" /> {shop.location || shop.campusLocation}
                      </span>
                      {shop.hasCoordinates?.() && (
                        <a
                          href={shop.getMapUrl()}
                          target="_blank"
                          rel="noreferrer"
                          style={{ color: 'var(--brand-primary)', fontSize: '0.78rem', display: 'inline-flex', alignItems: 'center', gap: '3px' }}
                        >
                          <ExternalLink size={12} /> GPS Pin
                        </a>
                      )}
                    </div>
                    <p style={{ fontSize: '0.85rem', color: 'var(--text-muted)', marginTop: '8px', maxWidth: '600px' }}>
                      {shop.description || 'No description provided.'}
                    </p>
                  </div>
                </div>

                {/* Actions */}
                <div style={{ display: 'flex', gap: '8px', flexWrap: 'wrap' }}>
                  {!shop.isDeactivated && shop.approvalStatus === 'PENDING' && (
                    <>
                      <button onClick={() => handleReview(shop.id, 'APPROVED')} className="btn btn-success btn-sm">
                        <Check size={14} /> Verify & Approve
                      </button>
                      <button onClick={() => handleReview(shop.id, 'REJECTED')} className="btn btn-danger btn-sm">
                        <X size={14} /> Reject
                      </button>
                    </>
                  )}

                  {!shop.isDeactivated && shop.approvalStatus === 'APPROVED' && (
                    <button onClick={() => handleReview(shop.id, 'SUSPENDED')} className="btn btn-secondary btn-sm" style={{ color: 'var(--warning)' }}>
                      <Ban size={14} /> Suspend
                    </button>
                  )}

                  {!shop.isDeactivated && (shop.approvalStatus === 'SUSPENDED' || shop.approvalStatus === 'REJECTED') && (
                    <button onClick={() => handleReview(shop.id, 'APPROVED')} className="btn btn-success btn-sm">
                      <Check size={14} /> Approve Shop
                    </button>
                  )}

                  {!shop.isDeactivated && (
                    <button onClick={() => handleDeactivateShop(shop.id)} className="btn btn-danger btn-sm">
                      <Trash2 size={14} /> Remove from Platform
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
