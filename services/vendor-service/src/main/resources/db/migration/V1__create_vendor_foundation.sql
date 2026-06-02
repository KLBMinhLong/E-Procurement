CREATE EXTENSION IF NOT EXISTS pgcrypto;
CREATE SCHEMA IF NOT EXISTS vendor;

CREATE OR REPLACE FUNCTION vendor.touch_updated_at()
RETURNS TRIGGER
LANGUAGE plpgsql
AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$;

CREATE SEQUENCE IF NOT EXISTS vendor.vendor_code_seq START WITH 100 INCREMENT BY 1;

CREATE TABLE vendor.vendors (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    vendor_code VARCHAR(20) NOT NULL,
    name VARCHAR(300) NOT NULL,
    tax_code VARCHAR(50) NOT NULL,
    email VARCHAR(320) NOT NULL,
    phone VARCHAR(50) NOT NULL,
    address_street VARCHAR(300) NULL,
    address_district VARCHAR(120) NULL,
    address_city VARCHAR(120) NULL,
    address_country VARCHAR(120) NOT NULL DEFAULT 'Vietnam',
    categories JSONB NOT NULL DEFAULT '[]'::jsonb,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    is_on_approved_vendor_list BOOLEAN NOT NULL DEFAULT FALSE,
    notes TEXT NULL,
    approved_by UUID NULL,
    approved_at TIMESTAMPTZ NULL,
    idempotency_key UUID NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by UUID NOT NULL,
    updated_by UUID NULL,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at TIMESTAMPTZ NULL,
    deleted_by UUID NULL,
    CONSTRAINT ck_vendors_status CHECK (status IN ('PENDING','APPROVED','BLACKLISTED','INACTIVE')),
    CONSTRAINT ck_vendors_categories_array CHECK (jsonb_typeof(categories) = 'array')
);

CREATE UNIQUE INDEX ux_vendors_code ON vendor.vendors (vendor_code);
CREATE UNIQUE INDEX ux_vendors_tax_code_active
    ON vendor.vendors (tax_code)
    WHERE is_deleted = FALSE;
CREATE UNIQUE INDEX ux_vendors_idempotency_active
    ON vendor.vendors (idempotency_key)
    WHERE idempotency_key IS NOT NULL AND is_deleted = FALSE;
CREATE INDEX ix_vendors_status_active
    ON vendor.vendors (status)
    WHERE is_deleted = FALSE;
CREATE INDEX ix_vendors_avl_active
    ON vendor.vendors (is_on_approved_vendor_list, status)
    WHERE is_deleted = FALSE;
CREATE INDEX ix_vendors_categories_gin
    ON vendor.vendors USING GIN (categories)
    WHERE is_deleted = FALSE;
CREATE INDEX ix_vendors_search_active
    ON vendor.vendors (lower(name), lower(tax_code), lower(vendor_code))
    WHERE is_deleted = FALSE;

CREATE TRIGGER trg_vendors_touch_updated_at
BEFORE UPDATE ON vendor.vendors
FOR EACH ROW
EXECUTE FUNCTION vendor.touch_updated_at();

COMMENT ON TABLE vendor.vendors IS 'Vendor master records and AVL status.';
COMMENT ON COLUMN vendor.vendors.categories IS 'JSON array of vendor category codes used for RFQ filtering.';
COMMENT ON COLUMN vendor.vendors.is_on_approved_vendor_list IS 'True when vendor can participate in RFQ/PO workflows.';

CREATE TABLE vendor.vendor_contacts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    vendor_id UUID NOT NULL REFERENCES vendor.vendors(id),
    name VARCHAR(200) NOT NULL,
    role_name VARCHAR(120) NULL,
    email VARCHAR(320) NOT NULL,
    phone VARCHAR(50) NOT NULL,
    is_primary BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by UUID NOT NULL,
    updated_by UUID NULL,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at TIMESTAMPTZ NULL,
    deleted_by UUID NULL
);

CREATE INDEX ix_vendor_contacts_vendor_active
    ON vendor.vendor_contacts (vendor_id)
    WHERE is_deleted = FALSE;
CREATE UNIQUE INDEX ux_vendor_contacts_one_primary
    ON vendor.vendor_contacts (vendor_id)
    WHERE is_primary = TRUE AND is_deleted = FALSE;

CREATE TRIGGER trg_vendor_contacts_touch_updated_at
BEFORE UPDATE ON vendor.vendor_contacts
FOR EACH ROW
EXECUTE FUNCTION vendor.touch_updated_at();

COMMENT ON TABLE vendor.vendor_contacts IS 'Vendor contact people for procurement communication.';

