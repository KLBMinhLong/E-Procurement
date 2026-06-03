CREATE SCHEMA IF NOT EXISTS inventory;

CREATE TABLE inventory.stock_issue_out_requests (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    idempotency_key UUID NOT NULL,
    warehouse_id UUID NOT NULL REFERENCES inventory.warehouses(id),
    pr_id UUID NULL,
    recipient_id UUID NOT NULL,
    issued_by UUID NOT NULL,
    issued_at TIMESTAMPTZ NOT NULL,
    notes TEXT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by UUID NOT NULL,
    updated_by UUID NULL,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at TIMESTAMPTZ NULL,
    deleted_by UUID NULL
);

CREATE UNIQUE INDEX ux_stock_issue_out_idempotency_active
    ON inventory.stock_issue_out_requests (idempotency_key)
    WHERE is_deleted = FALSE;
CREATE INDEX ix_stock_issue_out_warehouse_issued_active
    ON inventory.stock_issue_out_requests (warehouse_id, issued_at DESC)
    WHERE is_deleted = FALSE;
CREATE INDEX ix_stock_issue_out_recipient_issued_active
    ON inventory.stock_issue_out_requests (recipient_id, issued_at DESC)
    WHERE is_deleted = FALSE;
CREATE INDEX ix_stock_issue_out_pr_active
    ON inventory.stock_issue_out_requests (pr_id)
    WHERE pr_id IS NOT NULL AND is_deleted = FALSE;

CREATE TRIGGER trg_stock_issue_out_requests_touch_updated_at
BEFORE UPDATE ON inventory.stock_issue_out_requests
FOR EACH ROW
EXECUTE FUNCTION inventory.touch_updated_at();

COMMENT ON TABLE inventory.stock_issue_out_requests
    IS 'Idempotent stock issue-out request header used to replay multi-line ISSUE_OUT movements.';
COMMENT ON COLUMN inventory.stock_issue_out_requests.idempotency_key
    IS 'Client Idempotency-Key for safe retry of POST /api/v1/stock/issue-out.';
