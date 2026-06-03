CREATE EXTENSION IF NOT EXISTS pgcrypto;
CREATE SCHEMA IF NOT EXISTS inventory;

CREATE OR REPLACE FUNCTION inventory.touch_updated_at()
RETURNS TRIGGER
LANGUAGE plpgsql
AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$;

CREATE TABLE inventory.warehouses (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    code VARCHAR(20) NOT NULL,
    name VARCHAR(200) NOT NULL,
    address TEXT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by UUID NOT NULL,
    updated_by UUID NULL,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at TIMESTAMPTZ NULL,
    deleted_by UUID NULL
);

CREATE UNIQUE INDEX ux_warehouses_code_active
    ON inventory.warehouses (code)
    WHERE is_deleted = FALSE;
CREATE INDEX ix_warehouses_active
    ON inventory.warehouses (is_active)
    WHERE is_deleted = FALSE;

CREATE TRIGGER trg_warehouses_touch_updated_at
BEFORE UPDATE ON inventory.warehouses
FOR EACH ROW
EXECUTE FUNCTION inventory.touch_updated_at();

COMMENT ON TABLE inventory.warehouses IS 'Inventory warehouses available for stock receipt and issue-out workflows.';
COMMENT ON COLUMN inventory.warehouses.code IS 'Business warehouse code unique among non-deleted warehouses.';

CREATE TABLE inventory.items (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    item_code VARCHAR(20) NOT NULL,
    name VARCHAR(300) NOT NULL,
    description TEXT NULL,
    category_code VARCHAR(50) NOT NULL,
    unit VARCHAR(20) NOT NULL,
    unit_price NUMERIC(19,4) NOT NULL DEFAULT 0,
    currency VARCHAR(3) NOT NULL DEFAULT 'VND',
    preferred_vendor_id UUID NULL,
    reorder_point NUMERIC(19,4) NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by UUID NOT NULL,
    updated_by UUID NULL,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at TIMESTAMPTZ NULL,
    deleted_by UUID NULL,
    CONSTRAINT ck_items_unit_price CHECK (unit_price >= 0),
    CONSTRAINT ck_items_reorder_point CHECK (reorder_point IS NULL OR reorder_point >= 0),
    CONSTRAINT ck_items_currency CHECK (char_length(currency) = 3)
);

CREATE UNIQUE INDEX ux_items_code_active
    ON inventory.items (item_code)
    WHERE is_deleted = FALSE;
CREATE INDEX ix_items_category_active
    ON inventory.items (category_code)
    WHERE is_deleted = FALSE;
CREATE INDEX ix_items_vendor_active
    ON inventory.items (preferred_vendor_id)
    WHERE preferred_vendor_id IS NOT NULL AND is_deleted = FALSE;

CREATE TRIGGER trg_items_touch_updated_at
BEFORE UPDATE ON inventory.items
FOR EACH ROW
EXECUTE FUNCTION inventory.touch_updated_at();

COMMENT ON TABLE inventory.items IS 'Catalog item master owned by inventory-service.';
COMMENT ON COLUMN inventory.items.unit_price IS 'Reference unit price stored as NUMERIC(19,4).';

CREATE TABLE inventory.stock_entries (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    item_code VARCHAR(20) NOT NULL,
    warehouse_id UUID NOT NULL REFERENCES inventory.warehouses(id),
    quantity_on_hand NUMERIC(19,4) NOT NULL DEFAULT 0,
    unit VARCHAR(20) NOT NULL,
    last_updated TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by UUID NOT NULL,
    updated_by UUID NULL,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at TIMESTAMPTZ NULL,
    deleted_by UUID NULL,
    CONSTRAINT ck_stock_entries_quantity CHECK (quantity_on_hand >= 0)
);

CREATE UNIQUE INDEX ux_stock_entries_item_warehouse_active
    ON inventory.stock_entries (item_code, warehouse_id)
    WHERE is_deleted = FALSE;
