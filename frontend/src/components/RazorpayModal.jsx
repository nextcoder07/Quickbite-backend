import React, { useState } from 'react';
import { CreditCard, Smartphone, Building, ShieldCheck, X, CheckCircle, AlertTriangle } from 'lucide-react';

export const RazorpayModal = ({ isOpen, onClose, orderId, orderNumber, amount, onPaymentSuccess }) => {
  const [selectedMethod, setSelectedMethod] = useState('card');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  if (!isOpen) return null;

  const handleSimulatePayment = async () => {
    setLoading(true);
    setError('');

    try {
      // Simulate Razorpay client callback
      const mockPaymentId = `pay_test_${Date.now().toString(36)}${Math.random().toString(36).substring(2, 6)}`;
      const mockOrderId = `order_test_${orderId}`;
      const mockSignature = 'test_signature';

      await onPaymentSuccess({
        orderId,
        providerOrderId: mockOrderId,
        providerPaymentId: mockPaymentId,
        signature: mockSignature,
      });

      onClose();
    } catch (err) {
      setError(err.message || 'Payment simulation failed');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div style={{
      position: 'fixed',
      top: 0,
      left: 0,
      right: 0,
      bottom: 0,
      backgroundColor: 'rgba(0, 0, 0, 0.75)',
      backdropFilter: 'blur(6px)',
      display: 'flex',
      alignItems: 'center',
      justifyContent: 'center',
      zIndex: 9999,
      padding: '20px'
    }}>
      <div className="glass-panel" style={{
        width: '100%',
        maxWidth: '460px',
        backgroundColor: '#161922',
        border: '1px solid rgba(255, 255, 255, 0.12)',
        borderRadius: '20px',
        overflow: 'hidden',
        boxShadow: '0 20px 50px rgba(0, 0, 0, 0.8)'
      }}>
        {/* Header */}
        <div style={{
          padding: '20px 24px',
          background: 'linear-gradient(135deg, #1e2330 0%, #151821 100%)',
          borderBottom: '1px solid rgba(255, 255, 255, 0.08)',
          display: 'flex',
          justifyContent: 'space-between',
          alignItems: 'center'
        }}>
          <div>
            <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
              <span style={{
                background: '#0c2340',
                color: '#38bdf8',
                padding: '2px 8px',
                borderRadius: '6px',
                fontSize: '0.75rem',
                fontWeight: 700
              }}>TEST MODE</span>
              <h3 style={{ fontSize: '1.2rem', color: '#fff', margin: 0 }}>Razorpay Checkout</h3>
            </div>
            <p style={{ fontSize: '0.8rem', color: '#94a3b8', margin: '4px 0 0 0' }}>Order #{orderNumber}</p>
          </div>
          <button
            onClick={onClose}
            style={{
              background: 'transparent',
              border: 'none',
              color: '#94a3b8',
              cursor: 'pointer',
              padding: '6px',
              borderRadius: '8px'
            }}
          >
            <X size={20} />
          </button>
        </div>

        {/* Amount Banner */}
        <div style={{
          padding: '18px 24px',
          background: 'rgba(255, 87, 34, 0.08)',
          borderBottom: '1px solid rgba(255, 87, 34, 0.15)',
          display: 'flex',
          justifyContent: 'space-between',
          alignItems: 'center'
        }}>
          <span style={{ color: '#94a3b8', fontSize: '0.9rem' }}>Amount to Pay</span>
          <span style={{ fontSize: '1.6rem', fontWeight: 800, color: '#ff5722', fontFamily: 'var(--font-heading)' }}>
            ₹{Number(amount).toFixed(2)}
          </span>
        </div>

        {/* Payment Methods */}
        <div style={{ padding: '24px' }}>
          <p style={{ fontSize: '0.85rem', color: '#94a3b8', marginBottom: '14px', fontWeight: 600 }}>
            SELECT TEST PAYMENT METHOD
          </p>

          <div style={{ display: 'flex', flexDirection: 'column', gap: '10px', marginBottom: '20px' }}>
            <div
              onClick={() => setSelectedMethod('card')}
              style={{
                display: 'flex',
                alignItems: 'center',
                gap: '14px',
                padding: '12px 16px',
                borderRadius: '12px',
                border: `1px solid ${selectedMethod === 'card' ? '#ff5722' : 'rgba(255,255,255,0.08)'}`,
                background: selectedMethod === 'card' ? 'rgba(255, 87, 34, 0.08)' : 'rgba(255,255,255,0.02)',
                cursor: 'pointer'
              }}
            >
              <CreditCard size={20} color={selectedMethod === 'card' ? '#ff5722' : '#94a3b8'} />
              <div style={{ flex: 1 }}>
                <div style={{ fontSize: '0.92rem', fontWeight: 600 }}>Test Card (Visa / Mastercard)</div>
                <div style={{ fontSize: '0.78rem', color: '#64748b' }}>•••• 4242 (Auto-approved in Test Mode)</div>
              </div>
              {selectedMethod === 'card' && <CheckCircle size={18} color="#ff5722" />}
            </div>

            <div
              onClick={() => setSelectedMethod('upi')}
              style={{
                display: 'flex',
                alignItems: 'center',
                gap: '14px',
                padding: '12px 16px',
                borderRadius: '12px',
                border: `1px solid ${selectedMethod === 'upi' ? '#ff5722' : 'rgba(255,255,255,0.08)'}`,
                background: selectedMethod === 'upi' ? 'rgba(255, 87, 34, 0.08)' : 'rgba(255,255,255,0.02)',
                cursor: 'pointer'
              }}
            >
              <Smartphone size={20} color={selectedMethod === 'upi' ? '#ff5722' : '#94a3b8'} />
              <div style={{ flex: 1 }}>
                <div style={{ fontSize: '0.92rem', fontWeight: 600 }}>Instant UPI / QR</div>
                <div style={{ fontSize: '0.78rem', color: '#64748b' }}>student@okhdfcbank</div>
              </div>
              {selectedMethod === 'upi' && <CheckCircle size={18} color="#ff5722" />}
            </div>

            <div
              onClick={() => setSelectedMethod('netbanking')}
              style={{
                display: 'flex',
                alignItems: 'center',
                gap: '14px',
                padding: '12px 16px',
                borderRadius: '12px',
                border: `1px solid ${selectedMethod === 'netbanking' ? '#ff5722' : 'rgba(255,255,255,0.08)'}`,
                background: selectedMethod === 'netbanking' ? 'rgba(255, 87, 34, 0.08)' : 'rgba(255,255,255,0.02)',
                cursor: 'pointer'
              }}
            >
              <Building size={20} color={selectedMethod === 'netbanking' ? '#ff5722' : '#94a3b8'} />
              <div style={{ flex: 1 }}>
                <div style={{ fontSize: '0.92rem', fontWeight: 600 }}>Campus Student Wallet</div>
                <div style={{ fontSize: '0.78rem', color: '#64748b' }}>QuickBite OneCard Balance</div>
              </div>
              {selectedMethod === 'netbanking' && <CheckCircle size={18} color="#ff5722" />}
            </div>
          </div>

          {error && (
            <div style={{
              display: 'flex',
              alignItems: 'center',
              gap: '8px',
              padding: '10px 14px',
              borderRadius: '8px',
              background: 'rgba(239, 68, 68, 0.15)',
              border: '1px solid rgba(239, 68, 68, 0.3)',
              color: '#ef4444',
              fontSize: '0.82rem',
              marginBottom: '16px'
            }}>
              <AlertTriangle size={16} />
              <span>{error}</span>
            </div>
          )}

          <button
            onClick={handleSimulatePayment}
            disabled={loading}
            className="btn btn-primary"
            style={{ width: '100%', padding: '13px', fontSize: '1rem' }}
          >
            {loading ? 'Verifying with Server...' : `Pay ₹${Number(amount).toFixed(2)}`}
          </button>

          <div style={{
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            gap: '6px',
            marginTop: '16px',
            color: '#64748b',
            fontSize: '0.76rem'
          }}>
            <ShieldCheck size={14} color="#10b981" />
            <span>Server-side HMAC-SHA256 signature verification enabled</span>
          </div>
        </div>
      </div>
    </div>
  );
};
