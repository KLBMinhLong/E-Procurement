CREATE TABLE vendor.vendor_quotes (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    rfq_id UUID NOT NULL REFERENCES vendor.rfqs(id),
    vendor_id UUID NOT NULL REFERENCES vendor.vendors(id),
    vendor_name VARCHAR(300) NOT NULL,
    total_amount NUMERIC(19,4) NOT NULL,
    currency VARCHAR(3) NOT NULL,
    valid_until DATE NOT NULL,
    payment_terms TEXT NULL,
    notes TEXT NULL,
    submitted_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    evaluation_score NUMERIC(5,2) NULL,
    evaluation_note TEXT NULL,
    evaluated_by UUID NULL,
    evaluated_at TIMESTAMPTZ NULL,
    idempotency_key UUID NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by UUID NOT NULL,
    updated_by UUID NULL,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at TIMESTAMPTZ NULL,
    deleted_by UUID NULL,
    CONSTRAINT ck_vendor_quotes_amount CHECK (total_amount > 0),
    CONSTRAINT ck_vendor_quotes_score CHECK (evaluation_score IS NULL OR evaluation_score BETWEEN 0 AND 100),
    CONSTRAINT ck_vendor_quotes_currency CHECK (currency ~ '^[A-Z]{3}$')
);

CREATE UNIQUE INDEX ux_vendor_quotes_vendor_active
    ON vendor.vendor_quotes (rfq_id, vendor_id)
    WHERE is_deleted = FALSE;
CREATE UNIQUE INDEX ux_vendor_quotes_idempotency_active
    ON vendor.vendor_quotes (idempotency_key)
    WHERE idempotency_key IS NOT NULL AND is_deleted = FALSE;
CREATE INDEX ix_vendor_quotes_rfq_active
    ON vendor.vendor_quotes (rfq_id)
    WHERE is_deleted = FALSE;
CREATE INDEX ix_vendor_quotes_vendor_active
    ON vendor.vendor_quotes (vendor_id)
    WHERE is_deleted = FALSE;
CREATE INDEX ix_vendor_quotes_evaluation_active
    ON vendor.vendor_quotes (rfq_id, evaluation_score DESC)
    WHERE evaluation_score IS NOT NULL AND is_deleted = FALSE;

CREATE TRIGGER trg_vendor_quotes_touch_updated_at
BEFORE UPDATE ON vendor.vendor_quotes
FOR EACH ROW
EXECUTE FUNCTION vendor.touch_updated_at();

COMMENT ON TABLE vendor.vendor_quotes IS 'Vendor submitted quotes for RFQ evaluation and award.';
COMMENT ON COLUMN vendor.vendor_quotes.total_amount IS 'Total quote amount as sum of quote line items.';
COMMENT ON COLUMN vendor.vendor_quotes.evaluation_score IS 'Procurement evaluation score from 0 to 100.';

CREATE TABLE vendor.vendor_quote_line_items (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    quote_id UUID NOT NULL REFERENCES vendor.vendor_quotes(id),
    rfq_line_item_id UUID NOT NULL REFERENCES vendor.rfq_line_items(id),
    item_name VARCHAR(300) NOT NULL,
    quantity NUMERIC(19,4) NOT NULL,
    unit_price NUMERIC(19,4) NOT NULL,
    currency VARCHAR(3) NOT NULL,
    total_price NUMERIC(19,4) NOT NULL,
    delivery_days INTEGER NULL,
    warranty TEXT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by UUID NOT NULL,
    updated_by UUID NULL,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at TIMESTAMPTZ NULL,
    deleted_by UUID NULL,
    CONSTRAINT ck_vendor_quote_line_items_quantity CHECK (quantity > 0),
    CONSTRAINT ck_vendor_quote_line_items_unit_price CHECK (unit_price > 0),
    CONSTRAINT ck_vendor_quote_line_items_total_price CHECK (total_price > 0),
    CONSTRAINT ck_vendor_quote_line_items_delivery CHECK (delivery_days IS NULL OR delivery_days >= 0),
    CONSTRAINT ck_vendor_quote_line_items_currency CHECK (currency ~ '^[A-Z]{3}$')
);

CREATE UNIQUE INDEX ux_vendor_quote_line_items_quote_rfq_line_active
    ON vendor.vendor_quote_line_items (quote_id, rfq_line_item_id)
    WHERE is_deleted = FALSE;
CREATE INDEX ix_vendor_quote_line_items_quote_active
    ON vendor.vendor_quote_line_items (quote_id)
    WHERE is_deleted = FALSE;
CREATE INDEX ix_vendor_quote_line_items_rfq_line_active
    ON vendor.vendor_quote_line_items (rfq_line_item_id)
    WHERE is_deleted = FALSE;

CREATE TRIGGER trg_vendor_quote_line_items_touch_updated_at
BEFORE UPDATE ON vendor.vendor_quote_line_items
FOR EACH ROW
EXECUTE FUNCTION vendor.touch_updated_at();

COMMENT ON TABLE vendor.vendor_quote_line_items IS 'Line-item prices submitted by a vendor for a specific RFQ quote.';
