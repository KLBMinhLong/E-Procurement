CREATE SCHEMA IF NOT EXISTS finance;

ALTER TABLE finance.invoice_line_items
    ADD COLUMN IF NOT EXISTS po_line_item_id UUID NULL;

UPDATE finance.invoice_line_items ili
SET po_line_item_id = pli.id
FROM finance.invoices inv
JOIN finance.po_line_items pli ON pli.po_id = inv.po_id
WHERE ili.invoice_id = inv.id
  AND pli.line_number = ili.line_number
  AND pli.is_deleted = FALSE
  AND ili.po_line_item_id IS NULL
  AND ili.is_deleted = FALSE
  AND inv.is_deleted = FALSE;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'fk_invoice_line_items_po_line_item'
          AND conrelid = 'finance.invoice_line_items'::regclass
    ) THEN
        ALTER TABLE finance.invoice_line_items
            ADD CONSTRAINT fk_invoice_line_items_po_line_item
            FOREIGN KEY (po_line_item_id) REFERENCES finance.po_line_items(id);
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS ix_invoice_line_items_po_line_active
    ON finance.invoice_line_items (po_line_item_id)
    WHERE po_line_item_id IS NOT NULL AND is_deleted = FALSE;

COMMENT ON COLUMN finance.invoice_line_items.po_line_item_id IS 'Referenced PO line used for PO/GR/Invoice 3-way match.';

ALTER TABLE finance.invoices
    ADD COLUMN IF NOT EXISTS match_idempotency_key UUID NULL;

CREATE UNIQUE INDEX IF NOT EXISTS ux_invoices_match_idempotency_active
    ON finance.invoices (match_idempotency_key)
    WHERE match_idempotency_key IS NOT NULL AND is_deleted = FALSE;

COMMENT ON COLUMN finance.invoices.match_idempotency_key IS 'Idempotency-Key for the latest invoice match action.';

CREATE TABLE IF NOT EXISTS finance.goods_receipt_snapshots (
    id UUID PRIMARY KEY,
    gr_number VARCHAR(50) NOT NULL,
    po_id UUID NOT NULL REFERENCES finance.purchase_orders(id),
    po_number VARCHAR(50) NOT NULL,
    warehouse_id UUID NOT NULL,
    warehouse_keeper_id UUID NOT NULL,
    status VARCHAR(30) NOT NULL,
    received_at TIMESTAMPTZ NOT NULL,
    completed_at TIMESTAMPTZ NOT NULL,
    source_event_id VARCHAR(80) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by UUID NOT NULL,
    updated_by UUID NULL,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at TIMESTAMPTZ NULL,
    deleted_by UUID NULL,
    CONSTRAINT ck_goods_receipt_snapshots_status CHECK (status IN ('DRAFT','PARTIAL','COMPLETE','DISCREPANCY'))
);

CREATE UNIQUE INDEX IF NOT EXISTS ux_goods_receipt_snapshots_source_event_active
    ON finance.goods_receipt_snapshots (source_event_id)
    WHERE is_deleted = FALSE;
CREATE INDEX IF NOT EXISTS ix_goods_receipt_snapshots_po_active
    ON finance.goods_receipt_snapshots (po_id)
    WHERE is_deleted = FALSE;
CREATE INDEX IF NOT EXISTS ix_goods_receipt_snapshots_status_active
    ON finance.goods_receipt_snapshots (status)
    WHERE is_deleted = FALSE;

DROP TRIGGER IF EXISTS trg_goods_receipt_snapshots_touch_updated_at ON finance.goods_receipt_snapshots;
CREATE TRIGGER trg_goods_receipt_snapshots_touch_updated_at
BEFORE UPDATE ON finance.goods_receipt_snapshots
FOR EACH ROW
EXECUTE FUNCTION finance.touch_updated_at();

COMMENT ON TABLE finance.goods_receipt_snapshots IS 'Finance-side read model of completed goods receipts for invoice 3-way matching.';
COMMENT ON COLUMN finance.goods_receipt_snapshots.source_event_id IS 'inventory.gr.created event id that produced this GR snapshot.';

CREATE TABLE IF NOT EXISTS finance.goods_receipt_line_snapshots (
    gr_line_item_id UUID PRIMARY KEY,
    gr_id UUID NOT NULL REFERENCES finance.goods_receipt_snapshots(id),
    po_line_item_id UUID NOT NULL REFERENCES finance.po_line_items(id),
    item_code VARCHAR(100) NULL,
    item_name VARCHAR(500) NOT NULL,
    ordered_quantity NUMERIC(19,4) NOT NULL,
    received_quantity NUMERIC(19,4) NOT NULL,
    rejected_quantity NUMERIC(19,4) NOT NULL DEFAULT 0,
    unit VARCHAR(50) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by UUID NOT NULL,
    updated_by UUID NULL,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at TIMESTAMPTZ NULL,
    deleted_by UUID NULL,
    CONSTRAINT ck_goods_receipt_line_quantities CHECK (
        ordered_quantity >= 0 AND received_quantity >= 0 AND rejected_quantity >= 0
    )
);

CREATE INDEX IF NOT EXISTS ix_goods_receipt_line_snapshots_gr_active
    ON finance.goods_receipt_line_snapshots (gr_id)
    WHERE is_deleted = FALSE;
CREATE INDEX IF NOT EXISTS ix_goods_receipt_line_snapshots_po_line_active
    ON finance.goods_receipt_line_snapshots (po_line_item_id)
    WHERE is_deleted = FALSE;

DROP TRIGGER IF EXISTS trg_goods_receipt_line_snapshots_touch_updated_at ON finance.goods_receipt_line_snapshots;
CREATE TRIGGER trg_goods_receipt_line_snapshots_touch_updated_at
BEFORE UPDATE ON finance.goods_receipt_line_snapshots
FOR EACH ROW
EXECUTE FUNCTION finance.touch_updated_at();

COMMENT ON TABLE finance.goods_receipt_line_snapshots IS 'Finance-side GR line quantities grouped by PO line for invoice matching.';
COMMENT ON COLUMN finance.goods_receipt_line_snapshots.received_quantity IS 'Received quantity from inventory.gr.created stored as NUMERIC(19,4).';
