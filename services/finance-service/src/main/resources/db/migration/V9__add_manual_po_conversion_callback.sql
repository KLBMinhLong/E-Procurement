CREATE SCHEMA IF NOT EXISTS finance;

CREATE UNIQUE INDEX IF NOT EXISTS ux_purchase_orders_manual_pr_active
    ON finance.purchase_orders (pr_id)
    WHERE rfq_id IS NULL
      AND status <> 'CANCELLED'
      AND is_deleted = FALSE;

CREATE TABLE IF NOT EXISTS finance.po_pr_conversion_callbacks (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    po_id UUID NOT NULL REFERENCES finance.purchase_orders(id),
    pr_id UUID NOT NULL,
    idempotency_key UUID NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    attempts INTEGER NOT NULL DEFAULT 0,
    next_retry_at TIMESTAMPTZ NULL,
    delivered_at TIMESTAMPTZ NULL,
    last_error TEXT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT ck_po_pr_conversion_callbacks_status CHECK (status IN (
        'PENDING',
        'DELIVERED',
        'FAILED_RETRYABLE',
        'FAILED_EXHAUSTED'
    )),
    CONSTRAINT ck_po_pr_conversion_callbacks_attempts CHECK (attempts >= 0)
);

CREATE UNIQUE INDEX IF NOT EXISTS ux_po_pr_conversion_callbacks_po
    ON finance.po_pr_conversion_callbacks (po_id);
CREATE INDEX IF NOT EXISTS ix_po_pr_conversion_callbacks_pr
    ON finance.po_pr_conversion_callbacks (pr_id);
CREATE INDEX IF NOT EXISTS ix_po_pr_conversion_callbacks_dispatchable
    ON finance.po_pr_conversion_callbacks (status, next_retry_at, created_at)
    WHERE status IN ('PENDING', 'FAILED_RETRYABLE');

CREATE TRIGGER trg_po_pr_conversion_callbacks_touch_updated_at
BEFORE UPDATE ON finance.po_pr_conversion_callbacks
FOR EACH ROW
EXECUTE FUNCTION finance.touch_updated_at();

COMMENT ON TABLE finance.po_pr_conversion_callbacks IS 'Durable outbox used by finance-service to mark PRs as converted to PO after local PO persistence.';
COMMENT ON COLUMN finance.po_pr_conversion_callbacks.idempotency_key IS 'UUID v4 key sent to purchase-request-service converted-to-po callback.';
COMMENT ON COLUMN finance.po_pr_conversion_callbacks.status IS 'Delivery status for PR conversion callback.';
