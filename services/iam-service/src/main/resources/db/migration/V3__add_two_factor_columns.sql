CREATE SCHEMA IF NOT EXISTS iam;

ALTER TABLE iam.users
    ADD COLUMN IF NOT EXISTS two_factor_secret_encrypted TEXT NULL,
    ADD COLUMN IF NOT EXISTS two_factor_pending_secret_encrypted TEXT NULL,
    ADD COLUMN IF NOT EXISTS two_factor_confirmed_at TIMESTAMPTZ NULL,
    ADD COLUMN IF NOT EXISTS two_factor_backup_codes_hash JSONB NULL;

ALTER TABLE iam.users
    ADD CONSTRAINT ck_users_two_factor_backup_codes_json
        CHECK (
            two_factor_backup_codes_hash IS NULL
            OR jsonb_typeof(two_factor_backup_codes_hash) = 'array'
        );

CREATE INDEX IF NOT EXISTS ix_users_two_factor_enabled_active
    ON iam.users (two_factor_enabled)
    WHERE is_deleted = FALSE;

COMMENT ON COLUMN iam.users.two_factor_secret_encrypted IS 'AES-GCM encrypted confirmed TOTP secret.';
COMMENT ON COLUMN iam.users.two_factor_pending_secret_encrypted IS 'AES-GCM encrypted TOTP secret waiting for confirmation.';
COMMENT ON COLUMN iam.users.two_factor_confirmed_at IS 'Timestamp when TOTP setup was confirmed.';
COMMENT ON COLUMN iam.users.two_factor_backup_codes_hash IS 'JSON array of hashed one-time 2FA backup codes.';
