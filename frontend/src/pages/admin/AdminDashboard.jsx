import React, { useState, useEffect } from 'react';
import { api } from '../../api/apiClient';
import { adminService } from '../../services/AdminService';
import { Link } from 'react-router-dom';
import {
  Users,
  Store,
  TrendingUp,
  AlertCircle,
  ClipboardList,
  ArrowRight,
  Bike,
  Shield,
} from 'lucide-react';

export const AdminDashboard = () => {
  const cachedSummary = api.getCache('/admin/summary');
  const [summary, setSummary] = useState(() => cachedSummary || null);
  const [loading, setLoading] = useState(() => !cachedSummary);
  const [error, setError] = useState('');

  useEffect(() => {
    const fetchSummary = async () => {
      try {
        const data = await adminService.getDashboardSummary();
        setSummary(data);
      } catch (err) {
        setError(err.message || 'Failed to load admin summary');
      } finally {
        setLoading(false);
      }
    };

    fetchSummary();
  }, []);

  if (loading) {
    return <div style={{ textAlign: 'center', padding: '60px', color: 'var(--text-muted)' }}>Loading operational metrics...</div>;
  }

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: '28px' }}>
      <div>
        <h1 style={{ fontSize: '1.8rem', marginBottom: '4px' }}>Platform Operational Dashboard</h1>
        <p style={{ color: 'var(--text-muted)', fontSize: '0.9rem' }}>
          Overview of marketplace activity, partner & shop verifications, removals, and disputes
        </p>
      </div>

      {error && (
        <div style={{ padding: '12px 16px', borderRadius: '10px', background: 'var(--danger-bg)', color: 'var(--danger)' }}>
          {error}
        </div>
      )}

      {/* Primary KPI Metrics */}
      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(220px, 1fr))', gap: '20px' }}>
        <div className="glass-panel" style={{ padding: '22px' }}>
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '10px' }}>
            <span style={{ fontSize: '0.8rem', color: 'var(--text-muted)', fontWeight: 600 }}>REGISTERED BUYERS</span>
            <div style={{ padding: '8px', borderRadius: '10px', background: 'rgba(59, 130, 246, 0.15)', color: '#60a5fa' }}>
              <Users size={18} />
            </div>
          </div>
          <div style={{ fontSize: '1.8rem', fontWeight: 800 }}>{summary?.totalUsers || 0}</div>
        </div>

        <div className="glass-panel" style={{ padding: '22px' }}>
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '10px' }}>
            <span style={{ fontSize: '0.8rem', color: 'var(--text-muted)', fontWeight: 600 }}>DELIVERY FLEET</span>
            <div style={{ padding: '8px', borderRadius: '10px', background: 'rgba(20, 184, 166, 0.15)', color: '#2dd4bf' }}>
              <Bike size={18} />
            </div>
          </div>
          <div style={{ fontSize: '1.8rem', fontWeight: 800 }}>{summary?.totalDeliveryPartners || 0}</div>
        </div>

        <div className="glass-panel" style={{ padding: '22px' }}>
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '10px' }}>
            <span style={{ fontSize: '0.8rem', color: 'var(--text-muted)', fontWeight: 600 }}>ACTIVE SHOPS</span>
            <div style={{ padding: '8px', borderRadius: '10px', background: 'rgba(245, 158, 11, 0.15)', color: '#fbbf24' }}>
              <Store size={18} />
            </div>
          </div>
          <div style={{ fontSize: '1.8rem', fontWeight: 800 }}>{summary?.totalShops || 0}</div>
        </div>

        <div className="glass-panel" style={{ padding: '22px' }}>
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '10px' }}>
            <span style={{ fontSize: '0.8rem', color: 'var(--text-muted)', fontWeight: 600 }}>TOTAL SALES VOLUME</span>
            <div style={{ padding: '8px', borderRadius: '10px', background: 'rgba(16, 185, 129, 0.15)', color: '#34d399' }}>
              <TrendingUp size={18} />
            </div>
          </div>
          <div style={{ fontSize: '1.8rem', fontWeight: 800, color: 'var(--primary)' }}>
            ₹{Number(summary?.totalVolume || 0).toFixed(2)}
          </div>
        </div>
      </div>

      {/* Moderation Queues */}
      <div>
        <h2 style={{ fontSize: '1.3rem', marginBottom: '14px' }}>Pending Moderation Queues</h2>
        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(280px, 1fr))', gap: '20px' }}>
          <Link to="/admin/admins" className="glass-panel" style={{ padding: '24px', textDecoration: 'none', color: '#fff', display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
            <div>
              <div style={{ display: 'flex', alignItems: 'center', gap: '8px', color: '#c084fc', fontSize: '0.85rem', fontWeight: 700, marginBottom: '6px' }}>
                <Shield size={16} /> ADMIN REQUESTS
              </div>
              <div style={{ fontSize: '1.6rem', fontWeight: 800 }}>
                {summary?.pendingAdminRequestsCount || 0} Pending
              </div>
            </div>
            <ArrowRight size={20} color="var(--text-muted)" />
          </Link>

          <Link to="/admin/shops" className="glass-panel" style={{ padding: '24px', textDecoration: 'none', color: '#fff', display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
            <div>
              <div style={{ display: 'flex', alignItems: 'center', gap: '8px', color: '#fbbf24', fontSize: '0.85rem', fontWeight: 700, marginBottom: '6px' }}>
                <Store size={16} /> SHOP VERIFICATIONS
              </div>
              <div style={{ fontSize: '1.6rem', fontWeight: 800 }}>
                {summary?.pendingShopsCount || 0} Pending
              </div>
            </div>
            <ArrowRight size={20} color="var(--text-muted)" />
          </Link>

          <Link to="/admin/delivery-partners" className="glass-panel" style={{ padding: '24px', textDecoration: 'none', color: '#fff', display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
            <div>
              <div style={{ display: 'flex', alignItems: 'center', gap: '8px', color: '#38bdf8', fontSize: '0.85rem', fontWeight: 700, marginBottom: '6px' }}>
                <Bike size={16} /> DELIVERY VERIFICATIONS
              </div>
              <div style={{ fontSize: '1.6rem', fontWeight: 800 }}>
                {summary?.pendingDeliveryPartnersCount || 0} Pending
              </div>
            </div>
            <ArrowRight size={20} color="var(--text-muted)" />
          </Link>

          <Link to="/admin/deletions" className="glass-panel" style={{ padding: '24px', textDecoration: 'none', color: '#fff', display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
            <div>
              <div style={{ display: 'flex', alignItems: 'center', gap: '8px', color: '#f87171', fontSize: '0.85rem', fontWeight: 700, marginBottom: '6px' }}>
                <ClipboardList size={16} /> REMOVAL CONFIRMATIONS
              </div>
              <div style={{ fontSize: '1.6rem', fontWeight: 800 }}>
                {summary?.pendingDeletionCount || 0} Pending
              </div>
            </div>
            <ArrowRight size={20} color="var(--text-muted)" />
          </Link>

          <Link to="/admin/disputes" className="glass-panel" style={{ padding: '24px', textDecoration: 'none', color: '#fff', display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
            <div>
              <div style={{ display: 'flex', alignItems: 'center', gap: '8px', color: '#c084fc', fontSize: '0.85rem', fontWeight: 700, marginBottom: '6px' }}>
                <AlertCircle size={16} /> DISPUTES & CLAIMS
              </div>
              <div style={{ fontSize: '1.6rem', fontWeight: 800 }}>
                {summary?.openDisputesCount || 0} Active
              </div>
            </div>
            <ArrowRight size={20} color="var(--text-muted)" />
          </Link>
        </div>
      </div>
    </div>
  );
};
