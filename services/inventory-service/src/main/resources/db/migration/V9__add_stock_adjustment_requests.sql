CREATE SCHEMA IF NOT EXISTS inventory;

CREATE TABLE inventory.stock_adjustment_requests (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    idempotency_key UUID NOT NULL,
    warehouse_id UUID NOT NULL REFERENCES inventory.warehouses(id),
    item_code VARCHAR(20) NOT NULL,
    previous_quantity NUMERIC(19,4) NOT NULL,
    new_quantity NUMERIC(19,4) NOT NULL,
    unit VARCHAR(20) NOT NULL,
    adjusted_by UUID NOT NULL,
    adjusted_at TIMESTAMPTZ NOT NULL,
    reason TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by UUID NOT NULL,
    updated_by UUID NULL,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at TIMESTAMPTZ NULL,
    deleted_by UUID NULL,
    CONSTRAINT ck_stock_adjustment_previous_quantity CHECK (previous_quantity >= 0),
    CONSTRAINT ck_stock_adjustment_new_quantity CHECK (new_quantity >= 0),
    CONSTRAINT ck_stock_adjustment_delta CHECK (previous_quantity <> new_quantity)
);

CREATE UNIQUE INDEX ux_stock_adjustment_idempotency_active
    ON inventory.stock_adjustment_requests (idempotency_key)
    WHERE is_deleted = FALSE;
CREATE INDEX ix_stock_adjustment_item_adjusted_active
    ON inventory.stock_adjustment_requests (item_code, adjusted_at DESC)
    WHERE is_deleted = FALSE;
CREATE INDEX ix_stock_adjustment_warehouse_adjusted_active
    ON inventory.stock_adjustment_requests (warehouse_id, adjusted_at DESC)
    WHERE is_deleted = FALSE;

CREATE TRIGGER trg_stock_adjustment_requests_touch_updated_at
BEFORE UPDATE ON inventory.stock_adjustment_requests
FOR EACH ROW
EXECUTE FUNCTION inventory.touch_updated_at();

COMMENT ON TABLE inventory.stock_adjustment_requests
    IS 'Idempotent stock adjustment request header used to replay single ADJUSTMENT movement.';
COMMENT ON COLUMN inventory.stock_adjustment_requests.idempotency_key
    IS 'Client Idempotency-Key for safe retry of POST /api/v1/stock/adjustment.';
COMMENT ON COLUMN inventory.stock_adjustment_requests.previous_quantity
    IS 'Stock quantity before adjustment stored as NUMERIC(19,4).';
COMMENT ON COLUMN inventory.stock_adjustment_requests.new_quantity
    IS 'Stock quantity after adjustment stored as NUMERIC(19,4).';
