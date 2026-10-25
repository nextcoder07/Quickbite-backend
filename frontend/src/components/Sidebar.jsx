import React from 'react';
import { NavLink } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { 
  Store, 
  ShoppingBag, 
  Clock, 
  User, 
  Package, 
  Bike, 
  ShieldCheck, 
  ClipboardList, 
  AlertCircle, 
  RotateCcw, 
  FileText,
  Sliders
} from 'lucide-react';

export const Sidebar = () => {
  const { role } = useAuth();

  const getLinks = () => {
    switch (role) {
      case 'SHOP_OWNER':
        return [
          { to: '/shop/dashboard', label: 'Shop Overview', icon: <Store size={18} /> },
          { to: '/shop/orders', label: 'Live Orders', icon: <ShoppingBag size={18} /> },
          { to: '/shop/products', label: 'Product Catalog', icon: <Package size={18} /> },
          { to: '/shop/settings', label: 'Shop Settings', icon: <Sliders size={18} /> },
        ];
      case 'DELIVERY_PARTNER':
        return [
          { to: '/delivery/dashboard', label: 'Campus Deliveries', icon: <Bike size={18} /> },
          { to: '/delivery/history', label: 'Delivery History', icon: <Clock size={18} /> },
        ];
      case 'ADMIN':
        return [
          { to: '/admin/dashboard', label: 'Overview Metrics', icon: <ShieldCheck size={18} /> },
          { to: '/admin/shops', label: 'Shop Approvals', icon: <Store size={18} /> },
          { to: '/admin/deletions', label: 'Deletion Requests', icon: <ClipboardList size={18} /> },
          { to: '/admin/disputes', label: 'Dispute Cases', icon: <AlertCircle size={18} /> },
          { to: '/admin/refunds', label: 'Refund Authorizations', icon: <RotateCcw size={18} /> },
          { to: '/admin/audit-logs', label: 'Audit Trail', icon: <FileText size={18} /> },
        ];
      default: // BUYER
        return [
          { to: '/', label: 'Browse Shops', icon: <Store size={18} /> },
          { to: '/cart', label: 'My Cart', icon: <ShoppingBag size={18} /> },
          { to: '/orders', label: 'My Orders', icon: <Clock size={18} /> },
          { to: '/profile', label: 'Account Profile', icon: <User size={18} /> },
        ];
    }
  };

  const links = getLinks();

  return (
    <aside style={{
      width: '240px',
      background: 'var(--bg-secondary)',
      borderRight: '1px solid var(--border-subtle)',
      padding: '24px 16px',
      display: 'flex',
      flexDirection: 'column',
      gap: '8px',
      flexShrink: 0,
    }}>
      <div style={{ fontSize: '0.72rem', color: 'var(--text-faint)', fontWeight: 700, padding: '0 12px 10px 12px', letterSpacing: '0.06em' }}>
        MENU NAVIGATION
      </div>
      {links.map((link) => (
        <NavLink
          key={link.to}
          to={link.to}
          end={link.to === '/' || link.to === '/shop/dashboard' || link.to === '/delivery/dashboard' || link.to === '/admin/dashboard'}
          style={({ isActive }) => ({
            display: 'flex',
            alignItems: 'center',
            gap: '12px',
            padding: '11px 16px',
            borderRadius: '12px',
            textDecoration: 'none',
            fontSize: '0.9rem',
            fontWeight: 600,
            color: isActive ? '#fff' : 'var(--text-muted)',
            backgroundColor: isActive ? 'var(--primary-subtle)' : 'transparent',
            border: isActive ? '1px solid var(--border-accent)' : '1px solid transparent',
            transition: 'all 0.18s ease'
          })}
        >
          {link.icon}
          <span>{link.label}</span>
        </NavLink>
      ))}
    </aside>
  );
};
