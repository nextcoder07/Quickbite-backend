-- ==============================================================================
-- QuickBite Initial Database Schema Migration
-- 16 Tables, Referential Integrity, Invariants & Indexes
-- ==============================================================================

-- 1. Account Identities (Shared authentication identity)
CREATE TABLE IF NOT EXISTS account_identities (
    id BIGSERIAL PRIMARY KEY,
    firebase_uid VARCHAR(128) NOT NULL UNIQUE,
    email VARCHAR(255) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE UNIQUE INDEX IF NOT EXISTS idx_account_identities_email_lower 
ON account_identities (LOWER(email));

-- 2. Users (Buyer-only profile)
CREATE TABLE IF NOT EXISTS users (
    id BIGSERIAL PRIMARY KEY,
    identity_id BIGINT NOT NULL UNIQUE REFERENCES account_identities(id) ON DELETE RESTRICT,
    display_name VARCHAR(100) NOT NULL,
    phone VARCHAR(20),
    account_status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' 
        CHECK (account_status IN ('ACTIVE', 'DEACTIVATED', 'SUSPENDED')),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deactivated_at TIMESTAMPTZ
);

CREATE INDEX IF NOT EXISTS idx_users_identity_id ON users (identity_id);

-- 3. Delivery Partners (Delivery-partner profile)
CREATE TABLE IF NOT EXISTS delivery_partners (
    id BIGSERIAL PRIMARY KEY,
    identity_id BIGINT NOT NULL UNIQUE REFERENCES account_identities(id) ON DELETE RESTRICT,
    display_name VARCHAR(100) NOT NULL,
    phone VARCHAR(20),
    account_status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' 
        CHECK (account_status IN ('ACTIVE', 'DEACTIVATED', 'SUSPENDED')),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deactivated_at TIMESTAMPTZ
);

CREATE INDEX IF NOT EXISTS idx_delivery_partners_identity_id ON delivery_partners (identity_id);

-- 4. Shop Owners (Shop-owner profile)
CREATE TABLE IF NOT EXISTS shop_owners (
    id BIGSERIAL PRIMARY KEY,
    identity_id BIGINT NOT NULL UNIQUE REFERENCES account_identities(id) ON DELETE RESTRICT,
    display_name VARCHAR(100) NOT NULL,
    phone VARCHAR(20),
    account_status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' 
        CHECK (account_status IN ('ACTIVE', 'DEACTIVATED', 'SUSPENDED')),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deactivated_at TIMESTAMPTZ
);

CREATE INDEX IF NOT EXISTS idx_shop_owners_identity_id ON shop_owners (identity_id);

-- 5. Admins (Administrator profile)
CREATE TABLE IF NOT EXISTS admins (
    id BIGSERIAL PRIMARY KEY,
    identity_id BIGINT NOT NULL UNIQUE REFERENCES account_identities(id) ON DELETE RESTRICT,
    display_name VARCHAR(100) NOT NULL,
    account_status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' 
        CHECK (account_status IN ('ACTIVE', 'DEACTIVATED', 'SUSPENDED')),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deactivated_at TIMESTAMPTZ
);

CREATE INDEX IF NOT EXISTS idx_admins_identity_id ON admins (identity_id);

-- 6. Shops (Campus food shops owned by Shop Owners)
CREATE TABLE IF NOT EXISTS shops (
    id BIGSERIAL PRIMARY KEY,
    owner_id BIGINT NOT NULL UNIQUE REFERENCES shop_owners(id) ON DELETE RESTRICT,
    name VARCHAR(150) NOT NULL,
    description TEXT,
    image_path VARCHAR(500),
    campus_location VARCHAR(200) NOT NULL,
    approval_status VARCHAR(20) NOT NULL DEFAULT 'PENDING' 
        CHECK (approval_status IN ('PENDING', 'APPROVED', 'REJECTED', 'SUSPENDED')),
    is_open BOOLEAN NOT NULL DEFAULT FALSE,
    is_deactivated BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deactivated_at TIMESTAMPTZ
);

CREATE INDEX IF NOT EXISTS idx_shops_owner_id ON shops (owner_id);
CREATE INDEX IF NOT EXISTS idx_shops_approval_status ON shops (approval_status);

-- 7. Products (Menu items belonging to a shop)
CREATE TABLE IF NOT EXISTS products (
    id BIGSERIAL PRIMARY KEY,
    shop_id BIGINT NOT NULL REFERENCES shops(id) ON DELETE RESTRICT,
    name VARCHAR(150) NOT NULL,
    description TEXT,
    price NUMERIC(10, 2) NOT NULL CHECK (price >= 0),
    stock_quantity INT NOT NULL DEFAULT 0 CHECK (stock_quantity >= 0),
    image_path VARCHAR(500),
    is_available BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_products_shop_id ON products (shop_id);

-- 8. Orders (Customer orders placed by buyers)
CREATE TABLE IF NOT EXISTS orders (
    id BIGSERIAL PRIMARY KEY,
    order_number VARCHAR(32) NOT NULL UNIQUE,
    buyer_id BIGINT NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    shop_id BIGINT NOT NULL REFERENCES shops(id) ON DELETE RESTRICT,
    status VARCHAR(30) NOT NULL DEFAULT 'PLACED' 
        CHECK (status IN ('PLACED', 'ACCEPTED', 'REJECTED', 'PREPARING', 'READY', 'OUT_FOR_DELIVERY', 'DELIVERED', 'CANCELLED')),
    subtotal NUMERIC(10, 2) NOT NULL CHECK (subtotal >= 0),
    delivery_fee NUMERIC(10, 2) NOT NULL DEFAULT 0.00 CHECK (delivery_fee >= 0),
    discount_amount NUMERIC(10, 2) NOT NULL DEFAULT 0.00 CHECK (discount_amount >= 0),
    total_amount NUMERIC(10, 2) NOT NULL CHECK (total_amount >= 0),
    buyer_note TEXT,
    cancellation_reason TEXT,
    placed_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_orders_total_calc CHECK (total_amount = subtotal + delivery_fee - discount_amount)
);

CREATE INDEX IF NOT EXISTS idx_orders_buyer_id ON orders (buyer_id);
CREATE INDEX IF NOT EXISTS idx_orders_shop_id ON orders (shop_id);
CREATE INDEX IF NOT EXISTS idx_orders_status ON orders (status);

-- 9. Order Items (Snapshot line items for orders)
CREATE TABLE IF NOT EXISTS order_items (
    id BIGSERIAL PRIMARY KEY,
    order_id BIGINT NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
    product_id BIGINT NOT NULL REFERENCES products(id) ON DELETE RESTRICT,
    product_name_snapshot VARCHAR(150) NOT NULL,
    unit_price_snapshot NUMERIC(10, 2) NOT NULL CHECK (unit_price_snapshot >= 0),
    quantity INT NOT NULL CHECK (quantity > 0),
    line_total NUMERIC(10, 2) NOT NULL CHECK (line_total >= 0),
    CONSTRAINT uq_order_product UNIQUE (order_id, product_id)
);

CREATE INDEX IF NOT EXISTS idx_order_items_order_id ON order_items (order_id);

-- 10. Payments (Payment records and provider transaction references)
CREATE TABLE IF NOT EXISTS payments (
    id BIGSERIAL PRIMARY KEY,
    order_id BIGINT NOT NULL REFERENCES orders(id) ON DELETE RESTRICT,
    provider VARCHAR(50) NOT NULL,
    provider_order_id VARCHAR(100),
    provider_payment_id VARCHAR(100),
    amount NUMERIC(10, 2) NOT NULL CHECK (amount >= 0),
    currency VARCHAR(10) NOT NULL DEFAULT 'INR',
    status VARCHAR(30) NOT NULL DEFAULT 'CREATED' 
        CHECK (status IN ('CREATED', 'PENDING', 'AUTHORIZED', 'CAPTURED', 'FAILED', 'REFUND_PENDING', 'REFUNDED')),
    idempotency_key VARCHAR(100),
    verified_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_payments_order_id ON payments (order_id);
CREATE INDEX IF NOT EXISTS idx_payments_provider_payment_id ON payments (provider_payment_id);

-- 11. Refunds (Refund transactions)
CREATE TABLE IF NOT EXISTS refunds (
    id BIGSERIAL PRIMARY KEY,
    payment_id BIGINT NOT NULL REFERENCES payments(id) ON DELETE RESTRICT,
    requested_by_identity_id BIGINT NOT NULL REFERENCES account_identities(id) ON DELETE RESTRICT,
    reviewed_by_admin_id BIGINT REFERENCES admins(id) ON DELETE RESTRICT,
    amount NUMERIC(10, 2) NOT NULL CHECK (amount > 0),
    status VARCHAR(30) NOT NULL DEFAULT 'REQUESTED' 
        CHECK (status IN ('REQUESTED', 'APPROVED', 'REJECTED', 'PROCESSING', 'PROCESSED', 'FAILED')),
    provider_refund_id VARCHAR(100),
    reason TEXT NOT NULL,
    idempotency_key VARCHAR(100),
    requested_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    processed_at TIMESTAMPTZ
);

CREATE INDEX IF NOT EXISTS idx_refunds_payment_id ON refunds (payment_id);

-- 12. Delivery Assignments (Delivery partner fulfillment)
CREATE TABLE IF NOT EXISTS delivery_assignments (
    id BIGSERIAL PRIMARY KEY,
    order_id BIGINT NOT NULL UNIQUE REFERENCES orders(id) ON DELETE RESTRICT,
    delivery_partner_id BIGINT REFERENCES delivery_partners(id) ON DELETE RESTRICT,
    status VARCHAR(30) NOT NULL DEFAULT 'UNASSIGNED' 
        CHECK (status IN ('UNASSIGNED', 'ASSIGNED', 'ACCEPTED', 'PICKED_UP', 'DELIVERED', 'FAILED', 'CANCELLED')),
    assigned_at TIMESTAMPTZ,
    accepted_at TIMESTAMPTZ,
    picked_up_at TIMESTAMPTZ,
    delivered_at TIMESTAMPTZ,
    failure_reason TEXT,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_delivery_assignments_partner_id ON delivery_assignments (delivery_partner_id);

-- 13. Account Deletion Requests (Shop Owners & Delivery Partners)
CREATE TABLE IF NOT EXISTS account_deletion_requests (
    id BIGSERIAL PRIMARY KEY,
    requester_identity_id BIGINT NOT NULL REFERENCES account_identities(id) ON DELETE RESTRICT,
    requester_type VARCHAR(30) NOT NULL 
        CHECK (requester_type IN ('BUYER', 'DELIVERY_PARTNER', 'SHOP_OWNER', 'ADMIN')),
    status VARCHAR(30) NOT NULL DEFAULT 'PENDING' 
        CHECK (status IN ('PENDING', 'CONFIRMED', 'DECLINED', 'CANCELLED', 'COMPLETED')),
    reason TEXT NOT NULL,
    reviewed_by_admin_id BIGINT REFERENCES admins(id) ON DELETE RESTRICT,
    review_note TEXT,
    requested_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    reviewed_at TIMESTAMPTZ,
    completed_at TIMESTAMPTZ
);

CREATE INDEX IF NOT EXISTS idx_deletion_requests_requester ON account_deletion_requests (requester_identity_id);

-- 14. Feedback (Buyer rating and reviews for delivered orders)
CREATE TABLE IF NOT EXISTS feedback (
    id BIGSERIAL PRIMARY KEY,
    order_id BIGINT NOT NULL UNIQUE REFERENCES orders(id) ON DELETE RESTRICT,
    buyer_id BIGINT NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    rating INT NOT NULL CHECK (rating >= 1 AND rating <= 5),
    comment TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_feedback_order_id ON feedback (order_id);
CREATE INDEX IF NOT EXISTS idx_feedback_buyer_id ON feedback (buyer_id);

-- 15. Disputes (Disputes on orders)
CREATE TABLE IF NOT EXISTS disputes (
    id BIGSERIAL PRIMARY KEY,
    order_id BIGINT NOT NULL REFERENCES orders(id) ON DELETE RESTRICT,
    raised_by_identity_id BIGINT NOT NULL REFERENCES account_identities(id) ON DELETE RESTRICT,
    category VARCHAR(100) NOT NULL,
    description TEXT NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'OPEN' 
        CHECK (status IN ('OPEN', 'UNDER_REVIEW', 'RESOLVED', 'REJECTED', 'CLOSED')),
    resolution_note TEXT,
    resolved_by_admin_id BIGINT REFERENCES admins(id) ON DELETE RESTRICT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    resolved_at TIMESTAMPTZ
);

CREATE INDEX IF NOT EXISTS idx_disputes_order_id ON disputes (order_id);
CREATE INDEX IF NOT EXISTS idx_disputes_raised_by ON disputes (raised_by_identity_id);

-- 16. Audit Logs (Append-only audit trail with safe metadata)
CREATE TABLE IF NOT EXISTS audit_logs (
    id BIGSERIAL PRIMARY KEY,
    actor_identity_id BIGINT REFERENCES account_identities(id) ON DELETE RESTRICT,
    action VARCHAR(100) NOT NULL,
    target_type VARCHAR(100) NOT NULL,
    target_id VARCHAR(100) NOT NULL,
    safe_metadata JSONB,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_audit_logs_actor ON audit_logs (actor_identity_id);
CREATE INDEX IF NOT EXISTS idx_audit_logs_action ON audit_logs (action);
