CREATE SEQUENCE IF NOT EXISTS vendor.rfq_number_seq START WITH 1 INCREMENT BY 1;

CREATE TABLE vendor.rfqs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    rfq_number VARCHAR(30) NOT NULL,
    pr_id UUID NOT NULL,
    pr_number VARCHAR(50) NOT NULL,
    title VARCHAR(300) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PUBLISHED',
    submission_deadline TIMESTAMPTZ NOT NULL,
    requirements TEXT NULL,
    awarded_vendor_id UUID NULL,
    awarded_quote_id UUID NULL,
    award_reason TEXT NULL,
    closed_at TIMESTAMPTZ NULL,
    idempotency_key UUID NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by UUID NOT NULL,
    updated_by UUID NULL,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at TIMESTAMPTZ NULL,
    deleted_by UUID NULL,
    CONSTRAINT ck_rfqs_status CHECK (status IN ('DRAFT','PUBLISHED','CLOSED','AWARDED','CANCELLED'))
);

CREATE UNIQUE INDEX ux_rfqs_number ON vendor.rfqs (rfq_number);
CREATE INDEX ix_rfqs_pr_active
    ON vendor.rfqs (pr_id)
    WHERE is_deleted = FALSE;
CREATE UNIQUE INDEX ux_rfqs_idempotency_active
    ON vendor.rfqs (idempotency_key)
    WHERE idempotency_key IS NOT NULL AND is_deleted = FALSE;
CREATE INDEX ix_rfqs_status_active
    ON vendor.rfqs (status)
    WHERE is_deleted = FALSE;
CREATE INDEX ix_rfqs_deadline_active
    ON vendor.rfqs (submission_deadline)
    WHERE is_deleted = FALSE;

CREATE TRIGGER trg_rfqs_touch_updated_at
BEFORE UPDATE ON vendor.rfqs
FOR EACH ROW
EXECUTE FUNCTION vendor.touch_updated_at();

COMMENT ON TABLE vendor.rfqs IS 'Request for Quotation records created from approved purchase requests.';
COMMENT ON COLUMN vendor.rfqs.pr_id IS 'Purchase request id used as RFQ source snapshot.';
COMMENT ON COLUMN vendor.rfqs.submission_deadline IS 'Deadline after which RFQ should no longer accept quotes.';

CREATE TABLE vendor.rfq_line_items (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    rfq_id UUID NOT NULL REFERENCES vendor.rfqs(id),
    pr_line_item_id UUID NOT NULL,
    item_name VARCHAR(300) NOT NULL,
    category_code VARCHAR(50) NOT NULL,
    quantity NUMERIC(19,4) NOT NULL,
    unit VARCHAR(50) NOT NULL,
    specifications TEXT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by UUID NOT NULL,
    updated_by UUID NULL,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at TIMESTAMPTZ NULL,
    deleted_by UUID NULL,
    CONSTRAINT ck_rfq_line_items_quantity CHECK (quantity > 0)
);

CREATE INDEX ix_rfq_line_items_rfq_active
    ON vendor.rfq_line_items (rfq_id)
    WHERE is_deleted = FALSE;
CREATE INDEX ix_rfq_line_items_category_active
    ON vendor.rfq_line_items (category_code)
    WHERE is_deleted = FALSE;

CREATE TRIGGER trg_rfq_line_items_touch_updated_at
BEFORE UPDATE ON vendor.rfq_line_items
FOR EACH ROW
EXECUTE FUNCTION vendor.touch_updated_at();

COMMENT ON TABLE vendor.rfq_line_items IS 'Immutable-ish RFQ line item snapshot copied from approved PR at RFQ creation.';

CREATE TABLE vendor.rfq_invitations (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    rfq_id UUID NOT NULL REFERENCES vendor.rfqs(id),
    vendor_id UUID NOT NULL REFERENCES vendor.vendors(id),
    vendor_name VARCHAR(300) NOT NULL,
    invited_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    has_submitted BOOLEAN NOT NULL DEFAULT FALSE,
    submitted_at TIMESTAMPTZ NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by UUID NOT NULL,
    updated_by UUID NULL,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at TIMESTAMPTZ NULL,
    deleted_by UUID NULL
);

CREATE UNIQUE INDEX ux_rfq_invitations_vendor_active
    ON vendor.rfq_invitations (rfq_id, vendor_id)
    WHERE is_deleted = FALSE;
CREATE INDEX ix_rfq_invitations_vendor_active
    ON vendor.rfq_invitations (vendor_id)
    WHERE is_deleted = FALSE;

CREATE TRIGGER trg_rfq_invitations_touch_updated_at
BEFORE UPDATE ON vendor.rfq_invitations
FOR EACH ROW
EXECUTE FUNCTION vendor.touch_updated_at();

COMMENT ON TABLE vendor.rfq_invitations IS 'Vendor invitations for an RFQ. Quote submission is implemented in a later slice.';
