CREATE SCHEMA IF NOT EXISTS analytics;

CREATE TABLE IF NOT EXISTS analytics.event_processing_log (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    event_id VARCHAR(100) NOT NULL,
    topic VARCHAR(255) NOT NULL,
    partition_id INTEGER NOT NULL,
    offset_value BIGINT NOT NULL,
    handler_name VARCHAR(120) NOT NULL,
    event_timestamp TIMESTAMPTZ NOT NULL,
    processed_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    status VARCHAR(30) NOT NULL DEFAULT 'PROCESSED',
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by UUID NULL,
    updated_by UUID NULL,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at TIMESTAMPTZ NULL,
    deleted_by UUID NULL,
    CONSTRAINT ck_analytics_event_processing_status CHECK (status IN ('PROCESSED'))
);

CREATE UNIQUE INDEX IF NOT EXISTS ux_analytics_event_processing_event_active
    ON analytics.event_processing_log (event_id)
    WHERE is_deleted = FALSE;

CREATE UNIQUE INDEX IF NOT EXISTS ux_analytics_event_processing_record_active
    ON analytics.event_processing_log (topic, partition_id, offset_value)
    WHERE is_deleted = FALSE;

CREATE INDEX IF NOT EXISTS ix_analytics_event_processing_topic_active
    ON analytics.event_processing_log (topic, processed_at DESC)
    WHERE is_deleted = FALSE;

CREATE TABLE IF NOT EXISTS analytics.po_issued_projections (
    po_id UUID PRIMARY KEY,
    po_number VARCHAR(80) NOT NULL,
    pr_id UUID NULL,
    pr_number VARCHAR(80) NULL,
    vendor_id UUID NULL,
    vendor_name VARCHAR(255) NOT NULL,
    total_amount NUMERIC(19,4) NOT NULL DEFAULT 0,
    currency VARCHAR(3) NOT NULL DEFAULT 'VND',
    issued_at TIMESTAMPTZ NOT NULL,
    source_event_id VARCHAR(100) NOT NULL,
    event_timestamp TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by UUID NULL,
    updated_by UUID NULL,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at TIMESTAMPTZ NULL,
    deleted_by UUID NULL,
    CONSTRAINT ck_po_issued_amount CHECK (total_amount >= 0),
    CONSTRAINT ck_po_issued_currency CHECK (char_length(currency) = 3)
);

CREATE INDEX IF NOT EXISTS ix_po_issued_period_active
    ON analytics.po_issued_projections (issued_at DESC)
    WHERE is_deleted = FALSE;

CREATE INDEX IF NOT EXISTS ix_po_issued_vendor_active
    ON analytics.po_issued_projections (vendor_id, total_amount DESC)
    WHERE is_deleted = FALSE;

CREATE INDEX IF NOT EXISTS ix_po_issued_pr_active
    ON analytics.po_issued_projections (pr_id)
    WHERE is_deleted = FALSE AND pr_id IS NOT NULL;

CREATE TABLE IF NOT EXISTS analytics.po_issued_line_projections (
    po_line_item_id UUID PRIMARY KEY,
    po_id UUID NOT NULL REFERENCES analytics.po_issued_projections(po_id),
    pr_line_item_id UUID NULL,
    item_name VARCHAR(255) NOT NULL,
    category_code VARCHAR(80) NOT NULL,
    quantity NUMERIC(19,4) NOT NULL DEFAULT 0,
    unit VARCHAR(40) NOT NULL,
    unit_price NUMERIC(19,4) NOT NULL DEFAULT 0,
    total_price NUMERIC(19,4) NOT NULL DEFAULT 0,
    currency VARCHAR(3) NOT NULL DEFAULT 'VND',
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by UUID NULL,
    updated_by UUID NULL,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at TIMESTAMPTZ NULL,
    deleted_by UUID NULL,
    CONSTRAINT ck_po_line_projection_amounts CHECK (quantity >= 0 AND unit_price >= 0 AND total_price >= 0),
    CONSTRAINT ck_po_line_projection_currency CHECK (char_length(currency) = 3)
);

CREATE INDEX IF NOT EXISTS ix_po_line_projection_po_active
    ON analytics.po_issued_line_projections (po_id)
    WHERE is_deleted = FALSE;

CREATE INDEX IF NOT EXISTS ix_po_line_projection_category_active
    ON analytics.po_issued_line_projections (category_code, total_price DESC)
    WHERE is_deleted = FALSE;

CREATE TABLE IF NOT EXISTS analytics.invoice_matched_projections (
    invoice_id UUID PRIMARY KEY,
    invoice_number VARCHAR(80) NOT NULL,
    po_id UUID NOT NULL,
    po_number VARCHAR(80) NOT NULL,
    vendor_id UUID NULL,
    vendor_name VARCHAR(255) NOT NULL,
    total_amount NUMERIC(19,4) NOT NULL DEFAULT 0,
    currency VARCHAR(3) NOT NULL DEFAULT 'VND',
    due_date DATE NULL,
    matched_at TIMESTAMPTZ NOT NULL,
    source_event_id VARCHAR(100) NOT NULL,
    event_timestamp TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by UUID NULL,
    updated_by UUID NULL,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at TIMESTAMPTZ NULL,
    deleted_by UUID NULL,
    CONSTRAINT ck_invoice_matched_amount CHECK (total_amount >= 0),
    CONSTRAINT ck_invoice_matched_currency CHECK (char_length(currency) = 3)
);

