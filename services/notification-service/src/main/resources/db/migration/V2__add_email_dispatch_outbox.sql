-- V2__add_email_dispatch_outbox.sql
-- Migration: Add email dispatch metadata and dead-letter persistence.
-- Date: 2026-05-30

CREATE SCHEMA IF NOT EXISTS notification;

ALTER TABLE notification.notifications
    ADD COLUMN IF NOT EXISTS email_to VARCHAR(320),
    ADD COLUMN IF NOT EXISTS provider_message_id VARCHAR(200),
    ADD COLUMN IF NOT EXISTS last_attempt_at TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS next_attempt_at TIMESTAMPTZ;

ALTER TABLE notification.notifications
    DROP CONSTRAINT IF EXISTS chk_notifications_email_to_required;
ALTER TABLE notification.notifications
    ADD CONSTRAINT chk_notifications_email_to_required
        CHECK (channel <> 'EMAIL' OR email_to IS NOT NULL);

COMMENT ON COLUMN notification.notifications.email_to IS 'Recipient email address for EMAIL channel notifications.';
COMMENT ON COLUMN notification.notifications.provider_message_id IS 'Message id returned by email provider when available.';
COMMENT ON COLUMN notification.notifications.last_attempt_at IS 'Last email dispatch attempt timestamp.';
COMMENT ON COLUMN notification.notifications.next_attempt_at IS 'Next retry timestamp for EMAIL channel dispatch.';

CREATE INDEX IF NOT EXISTS idx_notifications_email_dispatch_due
    ON notification.notifications((COALESCE(next_attempt_at, created_at)), created_at)
    WHERE channel = 'EMAIL' AND status = 'PENDING' AND is_deleted = FALSE;

CREATE TABLE IF NOT EXISTS notification.email_dispatch_dead_letters (
    id                  UUID            PRIMARY KEY DEFAULT gen_random_uuid(),
    notification_id     UUID            NOT NULL UNIQUE REFERENCES notification.notifications(id),
    recipient_email     VARCHAR(320)    NOT NULL,
    event_type          VARCHAR(100)    NOT NULL,
    failure_reason      TEXT            NOT NULL,
    retry_count         SMALLINT        NOT NULL,
    failed_at           TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    created_at          TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    created_by          UUID,
    is_deleted          BOOLEAN         NOT NULL DEFAULT FALSE,
    deleted_at          TIMESTAMPTZ,
    deleted_by          UUID,

    CONSTRAINT chk_email_dispatch_dead_letters_retry_count
        CHECK (retry_count >= 0)
);

COMMENT ON TABLE notification.email_dispatch_dead_letters IS 'Dead-letter records for EMAIL notifications exhausted after configured retries.';
COMMENT ON COLUMN notification.email_dispatch_dead_letters.failure_reason IS 'Sanitized failure summary without provider secrets or tokens.';

CREATE INDEX IF NOT EXISTS idx_email_dispatch_dead_letters_failed_at
    ON notification.email_dispatch_dead_letters(failed_at DESC)
    WHERE is_deleted = FALSE;

DROP TRIGGER IF EXISTS trg_email_dispatch_dead_letters_updated_at ON notification.email_dispatch_dead_letters;
CREATE TRIGGER trg_email_dispatch_dead_letters_updated_at
    BEFORE UPDATE ON notification.email_dispatch_dead_letters
    FOR EACH ROW
    EXECUTE FUNCTION notification.update_updated_at();

INSERT INTO notification.notification_templates (
    code, event_type, channel, language, subject_template, body_template
) VALUES
    ('EMAIL_GENERIC_VI', 'EMAIL_SEND', 'EMAIL', 'vi',
     '{{subject}}',
     '{{body}}'),
    ('EMAIL_PASSWORD_RESET_VI', 'PASSWORD_RESET', 'EMAIL', 'vi',
     'Đặt lại mật khẩu eProcure',
     'Bạn vừa yêu cầu đặt lại mật khẩu eProcure. Vui lòng mở liên kết sau trong thời hạn hiệu lực: {{resetUrl}}')
ON CONFLICT (code) DO NOTHING;
