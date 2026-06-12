CREATE SCHEMA IF NOT EXISTS inventory;

CREATE TABLE inventory.catalog_item_mutation_requests (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    idempotency_key UUID NOT NULL,
    operation VARCHAR(20) NOT NULL,
    item_code VARCHAR(20) NOT NULL,
    actor_id UUID NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by UUID NOT NULL DEFAULT '00000000-0000-0000-0000-000000000000',
    updated_by UUID NULL,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at TIMESTAMPTZ NULL,
    deleted_by UUID NULL,
    CONSTRAINT ck_catalog_item_mutation_operation CHECK (operation IN ('CREATE','UPDATE'))
);

CREATE UNIQUE INDEX ux_catalog_item_mutation_idempotency_active
    ON inventory.catalog_item_mutation_requests (idempotency_key)
    WHERE is_deleted = FALSE;

CREATE INDEX ix_catalog_item_mutation_item_active
    ON inventory.catalog_item_mutation_requests (item_code, created_at DESC)
    WHERE is_deleted = FALSE;

CREATE TRIGGER trg_catalog_item_mutation_touch_updated_at
BEFORE UPDATE ON inventory.catalog_item_mutation_requests
FOR EACH ROW
EXECUTE FUNCTION inventory.touch_updated_at();

COMMENT ON TABLE inventory.catalog_item_mutation_requests IS 'Idempotency log for catalog item create/update API requests.';
COMMENT ON COLUMN inventory.catalog_item_mutation_requests.idempotency_key IS 'API Idempotency-Key for POST/PUT item catalog mutation.';
COMMENT ON COLUMN inventory.catalog_item_mutation_requests.operation IS 'Catalog mutation type: CREATE or UPDATE.';
