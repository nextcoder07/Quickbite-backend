import React, { useState, useEffect } from 'react';
import { api } from '../../api/apiClient';
import { Link } from 'react-router-dom';
import { Search, MapPin, Store, Clock, Utensils, Sparkles } from 'lucide-react';
import { StatusBadge } from '../../components/StatusBadge';

export const BrowseShops = () => {
  const [shops, setShops] = useState([]);
  const [loading, setLoading] = useState(true);
  const [searchTerm, setSearchTerm] = useState('');
  const [filterLocation, setFilterLocation] = useState('ALL');

  useEffect(() => {
    const fetchShops = async () => {
      try {
        const data = await api.get('/marketplace/shops?includeClosed=true');
        setShops(data);
      } catch (err) {
        console.error('Failed to fetch shops:', err);
      } finally {
        setLoading(false);
      }
    };

    fetchShops();
  }, []);

  const filteredShops = shops.filter((shop) => {
    const matchesSearch = shop.name.toLowerCase().includes(searchTerm.toLowerCase()) ||
                          (shop.description && shop.description.toLowerCase().includes(searchTerm.toLowerCase()));
    const matchesLocation = filterLocation === 'ALL' || shop.campusLocation.toLowerCase().includes(filterLocation.toLowerCase());
    return matchesSearch && matchesLocation;
  });

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: '28px' }}>
      {/* Hero Welcome Banner */}
      <div className="glass-panel" style={{
        padding: '32px 36px',
        background: 'linear-gradient(135deg, rgba(255, 87, 34, 0.15) 0%, rgba(245, 158, 11, 0.08) 100%)',
        border: '1px solid rgba(255, 87, 34, 0.25)',
        position: 'relative',
        overflow: 'hidden'
      }}>
        <div style={{ maxWidth: '620px' }}>
          <div style={{ display: 'inline-flex', alignItems: 'center', gap: '6px', color: '#ff6433', fontSize: '0.82rem', fontWeight: 700, marginBottom: '8px' }}>
            <Sparkles size={16} /> CAMPUS FOOD MARKETPLACE
          </div>
          <h1 style={{ fontSize: '2.2rem', marginBottom: '10px', color: '#fff' }}>
            Hungry on Campus? QuickBite delivers.
          </h1>
          <p style={{ color: 'var(--text-muted)', fontSize: '1rem', lineHeight: '1.5' }}>
            Order freshly prepared food from approved campus shops, track live delivery right to your hostel or department, and enjoy quick contactless payment.
          </p>
        </div>
      </div>

      {/* Filter and Search Bar */}
      <div style={{ display: 'flex', gap: '14px', flexWrap: 'wrap', alignItems: 'center', justifyContent: 'space-between' }}>
        <div style={{ position: 'relative', flex: 1, minWidth: '280px' }}>
          <Search size={18} color="var(--text-faint)" style={{ position: 'absolute', left: '14px', top: '50%', transform: 'translateY(-50%)' }} />
          <input
            type="text"
            className="form-input"
            style={{ paddingLeft: '42px' }}
            placeholder="Search campus shops or food items..."
            value={searchTerm}
            onChange={(e) => setSearchTerm(e.target.value)}
          />
        </div>

        <div style={{ display: 'flex', gap: '10px' }}>
          {['ALL', 'SAC', 'Hostel', 'Food Court'].map((loc) => (
            <button
              key={loc}
              onClick={() => setFilterLocation(loc)}
              className={`btn btn-sm ${filterLocation === loc ? 'btn-primary' : 'btn-secondary'}`}
            >
              <MapPin size={14} /> {loc === 'ALL' ? 'All Locations' : loc}
            </button>
          ))}
        </div>
      </div>

      {/* Shop Grid */}
      {loading ? (
        <div style={{ textAlign: 'center', padding: '60px 0', color: 'var(--text-muted)' }}>
          Loading campus shops...
        </div>
      ) : filteredShops.length === 0 ? (
        <div className="glass-panel" style={{ textAlign: 'center', padding: '60px 20px' }}>
          <Store size={48} color="var(--text-faint)" style={{ margin: '0 auto 16px' }} />
          <h3 style={{ marginBottom: '8px' }}>No shops found</h3>
          <p style={{ color: 'var(--text-muted)', fontSize: '0.9rem' }}>Try clearing your search query or check back later.</p>
        </div>
      ) : (
        <div style={{
          display: 'grid',
          gridTemplateColumns: 'repeat(auto-fill, minmax(320px, 1fr))',
          gap: '24px'
        }}>
          {filteredShops.map((shop) => (
            <div key={shop.id} className="glass-panel" style={{ overflow: 'hidden', display: 'flex', flexDirection: 'column' }}>
              {/* Shop Cover Image */}
              <div style={{ height: '170px', position: 'relative', background: '#1c202a' }}>
                <img
                  src={shop.imagePath || 'https://images.unsplash.com/photo-1555396273-367ea4eb4db5?w=600&auto=format&fit=crop'}
                  alt={shop.name}
                  style={{ width: '100%', height: '100%', objectFit: 'cover' }}
                />
                <div style={{
                  position: 'absolute',
                  top: '12px',
                  right: '12px',
                  display: 'flex',
                  gap: '6px'
                }}>
                  {shop.open ? (
                    <span className="badge badge-success"><Clock size={12} /> OPEN NOW</span>
                  ) : (
                    <span className="badge badge-danger"><Clock size={12} /> CLOSED</span>
                  )}
                </div>
              </div>

              {/* Content */}
              <div style={{ padding: '20px', flex: 1, display: 'flex', flexDirection: 'column', justifyContent: 'space-between' }}>
                <div>
                  <h3 style={{ fontSize: '1.25rem', color: '#fff', marginBottom: '6px' }}>{shop.name}</h3>
                  <p style={{
                    fontSize: '0.85rem',
                    color: 'var(--text-muted)',
                    marginBottom: '14px',
                    display: '-webkit-box',
                    WebkitLineClamp: 2,
                    WebkitBoxOrient: 'vertical',
                    overflow: 'hidden'
                  }}>
                    {shop.description || 'Fresh food and beverages prepared on campus.'}
                  </p>

                  <div style={{ display: 'flex', alignItems: 'center', gap: '6px', color: 'var(--text-faint)', fontSize: '0.82rem', marginBottom: '18px' }}>
                    <MapPin size={14} color="#ff6433" />
                    <span>{shop.campusLocation}</span>
                  </div>
                </div>

                <Link
                  to={`/shop/${shop.id}`}
                  className="btn btn-primary"
                  style={{ width: '100%', justifyContent: 'center' }}
                >
                  <Utensils size={16} /> View Menu & Order
                </Link>
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  );
};
