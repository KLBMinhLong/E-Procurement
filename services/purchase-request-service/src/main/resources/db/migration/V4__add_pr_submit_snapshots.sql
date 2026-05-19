ALTER TABLE pr.purchase_requests
    ADD COLUMN IF NOT EXISTS budget_warning_message TEXT NULL,
    ADD COLUMN IF NOT EXISTS inventory_check JSONB NULL;

COMMENT ON COLUMN pr.purchase_requests.budget_warning_message IS 'Budget check warning or failure message captured at submit time.';
COMMENT ON COLUMN pr.purchase_requests.inventory_check IS 'Inventory check result snapshot captured at submit time.';