CREATE INDEX ix_stock_entries_item_active
    ON inventory.stock_entries (item_code)
    WHERE is_deleted = FALSE;
CREATE INDEX ix_stock_entries_warehouse_active
    ON inventory.stock_entries (warehouse_id)
    WHERE is_deleted = FALSE;

CREATE TRIGGER trg_stock_entries_touch_updated_at
BEFORE UPDATE ON inventory.stock_entries
FOR EACH ROW
EXECUTE FUNCTION inventory.touch_updated_at();

COMMENT ON TABLE inventory.stock_entries IS 'Current stock balance by item and warehouse.';
COMMENT ON COLUMN inventory.stock_entries.quantity_on_hand IS 'Available stock quantity stored as NUMERIC(19,4).';

CREATE TABLE inventory.purchase_order_snapshots (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    po_id UUID NOT NULL,
    po_number VARCHAR(30) NOT NULL,
    pr_id UUID NULL,
    pr_number VARCHAR(30) NULL,
    vendor_id UUID NOT NULL,
    vendor_name VARCHAR(300) NOT NULL,
    vendor_email VARCHAR(320) NULL,
    vendor_tax_code VARCHAR(50) NULL,
    purchasing_officer_id UUID NULL,
    total_amount NUMERIC(19,4) NOT NULL,
    currency VARCHAR(3) NOT NULL DEFAULT 'VND',
    delivery_address TEXT NULL,
    delivery_deadline DATE NULL,
    payment_terms VARCHAR(200) NULL,
    issued_at TIMESTAMPTZ NOT NULL,
    sent_to_vendor_at TIMESTAMPTZ NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by UUID NOT NULL,
    updated_by UUID NULL,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at TIMESTAMPTZ NULL,
    deleted_by UUID NULL,
    source_event_id VARCHAR(100) NOT NULL,
    CONSTRAINT ck_po_snapshots_total_amount CHECK (total_amount >= 0),
    CONSTRAINT ck_po_snapshots_currency CHECK (char_length(currency) = 3)
);

CREATE UNIQUE INDEX ux_po_snapshots_po_active
    ON inventory.purchase_order_snapshots (po_id)
    WHERE is_deleted = FALSE;
CREATE UNIQUE INDEX ux_po_snapshots_source_event_active
    ON inventory.purchase_order_snapshots (source_event_id)
    WHERE is_deleted = FALSE;
CREATE INDEX ix_po_snapshots_vendor_active
    ON inventory.purchase_order_snapshots (vendor_id)
    WHERE is_deleted = FALSE;
CREATE INDEX ix_po_snapshots_pr_active
    ON inventory.purchase_order_snapshots (pr_id)
    WHERE pr_id IS NOT NULL AND is_deleted = FALSE;

CREATE TRIGGER trg_po_snapshots_touch_updated_at
BEFORE UPDATE ON inventory.purchase_order_snapshots
FOR EACH ROW
EXECUTE FUNCTION inventory.touch_updated_at();

COMMENT ON TABLE inventory.purchase_order_snapshots IS 'Issued Purchase Order snapshot consumed from procurement.po.issued for downstream Goods Receipt.';
COMMENT ON COLUMN inventory.purchase_order_snapshots.source_event_id IS 'Kafka event id that created this inventory snapshot.';

CREATE TABLE inventory.purchase_order_line_snapshots (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    snapshot_id UUID NOT NULL REFERENCES inventory.purchase_order_snapshots(id),
    po_id UUID NOT NULL,
    po_line_item_id UUID NOT NULL,
    pr_line_item_id UUID NULL,
    item_name VARCHAR(300) NOT NULL,
    category_code VARCHAR(50) NOT NULL,
    quantity NUMERIC(19,4) NOT NULL,
    unit VARCHAR(20) NOT NULL,
    unit_price NUMERIC(19,4) NOT NULL,
    total_price NUMERIC(19,4) NOT NULL,
    currency VARCHAR(3) NOT NULL DEFAULT 'VND',
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by UUID NOT NULL,
    updated_by UUID NULL,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at TIMESTAMPTZ NULL,
    deleted_by UUID NULL,
    CONSTRAINT ck_po_line_snapshots_quantity CHECK (quantity > 0),
    CONSTRAINT ck_po_line_snapshots_unit_price CHECK (unit_price >= 0),
    CONSTRAINT ck_po_line_snapshots_total_price CHECK (total_price >= 0),
    CONSTRAINT ck_po_line_snapshots_currency CHECK (char_length(currency) = 3)
);

