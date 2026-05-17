CREATE SCHEMA IF NOT EXISTS iam;

CREATE TABLE IF NOT EXISTS iam.password_reset_tokens (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES iam.users(id),
    token_hash CHAR(64) NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    used_at TIMESTAMPTZ NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NULL,
    created_by UUID NULL,
    updated_by UUID NULL,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at TIMESTAMPTZ NULL,
    deleted_by UUID NULL,
    CONSTRAINT ck_password_reset_tokens_token_hash CHECK (token_hash ~ '^[a-f0-9]{64}$')
);

CREATE UNIQUE INDEX IF NOT EXISTS ux_password_reset_tokens_hash_active
    ON iam.password_reset_tokens (token_hash)
    WHERE is_deleted = FALSE;

CREATE INDEX IF NOT EXISTS ix_password_reset_tokens_user_active
    ON iam.password_reset_tokens (user_id, expires_at DESC)
    WHERE is_deleted = FALSE AND used_at IS NULL;

CREATE TRIGGER trg_password_reset_tokens_touch_updated_at
BEFORE UPDATE ON iam.password_reset_tokens
FOR EACH ROW
EXECUTE FUNCTION iam.touch_updated_at();

COMMENT ON TABLE iam.password_reset_tokens IS 'Password reset token hashes. Raw reset tokens are never persisted.';
COMMENT ON COLUMN iam.password_reset_tokens.token_hash IS 'SHA-256 hex of opaque 64-char reset token.';
COMMENT ON COLUMN iam.password_reset_tokens.is_deleted IS 'Soft delete flag; physical delete is not used.';

CREATE TABLE IF NOT EXISTS iam.password_history (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES iam.users(id),
    password_hash TEXT NOT NULL,
    changed_at TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NULL,
    created_by UUID NULL,
    updated_by UUID NULL,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at TIMESTAMPTZ NULL,
    deleted_by UUID NULL
);

CREATE INDEX IF NOT EXISTS ix_password_history_user_changed_at_active
    ON iam.password_history (user_id, changed_at DESC)
    WHERE is_deleted = FALSE;

CREATE TRIGGER trg_password_history_touch_updated_at
BEFORE UPDATE ON iam.password_history
FOR EACH ROW
EXECUTE FUNCTION iam.touch_updated_at();

COMMENT ON TABLE iam.password_history IS 'Recent password hashes used to prevent password reuse.';
COMMENT ON COLUMN iam.password_history.password_hash IS 'BCrypt hash of password plus userId salt.';
COMMENT ON COLUMN iam.password_history.is_deleted IS 'Soft delete flag; physical delete is not used.';
