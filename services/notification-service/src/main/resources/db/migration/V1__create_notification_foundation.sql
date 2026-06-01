-- V1__create_notification_foundation.sql
-- Migration: Create notification templates, in-app notifications, and event dedup log.
-- Date: 2026-05-30

CREATE SCHEMA IF NOT EXISTS notification;
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

CREATE OR REPLACE FUNCTION notification.update_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TABLE notification.notification_templates (
    code                VARCHAR(100)    PRIMARY KEY,
    event_type          VARCHAR(100)    NOT NULL,
    channel             VARCHAR(20)     NOT NULL,
    language            VARCHAR(5)      NOT NULL DEFAULT 'vi',
    subject_template    TEXT,
    body_template       TEXT            NOT NULL,
    is_active           BOOLEAN         NOT NULL DEFAULT TRUE,
    created_at          TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    created_by          UUID,
    is_deleted          BOOLEAN         NOT NULL DEFAULT FALSE,
    deleted_at          TIMESTAMPTZ,
    deleted_by          UUID,

    CONSTRAINT chk_notification_templates_channel
        CHECK (channel IN ('EMAIL','IN_APP','PUSH'))
);

COMMENT ON TABLE notification.notification_templates IS 'Notification rendering templates by event type, channel, and language.';
COMMENT ON COLUMN notification.notification_templates.code IS 'Stable template code.';
COMMENT ON COLUMN notification.notification_templates.body_template IS 'Safe string interpolation template using {{variable}} placeholders.';

CREATE UNIQUE INDEX idx_notification_templates_active_unique
    ON notification.notification_templates(event_type, channel, language)
    WHERE is_deleted = FALSE;
CREATE INDEX idx_notification_templates_event_channel
    ON notification.notification_templates(event_type, channel, language)
    WHERE is_active = TRUE AND is_deleted = FALSE;

CREATE TRIGGER trg_notification_templates_updated_at
    BEFORE UPDATE ON notification.notification_templates
    FOR EACH ROW
    EXECUTE FUNCTION notification.update_updated_at();

CREATE TABLE notification.notifications (
    id                  UUID            PRIMARY KEY DEFAULT gen_random_uuid(),
    recipient_id        UUID            NOT NULL,
    event_type          VARCHAR(100)    NOT NULL,
    channel             VARCHAR(20)     NOT NULL,
    subject             VARCHAR(500),
    body                TEXT            NOT NULL,
    reference_type      VARCHAR(50),
    reference_id        UUID,
    reference_number    VARCHAR(100),
    action_url          VARCHAR(500),
    status              VARCHAR(20)     NOT NULL DEFAULT 'PENDING',
    is_read             BOOLEAN         NOT NULL DEFAULT FALSE,
    read_at             TIMESTAMPTZ,
    sent_at             TIMESTAMPTZ,
    retry_count         SMALLINT        NOT NULL DEFAULT 0,
    last_error          TEXT,
    created_at          TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    created_by          UUID,
    is_deleted          BOOLEAN         NOT NULL DEFAULT FALSE,
    deleted_at          TIMESTAMPTZ,
    deleted_by          UUID,

    CONSTRAINT chk_notifications_channel
        CHECK (channel IN ('EMAIL','IN_APP','PUSH')),
    CONSTRAINT chk_notifications_status
        CHECK (status IN ('PENDING','SENT','FAILED','CANCELLED')),
    CONSTRAINT chk_notifications_retry_count
        CHECK (retry_count >= 0),
    CONSTRAINT chk_notifications_action_url
        CHECK (action_url IS NULL OR (action_url LIKE '/%' AND action_url NOT LIKE '//%'))
);

COMMENT ON TABLE notification.notifications IS 'Persisted in-app/email/push notifications.';
COMMENT ON COLUMN notification.notifications.recipient_id IS 'IAM user id receiving the notification.';
COMMENT ON COLUMN notification.notifications.action_url IS 'Relative frontend route for notification action.';

CREATE INDEX idx_notifications_recipient_unread_created
    ON notification.notifications(recipient_id, is_read, created_at DESC)
    WHERE is_deleted = FALSE;
CREATE INDEX idx_notifications_recipient_event_created
    ON notification.notifications(recipient_id, event_type, created_at DESC)
    WHERE is_deleted = FALSE;