CREATE UNIQUE INDEX ux_po_line_snapshots_line_active
    ON inventory.purchase_order_line_snapshots (po_line_item_id)
    WHERE is_deleted = FALSE;
CREATE INDEX ix_po_line_snapshots_snapshot_active
    ON inventory.purchase_order_line_snapshots (snapshot_id)
    WHERE is_deleted = FALSE;
CREATE INDEX ix_po_line_snapshots_po_active
    ON inventory.purchase_order_line_snapshots (po_id)
    WHERE is_deleted = FALSE;

CREATE TRIGGER trg_po_line_snapshots_touch_updated_at
BEFORE UPDATE ON inventory.purchase_order_line_snapshots
FOR EACH ROW
EXECUTE FUNCTION inventory.touch_updated_at();

COMMENT ON TABLE inventory.purchase_order_line_snapshots IS 'Issued PO line item snapshot used to create and validate Goods Receipt lines.';
COMMENT ON COLUMN inventory.purchase_order_line_snapshots.quantity IS 'Ordered quantity stored as NUMERIC(19,4).';

CREATE TABLE inventory.goods_receipts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    gr_number VARCHAR(30) NOT NULL,
    po_id UUID NOT NULL,
    warehouse_id UUID NOT NULL REFERENCES inventory.warehouses(id),
    warehouse_keeper_id UUID NOT NULL,
    received_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    status VARCHAR(30) NOT NULL DEFAULT 'DRAFT',
    notes TEXT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by UUID NOT NULL,
    updated_by UUID NULL,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at TIMESTAMPTZ NULL,
    deleted_by UUID NULL,
    CONSTRAINT ck_goods_receipts_status CHECK (status IN ('DRAFT','PARTIAL','COMPLETE','DISCREPANCY'))
);

CREATE UNIQUE INDEX ux_goods_receipts_number_active
    ON inventory.goods_receipts (gr_number)
    WHERE is_deleted = FALSE;
CREATE INDEX ix_goods_receipts_po_active
    ON inventory.goods_receipts (po_id)
    WHERE is_deleted = FALSE;
CREATE INDEX ix_goods_receipts_warehouse_active
    ON inventory.goods_receipts (warehouse_id)
    WHERE is_deleted = FALSE;
CREATE INDEX ix_goods_receipts_status_active
    ON inventory.goods_receipts (status)
    WHERE is_deleted = FALSE;

CREATE TRIGGER trg_goods_receipts_touch_updated_at
BEFORE UPDATE ON inventory.goods_receipts
FOR EACH ROW
EXECUTE FUNCTION inventory.touch_updated_at();

COMMENT ON TABLE inventory.goods_receipts IS 'Goods Receipt header for confirming received goods against issued Purchase Orders.';
COMMENT ON COLUMN inventory.goods_receipts.status IS 'DRAFT, PARTIAL, COMPLETE or DISCREPANCY.';

CREATE TABLE inventory.goods_receipt_line_items (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    goods_receipt_id UUID NOT NULL REFERENCES inventory.goods_receipts(id),
    po_line_item_id UUID NOT NULL,
    item_code VARCHAR(20) NULL,
    item_name VARCHAR(300) NOT NULL,
    ordered_quantity NUMERIC(19,4) NOT NULL,
    received_quantity NUMERIC(19,4) NOT NULL DEFAULT 0,
    rejected_quantity NUMERIC(19,4) NOT NULL DEFAULT 0,
    unit VARCHAR(20) NOT NULL,
    rejection_reason TEXT NULL,
    lot_number VARCHAR(100) NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by UUID NOT NULL,
    updated_by UUID NULL,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at TIMESTAMPTZ NULL,
    deleted_by UUID NULL,
    CONSTRAINT ck_gr_line_ordered_qty CHECK (ordered_quantity > 0),
    CONSTRAINT ck_gr_line_received_qty CHECK (received_quantity >= 0),
    CONSTRAINT ck_gr_line_rejected_qty CHECK (rejected_quantity >= 0)
);

