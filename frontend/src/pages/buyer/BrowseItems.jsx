import React, { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import { marketplaceService } from '../../services/MarketplaceService';
import { useCart } from '../../context/CartContext';
import { Product } from '../../models/Product';
import { Shop } from '../../models/Shop';
import {
  Sparkles,
  Search,
  MapPin,
  Star,
  Store,
  Plus,
  Minus,
  SlidersHorizontal,
  ArrowRight,
  Clock,
  Utensils,
} from 'lucide-react';

/**
 * Default Buyer Discovery Page ("/").
 * Shows food items ranked by top ratings and buyer interests (from previous purchases & profile),
 * or falls back seamlessly to the backend Cold-Start algorithm for fresh users.
 */
export const BrowseItems = () => {
  const { cart, addToCart, updateQuantity } = useCart();

  const [feed, setFeed] = useState({
    personalized: false,
    coldStart: true,
    strategySummary: '',
    activeInterests: [],
    items: [],
  });
  const [loading, setLoading] = useState(true);
  const [searchTerm, setSearchTerm] = useState('');
  const [locationQuery, setLocationQuery] = useState('');

  useEffect(() => {
    let active = true;
    const timer = setTimeout(async () => {
      try {
        const response = await marketplaceService.getRecommendedProducts(searchTerm, locationQuery);
        if (active && response) {
          setFeed({
            personalized: Boolean(response.personalized),
            coldStart: Boolean(response.coldStart),
            strategySummary: response.strategySummary || '',
            activeInterests: Array.isArray(response.activeInterests) ? response.activeInterests : [],
            items: Array.isArray(response.items) ? response.items : [],
          });
        }
      } catch (err) {
        console.error('Failed to load recommended items:', err);
      } finally {
        if (active) setLoading(false);
      }
    }, 150);

    return () => {
      active = false;
      clearTimeout(timer);
    };
  }, [searchTerm, locationQuery]);

  const getItemQuantity = (productId) => {
    const found = cart.find((i) => i.product.id === productId);
    return found ? found.quantity : 0;
  };

  const handleAddItem = (item) => {
    const productModel = new Product({
      id: item.productId,
      shopId: item.shopId,
      name: item.productName,
      description: item.description,
      price: item.price,
      stockQuantity: item.stockQuantity,
      imagePath: item.imagePath,
      available: true,
    });
    const shopModel = new Shop({
      id: item.shopId,
      name: item.shopName,
      location: item.shopLocation,
      latitude: item.shopLatitude,
      longitude: item.shopLongitude,
      approvalStatus: 'APPROVED',
      isOpen: item.shopOpen,
      isDeactivated: false,
    });
    addToCart(productModel, shopModel);
  };

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: '24px' }}>
      {/* Discovery Hero Banner */}
      <div
        className="glass-panel"
        style={{
          padding: '28px 32px',
          background: feed.personalized
            ? 'linear-gradient(135deg, rgba(255, 87, 34, 0.16) 0%, rgba(16, 185, 129, 0.08) 100%)'
            : 'linear-gradient(135deg, rgba(59, 130, 246, 0.15) 0%, rgba(255, 100, 51, 0.1) 100%)',
          border: '1px solid rgba(255, 100, 51, 0.25)',
          display: 'flex',
          justifyContent: 'space-between',
          alignItems: 'flex-start',
          flexWrap: 'wrap',
          gap: '16px',
        }}
      >
        <div style={{ maxWidth: '680px' }}>
          <div
            style={{
              display: 'inline-flex',
              alignItems: 'center',
              gap: '6px',
              color: '#ff6433',
              fontSize: '0.8rem',
              fontWeight: 700,
              marginBottom: '8px',
            }}
          >
            <Sparkles size={15} />
            <span>{feed.personalized ? 'PERSONALIZED DISCOVERY FEED' : 'SMART COLD-START TOP PICKS'}</span>
          </div>
          <h1 style={{ fontSize: '2rem', marginBottom: '8px', color: '#fff' }}>
            {feed.personalized ? 'Picked For Your Taste & Top Rated' : 'Top Rated Dishes Near You'}
          </h1>
          <p style={{ color: 'var(--text-muted)', fontSize: '0.94rem', lineHeight: '1.5' }}>
            {feed.strategySummary ||
              'Ranked automatically by ratings, profile preferences, past purchases, and location.'}
          </p>

          {feed.activeInterests.length > 0 && (
            <div style={{ display: 'flex', gap: '8px', flexWrap: 'wrap', marginTop: '12px', alignItems: 'center' }}>
              <span style={{ fontSize: '0.76rem', color: 'var(--text-muted)', fontWeight: 600 }}>Your Taste Signals:</span>
              {feed.activeInterests.map((tag) => (
                <span key={tag} className="badge badge-info" style={{ textTransform: 'none' }}>
                  {tag}
                </span>
              ))}
            </div>
          )}
        </div>

        <div style={{ display: 'flex', gap: '10px', flexWrap: 'wrap' }}>
          <Link to="/profile" className="btn btn-secondary btn-sm">
            <SlidersHorizontal size={14} /> Edit Interests & Location
          </Link>
          <Link to="/shops" className="btn btn-primary btn-sm">
            <Store size={14} /> Browse All Shops <ArrowRight size={14} />
          </Link>
        </div>
      </div>

      {/* Quick Filter Bar */}
      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(250px, 1fr))', gap: '12px' }}>
        <div style={{ position: 'relative' }}>
          <Search size={18} color="var(--text-faint)" style={{ position: 'absolute', left: '14px', top: '50%', transform: 'translateY(-50%)' }} />
          <input
            type="text"
            className="form-input"
            style={{ paddingLeft: '42px' }}
            placeholder="Search dishes, ingredients, or shop names..."
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
            placeholder="Filter by any location, area, or city..."
            value={locationQuery}
            onChange={(e) => setLocationQuery(e.target.value)}
          />
        </div>
      </div>

      {/* Recommended Food Items Grid */}
      {loading ? (
        <div style={{ textAlign: 'center', padding: '60px 0', color: 'var(--text-muted)' }}>
          Ranking dishes by top ratings and your interests...
        </div>
      ) : feed.items.length === 0 ? (
        <div className="glass-panel" style={{ textAlign: 'center', padding: '54px 20px' }}>
          <Utensils size={44} color="var(--text-faint)" style={{ margin: '0 auto 14px' }} />
          <h3 style={{ marginBottom: '8px' }}>No matching food items found</h3>
          <p style={{ color: 'var(--text-muted)', fontSize: '0.9rem', marginBottom: '16px' }}>
            Try clearing filters or browsing the full shop directory.
          </p>
          <Link to="/shops" className="btn btn-primary btn-sm">
            Browse All Shops
          </Link>
        </div>
      ) : (
        <div
          style={{
            display: 'grid',
            gridTemplateColumns: 'repeat(auto-fill, minmax(300px, 1fr))',
            gap: '22px',
          }}
        >
          {feed.items.map((item) => {
            const qty = getItemQuantity(item.productId);

            return (
              <div key={item.productId} className="glass-panel" style={{ overflow: 'hidden', display: 'flex', flexDirection: 'column' }}>
                <div style={{ height: '165px', position: 'relative', background: '#1c202a' }}>
                  <img
                    src={item.imagePath || 'https://images.unsplash.com/photo-1546069901-ba9599a7e63c?w=500&auto=format&fit=crop'}
                    alt={item.productName}
                    loading="lazy"
                    decoding="async"
                    style={{ width: '100%', height: '100%', objectFit: 'cover' }}
                  />
                  <div style={{ position: 'absolute', top: '10px', left: '10px', right: '10px', display: 'flex', justifyContent: 'space-between', alignItems: 'center', gap: '6px' }}>
                    <span
                      className="badge"
                      style={{
                        background: 'rgba(15, 23, 42, 0.88)',
                        color: '#fbbf24',
                        border: '1px solid rgba(251, 191, 36, 0.35)',
                      }}
                    >
                      <Star size={11} fill="#fbbf24" color="#fbbf24" /> {Number(item.averageRating || 4.2).toFixed(1)}
                      {item.ratingCount > 0 ? ` (${item.ratingCount})` : ''}
                    </span>

                    {item.shopOpen ? (
                      <span className="badge badge-success"><Clock size={11} /> OPEN</span>
                    ) : (
                      <span className="badge badge-danger"><Clock size={11} /> CLOSED</span>
                    )}
                  </div>
                </div>

                <div style={{ padding: '18px', flex: 1, display: 'flex', flexDirection: 'column', justifyContent: 'space-between', gap: '12px' }}>
                  <div>
                    <div
                      style={{
                        display: 'inline-flex',
                        alignItems: 'center',
                        gap: '5px',
                        padding: '3px 9px',
                        borderRadius: '999px',
                        fontSize: '0.72rem',
                        fontWeight: 700,
                        background: 'rgba(255, 100, 51, 0.14)',
                        color: '#ff8a65',
                        border: '1px solid rgba(255, 100, 51, 0.25)',
                        marginBottom: '8px',
                      }}
                    >
                      <Sparkles size={11} /> {item.recommendationReason}
                    </div>

                    <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', gap: '8px', marginBottom: '6px' }}>
                      <h3 style={{ fontSize: '1.12rem', color: '#fff', lineHeight: 1.3 }}>{item.productName}</h3>
                      <span style={{ fontSize: '1.12rem', fontWeight: 800, color: '#ff6433', flexShrink: 0 }}>
                        ₹{Number(item.price).toFixed(0)}
                      </span>
                    </div>

                    <p
                      style={{
                        fontSize: '0.83rem',
                        color: 'var(--text-muted)',
                        marginBottom: '10px',
                        display: '-webkit-box',
                        WebkitLineClamp: 2,
                        WebkitBoxOrient: 'vertical',
                        overflow: 'hidden',
                      }}
                    >
                      {item.description || 'Prepared fresh to order.'}
                    </p>

                    <div style={{ fontSize: '0.8rem', color: 'var(--text-faint)', display: 'flex', flexDirection: 'column', gap: '4px' }}>
                      <Link to={`/shop/${item.shopId}`} style={{ color: '#cbd5e1', fontWeight: 600, textDecoration: 'none', display: 'inline-flex', alignItems: 'center', gap: '5px' }}>
                        <Store size={13} color="#ff6433" /> {item.shopName}
                      </Link>
                      <span style={{ display: 'inline-flex', alignItems: 'center', gap: '5px' }}>
                        <MapPin size={13} color="var(--brand-primary)" /> {item.shopLocation}
                      </span>
                    </div>
                  </div>

                  <div style={{ display: 'flex', gap: '8px', alignItems: 'center', justifyContent: 'space-between', paddingTop: '6px', borderTop: '1px solid var(--border-subtle)' }}>
                    <Link to={`/shop/${item.shopId}`} className="btn btn-secondary btn-sm">
                      View Shop
                    </Link>

                    {!item.shopOpen ? (
                      <span style={{ fontSize: '0.78rem', color: 'var(--text-faint)' }}>Currently Closed</span>
                    ) : qty > 0 ? (
                      <div style={{ display: 'flex', alignItems: 'center', gap: '8px', background: 'var(--bg-surface)', padding: '4px 8px', borderRadius: '8px' }}>
                        <button
                          type="button"
                          onClick={() => updateQuantity(item.productId, -1)}
                          style={{ background: 'transparent', border: 'none', color: '#fff', cursor: 'pointer' }}
                        >
                          <Minus size={14} />
                        </button>
                        <span style={{ fontWeight: 700, fontSize: '0.9rem' }}>{qty}</span>
                        <button
                          type="button"
                          onClick={() => updateQuantity(item.productId, 1)}
                          style={{ background: 'transparent', border: 'none', color: '#fff', cursor: 'pointer' }}
                        >
                          <Plus size={14} />
                        </button>
                      </div>
                    ) : (
                      <button
                        type="button"
                        onClick={() => handleAddItem(item)}
                        className="btn btn-primary btn-sm"
                      >
                        <Plus size={14} /> Add to Cart
                      </button>
                    )}
                  </div>
                </div>
              </div>
            );
          })}
        </div>
      )}
    </div>
  );
};

