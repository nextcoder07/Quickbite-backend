import React, { useState, useEffect } from 'react';
import { api } from '../../api/apiClient';
import { StatusBadge } from '../../components/StatusBadge';
import { ShoppingBag, Clock, Check, X, ChefHat, CheckCircle2, MessageSquare } from 'lucide-react';

export const IncomingOrders = () => {
  const [orders, setOrders] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  const fetchShopOrders = async () => {
    try {
      const data = await api.get('/orders/shop');
      setOrders(data);
    } catch (err) {
      setError(err.message || 'Failed to load shop orders');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchShopOrders();
    const interval = setInterval(fetchShopOrders, 5000); // 5s kitchen order polling
    return () => clearInterval(interval);
  }, []);

  const handleUpdateStatus = async (orderId, newStatus) => {
    try {
      await api.patch(`/orders/${orderId}/status`, { status: newStatus });
      fetchShopOrders();
    } catch (err) {
      alert(err.message || 'Status transition failed');
    }
  };

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: '24px' }}>
      <div>
        <h1 style={{ fontSize: '1.8rem', marginBottom: '4px' }}>Live Kitchen Orders</h1>
        <p style={{ color: 'var(--text-muted)', fontSize: '0.9rem' }}>
          Accept incoming student orders and manage food preparation stages
        </p>
      </div>

      {error && (
        <div style={{ padding: '12px 16px', borderRadius: '10px', background: 'var(--danger-bg)', color: 'var(--danger)' }}>
          {error}
        </div>
      )}

      {loading ? (
        <div style={{ textAlign: 'center', padding: '60px', color: 'var(--text-muted)' }}>Loading incoming orders...</div>
      ) : orders.length === 0 ? (
        <div className="glass-panel" style={{ padding: '60px 20px', textAlign: 'center' }}>
          <ShoppingBag size={48} color="var(--text-faint)" style={{ margin: '0 auto 16px' }} />
          <h3>No Orders Received Yet</h3>
          <p style={{ color: 'var(--text-muted)', fontSize: '0.9rem', marginTop: '6px' }}>
            New student orders will automatically appear here in real time.
          </p>
        </div>
      ) : (
        <div style={{ display: 'flex', flexDirection: 'column', gap: '18px' }}>
          {orders.map((order) => (
            <div key={order.id} className="glass-panel" style={{ padding: '22px' }}>
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', marginBottom: '14px', flexWrap: 'wrap', gap: '10px' }}>
                <div>
                  <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
                    <span style={{ fontSize: '1.15rem', fontWeight: 800 }}>#{order.orderNumber}</span>
                    <StatusBadge status={order.status} />
                  </div>
                  <div style={{ fontSize: '0.85rem', color: 'var(--text-muted)', marginTop: '4px' }}>
                    Buyer: <strong style={{ color: '#fff' }}>{order.buyerName}</strong> • Placed at {new Date(order.placedAt).toLocaleTimeString()}
                  </div>
                </div>

                <div style={{ textAlign: 'right' }}>
                  <div style={{ fontSize: '1.3rem', fontWeight: 800, color: 'var(--primary)' }}>
                    ₹{Number(order.totalAmount).toFixed(2)}
                  </div>
                </div>
              </div>

              {/* Items List */}
              <div style={{ padding: '12px 16px', background: 'var(--bg-surface)', borderRadius: '10px', marginBottom: '16px' }}>
                {order.items.map((item) => (
                  <div key={item.id} style={{ display: 'flex', justifyContent: 'space-between', padding: '4px 0', fontSize: '0.88rem' }}>
                    <span>{item.quantity}x {item.productName}</span>
                    <span style={{ fontWeight: 600 }}>₹{Number(item.lineTotal).toFixed(2)}</span>
                  </div>
                ))}
              </div>

              {order.buyerNote && (
                <div style={{
                  padding: '10px 14px',
                  background: 'rgba(255, 87, 34, 0.08)',
                  border: '1px solid rgba(255, 87, 34, 0.2)',
                  borderRadius: '8px',
                  fontSize: '0.82rem',
                  color: '#fdba74',
                  marginBottom: '16px',
                  display: 'flex',
                  alignItems: 'center',
                  gap: '8px'
                }}>
                  <MessageSquare size={16} />
                  <span><strong>Student Note:</strong> {order.buyerNote}</span>
                </div>
              )}

              {/* Action Buttons */}
              <div style={{ display: 'flex', gap: '10px', justifyContent: 'flex-end', flexWrap: 'wrap' }}>
                {order.status === 'PLACED' && (
                  <>
                    <button
                      onClick={() => handleUpdateStatus(order.id, 'ACCEPTED')}
                      className="btn btn-success btn-sm"
                    >
                      <Check size={14} /> Accept Order
                    </button>
                    <button
                      onClick={() => handleUpdateStatus(order.id, 'REJECTED')}
                      className="btn btn-danger btn-sm"
                    >
                      <X size={14} /> Reject Order
                    </button>
                  </>
                )}

                {order.status === 'ACCEPTED' && (
                  <button
                    onClick={() => handleUpdateStatus(order.id, 'PREPARING')}
                    className="btn btn-primary btn-sm"
                  >
                    <ChefHat size={14} /> Start Preparing
                  </button>
                )}

                {order.status === 'PREPARING' && (
                  <button
                    onClick={() => handleUpdateStatus(order.id, 'READY')}
                    className="btn btn-success btn-sm"
                  >
                    <CheckCircle2 size={14} /> Mark Ready for Pickup
                  </button>
                )}

                {order.status === 'READY' && (
                  <span style={{ fontSize: '0.85rem', color: '#2dd4bf', fontWeight: 600, display: 'flex', alignItems: 'center', gap: '6px' }}>
                    <CheckCircle2 size={16} /> Ready: Awaiting Delivery Partner Pickup
                  </span>
                )}
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  );
};
