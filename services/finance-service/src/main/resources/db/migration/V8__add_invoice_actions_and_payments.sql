CREATE SCHEMA IF NOT EXISTS finance;

ALTER TABLE finance.invoices
    ADD COLUMN IF NOT EXISTS approval_idempotency_key UUID NULL,
    ADD COLUMN IF NOT EXISTS dispute_idempotency_key UUID NULL,
    ADD COLUMN IF NOT EXISTS dispute_reason TEXT NULL,
    ADD COLUMN IF NOT EXISTS disputed_by UUID NULL,
    ADD COLUMN IF NOT EXISTS disputed_at TIMESTAMPTZ NULL,
    ADD COLUMN IF NOT EXISTS paid_by UUID NULL,
    ADD COLUMN IF NOT EXISTS paid_at TIMESTAMPTZ NULL;

CREATE UNIQUE INDEX IF NOT EXISTS ux_invoices_approval_idempotency_active
    ON finance.invoices (approval_idempotency_key)
    WHERE approval_idempotency_key IS NOT NULL AND is_deleted = FALSE;
CREATE UNIQUE INDEX IF NOT EXISTS ux_invoices_dispute_idempotency_active
    ON finance.invoices (dispute_idempotency_key)
    WHERE dispute_idempotency_key IS NOT NULL AND is_deleted = FALSE;

COMMENT ON COLUMN finance.invoices.approval_idempotency_key IS 'Idempotency-Key for invoice approval action.';
COMMENT ON COLUMN finance.invoices.dispute_idempotency_key IS 'Idempotency-Key for invoice dispute action.';
COMMENT ON COLUMN finance.invoices.dispute_reason IS 'Supplier dispute reason supplied by finance.';
COMMENT ON COLUMN finance.invoices.paid_at IS 'Timestamp when payment was confirmed.';

CREATE TABLE IF NOT EXISTS finance.payments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    invoice_id UUID NOT NULL REFERENCES finance.invoices(id),
    payment_date DATE NOT NULL,
    payment_reference VARCHAR(120) NOT NULL,
    paid_amount NUMERIC(19,4) NOT NULL,
    currency VARCHAR(3) NOT NULL DEFAULT 'VND',
    notes TEXT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'CONFIRMED',
    idempotency_key UUID NOT NULL,
    confirmed_at TIMESTAMPTZ NOT NULL,
    confirmed_by UUID NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by UUID NOT NULL,
    updated_by UUID NULL,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at TIMESTAMPTZ NULL,
    deleted_by UUID NULL,
    CONSTRAINT ck_payments_amount CHECK (paid_amount > 0),
    CONSTRAINT ck_payments_currency CHECK (char_length(currency) = 3),
    CONSTRAINT ck_payments_status CHECK (status IN ('CONFIRMED','VOIDED'))
);

CREATE UNIQUE INDEX IF NOT EXISTS ux_payments_idempotency_active
    ON finance.payments (idempotency_key)
    WHERE is_deleted = FALSE;
CREATE UNIQUE INDEX IF NOT EXISTS ux_payments_reference_active
    ON finance.payments (payment_reference)
    WHERE is_deleted = FALSE;
CREATE INDEX IF NOT EXISTS ix_payments_invoice_active
    ON finance.payments (invoice_id)
    WHERE is_deleted = FALSE;
CREATE INDEX IF NOT EXISTS ix_payments_date_active
    ON finance.payments (payment_date)
    WHERE is_deleted = FALSE;

DROP TRIGGER IF EXISTS trg_payments_touch_updated_at ON finance.payments;
CREATE TRIGGER trg_payments_touch_updated_at
BEFORE UPDATE ON finance.payments
FOR EACH ROW
EXECUTE FUNCTION finance.touch_updated_at();

COMMENT ON TABLE finance.payments IS 'Confirmed invoice payment records. Rows are soft-delete capable but payment confirmation is append-only in normal flow.';
COMMENT ON COLUMN finance.payments.idempotency_key IS 'Client Idempotency-Key for safe payment confirmation replay.';
