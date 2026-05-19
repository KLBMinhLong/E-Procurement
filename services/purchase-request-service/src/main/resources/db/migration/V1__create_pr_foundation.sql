CREATE EXTENSION IF NOT EXISTS pgcrypto;
CREATE SCHEMA IF NOT EXISTS pr;

CREATE OR REPLACE FUNCTION pr.touch_updated_at()
RETURNS TRIGGER
LANGUAGE plpgsql
AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$;

CREATE TABLE pr.catalog_categories (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    code VARCHAR(50) NOT NULL,
    name VARCHAR(200) NOT NULL,
    parent_code VARCHAR(50) NULL,
    requires_special_approval BOOLEAN NOT NULL DEFAULT FALSE,
    special_approver_role VARCHAR(50) NULL,
    requires_rfq_above NUMERIC(19,4) NULL,
    currency VARCHAR(3) NOT NULL DEFAULT 'VND',
    is_capex BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NULL,
    created_by UUID NULL,
    updated_by UUID NULL,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at TIMESTAMPTZ NULL,
    deleted_by UUID NULL,
    CONSTRAINT uq_catalog_categories_code UNIQUE (code),
    CONSTRAINT fk_catalog_categories_parent
        FOREIGN KEY (parent_code) REFERENCES pr.catalog_categories(code),
    CONSTRAINT ck_catalog_categories_currency CHECK (char_length(currency) = 3),
    CONSTRAINT ck_catalog_categories_rfq_amount CHECK (requires_rfq_above IS NULL OR requires_rfq_above >= 0)
);

CREATE INDEX ix_catalog_categories_parent_active
    ON pr.catalog_categories (parent_code)
    WHERE is_deleted = FALSE;
CREATE INDEX ix_catalog_categories_capex_active
    ON pr.catalog_categories (is_capex)
    WHERE is_deleted = FALSE;

CREATE TRIGGER trg_catalog_categories_touch_updated_at
BEFORE UPDATE ON pr.catalog_categories
FOR EACH ROW
EXECUTE FUNCTION pr.touch_updated_at();

COMMENT ON TABLE pr.catalog_categories IS 'Danh mục hàng hóa/dịch vụ dùng khi tạo Purchase Request.';
COMMENT ON COLUMN pr.catalog_categories.code IS 'Business code của category, ví dụ IT_HARDWARE.';
COMMENT ON COLUMN pr.catalog_categories.requires_rfq_above IS 'Ngưỡng tiền bắt buộc đi RFQ, dùng NUMERIC(19,4).';

CREATE TABLE pr.catalog_items (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    item_code VARCHAR(20) NOT NULL,
    name VARCHAR(300) NOT NULL,
    description TEXT NULL,
    category_code VARCHAR(50) NOT NULL REFERENCES pr.catalog_categories(code),
    unit VARCHAR(20) NOT NULL,
    unit_price NUMERIC(19,4) NOT NULL,
    currency VARCHAR(3) NOT NULL DEFAULT 'VND',
    preferred_vendor_id UUID NULL,
    reorder_point NUMERIC(10,2) NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NULL,
    created_by UUID NULL,
    updated_by UUID NULL,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at TIMESTAMPTZ NULL,
    deleted_by UUID NULL,
    CONSTRAINT uq_catalog_items_item_code UNIQUE (item_code),
    CONSTRAINT ck_catalog_items_unit_price CHECK (unit_price >= 0),
    CONSTRAINT ck_catalog_items_currency CHECK (char_length(currency) = 3),
    CONSTRAINT ck_catalog_items_reorder_point CHECK (reorder_point IS NULL OR reorder_point >= 0)
);

CREATE INDEX ix_catalog_items_category_active
    ON pr.catalog_items (category_code)
    WHERE is_deleted = FALSE;
CREATE INDEX ix_catalog_items_active
    ON pr.catalog_items (is_active)
    WHERE is_deleted = FALSE;
CREATE INDEX ix_catalog_items_search
    ON pr.catalog_items
    USING GIN (to_tsvector('simple', name || ' ' || COALESCE(description, '')));

CREATE TRIGGER trg_catalog_items_touch_updated_at
BEFORE UPDATE ON pr.catalog_items
FOR EACH ROW
EXECUTE FUNCTION pr.touch_updated_at();

