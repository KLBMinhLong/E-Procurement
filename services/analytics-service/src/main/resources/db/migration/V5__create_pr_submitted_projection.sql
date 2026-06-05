CREATE TABLE IF NOT EXISTS analytics.pr_submitted_projections (
    pr_id UUID PRIMARY KEY,
    pr_number VARCHAR(80) NOT NULL,
    requester_id UUID NOT NULL,
    department_id UUID NOT NULL,
    priority VARCHAR(40) NOT NULL,
    fiscal_year INTEGER NOT NULL,
    total_amount NUMERIC(19,4) NOT NULL DEFAULT 0,
    currency VARCHAR(3) NOT NULL DEFAULT 'VND',
    submitted_at TIMESTAMPTZ NOT NULL,
    source_event_id VARCHAR(100) NOT NULL,
    event_timestamp TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by UUID NULL,
    updated_by UUID NULL,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at TIMESTAMPTZ NULL,
    deleted_by UUID NULL,
    CONSTRAINT ck_pr_submitted_fiscal_year CHECK (fiscal_year BETWEEN 2000 AND 2100),
    CONSTRAINT ck_pr_submitted_total_amount CHECK (total_amount >= 0),
    CONSTRAINT ck_pr_submitted_currency CHECK (char_length(currency) = 3)
);

CREATE INDEX IF NOT EXISTS ix_pr_submitted_period_active
    ON analytics.pr_submitted_projections (submitted_at DESC)
    WHERE is_deleted = FALSE;

CREATE INDEX IF NOT EXISTS ix_pr_submitted_department_period_active
    ON analytics.pr_submitted_projections (department_id, submitted_at DESC)
    WHERE is_deleted = FALSE;

CREATE INDEX IF NOT EXISTS ix_pr_submitted_priority_active
    ON analytics.pr_submitted_projections (priority, submitted_at DESC)
    WHERE is_deleted = FALSE;

DROP TRIGGER IF EXISTS trg_pr_submitted_projection_touch_updated_at ON analytics.pr_submitted_projections;
CREATE TRIGGER trg_pr_submitted_projection_touch_updated_at
BEFORE UPDATE ON analytics.pr_submitted_projections
FOR EACH ROW
EXECUTE FUNCTION analytics.touch_updated_at();

COMMENT ON TABLE analytics.pr_submitted_projections IS 'Submitted purchase request facts used for PR to PO cycle-time analytics.';
COMMENT ON COLUMN analytics.pr_submitted_projections.total_amount IS 'Submitted PR total amount stored as NUMERIC(19,4).';
COMMENT ON COLUMN analytics.pr_submitted_projections.submitted_at IS 'Timestamp used as PR lifecycle start for cycle-time KPI.';
