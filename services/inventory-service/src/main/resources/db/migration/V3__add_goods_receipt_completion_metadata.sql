CREATE SCHEMA IF NOT EXISTS inventory;

ALTER TABLE inventory.goods_receipts
    ADD COLUMN IF NOT EXISTS completed_at TIMESTAMPTZ NULL,
    ADD COLUMN IF NOT EXISTS completed_by UUID NULL,
    ADD COLUMN IF NOT EXISTS completed_idempotency_key UUID NULL;

CREATE UNIQUE INDEX IF NOT EXISTS ux_goods_receipts_completed_idempotency_active
    ON inventory.goods_receipts (completed_idempotency_key)
    WHERE completed_idempotency_key IS NOT NULL AND is_deleted = FALSE;

CREATE INDEX IF NOT EXISTS ix_goods_receipts_completed_at_active
    ON inventory.goods_receipts (completed_at DESC)
    WHERE completed_at IS NOT NULL AND is_deleted = FALSE;

COMMENT ON COLUMN inventory.goods_receipts.completed_at IS 'Timestamp when GR completion posted stock receipt movements.';
COMMENT ON COLUMN inventory.goods_receipts.completed_by IS 'User that completed the GR and posted stock receipt movements.';
COMMENT ON COLUMN inventory.goods_receipts.completed_idempotency_key IS 'Idempotency-Key used by POST /goods-receipts/{id}/complete.';
