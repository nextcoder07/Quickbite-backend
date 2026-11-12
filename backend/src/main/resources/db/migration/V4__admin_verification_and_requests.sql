-- V4: Admin verification status, permission request notes, and buyer-to-admin promotion requests
-- Compatible with both PostgreSQL (Supabase) and H2 PostgreSQL Mode

ALTER TABLE admins
    ADD COLUMN IF NOT EXISTS verification_status VARCHAR(20) NOT NULL DEFAULT 'APPROVED';

ALTER TABLE admins
    ADD COLUMN IF NOT EXISTS request_note VARCHAR(500);

CREATE TABLE IF NOT EXISTS admin_role_requests (
    id BIGSERIAL PRIMARY KEY,
    identity_id BIGINT NOT NULL REFERENCES account_identities(id) ON DELETE RESTRICT,
    requester_role VARCHAR(30) NOT NULL,
    reason TEXT,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING'
        CHECK (status IN ('PENDING', 'APPROVED', 'REJECTED')),
    reviewed_by_admin_id BIGINT REFERENCES admins(id) ON DELETE RESTRICT,
    review_note TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    reviewed_at TIMESTAMP WITH TIME ZONE
);

CREATE INDEX IF NOT EXISTS idx_admin_role_requests_identity ON admin_role_requests (identity_id);
CREATE INDEX IF NOT EXISTS idx_admin_role_requests_status ON admin_role_requests (status);

