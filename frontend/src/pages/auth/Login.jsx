import React, { useState } from 'react';
import { useAuth } from '../../context/AuthContext';
import { useNavigate, Link } from 'react-router-dom';
import { Sparkles, ArrowRight, Lock } from 'lucide-react';
import { RolePolicyFactory } from '../../policies/RolePolicy';
import { PasswordInput } from '../../components/PasswordInput';

export const Login = () => {
  const { loginWithPassword } = useAuth();
  const navigate = useNavigate();
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  const handleLogin = async (e) => {
    e.preventDefault();
    if (!email.trim() || !password) {
      setError('Email and password are required');
      return;
    }
    setLoading(true);
    setError('');
    try {
      const loggedInAccount = await loginWithPassword(email.trim(), password);
      const policy = RolePolicyFactory.getPolicy(loggedInAccount?.accountType);
      navigate(policy.getDefaultRoute(loggedInAccount), { replace: true });
    } catch (err) {
      setError(err.message || 'Invalid email or password.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div
      style={{
        minHeight: '100vh',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        padding: '24px',
        background: 'radial-gradient(circle at top right, #1f1a24 0%, #0c0d12 60%)',
      }}
    >
      <div className="glass-panel" style={{ width: '100%', maxWidth: '470px', padding: '36px' }}>
        <div style={{ textAlign: 'center', marginBottom: '26px' }}>
          <div
            style={{
              width: '48px',
              height: '48px',
              borderRadius: '14px',
              background: 'linear-gradient(135deg, #FF6433 0%, #FF4500 100%)',
              display: 'inline-flex',
              alignItems: 'center',
              justifyContent: 'center',
              marginBottom: '12px',
              boxShadow: '0 6px 18px rgba(255, 100, 51, 0.35)',
            }}
          >
            <Sparkles size={28} color="#fff" />
          </div>
          <h2 style={{ fontSize: '1.7rem', color: '#fff', marginBottom: '6px' }}>Sign In to QuickBite</h2>
          <p style={{ color: 'var(--text-muted)', fontSize: '0.9rem' }}>
            Order food anywhere, manage shops, deliver, or administer QuickBite
          </p>
        </div>

        {error && (
          <div
            style={{
              padding: '10px 14px',
              borderRadius: '8px',
              background: 'var(--danger-bg)',
              color: 'var(--danger)',
              fontSize: '0.85rem',
              marginBottom: '16px',
            }}
          >
            {error}
          </div>
        )}

        <form onSubmit={handleLogin}>
          <div className="form-group">
            <label className="form-label">Email Address</label>
            <input
              type="email"
              className="form-input"
              placeholder="you@example.com"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              required
            />
          </div>

          <div className="form-group">
            <label className="form-label" style={{ display: 'flex', alignItems: 'center', gap: '6px' }}>
              <Lock size={13} /> Password
            </label>
            <PasswordInput
              placeholder="Enter your password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              autoComplete="current-password"
              required
            />
          </div>

          <button
            type="submit"
            className="btn btn-primary"
            disabled={loading}
            style={{ width: '100%', padding: '12px', marginTop: '8px' }}
          >
            {loading ? 'Signing In...' : 'Sign In'} <ArrowRight size={16} />
          </button>
        </form>

        <div style={{ marginTop: '22px', textAlign: 'center', fontSize: '0.85rem', color: 'var(--text-muted)' }}>
          New to QuickBite?{' '}
          <Link to="/signup" style={{ color: 'var(--primary)', fontWeight: 600, textDecoration: 'none' }}>
            Create an account
          </Link>
        </div>
      </div>
    </div>
  );
};
