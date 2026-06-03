CREATE SCHEMA IF NOT EXISTS inventory;

CREATE SEQUENCE IF NOT EXISTS inventory.gr_number_seq
    START WITH 1
    INCREMENT BY 1;

ALTER TABLE inventory.goods_receipts
    ADD COLUMN IF NOT EXISTS warehouse_keeper_full_name VARCHAR(200) NULL,
    ADD COLUMN IF NOT EXISTS idempotency_key UUID NULL;

CREATE UNIQUE INDEX IF NOT EXISTS ux_goods_receipts_idempotency_active
    ON inventory.goods_receipts (idempotency_key)
    WHERE idempotency_key IS NOT NULL AND is_deleted = FALSE;

CREATE INDEX IF NOT EXISTS ix_goods_receipts_received_at_active
    ON inventory.goods_receipts (received_at)
    WHERE is_deleted = FALSE;

CREATE INDEX IF NOT EXISTS ix_goods_receipts_created_at_active
    ON inventory.goods_receipts (created_at DESC, gr_number DESC)
    WHERE is_deleted = FALSE;

COMMENT ON COLUMN inventory.goods_receipts.warehouse_keeper_full_name IS 'Display-name snapshot of the actor creating the GR.';
COMMENT ON COLUMN inventory.goods_receipts.idempotency_key IS 'API Idempotency-Key used to prevent duplicate GR creation.';