CREATE INDEX IF NOT EXISTS ix_invoice_matched_period_active
    ON analytics.invoice_matched_projections (matched_at DESC)
    WHERE is_deleted = FALSE;

CREATE INDEX IF NOT EXISTS ix_invoice_matched_po_active
    ON analytics.invoice_matched_projections (po_id)
    WHERE is_deleted = FALSE;

CREATE TABLE IF NOT EXISTS analytics.approval_sla_breach_projections (
    approval_step_id UUID PRIMARY KEY,
    process_id UUID NOT NULL,
    purchase_request_id UUID NOT NULL,
    pr_number VARCHAR(80) NOT NULL,
    priority VARCHAR(40) NOT NULL,
    step_index INTEGER NOT NULL,
    step_type VARCHAR(80) NOT NULL,
    approver_role VARCHAR(80) NOT NULL,
    breached_approver_id UUID NOT NULL,
    escalated_to_approver_id UUID NULL,
    reassigned BOOLEAN NOT NULL DEFAULT FALSE,
    assigned_at TIMESTAMPTZ NOT NULL,
    sla_deadline TIMESTAMPTZ NOT NULL,
    breached_at TIMESTAMPTZ NOT NULL,
    source_event_id VARCHAR(100) NOT NULL,
    event_timestamp TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by UUID NULL,
    updated_by UUID NULL,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at TIMESTAMPTZ NULL,
    deleted_by UUID NULL,
    CONSTRAINT ck_sla_breach_step_index CHECK (step_index >= 1)
);

CREATE INDEX IF NOT EXISTS ix_sla_breach_period_active
    ON analytics.approval_sla_breach_projections (breached_at DESC)
    WHERE is_deleted = FALSE;

CREATE INDEX IF NOT EXISTS ix_sla_breach_pr_active
    ON analytics.approval_sla_breach_projections (purchase_request_id)
    WHERE is_deleted = FALSE;

DROP TRIGGER IF EXISTS trg_event_processing_touch_updated_at ON analytics.event_processing_log;
CREATE TRIGGER trg_event_processing_touch_updated_at
BEFORE UPDATE ON analytics.event_processing_log
FOR EACH ROW
EXECUTE FUNCTION analytics.touch_updated_at();

DROP TRIGGER IF EXISTS trg_po_issued_touch_updated_at ON analytics.po_issued_projections;
CREATE TRIGGER trg_po_issued_touch_updated_at
BEFORE UPDATE ON analytics.po_issued_projections
FOR EACH ROW
EXECUTE FUNCTION analytics.touch_updated_at();

DROP TRIGGER IF EXISTS trg_po_line_projection_touch_updated_at ON analytics.po_issued_line_projections;
CREATE TRIGGER trg_po_line_projection_touch_updated_at
BEFORE UPDATE ON analytics.po_issued_line_projections
FOR EACH ROW
EXECUTE FUNCTION analytics.touch_updated_at();

DROP TRIGGER IF EXISTS trg_invoice_matched_touch_updated_at ON analytics.invoice_matched_projections;
CREATE TRIGGER trg_invoice_matched_touch_updated_at
BEFORE UPDATE ON analytics.invoice_matched_projections
FOR EACH ROW
EXECUTE FUNCTION analytics.touch_updated_at();

DROP TRIGGER IF EXISTS trg_sla_breach_touch_updated_at ON analytics.approval_sla_breach_projections;
CREATE TRIGGER trg_sla_breach_touch_updated_at
BEFORE UPDATE ON analytics.approval_sla_breach_projections
FOR EACH ROW
EXECUTE FUNCTION analytics.touch_updated_at();

COMMENT ON TABLE analytics.event_processing_log IS 'Idempotency log for analytics-service Kafka event projection consumers.';
COMMENT ON TABLE analytics.po_issued_projections IS 'Issued purchase order facts used to build analytics dashboard snapshots.';
COMMENT ON TABLE analytics.po_issued_line_projections IS 'Issued purchase order line facts used for category spend analytics.';
COMMENT ON TABLE analytics.invoice_matched_projections IS 'Matched invoice facts retained for payment and spend analytics.';
COMMENT ON TABLE analytics.approval_sla_breach_projections IS 'Approval SLA breach facts used for executive dashboard SLA metrics.';
COMMENT ON COLUMN analytics.po_issued_projections.total_amount IS 'Issued PO total amount stored as NUMERIC(19,4).';
COMMENT ON COLUMN analytics.po_issued_line_projections.total_price IS 'Issued PO line total amount stored as NUMERIC(19,4).';
COMMENT ON COLUMN analytics.invoice_matched_projections.total_amount IS 'Matched invoice total amount stored as NUMERIC(19,4).';