CREATE INDEX ix_gr_line_items_gr_active
    ON inventory.goods_receipt_line_items (goods_receipt_id)
    WHERE is_deleted = FALSE;
CREATE INDEX ix_gr_line_items_po_line_active
    ON inventory.goods_receipt_line_items (po_line_item_id)
    WHERE is_deleted = FALSE;

CREATE TRIGGER trg_gr_line_items_touch_updated_at
BEFORE UPDATE ON inventory.goods_receipt_line_items
FOR EACH ROW
EXECUTE FUNCTION inventory.touch_updated_at();

COMMENT ON TABLE inventory.goods_receipt_line_items IS 'Goods Receipt line-level quantity inspection results.';
COMMENT ON COLUMN inventory.goods_receipt_line_items.received_quantity IS 'Accepted received quantity stored as NUMERIC(19,4).';

CREATE TABLE inventory.stock_movements (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    item_code VARCHAR(20) NOT NULL,
    warehouse_id UUID NOT NULL REFERENCES inventory.warehouses(id),
    movement_type VARCHAR(30) NOT NULL,
    quantity NUMERIC(19,4) NOT NULL,
    unit VARCHAR(20) NOT NULL,
    balance_after NUMERIC(19,4) NOT NULL,
    source_ref_type VARCHAR(50) NULL,
    source_ref_id UUID NULL,
    performed_by UUID NOT NULL,
    performed_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    notes TEXT NULL,
    CONSTRAINT ck_stock_movements_type CHECK (movement_type IN ('RECEIPT_IN','ISSUE_OUT','ADJUSTMENT','TRANSFER')),
    CONSTRAINT ck_stock_movements_quantity CHECK (quantity <> 0),
    CONSTRAINT ck_stock_movements_balance_after CHECK (balance_after >= 0)
);

CREATE INDEX ix_stock_movements_item_performed
    ON inventory.stock_movements (item_code, performed_at DESC);
CREATE INDEX ix_stock_movements_warehouse_performed
    ON inventory.stock_movements (warehouse_id, performed_at DESC);
CREATE INDEX ix_stock_movements_ref
    ON inventory.stock_movements (source_ref_type, source_ref_id);

COMMENT ON TABLE inventory.stock_movements IS 'Immutable inventory movement ledger. Rows are append-only and are never soft deleted.';
COMMENT ON COLUMN inventory.stock_movements.quantity IS 'Movement quantity stored as NUMERIC(19,4).';

CREATE TABLE inventory.event_processing_log (
    event_id VARCHAR(100) PRIMARY KEY,
    topic VARCHAR(200) NOT NULL,
    partition_id INTEGER NULL,
    offset_value BIGINT NULL,
    handler_name VARCHAR(100) NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'PROCESSED',
    processed_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT ck_event_processing_log_status CHECK (status IN ('PROCESSED','SKIPPED'))
);

CREATE INDEX ix_event_processing_log_topic
    ON inventory.event_processing_log (topic, processed_at DESC);

COMMENT ON TABLE inventory.event_processing_log IS 'Idempotency log for Kafka event handlers in inventory-service.';

INSERT INTO inventory.warehouses (
    id, code, name, address, created_by
) VALUES (
    '81000000-0000-4000-8000-000000000001',
    'MAIN',
    'Main Warehouse',
    'eProcure Main Office',
    '00000000-0000-0000-0000-000000000000'
)
ON CONFLICT DO NOTHING;