CREATE TABLE vendor.vendor_scores (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    vendor_id UUID NOT NULL REFERENCES vendor.vendors(id),
    quality_score SMALLINT NOT NULL DEFAULT 0,
    delivery_score SMALLINT NOT NULL DEFAULT 0,
    price_score SMALLINT NOT NULL DEFAULT 0,
    responsiveness_score SMALLINT NOT NULL DEFAULT 0,
    overall_score SMALLINT NOT NULL DEFAULT 0,
    last_evaluated_at TIMESTAMPTZ NULL,
    total_orders INTEGER NOT NULL DEFAULT 0,
    on_time_delivery_rate NUMERIC(5,2) NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by UUID NOT NULL,
    updated_by UUID NULL,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at TIMESTAMPTZ NULL,
    deleted_by UUID NULL,
    CONSTRAINT ck_vendor_scores_quality CHECK (quality_score BETWEEN 0 AND 100),
    CONSTRAINT ck_vendor_scores_delivery CHECK (delivery_score BETWEEN 0 AND 100),
    CONSTRAINT ck_vendor_scores_price CHECK (price_score BETWEEN 0 AND 100),
    CONSTRAINT ck_vendor_scores_responsiveness CHECK (responsiveness_score BETWEEN 0 AND 100),
    CONSTRAINT ck_vendor_scores_overall CHECK (overall_score BETWEEN 0 AND 100),
    CONSTRAINT ck_vendor_scores_total_orders CHECK (total_orders >= 0),
    CONSTRAINT ck_vendor_scores_otd CHECK (on_time_delivery_rate BETWEEN 0 AND 100)
);

CREATE UNIQUE INDEX ux_vendor_scores_vendor_active
    ON vendor.vendor_scores (vendor_id)
    WHERE is_deleted = FALSE;
CREATE INDEX ix_vendor_scores_overall_active
    ON vendor.vendor_scores (overall_score DESC)
    WHERE is_deleted = FALSE;

CREATE TRIGGER trg_vendor_scores_touch_updated_at
BEFORE UPDATE ON vendor.vendor_scores
FOR EACH ROW
EXECUTE FUNCTION vendor.touch_updated_at();

COMMENT ON TABLE vendor.vendor_scores IS 'Current vendor scorecard used for procurement selection.';
COMMENT ON COLUMN vendor.vendor_scores.overall_score IS '0..100 aggregate score derived from quality, delivery, price and responsiveness.';

INSERT INTO vendor.vendors (
    id, vendor_code, name, tax_code, email, phone,
    address_city, categories, status, is_on_approved_vendor_list,
    approved_by, approved_at, created_by
) VALUES
    (
        '80000000-0000-0000-0000-000000000101',
        'VND-001',
        'FIS Office Supplies Co.',
        '0100000001',
        'sales@fis-office.example',
        '0900000001',
        'Ha Noi',
        '["OFFICE_SUPPLIES"]'::jsonb,
        'APPROVED',
        TRUE,
        '30000000-0000-0000-0000-000000000004',
        NOW(),
        '00000000-0000-0000-0000-000000000000'
    ),
    (
        '80000000-0000-0000-0000-000000000102',
        'VND-002',
        'FIS IT Equipment Co.',
        '0100000002',
        'sales@fis-it.example',
        '0900000002',
        'Ho Chi Minh',
        '["IT_EQUIPMENT"]'::jsonb,
        'APPROVED',
        TRUE,
        '30000000-0000-0000-0000-000000000004',
        NOW(),
        '00000000-0000-0000-0000-000000000000'
    )
ON CONFLICT DO NOTHING;

INSERT INTO vendor.vendor_contacts (
    vendor_id, name, role_name, email, phone, is_primary, created_by
) VALUES
    (
        '80000000-0000-0000-0000-000000000101',
        'Office Sales',
        'Sales Manager',
        'sales@fis-office.example',
        '0900000001',
        TRUE,
        '00000000-0000-0000-0000-000000000000'
    ),
    (
        '80000000-0000-0000-0000-000000000102',
        'IT Sales',
        'Sales Manager',
        'sales@fis-it.example',
        '0900000002',
        TRUE,
        '00000000-0000-0000-0000-000000000000'
    )
ON CONFLICT DO NOTHING;

INSERT INTO vendor.vendor_scores (
    vendor_id, quality_score, delivery_score, price_score, responsiveness_score,
    overall_score, last_evaluated_at, total_orders, on_time_delivery_rate, created_by
) VALUES
    (
        '80000000-0000-0000-0000-000000000101',
        82, 86, 80, 84, 83, NOW(), 12, 92.00,
        '00000000-0000-0000-0000-000000000000'
    ),
    (
        '80000000-0000-0000-0000-000000000102',
        88, 84, 78, 86, 84, NOW(), 8, 87.50,
        '00000000-0000-0000-0000-000000000000'
    )
ON CONFLICT DO NOTHING;
