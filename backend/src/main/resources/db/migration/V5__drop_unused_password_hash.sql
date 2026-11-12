-- V5: Remove unused password_hash column from account_identities (auth is handled strictly by Firebase Auth)
ALTER TABLE account_identities
    DROP COLUMN IF EXISTS password_hash;

