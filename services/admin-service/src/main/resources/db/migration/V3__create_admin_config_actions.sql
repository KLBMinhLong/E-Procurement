-- V3__create_admin_config_actions.sql
-- Migration: Create high-risk admin config action registry
-- Date: 2026-06-15

CREATE SCHEMA IF NOT EXISTS audit;
CREATE EXTENSION IF NOT EXISTS pgcrypto;

CREATE TABLE IF NOT EXISTS audit.admin_config_actions (
    id                           UUID            PRIMARY KEY DEFAULT gen_random_uuid(),
    action_type                  VARCHAR(40)     NOT NULL,
    status                       VARCHAR(40)     NOT NULL DEFAULT 'PENDING_MANUAL_APPLY',
    service_name                 VARCHAR(100),
    change_reason                TEXT,
    variable_count               INTEGER         NOT NULL DEFAULT 0,
    requires_restart             BOOLEAN         NOT NULL DEFAULT FALSE,
    estimated_downtime_seconds   INTEGER,
    key_version                  VARCHAR(80),
    idempotency_key              UUID            NOT NULL,
    requested_at                 TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    created_at                   TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at                   TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    created_by                   UUID            NOT NULL,
    updated_by                   UUID,
    is_deleted                   BOOLEAN         NOT NULL DEFAULT FALSE,
    deleted_at                   TIMESTAMPTZ,
    deleted_by                   UUID,
    CONSTRAINT chk_admin_config_actions_type CHECK (
        action_type IN ('UPDATE_CONFIG','RESTART_SERVICE','ROTATE_ENCRYPTION_KEY')
    ),
    CONSTRAINT chk_admin_config_actions_status CHECK (
        status IN ('PENDING_MANUAL_APPLY')
    ),
    CONSTRAINT chk_admin_config_actions_variable_count CHECK (variable_count >= 0),
    CONSTRAINT chk_admin_config_actions_estimated_downtime CHECK (
        estimated_downtime_seconds IS NULL OR estimated_downtime_seconds >= 0
    )
);

CREATE UNIQUE INDEX IF NOT EXISTS ux_admin_config_actions_idempotency_active
    ON audit.admin_config_actions (created_by, idempotency_key)
    WHERE is_deleted = FALSE;

CREATE INDEX IF NOT EXISTS idx_admin_config_actions_status_active
    ON audit.admin_config_actions (status, requested_at DESC)
    WHERE is_deleted = FALSE;

CREATE INDEX IF NOT EXISTS idx_admin_config_actions_type_active
    ON audit.admin_config_actions (action_type, requested_at DESC)
    WHERE is_deleted = FALSE;

CREATE INDEX IF NOT EXISTS idx_admin_config_actions_service_active
    ON audit.admin_config_actions (service_name, requested_at DESC)
    WHERE is_deleted = FALSE;

CREATE INDEX IF NOT EXISTS idx_admin_config_actions_actor_active
    ON audit.admin_config_actions (created_by, requested_at DESC)
    WHERE is_deleted = FALSE;

DROP TRIGGER IF EXISTS trg_admin_config_actions_touch_updated_at ON audit.admin_config_actions;
CREATE TRIGGER trg_admin_config_actions_touch_updated_at
BEFORE UPDATE ON audit.admin_config_actions
FOR EACH ROW
EXECUTE FUNCTION audit.touch_updated_at();

COMMENT ON TABLE audit.admin_config_actions IS 'Registry for high-risk Admin Portal config actions. Requests are verified and recorded; runtime apply requires an operator or orchestrator executor.';
COMMENT ON COLUMN audit.admin_config_actions.action_type IS 'High-risk config action requested through SYSTEM_CONFIG endpoints.';
COMMENT ON COLUMN audit.admin_config_actions.status IS 'Current execution status. Initial implementation only records PENDING_MANUAL_APPLY actions.';
COMMENT ON COLUMN audit.admin_config_actions.idempotency_key IS 'POST/PUT idempotency key scoped by created_by.';
COMMENT ON COLUMN audit.admin_config_actions.key_version IS 'Pending encryption key version label for rotation requests.';
