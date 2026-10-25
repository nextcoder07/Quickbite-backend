# QuickBite Desktop Frontend

Modern, high-performance desktop application interface for the **QuickBite Campus Food Marketplace**, built with React, Vite, and packaged for desktop with Electron.

---

## 🎨 Aesthetic & Design System

The QuickBite desktop frontend utilizes a modern campus food aesthetic:
- **Dark Mode by default**: High-contrast, sleek `#0a0f1d` background with glassmorphic cards (`rgba(30, 41, 59, 0.7)`).
- **Vibrant Accent Colors**:
  - Primary Amber/Orange: `#f59e0b` / `#fbbf24` (appetite, speed, energetic campus food vibe).
  - Emerald Green: `#10b981` (success, confirmed orders, active shops).
  - Rose/Red: `#ef4444` (cancellations, alerts, dispute flags).
  - Indigo/Cyan: `#6366f1` / `#06b6d4` (delivery markers, audit trails).
- **Typography**: Google Fonts `Outfit` (display/headings) & `Plus Jakarta Sans` (body/data tables).
- **Micro-Interactions**: Hover lifts, smooth spring transitions, animated shimmer skeletons, and interactive state badges.

---

## 📁 Architecture & File Layout

```
frontend/
├── electron/
│   ├── main.js             # Electron main process (secure window creation, CSP, IPC)
│   └── preload.js          # Secure context bridge (nodeIntegration disabled, contextIsolation enabled)
├── src/
│   ├── api/
│   │   └── apiClient.js    # Centralized HTTP client (automatic Bearer token, base URL switching)
│   ├── components/
│   │   ├── Navbar.jsx      # Top app bar (role pill, cart indicator, demo role switcher, user profile)
│   │   ├── Sidebar.jsx     # Dynamic role-based navigation sidebar
│   │   ├── StatusBadge.jsx # Color-coded status badge for all 9 application enums
│   │   └── RazorpayModal.jsx # Simulated interactive Razorpay test payment modal
│   ├── context/
│   │   ├── AuthContext.jsx # Auth state, Firebase token handling, persistent identity & profile
│   │   └── CartContext.jsx # Shopping cart state, shop single-source invariant, subtotal & delivery fee
│   ├── pages/
│   │   ├── auth/           # Login, Signup (Buyer, Shop Owner, Delivery Partner)
│   │   ├── buyer/          # BrowseShops, ShopDetail, CartCheckout, OrderHistory, Profile
│   │   ├── shop/           # ShopDashboard, ProductManagement, IncomingOrders, ShopSettings
│   │   ├── delivery/       # DeliveryDashboard (assigned/available deliveries), DeliveryHistory
│   │   └── admin/          # AdminDashboard, ShopApprovals, DeletionRequests, DisputeManagement, RefundManagement, AuditLogs
│   ├── App.jsx             # React Router routing & Protected Route enforcement
│   ├── main.jsx            # React root mount
│   └── index.css           # Global design system tokens and responsive utilities
├── index.html              # HTML5 template with Content-Security-Policy
├── vite.config.js          # Vite configuration
└── package.json            # Scripts and dependencies
```

---

## 🔐 Electron Security Best Practices

1. **Context Isolation**: `contextIsolation: true` is strictly enforced.
2. **Node Integration Disabled**: `nodeIntegration: false` ensures renderer code cannot execute arbitrary Node system commands.
3. **Preload Script**: Preload API (`electron/preload.js`) provides a secure bridge exposed under `window.quickbiteDesktop`.
4. **Content Security Policy (CSP)**: Configured in `index.html` preventing unauthorized script execution.

---

## 🚀 Running Locally

### 1. Web Browser Mode (Vite Dev Server)
```bash
cd frontend
npm run dev
```
Access at `http://localhost:5173`.

### 2. Electron Desktop Mode
Ensure the backend or Vite dev server is running, then run:
```bash
npm run electron:dev
```

### 3. Production Build
```bash
npm run build
```
Generates optimized static assets in `frontend/dist/`.
