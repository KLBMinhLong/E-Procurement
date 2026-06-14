-- Migration: Add idempotency tracking for draft Goods Receipt updates
-- Author: Codex
-- Date: 2026-06-12

ALTER TABLE inventory.goods_receipts
    ADD COLUMN IF NOT EXISTS update_idempotency_key UUID;

CREATE INDEX IF NOT EXISTS idx_goods_receipts_update_idempotency
    ON inventory.goods_receipts(id, update_idempotency_key)
    WHERE update_idempotency_key IS NOT NULL
      AND is_deleted = FALSE;

COMMENT ON COLUMN inventory.goods_receipts.update_idempotency_key
    IS 'Last Idempotency-Key accepted for PUT draft Goods Receipt updates.';