CREATE INDEX idx_notifications_status_pending
    ON notification.notifications(status)
    WHERE status = 'PENDING' AND is_deleted = FALSE;
CREATE INDEX idx_notifications_reference
    ON notification.notifications(reference_type, reference_id)
    WHERE reference_id IS NOT NULL AND is_deleted = FALSE;

CREATE TRIGGER trg_notifications_updated_at
    BEFORE UPDATE ON notification.notifications
    FOR EACH ROW
    EXECUTE FUNCTION notification.update_updated_at();

CREATE TABLE notification.event_processing_log (
    event_id            VARCHAR(100)    PRIMARY KEY,
    event_type          VARCHAR(100)    NOT NULL,
    source              VARCHAR(100)    NOT NULL,
    topic               VARCHAR(150)    NOT NULL,
    partition_id        INTEGER,
    offset_value        BIGINT,
    handler_name        VARCHAR(100)    NOT NULL,
    status              VARCHAR(20)     NOT NULL DEFAULT 'PROCESSED',
    processed_at        TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    created_at          TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    created_by          UUID,
    is_deleted          BOOLEAN         NOT NULL DEFAULT FALSE,
    deleted_at          TIMESTAMPTZ,
    deleted_by          UUID,

    CONSTRAINT chk_notification_event_log_status
        CHECK (status IN ('PROCESSED','SKIPPED','FAILED'))
);

COMMENT ON TABLE notification.event_processing_log IS 'Kafka business-event deduplication log for notification-service.';

CREATE INDEX idx_notification_event_log_topic_processed
    ON notification.event_processing_log(topic, processed_at DESC)
    WHERE is_deleted = FALSE;

CREATE TRIGGER trg_notification_event_log_updated_at
    BEFORE UPDATE ON notification.event_processing_log
    FOR EACH ROW
    EXECUTE FUNCTION notification.update_updated_at();

INSERT INTO notification.notification_templates (
    code, event_type, channel, language, subject_template, body_template
) VALUES
    ('IN_APP_BUDGET_WARNING_VI', 'BUDGET_WARNING', 'IN_APP', 'vi',
     'Cảnh báo ngân sách {{glAccountCode}}',
     'Ngân sách {{glAccountCode}} còn {{projectedAvailable.amount}} {{projectedAvailable.currency}} sau tác động {{impactAmount.amount}} {{impactAmount.currency}}.'),
    ('IN_APP_BUDGET_EXCEEDED_VI', 'BUDGET_EXCEEDED', 'IN_APP', 'vi',
     'Vượt ngân sách {{glAccountCode}}',
     'Ngân sách {{glAccountCode}} bị vượt sau tác động {{impactAmount.amount}} {{impactAmount.currency}}. Lý do: {{reason}}.'),
    ('IN_APP_APPROVAL_TASK_ASSIGNED_VI', 'APPROVAL_TASK_ASSIGNED', 'IN_APP', 'vi',
     'Có task phê duyệt mới',
     'Bạn có task cần xử lý cho {{prNumber}}.'),
    ('IN_APP_PR_APPROVED_VI', 'PR_APPROVED', 'IN_APP', 'vi',
     'PR đã được phê duyệt',
     '{{prNumber}} đã được phê duyệt.'),
    ('IN_APP_PR_REJECTED_VI', 'PR_REJECTED', 'IN_APP', 'vi',
     'PR bị từ chối',
     '{{prNumber}} đã bị từ chối.'),
    ('IN_APP_PR_CHANGES_REQUESTED_VI', 'PR_CHANGES_REQUESTED', 'IN_APP', 'vi',
     'PR cần bổ sung',
     '{{prNumber}} cần được bổ sung thông tin.'),
    ('IN_APP_SLA_WARNING_VI', 'SLA_WARNING', 'IN_APP', 'vi',
     'SLA sắp quá hạn',
     'Task {{taskId}} sắp quá hạn SLA.'),
    ('IN_APP_SLA_BREACHED_VI', 'SLA_BREACHED', 'IN_APP', 'vi',
     'SLA đã quá hạn',
     'Task {{taskId}} đã quá hạn SLA.')
ON CONFLICT (code) DO NOTHING;
