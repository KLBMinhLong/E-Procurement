CREATE SCHEMA IF NOT EXISTS finance;

CREATE SEQUENCE IF NOT EXISTS finance.po_number_seq
    START WITH 1
    INCREMENT BY 1;

CREATE TABLE finance.purchase_orders (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    po_number VARCHAR(30) NOT NULL UNIQUE,
    pr_id UUID NOT NULL,
    pr_number VARCHAR(40) NOT NULL,
    rfq_id UUID NULL,
    rfq_number VARCHAR(40) NULL,
    awarded_quote_id UUID NULL,
    vendor_id UUID NOT NULL,
    vendor_name VARCHAR(255) NOT NULL,
    vendor_email VARCHAR(255) NULL,
    vendor_tax_code VARCHAR(50) NULL,
    purchasing_officer_id UUID NOT NULL,
    purchasing_officer_full_name VARCHAR(255) NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'DRAFT',
    total_amount NUMERIC(19,4) NOT NULL DEFAULT 0,
    currency VARCHAR(3) NOT NULL DEFAULT 'VND',
    delivery_address TEXT NULL,
    delivery_deadline DATE NULL,
    payment_terms VARCHAR(100) NULL,
    is_blanket_release BOOLEAN NOT NULL DEFAULT FALSE,
    issued_at TIMESTAMPTZ NULL,
    sent_to_vendor_at TIMESTAMPTZ NULL,
    source_event_id VARCHAR(80) NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by UUID NOT NULL,
    updated_by UUID NULL,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at TIMESTAMPTZ NULL,
    deleted_by UUID NULL,
    CONSTRAINT ck_purchase_orders_status CHECK (status IN (
        'DRAFT',
        'PENDING_APPROVAL',
        'APPROVED',
        'SENT_TO_VENDOR',
        'PARTIALLY_RECEIVED',
        'FULLY_RECEIVED',
        'INVOICED',
        'PAID',
        'CLOSED',
        'CANCELLED'
    )),
    CONSTRAINT ck_purchase_orders_total_amount CHECK (total_amount >= 0),
    CONSTRAINT ck_purchase_orders_currency CHECK (char_length(currency) = 3)
);

CREATE UNIQUE INDEX ux_purchase_orders_rfq_active
    ON finance.purchase_orders (rfq_id)
    WHERE rfq_id IS NOT NULL AND is_deleted = FALSE;
CREATE UNIQUE INDEX ux_purchase_orders_source_event_active
    ON finance.purchase_orders (source_event_id)
    WHERE source_event_id IS NOT NULL AND is_deleted = FALSE;
CREATE INDEX ix_purchase_orders_pr_active
    ON finance.purchase_orders (pr_id)
    WHERE is_deleted = FALSE;
CREATE INDEX ix_purchase_orders_vendor_active
    ON finance.purchase_orders (vendor_id)
    WHERE is_deleted = FALSE;
CREATE INDEX ix_purchase_orders_officer_active
    ON finance.purchase_orders (purchasing_officer_id)
    WHERE is_deleted = FALSE;
CREATE INDEX ix_purchase_orders_status_active
    ON finance.purchase_orders (status)
    WHERE is_deleted = FALSE;
CREATE INDEX ix_purchase_orders_created_at_active
    ON finance.purchase_orders (created_at DESC)
    WHERE is_deleted = FALSE;

CREATE TRIGGER trg_purchase_orders_touch_updated_at
BEFORE UPDATE ON finance.purchase_orders
FOR EACH ROW
EXECUTE FUNCTION finance.touch_updated_at();

COMMENT ON TABLE finance.purchase_orders IS 'Purchase Order headers generated from RFQ awards and later issued to vendors.';
COMMENT ON COLUMN finance.purchase_orders.total_amount IS 'PO total amount stored as NUMERIC(19,4).';
COMMENT ON COLUMN finance.purchase_orders.source_event_id IS 'Kafka/source event id that created this PO.';
COMMENT ON COLUMN finance.purchase_orders.delivery_address IS 'Nullable while an RFQ-award-generated PO remains in DRAFT before purchasing confirms delivery details.';

CREATE TABLE finance.po_line_items (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    po_id UUID NOT NULL REFERENCES finance.purchase_orders(id),
    line_number INTEGER NOT NULL,
    rfq_line_item_id UUID NULL,
    pr_line_item_id UUID NOT NULL,
    item_name VARCHAR(255) NOT NULL,
    category_code VARCHAR(80) NOT NULL,
    quantity NUMERIC(19,4) NOT NULL,
    unit VARCHAR(30) NOT NULL,
    unit_price NUMERIC(19,4) NOT NULL,
    total_price NUMERIC(19,4) NOT NULL,
    currency VARCHAR(3) NOT NULL DEFAULT 'VND',
    delivery_days INTEGER NULL,
    warranty TEXT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by UUID NOT NULL,
    updated_by UUID NULL,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at TIMESTAMPTZ NULL,
    deleted_by UUID NULL,
    CONSTRAINT ck_po_line_items_line_number CHECK (line_number > 0),
    CONSTRAINT ck_po_line_items_quantity CHECK (quantity > 0),
    CONSTRAINT ck_po_line_items_unit_price CHECK (unit_price >= 0),
    CONSTRAINT ck_po_line_items_total_price CHECK (total_price >= 0),
    CONSTRAINT ck_po_line_items_currency CHECK (char_length(currency) = 3),
    CONSTRAINT ck_po_line_items_delivery_days CHECK (delivery_days IS NULL OR delivery_days >= 0)
);

CREATE UNIQUE INDEX ux_po_line_items_line_active
    ON finance.po_line_items (po_id, line_number)
    WHERE is_deleted = FALSE;
CREATE INDEX ix_po_line_items_po_active
    ON finance.po_line_items (po_id)
    WHERE is_deleted = FALSE;
CREATE INDEX ix_po_line_items_pr_line_active
    ON finance.po_line_items (pr_line_item_id)
    WHERE is_deleted = FALSE;
CREATE INDEX ix_po_line_items_rfq_line_active
    ON finance.po_line_items (rfq_line_item_id)
    WHERE rfq_line_item_id IS NOT NULL AND is_deleted = FALSE;

CREATE TRIGGER trg_po_line_items_touch_updated_at
BEFORE UPDATE ON finance.po_line_items
FOR EACH ROW
EXECUTE FUNCTION finance.touch_updated_at();

COMMENT ON TABLE finance.po_line_items IS 'Purchase Order line item snapshot copied from the awarded RFQ quote.';
COMMENT ON COLUMN finance.po_line_items.quantity IS 'Ordered quantity stored as NUMERIC(19,4).';
COMMENT ON COLUMN finance.po_line_items.unit_price IS 'Awarded quote unit price stored as NUMERIC(19,4).';
COMMENT ON COLUMN finance.po_line_items.total_price IS 'Awarded quote line total stored as NUMERIC(19,4).';
