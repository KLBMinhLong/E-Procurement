CREATE TABLE IF NOT EXISTS analytics.approval_step_assigned_projections (
    approval_step_id UUID PRIMARY KEY,
    process_id UUID NOT NULL,
    purchase_request_id UUID NOT NULL,
    pr_number VARCHAR(80) NOT NULL,
    priority VARCHAR(40) NOT NULL,
    step_index INTEGER NOT NULL,
    step_type VARCHAR(80) NOT NULL,
    approver_role VARCHAR(80) NOT NULL,
    approver_id UUID NOT NULL,
    assigned_at TIMESTAMPTZ NOT NULL,
    sla_deadline TIMESTAMPTZ NOT NULL,
    source_event_id VARCHAR(100) NOT NULL,
    event_timestamp TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by UUID NULL,
    updated_by UUID NULL,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at TIMESTAMPTZ NULL,
    deleted_by UUID NULL,
    CONSTRAINT ck_step_assigned_step_index CHECK (step_index >= 1)
);

CREATE INDEX IF NOT EXISTS ix_step_assigned_period_active
    ON analytics.approval_step_assigned_projections (assigned_at DESC)
    WHERE is_deleted = FALSE;

CREATE INDEX IF NOT EXISTS ix_step_assigned_approver_active
    ON analytics.approval_step_assigned_projections (approver_id, assigned_at DESC)
    WHERE is_deleted = FALSE;

CREATE INDEX IF NOT EXISTS ix_step_assigned_role_active
    ON analytics.approval_step_assigned_projections (approver_role, assigned_at DESC)
    WHERE is_deleted = FALSE;

DROP TRIGGER IF EXISTS trg_step_assigned_projection_touch_updated_at ON analytics.approval_step_assigned_projections;
CREATE TRIGGER trg_step_assigned_projection_touch_updated_at
BEFORE UPDATE ON analytics.approval_step_assigned_projections
FOR EACH ROW
EXECUTE FUNCTION analytics.touch_updated_at();

COMMENT ON TABLE analytics.approval_step_assigned_projections IS 'Approval step assignment facts used as SLA compliance denominator — total steps assigned vs. breached.';
COMMENT ON COLUMN analytics.approval_step_assigned_projections.sla_deadline IS 'Expected completion deadline used to determine on-time vs. overdue status.';
