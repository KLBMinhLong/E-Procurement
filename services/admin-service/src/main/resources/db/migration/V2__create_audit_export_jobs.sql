-- V2__create_audit_export_jobs.sql
-- Migration: Create async audit log export job queue
-- Date: 2026-06-15

CREATE SCHEMA IF NOT EXISTS audit;
CREATE EXTENSION IF NOT EXISTS pgcrypto;

CREATE OR REPLACE FUNCTION audit.touch_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TABLE IF NOT EXISTS audit.audit_export_jobs (
    id              UUID            PRIMARY KEY DEFAULT gen_random_uuid(),
    status          VARCHAR(30)     NOT NULL DEFAULT 'QUEUED',
    from_time       TIMESTAMPTZ     NOT NULL,
    to_time         TIMESTAMPTZ     NOT NULL,
    actor_id        UUID,
    entity_type     VARCHAR(50),
    action          VARCHAR(100),
    file_name       VARCHAR(255),
    storage_path    TEXT,
    failure_reason  TEXT,
    idempotency_key UUID            NOT NULL,
    requested_at    TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    completed_at    TIMESTAMPTZ,
    expires_at      TIMESTAMPTZ,
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    created_by      UUID            NOT NULL,
    updated_by      UUID,
    is_deleted      BOOLEAN         NOT NULL DEFAULT FALSE,
    deleted_at      TIMESTAMPTZ,
    deleted_by      UUID,
    CONSTRAINT chk_audit_export_jobs_status CHECK (status IN ('QUEUED','PROCESSING','COMPLETED','FAILED')),
    CONSTRAINT chk_audit_export_jobs_time_range CHECK (from_time <= to_time)
);

CREATE UNIQUE INDEX IF NOT EXISTS ux_audit_export_jobs_idempotency_active
    ON audit.audit_export_jobs (created_by, idempotency_key)
    WHERE is_deleted = FALSE;

CREATE INDEX IF NOT EXISTS idx_audit_export_jobs_actor_created_active
    ON audit.audit_export_jobs (created_by, created_at DESC)
    WHERE is_deleted = FALSE;

CREATE INDEX IF NOT EXISTS idx_audit_export_jobs_status_active
    ON audit.audit_export_jobs (status, created_at)
    WHERE is_deleted = FALSE;

CREATE INDEX IF NOT EXISTS idx_audit_export_jobs_filter_time_active
    ON audit.audit_export_jobs (from_time, to_time)
    WHERE is_deleted = FALSE;

DROP TRIGGER IF EXISTS trg_audit_export_jobs_touch_updated_at ON audit.audit_export_jobs;
CREATE TRIGGER trg_audit_export_jobs_touch_updated_at
BEFORE UPDATE ON audit.audit_export_jobs
FOR EACH ROW
EXECUTE FUNCTION audit.touch_updated_at();

COMMENT ON TABLE audit.audit_export_jobs IS 'Async audit log export job requests created by admin-service.';
COMMENT ON COLUMN audit.audit_export_jobs.idempotency_key IS 'POST /admin/audit-log/export idempotency key scoped by created_by.';
COMMENT ON COLUMN audit.audit_export_jobs.actor_id IS 'Optional audit actor filter captured for the export.';
COMMENT ON COLUMN audit.audit_export_jobs.storage_path IS 'Storage path for generated audit export file when completed.';
