CREATE SCHEMA IF NOT EXISTS finance;

CREATE TABLE finance.invoices (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    invoice_number VARCHAR(100) NOT NULL,
    vendor_id UUID NOT NULL,
    po_id UUID NOT NULL REFERENCES finance.purchase_orders(id),
    subtotal NUMERIC(19,4) NOT NULL,
    tax_amount NUMERIC(19,4) NOT NULL DEFAULT 0,
    total_amount NUMERIC(19,4) NOT NULL,
    currency VARCHAR(3) NOT NULL DEFAULT 'VND',
    invoice_date DATE NOT NULL,
    due_date DATE NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'PENDING_MATCH',
    po_match_status VARCHAR(20) NULL,
    gr_match_status VARCHAR(20) NULL,
    qty_variance NUMERIC(19,4) NULL,
    price_variance NUMERIC(19,4) NULL,
    matched_at TIMESTAMPTZ NULL,
    matched_by UUID NULL,
    approved_by UUID NULL,
    approved_at TIMESTAMPTZ NULL,
    idempotency_key UUID NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by UUID NOT NULL,
    updated_by UUID NULL,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at TIMESTAMPTZ NULL,
    deleted_by UUID NULL,
    CONSTRAINT ck_invoices_amounts CHECK (subtotal >= 0 AND tax_amount >= 0 AND total_amount >= 0),
    CONSTRAINT ck_invoices_currency CHECK (char_length(currency) = 3),
    CONSTRAINT ck_invoices_dates CHECK (due_date >= invoice_date),
    CONSTRAINT ck_invoices_status CHECK (status IN (
        'PENDING_MATCH','MATCHED','MISMATCHED','APPROVED','DISPUTED','PAID','CANCELLED'
    )),
    CONSTRAINT ck_invoices_match_status CHECK (
        po_match_status IS NULL OR po_match_status IN ('MATCHED','MISMATCHED','PARTIAL')
    ),
    CONSTRAINT ck_invoices_gr_match_status CHECK (
        gr_match_status IS NULL OR gr_match_status IN ('MATCHED','MISMATCHED','PARTIAL')
    )
);

CREATE UNIQUE INDEX ux_invoices_vendor_number_active
    ON finance.invoices (vendor_id, invoice_number)
    WHERE is_deleted = FALSE;
CREATE UNIQUE INDEX ux_invoices_idempotency_active
    ON finance.invoices (idempotency_key)
    WHERE is_deleted = FALSE;
CREATE INDEX ix_invoices_po_active
    ON finance.invoices (po_id)
    WHERE is_deleted = FALSE;
CREATE INDEX ix_invoices_vendor_active
    ON finance.invoices (vendor_id)
    WHERE is_deleted = FALSE;
CREATE INDEX ix_invoices_status_active
    ON finance.invoices (status)
    WHERE is_deleted = FALSE;
CREATE INDEX ix_invoices_due_date_open
    ON finance.invoices (due_date)
    WHERE status NOT IN ('PAID','CANCELLED') AND is_deleted = FALSE;
CREATE INDEX ix_invoices_created_at_active
    ON finance.invoices (created_at DESC)
    WHERE is_deleted = FALSE;

CREATE TRIGGER trg_invoices_touch_updated_at
BEFORE UPDATE ON finance.invoices
FOR EACH ROW
EXECUTE FUNCTION finance.touch_updated_at();

COMMENT ON TABLE finance.invoices IS 'Vendor invoice headers captured for PO/GR/Invoice 3-way matching.';
COMMENT ON COLUMN finance.invoices.idempotency_key IS 'Client Idempotency-Key for safe invoice creation replay.';

CREATE TABLE finance.invoice_line_items (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    invoice_id UUID NOT NULL REFERENCES finance.invoices(id),
    line_number INTEGER NOT NULL,
    description VARCHAR(500) NOT NULL,
    quantity NUMERIC(19,4) NOT NULL,
    unit_price NUMERIC(19,4) NOT NULL,
    tax_rate NUMERIC(7,4) NOT NULL DEFAULT 0.1000,
    tax_amount NUMERIC(19,4) NOT NULL DEFAULT 0,
    total_price NUMERIC(19,4) NOT NULL,
    currency VARCHAR(3) NOT NULL DEFAULT 'VND',
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by UUID NOT NULL,
    updated_by UUID NULL,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at TIMESTAMPTZ NULL,
    deleted_by UUID NULL,
    CONSTRAINT ck_invoice_line_items_line_number CHECK (line_number > 0),
    CONSTRAINT ck_invoice_line_items_quantity CHECK (quantity > 0),
    CONSTRAINT ck_invoice_line_items_unit_price CHECK (unit_price >= 0),
    CONSTRAINT ck_invoice_line_items_tax_rate CHECK (tax_rate >= 0 AND tax_rate <= 1),
    CONSTRAINT ck_invoice_line_items_amounts CHECK (tax_amount >= 0 AND total_price >= 0),
    CONSTRAINT ck_invoice_line_items_currency CHECK (char_length(currency) = 3)
);

CREATE UNIQUE INDEX ux_invoice_line_items_line_active
    ON finance.invoice_line_items (invoice_id, line_number)
    WHERE is_deleted = FALSE;
CREATE INDEX ix_invoice_line_items_invoice_active
    ON finance.invoice_line_items (invoice_id)
    WHERE is_deleted = FALSE;

CREATE TRIGGER trg_invoice_line_items_touch_updated_at
BEFORE UPDATE ON finance.invoice_line_items
FOR EACH ROW
EXECUTE FUNCTION finance.touch_updated_at();

COMMENT ON TABLE finance.invoice_line_items IS 'Vendor invoice line item snapshot used for 3-way matching.';
COMMENT ON COLUMN finance.invoice_line_items.total_price IS 'Line subtotal before tax stored as NUMERIC(19,4).';
