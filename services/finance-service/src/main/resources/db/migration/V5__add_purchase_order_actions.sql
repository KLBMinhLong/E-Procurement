CREATE SCHEMA IF NOT EXISTS finance;

ALTER TABLE finance.purchase_orders
    ADD COLUMN IF NOT EXISTS vendor_note TEXT NULL,
    ADD COLUMN IF NOT EXISTS cancelled_at TIMESTAMPTZ NULL,
    ADD COLUMN IF NOT EXISTS cancelled_by UUID NULL,
    ADD COLUMN IF NOT EXISTS cancel_reason TEXT NULL;

COMMENT ON COLUMN finance.purchase_orders.vendor_note IS 'Optional note sent to the vendor when the PO is issued.';
COMMENT ON COLUMN finance.purchase_orders.cancelled_at IS 'Timestamp when the PO was cancelled.';
COMMENT ON COLUMN finance.purchase_orders.cancelled_by IS 'Actor that cancelled the PO.';
COMMENT ON COLUMN finance.purchase_orders.cancel_reason IS 'Business reason supplied when cancelling the PO.';
