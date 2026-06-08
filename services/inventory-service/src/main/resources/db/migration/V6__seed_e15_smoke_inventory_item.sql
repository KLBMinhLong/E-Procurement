CREATE SCHEMA IF NOT EXISTS inventory;

INSERT INTO inventory.items (
    item_code,
    name,
    description,
    category_code,
    unit,
    unit_price,
    currency,
    preferred_vendor_id,
    reorder_point,
    is_active,
    created_by,
    updated_by,
    is_deleted
) VALUES (
    'E15-OFFICE-KIT',
    'E15 Smoke Office Package',
    'Runtime smoke package for eProcure baseline verification.',
    'OFFICE_SUPPLIES',
    'pcs',
    1000000.0000,
    'VND',
    '80000000-0000-0000-0000-000000000101',
    5.0000,
    TRUE,
    '00000000-0000-0000-0000-000000000001',
    '00000000-0000-0000-0000-000000000001',
    FALSE
)
ON CONFLICT (item_code) WHERE is_deleted = FALSE
DO UPDATE SET
    name = EXCLUDED.name,
    description = EXCLUDED.description,
    category_code = EXCLUDED.category_code,
    unit = EXCLUDED.unit,
    unit_price = EXCLUDED.unit_price,
    currency = EXCLUDED.currency,
    preferred_vendor_id = EXCLUDED.preferred_vendor_id,
    reorder_point = EXCLUDED.reorder_point,
    is_active = TRUE,
    updated_by = EXCLUDED.updated_by,
    updated_at = NOW();

COMMENT ON TABLE inventory.items IS 'Catalog item master owned by inventory-service. E15 smoke item is seeded for runtime API baseline checks.';
