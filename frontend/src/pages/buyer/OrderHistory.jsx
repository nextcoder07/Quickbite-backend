import React, { useState, useEffect } from 'react';
import { api } from '../../api/apiClient';
import { StatusBadge } from '../../components/StatusBadge';
import { 
  Clock, 
  Store, 
  MapPin, 
  RotateCcw, 
  Star, 
  AlertCircle, 
  CheckCircle2, 
  X,
  XCircle,
  MessageSquare
} from 'lucide-react';

export const OrderHistory = () => {
  const [orders, setOrders] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  // Modals
  const [feedbackOrder, setFeedbackOrder] = useState(null);
  const [rating, setRating] = useState(5);
  const [comment, setComment] = useState('');
  const [feedbackSubmitting, setFeedbackSubmitting] = useState(false);

  const [disputeOrder, setDisputeOrder] = useState(null);
  const [disputeCategory, setDisputeCategory] = useState('MISSING_ITEMS');
  const [disputeDescription, setDisputeDescription] = useState('');
  const [disputeSubmitting, setDisputeSubmitting] = useState(false);

  const fetchOrders = async () => {
    try {
      const data = await api.get('/orders/buyer');
      setOrders(data);
    } catch (err) {
      setError(err.message || 'Failed to fetch orders');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchOrders();
    const interval = setInterval(fetchOrders, 6000); // Poll order status updates
    return () => clearInterval(interval);
  }, []);

  const handleCancelOrder = async (orderId) => {
    const reason = prompt('Please enter cancellation reason:', 'Changed my mind');
    if (reason === null) return;

    try {
      await api.post(`/orders/${orderId}/cancel`, { reason });
      fetchOrders();
    } catch (err) {
      alert(err.message || 'Failed to cancel order');
    }
  };

  const handleFeedbackSubmit = async (e) => {
    e.preventDefault();
    if (!feedbackOrder) return;
    setFeedbackSubmitting(true);
    try {
      await api.post('/feedback', {
        orderId: feedbackOrder.id,
        rating,
        comment,
      });
      alert('Thank you! Your review has been submitted.');
      setFeedbackOrder(null);
      setComment('');
    } catch (err) {
      alert(err.message || 'Failed to submit review');
    } finally {
      setFeedbackSubmitting(false);
    }
  };

  const handleDisputeSubmit = async (e) => {
    e.preventDefault();
    if (!disputeOrder) return;
    setDisputeSubmitting(true);
    try {
      await api.post('/disputes', {
        orderId: disputeOrder.id,
        category: disputeCategory,
        description: disputeDescription,
      });
      alert('Dispute submitted for campus administrator moderation.');
      setDisputeOrder(null);
      setDisputeDescription('');
    } catch (err) {
      alert(err.message || 'Failed to raise dispute');
    } finally {
      setDisputeSubmitting(false);
    }
  };

  return (
    <div style={{ maxWidth: '900px', margin: '0 auto', display: 'flex', flexDirection: 'column', gap: '24px' }}>
      <div>
        <h1 style={{ fontSize: '1.8rem', marginBottom: '4px' }}>Order Tracking & History</h1>
        <p style={{ color: 'var(--text-muted)', fontSize: '0.9rem' }}>
          Real-time status updates from campus kitchens and delivery partners
        </p>
      </div>

      {loading ? (
        <div style={{ textAlign: 'center', padding: '60px', color: 'var(--text-muted)' }}>Loading orders...</div>
      ) : orders.length === 0 ? (
        <div className="glass-panel" style={{ padding: '60px 20px', textAlign: 'center' }}>
          <Clock size={48} color="var(--text-faint)" style={{ margin: '0 auto 16px' }} />
          <h3>No Orders Placed Yet</h3>
          <p style={{ color: 'var(--text-muted)', fontSize: '0.9rem', marginTop: '6px' }}>
            Browse shops to place your first campus food order.
          </p>
        </div>
      ) : (
        <div style={{ display: 'flex', flexDirection: 'column', gap: '20px' }}>
          {orders.map((order) => {
            const canCancel = order.status === 'PLACED' || order.status === 'ACCEPTED';
            const isDelivered = order.status === 'DELIVERED';

            return (
              <div key={order.id} className="glass-panel" style={{ padding: '24px' }}>
                {/* Header */}
                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', marginBottom: '16px', flexWrap: 'wrap', gap: '10px' }}>
                  <div>
                    <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
                      <span style={{ fontSize: '1.15rem', fontWeight: 800, color: '#fff' }}>#{order.orderNumber}</span>
                      <StatusBadge status={order.status} />
                    </div>
                    <div style={{ display: 'flex', alignItems: 'center', gap: '8px', color: 'var(--text-muted)', fontSize: '0.82rem', marginTop: '4px' }}>
                      <Store size={14} color="#ff6433" />
                      <span>{order.shopName}</span>
                      <span>•</span>
                      <span>{new Date(order.placedAt).toLocaleString()}</span>
                    </div>
                  </div>

                  <div style={{ textAlign: 'right' }}>
                    <div style={{ fontSize: '1.3rem', fontWeight: 800, color: 'var(--primary)' }}>
                      ₹{Number(order.totalAmount).toFixed(2)}
                    </div>
                    <div style={{ fontSize: '0.75rem', color: 'var(--text-faint)' }}>
                      {order.items.length} item{order.items.length > 1 ? 's' : ''}
                    </div>
                  </div>
                </div>

                {/* Items snapshot list */}
                <div style={{
                  padding: '12px 16px',
                  borderRadius: '10px',
                  background: 'var(--bg-surface)',
                  marginBottom: '18px'
                }}>
                  {order.items.map((item) => (
                    <div
                      key={item.id}
                      style={{
                        display: 'flex',
                        justifyContent: 'space-between',
                        padding: '6px 0',
                        fontSize: '0.88rem',
                        borderBottom: '1px solid rgba(255,255,255,0.04)'
                      }}
                    >
                      <span style={{ color: 'var(--text-main)' }}>
                        {item.quantity}x {item.productName}
                      </span>
                      <span style={{ fontWeight: 600 }}>₹{Number(item.lineTotal).toFixed(2)}</span>
                    </div>
                  ))}
                  <div style={{ display: 'flex', justifyContent: 'space-between', paddingTop: '8px', fontSize: '0.8rem', color: 'var(--text-muted)' }}>
                    <span>Delivery Fee</span>
                    <span>₹{Number(order.deliveryFee).toFixed(2)}</span>
                  </div>
                </div>

                {/* Action buttons */}
                <div style={{ display: 'flex', gap: '10px', justifyContent: 'flex-end', flexWrap: 'wrap' }}>
                  {canCancel && (
                    <button
                      onClick={() => handleCancelOrder(order.id)}
                      className="btn btn-danger btn-sm"
                    >
                      <XCircle size={14} /> Cancel Order
                    </button>
                  )}

                  {isDelivered && (
                    <button
                      onClick={() => setFeedbackOrder(order)}
                      className="btn btn-secondary btn-sm"
                    >
                      <Star size={14} color="#f59e0b" /> Rate & Review
                    </button>
                  )}

                  <button
                    onClick={() => setDisputeOrder(order)}
                    className="btn btn-secondary btn-sm"
                  >
                    <AlertCircle size={14} color="#ef4444" /> Raise Dispute
                  </button>
                </div>
              </div>
            );
          })}
        </div>
      )}

      {/* Review Modal */}
      {feedbackOrder && (
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
          <div className="glass-panel" style={{ width: '100%', maxWidth: '420px', padding: '28px', background: '#171b24' }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '16px' }}>
              <h3 style={{ fontSize: '1.2rem' }}>Rate Order #{feedbackOrder.orderNumber}</h3>
              <button onClick={() => setFeedbackOrder(null)} style={{ background: 'transparent', border: 'none', color: '#fff', cursor: 'pointer' }}>
                <X size={18} />
              </button>
            </div>

            <form onSubmit={handleFeedbackSubmit}>
              <div className="form-group">
                <label className="form-label">Rating (1 to 5 Stars)</label>
                <div style={{ display: 'flex', gap: '10px', margin: '8px 0' }}>
                  {[1, 2, 3, 4, 5].map((star) => (
                    <Star
                      key={star}
                      size={28}
                      onClick={() => setRating(star)}
                      color={star <= rating ? '#f59e0b' : '#475569'}
                      fill={star <= rating ? '#f59e0b' : 'none'}
                      style={{ cursor: 'pointer' }}
                    />
                  ))}
                </div>
              </div>

              <div className="form-group">
                <label className="form-label">Review Comment (Optional)</label>
                <textarea
                  className="form-textarea"
                  rows={3}
                  placeholder="How was the food and packaging?"
                  value={comment}
                  onChange={(e) => setComment(e.target.value)}
                />
              </div>

              <button type="submit" disabled={feedbackSubmitting} className="btn btn-primary" style={{ width: '100%', marginTop: '10px' }}>
                {feedbackSubmitting ? 'Submitting...' : 'Submit Review'}
              </button>
            </form>
          </div>
        </div>
      )}

      {/* Dispute Modal */}
      {disputeOrder && (
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
          <div className="glass-panel" style={{ width: '100%', maxWidth: '440px', padding: '28px', background: '#171b24' }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '16px' }}>
              <h3 style={{ fontSize: '1.2rem', color: '#f87171' }}>Raise Order Dispute</h3>
              <button onClick={() => setDisputeOrder(null)} style={{ background: 'transparent', border: 'none', color: '#fff', cursor: 'pointer' }}>
                <X size={18} />
              </button>
            </div>

            <form onSubmit={handleDisputeSubmit}>
              <div className="form-group">
                <label className="form-label">Dispute Category</label>
                <select
                  className="form-select"
                  value={disputeCategory}
                  onChange={(e) => setDisputeCategory(e.target.value)}
                >
                  <option value="MISSING_ITEMS">Missing Items</option>
                  <option value="WRONG_ITEMS">Wrong Items Delivered</option>
                  <option value="FOOD_QUALITY">Food Quality / Spoiled</option>
                  <option value="DELIVERY_DELAY">Extreme Delivery Delay</option>
                  <option value="PAYMENT_ISSUE">Payment / Billing Discrepancy</option>
                  <option value="OTHER">Other Issue</option>
                </select>
              </div>

              <div className="form-group">
                <label className="form-label">Detailed Explanation</label>
                <textarea
                  className="form-textarea"
                  rows={4}
                  placeholder="Please describe the issue clearly so campus admins can investigate..."
                  value={disputeDescription}
                  onChange={(e) => setDisputeDescription(e.target.value)}
                  required
                />
              </div>

              <button type="submit" disabled={disputeSubmitting} className="btn btn-danger" style={{ width: '100%', marginTop: '10px' }}>
                {disputeSubmitting ? 'Filing Dispute...' : 'Submit to Admin Moderation'}
              </button>
            </form>
          </div>
        </div>
      )}
    </div>
  );
};
