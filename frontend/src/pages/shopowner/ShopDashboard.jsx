import React, { useState, useEffect } from 'react';
import { api } from '../../api/apiClient';
import { StatusBadge } from '../../components/StatusBadge';
import { 
  Store, 
  MapPin, 
  Clock, 
  Power, 
  Package, 
  ShoppingBag, 
  TrendingUp, 
  AlertCircle,
  Plus
} from 'lucide-react';

export const ShopDashboard = () => {
  const [shop, setShop] = useState(null);
  const [products, setProducts] = useState([]);
  const [orders, setOrders] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  // Shop registration form for new shop owners
  const [shopName, setShopName] = useState('');
  const [description, setDescription] = useState('');
  const [campusLocation, setCampusLocation] = useState('');
  const [imagePath, setImagePath] = useState('');
  const [creating, setCreating] = useState(false);

  const fetchShopData = async () => {
    try {
      const shopData = await api.get('/marketplace/owner/shop');
      setShop(shopData);

      const [prods, ords] = await Promise.all([
        api.get('/marketplace/owner/products'),
        api.get('/orders/shop'),
      ]);
      setProducts(prods);
      setOrders(ords);
    } catch (err) {
      if (err.status === 404) {
        // No shop registered yet
        setShop(null);
      } else {
        setError(err.message || 'Failed to load shop dashboard');
      }
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchShopData();
  }, []);

  const handleCreateShop = async (e) => {
    e.preventDefault();
    setCreating(true);
    setError('');

    try {
      const res = await api.post('/marketplace/owner/shop', {
        name: shopName.trim(),
        description: description.trim(),
        campusLocation: campusLocation.trim(),
        imagePath: imagePath ? imagePath.trim() : null,
      });
      setShop(res);
      fetchShopData();
    } catch (err) {
      setError(err.message || 'Failed to register shop');
    } finally {
      setCreating(false);
    }
  };

  const handleToggleOpen = async () => {
    if (!shop) return;
    try {
      const updated = await api.put('/marketplace/owner/shop', {
        open: !shop.open,
      });
      setShop(updated);
    } catch (err) {
      alert(err.message || 'Failed to toggle shop status');
    }
  };

  if (loading) {
    return <div style={{ textAlign: 'center', padding: '60px', color: 'var(--text-muted)' }}>Loading shop portal...</div>;
  }

  // If Shop Owner has not yet registered their 1 shop
  if (!shop) {
    return (
      <div style={{ maxWidth: '600px', margin: '0 auto', display: 'flex', flexDirection: 'column', gap: '24px' }}>
        <div>
          <h1 style={{ fontSize: '1.8rem', marginBottom: '6px' }}>Shop Application</h1>
          <p style={{ color: 'var(--text-muted)', fontSize: '0.9rem' }}>
            Submit your campus food shop details for administrative approval.
          </p>
        </div>

        {error && (
          <div style={{ padding: '12px 16px', borderRadius: '10px', background: 'var(--danger-bg)', color: 'var(--danger)' }}>
            {error}
          </div>
        )}

        <div className="glass-panel" style={{ padding: '32px' }}>
          <form onSubmit={handleCreateShop}>
            <div className="form-group">
              <label className="form-label">Shop Name</label>
              <input
                type="text"
                className="form-input"
                placeholder="e.g. Campus Central Cafeteria"
                value={shopName}
                onChange={(e) => setShopName(e.target.value)}
                required
              />
            </div>

            <div className="form-group">
              <label className="form-label">Description / Specialties</label>
              <textarea
                className="form-textarea"
                rows={3}
                placeholder="Fresh rolls, burgers, coffee and hot meals..."
                value={description}
                onChange={(e) => setDescription(e.target.value)}
              />
            </div>

            <div className="form-group">
              <label className="form-label">Campus Location</label>
              <input
                type="text"
                className="form-input"
                placeholder="e.g. Student Activity Center (SAC), Ground Floor"
                value={campusLocation}
                onChange={(e) => setCampusLocation(e.target.value)}
                required
              />
            </div>

            <div className="form-group">
              <label className="form-label">Shop Banner Image URL</label>
              <input
                type="url"
                className="form-input"
                placeholder="https://images.unsplash.com/photo-..."
                value={imagePath}
                onChange={(e) => setImagePath(e.target.value)}
              />
            </div>

            <button
              type="submit"
              disabled={creating}
              className="btn btn-primary"
              style={{ width: '100%', padding: '13px', marginTop: '12px' }}
            >
              {creating ? 'Submitting Application...' : 'Submit Shop for Approval'}
            </button>
          </form>
        </div>
      </div>
    );
  }

  const activeOrdersCount = orders.filter((o) =>
    ['PLACED', 'ACCEPTED', 'PREPARING', 'READY'].includes(o.status)
  ).length;

  const totalRevenue = orders
    .filter((o) => o.status === 'DELIVERED')
    .reduce((sum, o) => sum + Number(o.subtotal), 0);

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: '28px' }}>
      {/* Header */}
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: '14px' }}>
        <div>
          <h1 style={{ fontSize: '1.8rem', marginBottom: '4px' }}>{shop.name}</h1>
          <div style={{ display: 'flex', alignItems: 'center', gap: '12px' }}>
            <span style={{ display: 'flex', alignItems: 'center', gap: '6px', color: 'var(--text-muted)', fontSize: '0.88rem' }}>
              <MapPin size={15} color="#ff6433" /> {shop.campusLocation}
            </span>
            <StatusBadge status={shop.approvalStatus} />
          </div>
        </div>

        {/* Operational Open/Close Toggle */}
        <div style={{ display: 'flex', alignItems: 'center', gap: '12px' }}>
          <button
            onClick={handleToggleOpen}
            className={`btn ${shop.open ? 'btn-success' : 'btn-secondary'}`}
            style={{ padding: '10px 18px', display: 'flex', alignItems: 'center', gap: '8px' }}
          >
            <Power size={16} />
            <span>{shop.open ? 'Store is OPEN' : 'Store is CLOSED'}</span>
          </button>
        </div>
      </div>

      {shop.approvalStatus === 'PENDING' && (
        <div style={{
          padding: '16px 20px',
          background: 'rgba(245, 158, 11, 0.12)',
          border: '1px solid rgba(245, 158, 11, 0.3)',
          borderRadius: '12px',
          color: '#fbbf24',
          display: 'flex',
          alignItems: 'center',
          gap: '12px'
        }}>
          <AlertCircle size={22} />
          <div>
            <div style={{ fontWeight: 700 }}>Shop Registration Under Review</div>
            <div style={{ fontSize: '0.85rem', color: '#fde68a' }}>
              Your shop application has been submitted to campus administration. Once approved, you can open your store to accept student orders.
            </div>
          </div>
        </div>
      )}

      {/* Metrics Cards */}
      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(220px, 1fr))', gap: '20px' }}>
        <div className="glass-panel" style={{ padding: '22px' }}>
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '12px' }}>
            <span style={{ fontSize: '0.85rem', color: 'var(--text-muted)', fontWeight: 600 }}>ACTIVE ORDERS</span>
            <div style={{ padding: '8px', borderRadius: '10px', background: 'rgba(59, 130, 246, 0.15)', color: '#60a5fa' }}>
              <ShoppingBag size={18} />
            </div>
          </div>
          <div style={{ fontSize: '1.8rem', fontWeight: 800 }}>{activeOrdersCount}</div>
        </div>

        <div className="glass-panel" style={{ padding: '22px' }}>
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '12px' }}>
            <span style={{ fontSize: '0.85rem', color: 'var(--text-muted)', fontWeight: 600 }}>TOTAL PRODUCTS</span>
            <div style={{ padding: '8px', borderRadius: '10px', background: 'rgba(245, 158, 11, 0.15)', color: '#fbbf24' }}>
              <Package size={18} />
            </div>
          </div>
          <div style={{ fontSize: '1.8rem', fontWeight: 800 }}>{products.length}</div>
        </div>

        <div className="glass-panel" style={{ padding: '22px' }}>
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '12px' }}>
            <span style={{ fontSize: '0.85rem', color: 'var(--text-muted)', fontWeight: 600 }}>DELIVERED REVENUE</span>
            <div style={{ padding: '8px', borderRadius: '10px', background: 'rgba(16, 185, 129, 0.15)', color: '#34d399' }}>
              <TrendingUp size={18} />
            </div>
          </div>
          <div style={{ fontSize: '1.8rem', fontWeight: 800, color: 'var(--primary)' }}>
            ₹{totalRevenue.toFixed(2)}
          </div>
        </div>
      </div>
    </div>
  );
};
