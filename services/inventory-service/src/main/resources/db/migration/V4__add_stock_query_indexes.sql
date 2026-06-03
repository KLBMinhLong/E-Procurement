CREATE SCHEMA IF NOT EXISTS inventory;

CREATE INDEX IF NOT EXISTS ix_stock_movements_type_performed
    ON inventory.stock_movements (movement_type, performed_at DESC);

COMMENT ON INDEX inventory.ix_stock_movements_type_performed
    IS 'Supports stock movement history filtering by movement type and performed_at.';
