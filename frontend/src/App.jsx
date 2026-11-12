import React from 'react';
import { HashRouter, Routes, Route, Navigate, Outlet, useLocation } from 'react-router-dom';
import { AuthProvider, useAuth } from './context/AuthContext';
import { CartProvider } from './context/CartContext';
import { DialogProvider } from './context/DialogContext';
import { Navbar } from './components/Navbar';
import { Sidebar } from './components/Sidebar';

// Pages
import { Login } from './pages/auth/Login';
import { Signup } from './pages/auth/Signup';
import { VerificationGate } from './pages/auth/VerificationGate';

// Buyer Pages
import { BrowseItems } from './pages/buyer/BrowseItems';
import { BrowseShops } from './pages/buyer/BrowseShops';
import { ShopDetail } from './pages/buyer/ShopDetail';
import { CartCheckout } from './pages/buyer/CartCheckout';
import { OrderHistory } from './pages/buyer/OrderHistory';
import { Profile } from './pages/buyer/Profile';

// Shop Owner Pages
import { ShopDashboard } from './pages/shopowner/ShopDashboard';
import { ProductManagement } from './pages/shopowner/ProductManagement';
import { IncomingOrders } from './pages/shopowner/IncomingOrders';
import { ShopSettings } from './pages/shopowner/ShopSettings';

// Delivery Partner Pages
import { DeliveryDashboard } from './pages/delivery/DeliveryDashboard';
import { DeliveryHistory } from './pages/delivery/DeliveryHistory';
import { DeliverySettings } from './pages/delivery/DeliverySettings';

// Admin Pages
import { AdminDashboard } from './pages/admin/AdminDashboard';
import { ShopApprovals } from './pages/admin/ShopApprovals';
import { DeliveryApprovals } from './pages/admin/DeliveryApprovals';
import { DeletionRequests } from './pages/admin/DeletionRequests';
import { DisputeManagement } from './pages/admin/DisputeManagement';
import { RefundManagement } from './pages/admin/RefundManagement';
import { AuditLogs } from './pages/admin/AuditLogs';
import { AdminManagement } from './pages/admin/AdminManagement';

// Main Protected Shell Layout with Role + Verification Route Guard
const MainLayout = () => {
  const { isAuthenticated, loading, account, policy } = useAuth();
  const location = useLocation();

  if (loading) {
    return (
      <div style={{ minHeight: '100vh', display: 'flex', alignItems: 'center', justifyContent: 'center', color: 'var(--text-muted)' }}>
        Loading QuickBite...
      </div>
    );
  }

  if (!isAuthenticated) {
    return <Navigate to="/login" replace />;
  }

  if (account && policy && !policy.canAccessRoute(location.pathname, account)) {
    return <Navigate to={policy.getDefaultRoute(account)} replace />;
  }

  return (
    <div className="app-container">
      <Sidebar />
      <div className="main-content">
        <Navbar />
        <main className="page-content">
          <Outlet />
        </main>
      </div>
    </div>
  );
};

export const App = () => {
  return (
    <DialogProvider>
      <AuthProvider>
        <CartProvider>
        <HashRouter>
          <Routes>
            {/* Public Auth Routes */}
            <Route path="/login" element={<Login />} />
            <Route path="/signup" element={<Signup />} />

            {/* Authenticated Application Shell */}
            <Route element={<MainLayout />}>
              {/* Buyer Routes */}
              <Route path="/" element={<BrowseItems />} />
              <Route path="/shops" element={<BrowseShops />} />
              <Route path="/shop/:shopId" element={<ShopDetail />} />
              <Route path="/cart" element={<CartCheckout />} />
              <Route path="/orders" element={<OrderHistory />} />
              <Route path="/profile" element={<Profile />} />

              {/* Shop Owner Routes */}
              <Route path="/shop/onboarding" element={<VerificationGate />} />
              <Route path="/shop/dashboard" element={<ShopDashboard />} />
              <Route path="/shop/orders" element={<IncomingOrders />} />
              <Route path="/shop/products" element={<ProductManagement />} />
              <Route path="/shop/settings" element={<ShopSettings />} />

              {/* Delivery Partner Routes */}
              <Route path="/delivery/verification" element={<VerificationGate />} />
              <Route path="/delivery/dashboard" element={<DeliveryDashboard />} />
              <Route path="/delivery/history" element={<DeliveryHistory />} />
              <Route path="/delivery/settings" element={<DeliverySettings />} />

              {/* Admin Routes */}
              <Route path="/admin/verification" element={<VerificationGate />} />
              <Route path="/admin/dashboard" element={<AdminDashboard />} />
              <Route path="/admin/admins" element={<AdminManagement />} />
              <Route path="/admin/shops" element={<ShopApprovals />} />
              <Route path="/admin/delivery-partners" element={<DeliveryApprovals />} />
              <Route path="/admin/deletions" element={<DeletionRequests />} />
              <Route path="/admin/disputes" element={<DisputeManagement />} />
              <Route path="/admin/refunds" element={<RefundManagement />} />
              <Route path="/admin/audit-logs" element={<AuditLogs />} />
            </Route>

            {/* Catch-all redirect */}
            <Route path="*" element={<Navigate to="/" replace />} />
          </Routes>
        </HashRouter>
      </CartProvider>
    </AuthProvider>
    </DialogProvider>
  );
};

export default App;
