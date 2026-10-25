import React, { useState, useEffect } from 'react';
import { useParams, Link } from 'react-router-dom';
import { api } from '../../api/apiClient';
import { useCart } from '../../context/CartContext';
import { MapPin, ArrowLeft, Plus, Minus, ShoppingBag, Clock, AlertCircle } from 'lucide-react';

export const ShopDetail = () => {
  const { shopId } = useParams();
  const { cart, addToCart, updateQuantity } = useCart();
  const [shop, setShop] = useState(null);
  const [products, setProducts] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  useEffect(() => {
    const fetchShopAndProducts = async () => {
      try {
        const [shopData, productsData] = await Promise.all([
          api.get(`/marketplace/shops/${shopId}`),
          api.get(`/marketplace/shops/${shopId}/products`),
        ]);
        setShop(shopData);
        setProducts(productsData);
      } catch (err) {
        setError(err.message || 'Failed to load shop details');
      } finally {
        setLoading(false);
      }
    };

    fetchShopAndProducts();
  }, [shopId]);

  const getItemQuantity = (productId) => {
    const item = cart.find((i) => i.product.id === productId);
    return item ? item.quantity : 0;
  };

  if (loading) {
    return <div style={{ textAlign: 'center', padding: '60px', color: 'var(--text-muted)' }}>Loading shop menu...</div>;
  }

  if (error || !shop) {
    return (
      <div className="glass-panel" style={{ padding: '36px', textAlign: 'center' }}>
        <AlertCircle size={40} color="var(--danger)" style={{ margin: '0 auto 12px' }} />
        <h3>{error || 'Shop not found'}</h3>
        <Link to="/" className="btn btn-secondary" style={{ marginTop: '16px' }}>Back to Shops</Link>
      </div>
    );
  }

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: '28px' }}>
      {/* Back button */}
      <div>
        <Link to="/" className="btn btn-secondary btn-sm" style={{ display: 'inline-flex', alignItems: 'center', gap: '6px' }}>
          <ArrowLeft size={16} /> Back to all shops
        </Link>
      </div>

      {/* Shop Header Banner */}
      <div className="glass-panel" style={{ overflow: 'hidden' }}>
        <div style={{ height: '220px', position: 'relative', background: '#191d26' }}>
          <img
            src={shop.imagePath || 'https://images.unsplash.com/photo-1555396273-367ea4eb4db5?w=1000&auto=format&fit=crop'}
            alt={shop.name}
            style={{ width: '100%', height: '100%', objectFit: 'cover' }}
          />
          <div style={{
            position: 'absolute',
            inset: 0,
            background: 'linear-gradient(to top, rgba(12, 13, 18, 0.95) 0%, rgba(12, 13, 18, 0.3) 100%)'
          }} />

          <div style={{
            position: 'absolute',
            bottom: '24px',
            left: '28px',
            right: '28px',
            display: 'flex',
            justifyContent: 'space-between',
            alignItems: 'flex-end'
          }}>
            <div>
              <h1 style={{ fontSize: '2rem', color: '#fff', marginBottom: '6px' }}>{shop.name}</h1>
              <div style={{ display: 'flex', alignItems: 'center', gap: '8px', color: '#cbd5e1', fontSize: '0.9rem' }}>
                <MapPin size={16} color="#ff6433" />
                <span>{shop.campusLocation}</span>
              </div>
            </div>

            <div>
              {shop.open ? (
                <span className="badge badge-success" style={{ padding: '6px 14px', fontSize: '0.85rem' }}>
                  <Clock size={14} /> ACCEPTING ORDERS
                </span>
              ) : (
                <span className="badge badge-danger" style={{ padding: '6px 14px', fontSize: '0.85rem' }}>
                  <Clock size={14} /> CURRENTLY CLOSED
                </span>
              )}
            </div>
          </div>
        </div>

        {shop.description && (
          <div style={{ padding: '16px 28px', background: 'rgba(255,255,255,0.02)', color: 'var(--text-muted)', fontSize: '0.9rem', borderTop: '1px solid var(--border-subtle)' }}>
            {shop.description}
          </div>
        )}
      </div>

      {!shop.open && (
        <div style={{
          padding: '14px 20px',
          background: 'rgba(239, 68, 68, 0.12)',
          border: '1px solid rgba(239, 68, 68, 0.25)',
          borderRadius: '12px',
          color: '#f87171',
          display: 'flex',
          alignItems: 'center',
          gap: '10px'
        }}>
          <AlertCircle size={20} />
          <span>This shop is currently closed. You can view the menu, but checkout is disabled until the shop re-opens.</span>
        </div>
      )}

      {/* Menu Header */}
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
        <h2 style={{ fontSize: '1.4rem' }}>Menu Items ({products.length})</h2>
      </div>

      {/* Product Grid */}
      {products.length === 0 ? (
        <div className="glass-panel" style={{ padding: '40px', textAlign: 'center', color: 'var(--text-muted)' }}>
          No available products listed for this shop yet.
        </div>
      ) : (
        <div style={{
          display: 'grid',
          gridTemplateColumns: 'repeat(auto-fill, minmax(280px, 1fr))',
          gap: '20px'
        }}>
          {products.map((product) => {
            const quantity = getItemQuantity(product.id);
            const inStock = product.stockQuantity > 0 && product.available;

            return (
              <div
                key={product.id}
                className="glass-panel"
                style={{
                  display: 'flex',
                  flexDirection: 'column',
                  overflow: 'hidden',
                  opacity: inStock ? 1 : 0.6
                }}
              >
                {/* Product Image */}
                <div style={{ height: '150px', background: '#1c202a', position: 'relative' }}>
                  <img
                    src={product.imagePath || 'https://images.unsplash.com/photo-1546069901-ba9599a7e63c?w=500&auto=format&fit=crop'}
                    alt={product.name}
                    style={{ width: '100%', height: '100%', objectFit: 'cover' }}
                  />
                  {!inStock && (
                    <div style={{
                      position: 'absolute',
                      inset: 0,
                      background: 'rgba(0,0,0,0.6)',
                      display: 'flex',
                      alignItems: 'center',
                      justifyContent: 'center',
                      color: '#fff',
                      fontWeight: 700,
                      fontSize: '0.85rem'
                    }}>
                      SOLD OUT
                    </div>
                  )}
                </div>

                {/* Info */}
                <div style={{ padding: '16px', flex: 1, display: 'flex', flexDirection: 'column', justifyContent: 'space-between' }}>
                  <div>
                    <h3 style={{ fontSize: '1.1rem', marginBottom: '4px' }}>{product.name}</h3>
                    <p style={{
                      fontSize: '0.82rem',
                      color: 'var(--text-muted)',
                      marginBottom: '12px',
                      display: '-webkit-box',
                      WebkitLineClamp: 2,
                      WebkitBoxOrient: 'vertical',
                      overflow: 'hidden'
                    }}>
                      {product.description || 'Prepared fresh upon order.'}
                    </p>
                  </div>

                  <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', paddingTop: '10px', borderTop: '1px solid var(--border-subtle)' }}>
                    <div>
                      <div style={{ fontSize: '1.25rem', fontWeight: 800, color: 'var(--primary)' }}>
                        ₹{Number(product.price).toFixed(2)}
                      </div>
                      <div style={{ fontSize: '0.72rem', color: 'var(--text-faint)' }}>
                        {inStock ? `${product.stockQuantity} left in stock` : 'Out of stock'}
                      </div>
                    </div>

                    {inStock && shop.open && (
                      <div>
                        {quantity === 0 ? (
                          <button
                            onClick={() => addToCart(product, shop)}
                            className="btn btn-primary btn-sm"
                          >
                            <Plus size={14} /> Add
                          </button>
                        ) : (
                          <div style={{
                            display: 'flex',
                            alignItems: 'center',
                            gap: '8px',
                            background: 'var(--bg-surface)',
                            border: '1px solid var(--border-strong)',
                            borderRadius: '8px',
                            padding: '4px 8px'
                          }}>
                            <button
                              onClick={() => updateQuantity(product.id, -1)}
                              style={{ background: 'transparent', border: 'none', color: '#fff', cursor: 'pointer' }}
                            >
                              <Minus size={14} />
                            </button>
                            <span style={{ fontWeight: 700, fontSize: '0.9rem', minWidth: '18px', textAlign: 'center' }}>
                              {quantity}
                            </span>
                            <button
                              onClick={() => updateQuantity(product.id, 1)}
                              style={{ background: 'transparent', border: 'none', color: '#fff', cursor: 'pointer' }}
                            >
                              <Plus size={14} />
                            </button>
                          </div>
                        )}
                      </div>
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
