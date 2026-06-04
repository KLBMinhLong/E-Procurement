CREATE SCHEMA IF NOT EXISTS analytics;

CREATE TABLE IF NOT EXISTS analytics.report_export_jobs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    report_type VARCHAR(80) NOT NULL,
    format VARCHAR(20) NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'QUEUED',
    filters JSONB NOT NULL DEFAULT '{}'::jsonb,
    download_url TEXT NULL,
    failure_reason TEXT NULL,
    idempotency_key UUID NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    completed_at TIMESTAMPTZ NULL,
    expires_at TIMESTAMPTZ NULL,
    created_by UUID NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_by UUID NULL,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at TIMESTAMPTZ NULL,
    deleted_by UUID NULL,
    CONSTRAINT ck_report_export_type CHECK (report_type IN (
        'SPENDING_BY_DEPARTMENT',
        'BUDGET_VS_PLAN',
        'VENDOR_SCORECARD',
        'CYCLE_TIME_ANALYSIS',
        'SLA_COMPLIANCE',
        'THREE_WAY_MATCH',
        'INVENTORY_PENDING',
        'RFQ_SAVINGS',
        'MAVERICK_SPENDING',
        'AUDIT_TRAIL',
        'PR_SUMMARY',
        'PO_SUMMARY'
    )),
    CONSTRAINT ck_report_export_format CHECK (format IN ('PDF','EXCEL')),
    CONSTRAINT ck_report_export_status CHECK (status IN ('QUEUED','PROCESSING','COMPLETED','FAILED'))
);

CREATE UNIQUE INDEX IF NOT EXISTS ux_report_export_idempotency_active
    ON analytics.report_export_jobs (created_by, idempotency_key)
    WHERE is_deleted = FALSE;

CREATE INDEX IF NOT EXISTS ix_report_export_actor_created_active
    ON analytics.report_export_jobs (created_by, created_at DESC)
    WHERE is_deleted = FALSE;

CREATE INDEX IF NOT EXISTS ix_report_export_status_active
    ON analytics.report_export_jobs (status, created_at)
    WHERE is_deleted = FALSE;

DROP TRIGGER IF EXISTS trg_report_export_touch_updated_at ON analytics.report_export_jobs;
CREATE TRIGGER trg_report_export_touch_updated_at
BEFORE UPDATE ON analytics.report_export_jobs
FOR EACH ROW
EXECUTE FUNCTION analytics.touch_updated_at();

COMMENT ON TABLE analytics.report_export_jobs IS 'Async report export job requests created by analytics-service.';
COMMENT ON COLUMN analytics.report_export_jobs.idempotency_key IS 'POST /reports/export idempotency key scoped by created_by.';
COMMENT ON COLUMN analytics.report_export_jobs.filters IS 'Report filter object captured as JSONB for async workers.';
