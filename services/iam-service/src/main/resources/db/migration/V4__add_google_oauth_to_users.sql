CREATE SCHEMA IF NOT EXISTS iam;

ALTER TABLE iam.users
    ADD COLUMN IF NOT EXISTS google_oauth_id VARCHAR(128) NULL;

CREATE UNIQUE INDEX IF NOT EXISTS ux_users_google_oauth_id_active
    ON iam.users (google_oauth_id)
    WHERE is_deleted = FALSE AND google_oauth_id IS NOT NULL;

COMMENT ON COLUMN iam.users.google_oauth_id IS 'Google OAuth subject identifier linked after successful SSO login.';