COMMENT ON TABLE pr.catalog_items IS 'Catalog item master data for quick PR line entry.';
COMMENT ON COLUMN pr.catalog_items.unit_price IS 'Default unit price, stored as NUMERIC(19,4).';

CREATE TABLE pr.purchase_requests (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    pr_number VARCHAR(20) NOT NULL,
    requester_id UUID NOT NULL,
    department_id UUID NOT NULL,
    title VARCHAR(500) NOT NULL,
    justification TEXT NOT NULL,
    priority VARCHAR(20) NOT NULL DEFAULT 'NORMAL',
    urgency_reason TEXT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'DRAFT',
    total_amount NUMERIC(19,4) NOT NULL DEFAULT 0,
    currency VARCHAR(3) NOT NULL DEFAULT 'VND',
    fiscal_year SMALLINT NOT NULL,
    need_by_date DATE NULL,
    related_contract_id UUID NULL,
    is_blanket_release BOOLEAN NOT NULL DEFAULT FALSE,
    budget_allocated NUMERIC(19,4) NULL,
    budget_committed NUMERIC(19,4) NULL,
    budget_spent NUMERIC(19,4) NULL,
    budget_available NUMERIC(19,4) NULL,
    budget_check_status VARCHAR(20) NULL,
    emergency_abuse_count SMALLINT NOT NULL DEFAULT 0,
    emergency_report_submitted_at TIMESTAMPTZ NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NULL,
    submitted_at TIMESTAMPTZ NULL,
    created_by UUID NOT NULL,
    updated_by UUID NULL,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at TIMESTAMPTZ NULL,
    deleted_by UUID NULL,
    CONSTRAINT uq_purchase_requests_pr_number UNIQUE (pr_number),
    CONSTRAINT ck_purchase_requests_priority CHECK (priority IN ('NORMAL','URGENT','EMERGENCY')),
    CONSTRAINT ck_purchase_requests_status CHECK (status IN (
        'DRAFT','SUBMITTED','PENDING_APPROVAL','CHANGES_REQUESTED',
        'APPROVED','REJECTED','CONVERTED_TO_PO','CANCELLED','CLOSED'
    )),
    CONSTRAINT ck_purchase_requests_budget_status CHECK (
        budget_check_status IS NULL OR budget_check_status IN ('PASS','WARNING','FAIL')
    ),
    CONSTRAINT ck_purchase_requests_currency CHECK (char_length(currency) = 3),
    CONSTRAINT ck_purchase_requests_total_amount CHECK (total_amount >= 0),
    CONSTRAINT ck_purchase_requests_budget_values CHECK (
        (budget_allocated IS NULL OR budget_allocated >= 0)
        AND (budget_committed IS NULL OR budget_committed >= 0)
        AND (budget_spent IS NULL OR budget_spent >= 0)
        AND (budget_available IS NULL OR budget_available >= 0)
    )
);

CREATE INDEX ix_purchase_requests_requester_active
    ON pr.purchase_requests (requester_id)
    WHERE is_deleted = FALSE;
CREATE INDEX ix_purchase_requests_department_active
    ON pr.purchase_requests (department_id)
    WHERE is_deleted = FALSE;
CREATE INDEX ix_purchase_requests_status_active
    ON pr.purchase_requests (status)
    WHERE is_deleted = FALSE;
CREATE INDEX ix_purchase_requests_priority_active
    ON pr.purchase_requests (priority)
    WHERE is_deleted = FALSE;
CREATE INDEX ix_purchase_requests_dept_status_active
    ON pr.purchase_requests (department_id, status)
    WHERE is_deleted = FALSE;
CREATE INDEX ix_purchase_requests_fiscal_year_active
    ON pr.purchase_requests (fiscal_year)
    WHERE is_deleted = FALSE;
CREATE INDEX ix_purchase_requests_created_at_active
    ON pr.purchase_requests (created_at DESC)
    WHERE is_deleted = FALSE;
CREATE INDEX ix_purchase_requests_pr_number_active
    ON pr.purchase_requests (pr_number)
    WHERE is_deleted = FALSE;

CREATE TRIGGER trg_purchase_requests_touch_updated_at
BEFORE UPDATE ON pr.purchase_requests
FOR EACH ROW
EXECUTE FUNCTION pr.touch_updated_at();

