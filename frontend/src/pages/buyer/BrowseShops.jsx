import React, { useState, useEffect, useMemo } from 'react';
import { marketplaceService } from '../../services/MarketplaceService';
import { Link } from 'react-router-dom';
import { Search, MapPin, Store, Clock, Utensils, Sparkles, ExternalLink, ArrowLeft } from 'lucide-react';

export const BrowseShops = () => {
  const [shops, setShops] = useState([]);
  const [matchingProducts, setMatchingProducts] = useState([]);
  const [loading, setLoading] = useState(true);
  const [searchTerm, setSearchTerm] = useState('');
  const [locationQuery, setLocationQuery] = useState('');

  useEffect(() => {
    let active = true;
    const fetchShops = async () => {
      try {
        const shopModels = await marketplaceService.getShops(true);
        if (active) setShops(shopModels);
      } catch (err) {
        console.error('Failed to fetch shops:', err);
      } finally {
        if (active) setLoading(false);
      }
    };

    fetchShops();
    return () => { active = false; };
  }, []);

  // Backend product search when buyer enters a food item keyword
  useEffect(() => {
    const trimmed = searchTerm.trim();
    if (!trimmed) {
      setMatchingProducts([]);
      return;
    }

    let active = true;
    const timer = setTimeout(async () => {
      try {
        const results = await marketplaceService.searchProducts(trimmed, locationQuery);
        if (active) setMatchingProducts(results);
      } catch (err) {
        console.error('Product search notice:', err);
      }
    }, 200);

    return () => {
      active = false;
      clearTimeout(timer);
    };
  }, [searchTerm, locationQuery]);

  // Extract unique locations dynamically from active shops (works across any cities/neighborhoods)
  const quickLocations = useMemo(() => {
    const seen = new Set();
    const list = [];
    for (const s of shops) {
      const raw = (s.location || s.campusLocation || '').trim();
      if (!raw) continue;
      // Extract area/city tag if comma-separated, else full string
      const parts = raw.split(',').map((p) => p.trim()).filter(Boolean);
      const label = parts.length > 1 ? parts[parts.length - 1] : parts[0];
      if (label && !seen.has(label.toLowerCase())) {
        seen.add(label.toLowerCase());
        list.push(label);
      }
    }
    return list.slice(0, 6);
  }, [shops]);

  const filteredShops = shops.filter((shop) => shop.matchesSearch(searchTerm, locationQuery || 'ALL'));

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: '26px' }}>
      {/* Hero Welcome Banner */}
      <div
        className="glass-panel"
        style={{
          padding: '32px 36px',
          background: 'linear-gradient(135deg, rgba(255, 87, 34, 0.15) 0%, rgba(245, 158, 11, 0.08) 100%)',
          border: '1px solid rgba(255, 87, 34, 0.25)',
          position: 'relative',
          overflow: 'hidden',
        }}
      >
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', gap: '20px', flexWrap: 'wrap' }}>
          <div style={{ maxWidth: '680px' }}>
            <div style={{ display: 'inline-flex', alignItems: 'center', gap: '6px', color: '#ff6433', fontSize: '0.82rem', fontWeight: 700, marginBottom: '8px' }}>
              <Sparkles size={16} /> ALL APPROVED SHOPS DIRECTORY
            </div>
            <h1 style={{ fontSize: '2.05rem', marginBottom: '10px', color: '#fff' }}>
              Browse Every Kitchen &amp; Storefront
            </h1>
            <p style={{ color: 'var(--text-muted)', fontSize: '0.96rem', lineHeight: '1.5' }}>
              All approved shops are loaded upfront below. Filter anytime by shop name, dish/product name (Burger, Biryani, Dosa, Latte), or any location.
            </p>
          </div>
          <Link to="/" className="btn btn-secondary" style={{ alignSelf: 'center', textDecoration: 'none' }}>
            <ArrowLeft size={16} /> For You • Top Items
          </Link>
        </div>
      </div>

      {/* Search & Freeform Location Filters */}
      <div style={{ display: 'flex', flexDirection: 'column', gap: '12px' }}>
        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(260px, 1fr))', gap: '12px' }}>
          <div style={{ position: 'relative' }}>
            <Search size={18} color="var(--text-faint)" style={{ position: 'absolute', left: '14px', top: '50%', transform: 'translateY(-50%)' }} />
            <input
              type="text"
              className="form-input"
              style={{ paddingLeft: '42px' }}
              placeholder="Search by product name or shop name (e.g. 'Paneer', 'Burger', 'Biryani')..."
              value={searchTerm}
              onChange={(e) => setSearchTerm(e.target.value)}
            />
          </div>

          <div style={{ position: 'relative' }}>
            <MapPin size={18} color="var(--brand-primary)" style={{ position: 'absolute', left: '14px', top: '50%', transform: 'translateY(-50%)' }} />
            <input
              type="text"
              className="form-input"
              style={{ paddingLeft: '42px' }}
              placeholder="Filter by any location, street, neighborhood, or city..."
              value={locationQuery}
              onChange={(e) => setLocationQuery(e.target.value)}
            />
          </div>
        </div>

        {quickLocations.length > 0 && (
          <div style={{ display: 'flex', gap: '8px', flexWrap: 'wrap', alignItems: 'center' }}>
            <span style={{ fontSize: '0.78rem', color: 'var(--text-muted)', fontWeight: 600 }}>Quick Areas:</span>
            <button
              type="button"
              onClick={() => setLocationQuery('')}
              className={`btn btn-sm ${!locationQuery ? 'btn-primary' : 'btn-secondary'}`}
            >
              All Locations
            </button>
            {quickLocations.map((loc) => {
              const active = locationQuery.toLowerCase() === loc.toLowerCase();
              return (
                <button
                  key={loc}
                  type="button"
                  onClick={() => setLocationQuery(active ? '' : loc)}
                  className={`btn btn-sm ${active ? 'btn-primary' : 'btn-secondary'}`}
                >
                  <MapPin size={13} /> {loc}
                </button>
              );
            })}
          </div>
        )}
      </div>

      {/* Matching Food Items Direct Results */}
      {searchTerm.trim() && matchingProducts.length > 0 && (
        <div className="glass-panel" style={{ padding: '20px' }}>
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '14px' }}>
            <h2 style={{ fontSize: '1.15rem', display: 'flex', alignItems: 'center', gap: '8px' }}>
              <Utensils size={18} color="#ff6433" /> Matching Menu Items ({matchingProducts.length})
            </h2>
            <span style={{ fontSize: '0.8rem', color: 'var(--text-muted)' }}>
              Click any item to open its shop & order
            </span>
          </div>

          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(250px, 1fr))', gap: '12px' }}>
            {matchingProducts.slice(0, 8).map((item) => (
              <Link
                key={item.productId}
                to={`/shop/${item.shopId}`}
                style={{
                  textDecoration: 'none',
                  color: 'inherit',
                  padding: '12px',
                  borderRadius: '10px',
                  backgroundColor: 'rgba(15, 23, 42, 0.5)',
                  border: '1px solid var(--border-subtle)',
                  display: 'flex',
                  justifyContent: 'space-between',
                  alignItems: 'center',
                  gap: '10px',
                }}
              >
                <div style={{ minWidth: 0 }}>
                  <div style={{ fontWeight: 700, fontSize: '0.94rem', color: '#fff', overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
                    {item.productName}
                  </div>
                  <div style={{ fontSize: '0.78rem', color: 'var(--text-muted)', overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
                    {item.shopName} • {item.shopLocation}
                  </div>
                </div>
                <div style={{ textAlign: 'right', flexShrink: 0 }}>
                  <div style={{ fontWeight: 800, color: '#ff6433', fontSize: '0.95rem' }}>
                    ₹{Number(item.price).toFixed(0)}
                  </div>
                  <div style={{ fontSize: '0.68rem', color: item.shopOpen ? '#34d399' : '#f87171' }}>
                    {item.shopOpen ? 'OPEN' : 'CLOSED'}
                  </div>
                </div>
              </Link>
            ))}
          </div>
        </div>
      )}

      {/* Shop Directory Status Bar & Grid */}
      {loading ? (
        <div style={{ textAlign: 'center', padding: '60px 0', color: 'var(--text-muted)' }}>
          Loading all approved shops upfront...
        </div>
      ) : (
        <>
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: '8px' }}>
            <div style={{ fontSize: '0.92rem', fontWeight: 700, color: '#fff', display: 'flex', alignItems: 'center', gap: '8px' }}>
              <Store size={17} color="#ff6433" />
              Showing {filteredShops.length} of {shops.length} shop{shops.length === 1 ? '' : 's'}
              {!searchTerm.trim() && !locationQuery.trim() && (
                <span style={{ fontSize: '0.78rem', fontWeight: 500, color: 'var(--text-muted)' }}>
                  — All shops loaded upfront
                </span>
              )}
            </div>
            {(searchTerm.trim() || locationQuery.trim()) && (
              <button
                type="button"
                className="btn btn-secondary btn-sm"
                onClick={() => {
                  setSearchTerm('');
                  setLocationQuery('');
                }}
              >
                Show All {shops.length} Shops
              </button>
            )}
          </div>

          {filteredShops.length === 0 ? (
            <div className="glass-panel" style={{ textAlign: 'center', padding: '60px 20px' }}>
              <Store size={48} color="var(--text-faint)" style={{ margin: '0 auto 16px' }} />
              <h3 style={{ marginBottom: '8px' }}>No matching shops or products found</h3>
              <p style={{ color: 'var(--text-muted)', fontSize: '0.9rem' }}>
                Try another dish name, shop name, or clear your location filter.
              </p>
            </div>
          ) : (
            <div
              style={{
                display: 'grid',
                gridTemplateColumns: 'repeat(auto-fill, minmax(320px, 1fr))',
                gap: '24px',
              }}
            >
              {filteredShops.map((shop) => {
                const matchingItems = shop.getMatchingProducts(searchTerm);
                const mapUrl = shop.getMapUrl();

                return (
                  <div key={shop.id} className="glass-panel" style={{ overflow: 'hidden', display: 'flex', flexDirection: 'column' }}>
                    <div style={{ height: '170px', position: 'relative', background: '#1c202a' }}>
                      <img
                        src={shop.imagePath || 'https://images.unsplash.com/photo-1555396273-367ea4eb4db5?w=600&auto=format&fit=crop'}
                        alt={shop.name}
                        loading="lazy"
                        decoding="async"
                        style={{ width: '100%', height: '100%', objectFit: 'cover' }}
                      />
                      <div style={{ position: 'absolute', top: '12px', right: '12px', display: 'flex', gap: '6px' }}>
                        {shop.isOpen ? (
                          <span className="badge badge-success"><Clock size={12} /> OPEN NOW</span>
                        ) : (
                          <span className="badge badge-danger"><Clock size={12} /> CLOSED</span>
                        )}
                      </div>
                    </div>

                    <div style={{ padding: '20px', flex: 1, display: 'flex', flexDirection: 'column', justifyContent: 'space-between' }}>
                      <div>
                        <h3 style={{ fontSize: '1.25rem', color: '#fff', marginBottom: '6px' }}>{shop.name}</h3>
                        <p
                          style={{
                            fontSize: '0.85rem',
                            color: 'var(--text-muted)',
                            marginBottom: '10px',
                            display: '-webkit-box',
                            WebkitLineClamp: 2,
                            WebkitBoxOrient: 'vertical',
                            overflow: 'hidden',
                          }}
                        >
                          {shop.description || 'Fresh meals and beverages prepared to order.'}
                        </p>

                        {matchingItems.length > 0 && (
                          <div
                            style={{
                              margin: '6px 0 12px 0',
                              padding: '6px 10px',
                              borderRadius: '8px',
                              background: 'rgba(255, 100, 51, 0.12)',
                              border: '1px solid rgba(255, 100, 51, 0.25)',
                              display: 'flex',
                              alignItems: 'center',
                              gap: '6px',
                              fontSize: '0.78rem',
                              color: '#ff6433',
                              fontWeight: 600,
                              flexWrap: 'wrap',
                            }}
                          >
                            <Sparkles size={13} />
                            <span>Item match:</span>
                            <span style={{ color: '#fff' }}>
                              {matchingItems.slice(0, 2).join(', ')}
                              {matchingItems.length > 2 ? ` +${matchingItems.length - 2} more` : ''}
                            </span>
                          </div>
                        )}

                        <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', gap: '8px', color: 'var(--text-faint)', fontSize: '0.82rem', marginBottom: '18px' }}>
                          <span style={{ display: 'flex', alignItems: 'center', gap: '6px' }}>
                            <MapPin size={14} color="#ff6433" />
                            <span>{shop.location || shop.campusLocation}</span>
                          </span>
                          {mapUrl && (
                            <a
                              href={mapUrl}
                              target="_blank"
                              rel="noreferrer"
                              style={{ color: 'var(--brand-primary)', display: 'inline-flex', alignItems: 'center', gap: '3px', fontSize: '0.75rem' }}
                            >
                              <ExternalLink size={12} /> Map
                            </a>
                          )}
                        </div>
                      </div>

                      <Link to={`/shop/${shop.id}`} className="btn btn-primary" style={{ width: '100%', justifyContent: 'center' }}>
                        <Utensils size={16} /> View Menu & Order
                      </Link>
                    </div>
                  </div>
                );
              })}
            </div>
          )}
        </>
      )}
    </div>
  );
};
