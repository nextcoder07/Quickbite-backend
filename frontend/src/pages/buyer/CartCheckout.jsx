import React, { useState } from 'react';
import { useCart } from '../../context/CartContext';
import { Link, useNavigate } from 'react-router-dom';
import { api } from '../../api/apiClient';
import { Trash2, Plus, Minus, ArrowLeft, ShoppingBag, ShieldCheck, CheckCircle2 } from 'lucide-react';
import { RazorpayModal } from '../../components/RazorpayModal';

export const CartCheckout = () => {
  const { cart, shop, subtotal, deliveryFee, totalAmount, updateQuantity, removeFromCart, clearCart } = useCart();
  const navigate = useNavigate();

  const [buyerNote, setBuyerNote] = useState('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  // Payment Modal state
  const [activePaymentOrder, setActivePaymentOrder] = useState(null);
  const [isPaymentModalOpen, setIsPaymentModalOpen] = useState(false);

  if (cart.length === 0) {
    return (
      <div className="glass-panel" style={{ padding: '60px 20px', textAlign: 'center', maxWidth: '500px', margin: '40px auto' }}>
        <ShoppingBag size={56} color="var(--text-faint)" style={{ margin: '0 auto 16px' }} />
        <h2 style={{ marginBottom: '8px' }}>Your Cart is Empty</h2>
        <p style={{ color: 'var(--text-muted)', fontSize: '0.9rem', marginBottom: '24px' }}>
          Explore delicious meals and drinks from campus shops.
        </p>
        <Link to="/" className="btn btn-primary">Browse Campus Shops</Link>
      </div>
    );
  }

  const handleInitiateCheckout = async () => {
    setLoading(true);
    setError('');

    try {
      // 1. Backend Checkout API
      const checkoutPayload = {
        shopId: shop.id,
        items: cart.map((item) => ({
          productId: item.product.id,
          quantity: item.quantity,
        })),
        buyerNote: buyerNote ? buyerNote.trim() : null,
      };

      const order = await api.post('/orders/checkout', checkoutPayload);

      // 2. Initialize Payment Order on Server
      const paymentOrder = await api.post('/payments/create-order', {
        orderId: order.id,
      });

      setActivePaymentOrder({
        orderId: order.id,
        orderNumber: order.orderNumber,
        amount: order.totalAmount,
      });

      setIsPaymentModalOpen(true);
    } catch (err) {
      setError(err.message || 'Checkout failed');
    } finally {
      setLoading(false);
    }
  };

  const handlePaymentSuccess = async (verifyData) => {
    // 3. Server-side signature verification
    await api.post('/payments/verify', verifyData);
    clearCart();
    navigate('/orders');
  };

  return (
    <div style={{ maxWidth: '860px', margin: '0 auto', display: 'flex', flexDirection: 'column', gap: '24px' }}>
      <div>
        <Link to={`/shop/${shop.id}`} className="btn btn-secondary btn-sm" style={{ display: 'inline-flex', alignItems: 'center', gap: '6px' }}>
          <ArrowLeft size={16} /> Back to {shop.name}
        </Link>
      </div>

      <h1 style={{ fontSize: '1.8rem' }}>Order Review & Checkout</h1>

      {error && (
        <div style={{
          padding: '12px 16px',
          borderRadius: '10px',
          background: 'var(--danger-bg)',
          color: 'var(--danger)',
          fontSize: '0.88rem'
        }}>
          {error}
        </div>
      )}

      <div style={{ display: 'grid', gridTemplateColumns: '1.4fr 1fr', gap: '24px' }}>
        {/* Cart Items List */}
        <div style={{ display: 'flex', flexDirection: 'column', gap: '16px' }}>
          <div className="glass-panel" style={{ padding: '20px' }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '16px' }}>
              <div>
                <h3 style={{ fontSize: '1.15rem' }}>Items from {shop.name}</h3>
                <span style={{ fontSize: '0.8rem', color: 'var(--text-muted)' }}>{shop.campusLocation}</span>
              </div>
              <button
                onClick={clearCart}
                className="btn btn-secondary btn-sm"
                style={{ color: 'var(--danger)' }}
              >
                Clear
              </button>
            </div>

            <div style={{ display: 'flex', flexDirection: 'column', gap: '12px' }}>
              {cart.map((item) => (
                <div
                  key={item.product.id}
                  style={{
                    display: 'flex',
                    alignItems: 'center',
                    justifyContent: 'space-between',
                    padding: '12px',
                    borderRadius: '10px',
                    background: 'var(--bg-surface)',
                    border: '1px solid var(--border-subtle)'
                  }}
                >
                  <div style={{ display: 'flex', alignItems: 'center', gap: '12px' }}>
                    <img
                      src={item.product.imagePath || 'https://images.unsplash.com/photo-1546069901-ba9599a7e63c?w=120'}
                      alt={item.product.name}
                      style={{ width: '48px', height: '48px', borderRadius: '8px', objectFit: 'cover' }}
                    />
                    <div>
                      <div style={{ fontWeight: 600, fontSize: '0.92rem' }}>{item.product.name}</div>
                      <div style={{ fontSize: '0.8rem', color: 'var(--primary)' }}>
                        ₹{Number(item.product.price).toFixed(2)} each
                      </div>
                    </div>
                  </div>

                  <div style={{ display: 'flex', alignItems: 'center', gap: '14px' }}>
                    <div style={{
                      display: 'flex',
                      alignItems: 'center',
                      gap: '8px',
                      background: 'var(--bg-secondary)',
                      borderRadius: '6px',
                      padding: '3px 6px'
                    }}>
                      <button
                        onClick={() => updateQuantity(item.product.id, -1)}
                        style={{ background: 'transparent', border: 'none', color: '#fff', cursor: 'pointer' }}
                      >
                        <Minus size={12} />
                      </button>
                      <span style={{ fontWeight: 700, fontSize: '0.85rem', minWidth: '16px', textAlign: 'center' }}>
                        {item.quantity}
                      </span>
                      <button
                        onClick={() => updateQuantity(item.product.id, 1)}
                        style={{ background: 'transparent', border: 'none', color: '#fff', cursor: 'pointer' }}
                      >
                        <Plus size={12} />
                      </button>
                    </div>

                    <div style={{ fontWeight: 700, fontSize: '0.95rem', minWidth: '60px', textAlign: 'right' }}>
                      ₹{(Number(item.product.price) * item.quantity).toFixed(2)}
                    </div>

                    <button
                      onClick={() => removeFromCart(item.product.id)}
                      style={{ background: 'transparent', border: 'none', color: 'var(--text-faint)', cursor: 'pointer' }}
                    >
                      <Trash2 size={16} />
                    </button>
                  </div>
                </div>
              ))}
            </div>
          </div>

          {/* Delivery Note */}
          <div className="glass-panel" style={{ padding: '20px' }}>
            <h4 style={{ fontSize: '0.95rem', marginBottom: '8px' }}>Delivery / Special Instructions</h4>
            <textarea
              className="form-textarea"
              rows={3}
              placeholder="e.g. Leave at SAC Room 204 or extra green chutney..."
              value={buyerNote}
              onChange={(e) => setBuyerNote(e.target.value)}
            />
          </div>
        </div>

        {/* Pricing Summary & Checkout Button */}
        <div>
          <div className="glass-panel" style={{ padding: '24px', display: 'flex', flexDirection: 'column', gap: '18px' }}>
            <h3 style={{ fontSize: '1.2rem' }}>Bill Breakdown</h3>

            <div style={{ display: 'flex', flexDirection: 'column', gap: '10px', fontSize: '0.9rem' }}>
              <div style={{ display: 'flex', justifyContent: 'space-between', color: 'var(--text-muted)' }}>
                <span>Items Subtotal</span>
                <span style={{ color: '#fff' }}>₹{subtotal.toFixed(2)}</span>
              </div>

              <div style={{ display: 'flex', justifyContent: 'space-between', color: 'var(--text-muted)' }}>
                <span>Campus Delivery Fee</span>
                {deliveryFee === 0 ? (
                  <span style={{ color: 'var(--success)', fontWeight: 700 }}>FREE (Orders &gt; ₹300)</span>
                ) : (
                  <span style={{ color: '#fff' }}>₹{deliveryFee.toFixed(2)}</span>
                )}
              </div>

              <div style={{
                borderTop: '1px solid var(--border-subtle)',
                paddingTop: '12px',
                marginTop: '4px',
                display: 'flex',
                justifyContent: 'space-between',
                fontSize: '1.2rem',
                fontWeight: 800
              }}>
                <span>Total Amount</span>
                <span style={{ color: 'var(--primary)' }}>₹{totalAmount.toFixed(2)}</span>
              </div>
            </div>

            <button
              onClick={handleInitiateCheckout}
              disabled={loading}
              className="btn btn-primary"
              style={{ width: '100%', padding: '14px', fontSize: '1rem', marginTop: '6px' }}
            >
              {loading ? 'Processing Order...' : `Proceed to Pay ₹${totalAmount.toFixed(2)}`}
            </button>

            <div style={{ display: 'flex', alignItems: 'center', gap: '6px', color: 'var(--text-faint)', fontSize: '0.78rem', justifyContent: 'center' }}>
              <ShieldCheck size={14} color="#10b981" />
              <span>Razorpay Test Mode Verified</span>
            </div>
          </div>
        </div>
      </div>

      {/* Razorpay Test Modal */}
      {isPaymentModalOpen && activePaymentOrder && (
        <RazorpayModal
          isOpen={isPaymentModalOpen}
          onClose={() => setIsPaymentModalOpen(false)}
          orderId={activePaymentOrder.orderId}
          orderNumber={activePaymentOrder.orderNumber}
          amount={activePaymentOrder.amount}
          onPaymentSuccess={handlePaymentSuccess}
        />
      )}
    </div>
  );
};
