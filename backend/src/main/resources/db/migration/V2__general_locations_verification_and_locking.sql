-- ==============================================================================
-- QuickBite V2 Migration: Anywhere Locations, Partner Verification & DB Locking
-- Compatible with both PostgreSQL (Supabase) and H2 PostgreSQL Mode
-- ==============================================================================

-- 1. Support email/password authentication alongside Firebase UIDs
ALTER TABLE account_identities
    ADD COLUMN IF NOT EXISTS password_hash VARCHAR(255);

-- 2. Buyer default delivery address & coordinates (anywhere, any place)
ALTER TABLE users
    ADD COLUMN IF NOT EXISTS default_address VARCHAR(500);

ALTER TABLE users
    ADD COLUMN IF NOT EXISTS default_latitude DOUBLE PRECISION;

ALTER TABLE users
    ADD COLUMN IF NOT EXISTS default_longitude DOUBLE PRECISION;

-- 3. Delivery partner admin verification workflow + courier metadata
ALTER TABLE delivery_partners
    ADD COLUMN IF NOT EXISTS verification_status VARCHAR(20) NOT NULL DEFAULT 'PENDING';

ALTER TABLE delivery_partners
    ADD COLUMN IF NOT EXISTS vehicle_details VARCHAR(150);

ALTER TABLE delivery_partners
    ADD COLUMN IF NOT EXISTS service_area VARCHAR(150);

-- 4. Shop exact coordinates / location point (alongside general location string)
ALTER TABLE shops
    ADD COLUMN IF NOT EXISTS latitude DOUBLE PRECISION;

ALTER TABLE shops
    ADD COLUMN IF NOT EXISTS longitude DOUBLE PRECISION;

-- 5. Order delivery destination address & location coordinates
ALTER TABLE orders
    ADD COLUMN IF NOT EXISTS delivery_address VARCHAR(500);

ALTER TABLE orders
    ADD COLUMN IF NOT EXISTS delivery_latitude DOUBLE PRECISION;

ALTER TABLE orders
    ADD COLUMN IF NOT EXISTS delivery_longitude DOUBLE PRECISION;

-- 6. Optimistic + Pessimistic locking support on delivery assignments
ALTER TABLE delivery_assignments
    ADD COLUMN IF NOT EXISTS version BIGINT NOT NULL DEFAULT 0;

CREATE INDEX IF NOT EXISTS idx_delivery_assignments_status_partner
    ON delivery_assignments (status, delivery_partner_id);

