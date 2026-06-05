CREATE SCHEMA IF NOT EXISTS analytics;

CREATE TABLE IF NOT EXISTS analytics.rfq_awarded_projections (
    rfq_id UUID PRIMARY KEY,
    rfq_number VARCHAR(80) NOT NULL,
    pr_id UUID NULL,
    pr_number VARCHAR(80) NULL,
    vendor_id UUID NOT NULL,
    vendor_name VARCHAR(255) NOT NULL,
    total_amount NUMERIC(19,4) NOT NULL DEFAULT 0,
    currency VARCHAR(3) NOT NULL DEFAULT 'VND',
    awarded_at TIMESTAMPTZ NOT NULL,
    source_event_id VARCHAR(100) NOT NULL,
    event_timestamp TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by UUID NULL,
    updated_by UUID NULL,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at TIMESTAMPTZ NULL,
    deleted_by UUID NULL,
    CONSTRAINT ck_rfq_awarded_amount CHECK (total_amount >= 0),
    CONSTRAINT ck_rfq_awarded_currency CHECK (char_length(currency) = 3)
);

CREATE TABLE IF NOT EXISTS analytics.rfq_awarded_line_projections (
    rfq_line_item_id UUID PRIMARY KEY,
    rfq_id UUID NOT NULL REFERENCES analytics.rfq_awarded_projections(rfq_id),
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
    CONSTRAINT ck_rfq_awarded_line_amounts CHECK (quantity >= 0 AND unit_price >= 0 AND total_price >= 0),
    CONSTRAINT ck_rfq_awarded_line_currency CHECK (char_length(currency) = 3)
);

CREATE TABLE IF NOT EXISTS analytics.goods_receipt_created_projections (
    gr_id UUID PRIMARY KEY,
    gr_number VARCHAR(80) NOT NULL,
    po_id UUID NOT NULL,
    po_number VARCHAR(80) NOT NULL,
    warehouse_id UUID NOT NULL,
    warehouse_keeper_id UUID NULL,
    status VARCHAR(40) NOT NULL,
    received_at TIMESTAMPTZ NOT NULL,
    completed_at TIMESTAMPTZ NOT NULL,
    source_event_id VARCHAR(100) NOT NULL,
    event_timestamp TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by UUID NULL,
    updated_by UUID NULL,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at TIMESTAMPTZ NULL,
    deleted_by UUID NULL,
    CONSTRAINT ck_gr_created_status CHECK (status IN ('DRAFT','PARTIAL','COMPLETE','DISCREPANCY'))
);

CREATE TABLE IF NOT EXISTS analytics.goods_receipt_line_projections (
    gr_line_item_id UUID PRIMARY KEY,
    gr_id UUID NOT NULL REFERENCES analytics.goods_receipt_created_projections(gr_id),
    po_line_item_id UUID NULL,
    item_code VARCHAR(120) NOT NULL,
    item_name VARCHAR(255) NOT NULL,
    ordered_quantity NUMERIC(19,4) NOT NULL DEFAULT 0,
    received_quantity NUMERIC(19,4) NOT NULL DEFAULT 0,
    rejected_quantity NUMERIC(19,4) NOT NULL DEFAULT 0,
    unit VARCHAR(40) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by UUID NULL,
    updated_by UUID NULL,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at TIMESTAMPTZ NULL,
    deleted_by UUID NULL,
    CONSTRAINT ck_gr_line_quantities CHECK (
        ordered_quantity >= 0 AND received_quantity >= 0 AND rejected_quantity >= 0
    )
);

CREATE INDEX IF NOT EXISTS ix_rfq_awarded_period_active
    ON analytics.rfq_awarded_projections (awarded_at DESC)
    WHERE is_deleted = FALSE;

CREATE INDEX IF NOT EXISTS ix_rfq_awarded_vendor_active
    ON analytics.rfq_awarded_projections (vendor_id, total_amount DESC)
    WHERE is_deleted = FALSE;

CREATE INDEX IF NOT EXISTS ix_rfq_awarded_line_rfq_active
    ON analytics.rfq_awarded_line_projections (rfq_id)
    WHERE is_deleted = FALSE;

CREATE INDEX IF NOT EXISTS ix_rfq_awarded_line_category_active
    ON analytics.rfq_awarded_line_projections (category_code, total_price DESC)
    WHERE is_deleted = FALSE;

CREATE INDEX IF NOT EXISTS ix_gr_created_period_active
    ON analytics.goods_receipt_created_projections (completed_at DESC)
    WHERE is_deleted = FALSE;

CREATE INDEX IF NOT EXISTS ix_gr_created_po_active
    ON analytics.goods_receipt_created_projections (po_id)
    WHERE is_deleted = FALSE;

CREATE INDEX IF NOT EXISTS ix_gr_line_gr_active
    ON analytics.goods_receipt_line_projections (gr_id)
    WHERE is_deleted = FALSE;

CREATE INDEX IF NOT EXISTS ix_gr_line_po_line_active
    ON analytics.goods_receipt_line_projections (po_line_item_id)
    WHERE is_deleted = FALSE AND po_line_item_id IS NOT NULL;

DROP TRIGGER IF EXISTS trg_rfq_awarded_touch_updated_at ON analytics.rfq_awarded_projections;
CREATE TRIGGER trg_rfq_awarded_touch_updated_at
BEFORE UPDATE ON analytics.rfq_awarded_projections
FOR EACH ROW
EXECUTE FUNCTION analytics.touch_updated_at();

DROP TRIGGER IF EXISTS trg_rfq_awarded_line_touch_updated_at ON analytics.rfq_awarded_line_projections;
CREATE TRIGGER trg_rfq_awarded_line_touch_updated_at
BEFORE UPDATE ON analytics.rfq_awarded_line_projections
FOR EACH ROW
EXECUTE FUNCTION analytics.touch_updated_at();

DROP TRIGGER IF EXISTS trg_gr_created_touch_updated_at ON analytics.goods_receipt_created_projections;
CREATE TRIGGER trg_gr_created_touch_updated_at
BEFORE UPDATE ON analytics.goods_receipt_created_projections
FOR EACH ROW
EXECUTE FUNCTION analytics.touch_updated_at();

DROP TRIGGER IF EXISTS trg_gr_line_touch_updated_at ON analytics.goods_receipt_line_projections;
CREATE TRIGGER trg_gr_line_touch_updated_at
BEFORE UPDATE ON analytics.goods_receipt_line_projections
FOR EACH ROW
EXECUTE FUNCTION analytics.touch_updated_at();

COMMENT ON TABLE analytics.rfq_awarded_projections IS 'RFQ award facts used for RFQ award and savings-oriented analytics reports.';
COMMENT ON TABLE analytics.rfq_awarded_line_projections IS 'Awarded RFQ line facts used for category and line-level RFQ analytics.';
COMMENT ON TABLE analytics.goods_receipt_created_projections IS 'Completed goods receipt facts used for inventory and 3-way-match analytics.';
COMMENT ON TABLE analytics.goods_receipt_line_projections IS 'Goods receipt line facts used for pending and rejected quantity analytics.';
COMMENT ON COLUMN analytics.rfq_awarded_projections.total_amount IS 'Awarded quote amount stored as NUMERIC(19,4); savings baseline is not available in the current source contract.';
COMMENT ON COLUMN analytics.goods_receipt_line_projections.rejected_quantity IS 'Rejected quantity captured from inventory.gr.created source event.';