COMMENT ON TABLE pr.purchase_requests IS 'Purchase Request aggregate root.';
COMMENT ON COLUMN pr.purchase_requests.pr_number IS 'Business number formatted PR-YYYY-MM-XXXXX.';
COMMENT ON COLUMN pr.purchase_requests.total_amount IS 'Calculated request total, stored as NUMERIC(19,4).';
COMMENT ON COLUMN pr.purchase_requests.is_deleted IS 'Soft delete flag; physical delete is not used.';

CREATE TABLE pr.pr_line_items (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    pr_id UUID NOT NULL REFERENCES pr.purchase_requests(id),
    line_number SMALLINT NOT NULL,
    item_code VARCHAR(20) NULL REFERENCES pr.catalog_items(item_code),
    item_name VARCHAR(300) NOT NULL,
    description TEXT NULL,
    category_code VARCHAR(50) NOT NULL REFERENCES pr.catalog_categories(code),
    quantity NUMERIC(10,2) NOT NULL,
    unit VARCHAR(20) NOT NULL,
    unit_price NUMERIC(19,4) NOT NULL,
    total_price NUMERIC(19,4) GENERATED ALWAYS AS (quantity * unit_price) STORED,
    currency VARCHAR(3) NOT NULL DEFAULT 'VND',
    preferred_vendor_id UUID NULL,
    specifications TEXT NULL,
    gl_account_code VARCHAR(10) NOT NULL,
    is_from_catalog BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NULL,
    created_by UUID NULL,
    updated_by UUID NULL,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at TIMESTAMPTZ NULL,
    deleted_by UUID NULL,
    CONSTRAINT uq_pr_line_items_pr_line UNIQUE (pr_id, line_number),
    CONSTRAINT ck_pr_line_items_quantity CHECK (quantity > 0),
    CONSTRAINT ck_pr_line_items_unit_price CHECK (unit_price >= 0),
    CONSTRAINT ck_pr_line_items_currency CHECK (char_length(currency) = 3)
);

CREATE INDEX ix_pr_line_items_pr_active
    ON pr.pr_line_items (pr_id)
    WHERE is_deleted = FALSE;
CREATE INDEX ix_pr_line_items_category_active
    ON pr.pr_line_items (category_code)
    WHERE is_deleted = FALSE;
CREATE INDEX ix_pr_line_items_item_code_active
    ON pr.pr_line_items (item_code)
    WHERE is_deleted = FALSE;

CREATE TRIGGER trg_pr_line_items_touch_updated_at
BEFORE UPDATE ON pr.pr_line_items
FOR EACH ROW
EXECUTE FUNCTION pr.touch_updated_at();

COMMENT ON TABLE pr.pr_line_items IS 'Line items owned by a Purchase Request.';
COMMENT ON COLUMN pr.pr_line_items.total_price IS 'Generated total price = quantity * unit_price, NUMERIC(19,4).';

CREATE TABLE pr.pr_attachments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    pr_id UUID NULL REFERENCES pr.purchase_requests(id),
    file_name VARCHAR(255) NOT NULL,
    file_path TEXT NOT NULL,
    file_size BIGINT NOT NULL,
    mime_type VARCHAR(100) NOT NULL,
    uploaded_by UUID NOT NULL,
    uploaded_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NULL,
    created_by UUID NULL,
    updated_by UUID NULL,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at TIMESTAMPTZ NULL,
    deleted_by UUID NULL,
    CONSTRAINT ck_pr_attachments_file_size CHECK (file_size > 0)
);

CREATE INDEX ix_pr_attachments_pr_active
    ON pr.pr_attachments (pr_id)
    WHERE is_deleted = FALSE;
CREATE INDEX ix_pr_attachments_uploaded_by_active
    ON pr.pr_attachments (uploaded_by)
    WHERE is_deleted = FALSE;

CREATE TRIGGER trg_pr_attachments_touch_updated_at
BEFORE UPDATE ON pr.pr_attachments
FOR EACH ROW
EXECUTE FUNCTION pr.touch_updated_at();

COMMENT ON TABLE pr.pr_attachments IS 'Metadata for PR attachments; binary content is stored outside PostgreSQL.';
COMMENT ON COLUMN pr.pr_attachments.file_path IS 'Storage location/path; file content is not logged or stored in this column.';
