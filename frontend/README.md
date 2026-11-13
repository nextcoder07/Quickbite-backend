# QuickBite Desktop Frontend

Modern, high-performance desktop & web application interface for the **QuickBite Food Marketplace**, built with React 18, Vite, and packaged for desktop with Electron 30.

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
│   │   └── apiClient.js    # Centralized HTTP client (automatic Bearer token, base URL switching, 45s cold-start timeout)
│   ├── components/
│   │   ├── Navbar.jsx          # Top app bar (role pill, cart indicator, demo role switcher, user profile)
│   │   ├── Sidebar.jsx         # Dynamic role-based navigation sidebar
│   │   ├── StatusBadge.jsx     # Color-coded status badge for all 9 application enums
│   │   ├── LocationPicker.jsx  # Leaflet map picker & address input with autofill protection
│   │   ├── PasswordInput.jsx   # Secure password field with reveal toggle
│   │   └── RazorpayModal.jsx   # Simulated interactive Razorpay test payment modal
│   ├── context/
│   │   ├── AuthContext.jsx   # Auth state, Firebase token handling, persistent identity & profile
│   │   ├── CartContext.jsx   # Shopping cart state, shop single-source invariant, subtotal & delivery fee
│   │   └── DialogContext.jsx # Custom modal dialog (prompt/confirm/alert) ensuring 100% Electron .exe compatibility
│   ├── pages/
│   │   ├── auth/             # Login, Signup, VerificationGate
│   │   ├── buyer/            # BrowseItems, BrowseShops, ShopDetail, CartCheckout, OrderHistory, Profile
│   │   ├── shopowner/        # ShopDashboard, ProductManagement, IncomingOrders, ShopSettings
│   │   ├── delivery/         # DeliveryDashboard (live queue & active transit), DeliveryHistory, DeliverySettings
│   │   └── admin/            # AdminDashboard, ShopApprovals, DeliveryApprovals, AdminManagement, DeletionRequests, DisputeManagement, RefundManagement, AuditLogs
│   ├── policies/
│   │   └── RolePolicy.js     # Strategy Pattern: route guards & role permissions (Buyer, ShopOwner, DeliveryPartner, Admin)
│   ├── services/             # BaseService, AuthService, MarketplaceService, ShopOwnerService, DeliveryService, OrderService, AdminService
│   ├── App.jsx               # React Router routing & Protected Route enforcement
│   ├── main.jsx              # React root mount
│   └── index.css             # Global design system tokens and responsive utilities
├── index.html                # HTML5 template with Content-Security-Policy
├── vite.config.js            # Vite configuration
└── package.json              # Scripts and dependencies
```

---

## 🔐 Electron Security & Native Compatibility

1. **Context Isolation**: `contextIsolation: true` is strictly enforced.
2. **Node Integration Disabled**: `nodeIntegration: false` ensures renderer code cannot execute arbitrary Node system commands.
3. **Preload Script**: Preload API (`electron/preload.js`) provides a secure bridge exposed under `window.quickbiteDesktop`.
4. **Content Security Policy (CSP)**: Configured in `index.html` preventing unauthorized script execution.
5. **Cross-Platform Dialogs**: Replaced native browser `window.prompt()`, `window.confirm()`, and `window.alert()` with React-driven `<DialogProvider>` and `useDialog()` hooks, since Electron silently suppresses native prompt inputs in packaged `.exe` builds.

---

## 👥 Seeded Demo Accounts

All pre-seeded test accounts use the common password: **`QuickBite@123`**

| Role | Email | Password | Details / Profile |
| :--- | :--- | :--- | :--- |
| **Admin** | `admin@quickbite.com` | `QuickBite@123` | Platform Operations Admin (Shop/Courier Approvals, Disputes, Audit Logs) |
| **Buyer** | `buyer@quickbite.com` | `QuickBite@123` | Aarav Sharma (Indiranagar, Bengaluru • Cold-start / personalized feed) |
| **Shop Owner 1** | `shop@quickbite.com` | `QuickBite@123` | Chef Vikram Das — **Urban Spice Kitchen** (Indiranagar, 4 products) |
| **Shop Owner 2** | `shop2@quickbite.com` | `QuickBite@123` | Meera Nair — **Brew & Bite Cafe** (Koramangala, 4 products) |
| **Delivery Partner 1** | `delivery@quickbite.com` | `QuickBite@123` | Rohan Patel — Scooter (`KA-01-EQ-4492`) • Indiranagar |
| **Delivery Partner 2** | `delivery2@quickbite.com` | `QuickBite@123` | Kabir Verma — Electric Bicycle (`KA-03-EB-1088`) • Koramangala |

---

## 🚀 Setup & Installation (For New Clones)

### 1. Configure Environment (`.env`)
Create `frontend/.env` (or copy from `.env.example`):
```ini
# Backend API Base URL
# Local Spring Boot:
# VITE_API_BASE_URL=http://localhost:8080/api/v1
# Hosted Cloud Backend (Render):
VITE_API_BASE_URL=https://quickbite-backend-8qgw.onrender.com/api/v1

# Firebase Client Configuration (Identitytoolkit / Auth SDK)
VITE_FIREBASE_API_KEY=AIzaSyCfu-4wxFsEyqBr82QZwy2HfbLmHoJ8ocM
VITE_FIREBASE_AUTH_DOMAIN=quick-bite-firebase.firebaseapp.com
VITE_FIREBASE_PROJECT_ID=quick-bite-firebase
VITE_FIREBASE_STORAGE_BUCKET=quick-bite-firebase.firebasestorage.app
VITE_FIREBASE_MESSAGING_SENDER_ID=710921652702
VITE_FIREBASE_APP_ID=1:710921652702:web:b25205bdd732fae6a2b54f
```

### 2. Install Dependencies
Navigate into `frontend/` and install packages:

```bash
cd frontend

# Clean lockfile install (recommended for fresh clones):
npm ci

# Or standard install:
npm install

# If peer dependency resolution warnings occur:
npm install --legacy-peer-deps
```

---

## 💻 Running the Application

### 1. Web Browser Mode (Vite Dev Server)
```bash
cd frontend
npm run dev
```
Open **`http://localhost:5173`** in any modern web browser.

### 2. Electron Desktop Mode (Development)
Launches Vite dev server with Hot Module Replacement alongside the native Electron shell:
```bash
cd frontend
npm run electron:dev
```

### 3. Production Build
Validates modules and compiles optimized static assets into `frontend/dist/`:
```bash
npm run build
```

### 4. Package Windows Desktop `.exe`
Generates the standalone Windows executable in `dist-electron/`:
```bash
npm run dist
```
