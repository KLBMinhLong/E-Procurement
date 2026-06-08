INSERT INTO pr.catalog_items (
    item_code,
    name,
    description,
    category_code,
    unit,
    unit_price,
    currency,
    preferred_vendor_id,
    reorder_point,
    created_by
) VALUES (
    'E15-OFFICE-KIT',
    'E15 Smoke Office Package',
    'Stable catalog item used by the E15 runtime/API smoke flow.',
    'OFFICE_SUPPLIES',
    'pcs',
    1000000.0000,
    'VND',
    '80000000-0000-0000-0000-000000000101',
    10.0000,
    '00000000-0000-0000-0000-000000000000'
)
ON CONFLICT (item_code) DO UPDATE SET
    name = EXCLUDED.name,
    description = EXCLUDED.description,
    category_code = EXCLUDED.category_code,
    unit = EXCLUDED.unit,
    unit_price = EXCLUDED.unit_price,
    currency = EXCLUDED.currency,
    preferred_vendor_id = EXCLUDED.preferred_vendor_id,
    reorder_point = EXCLUDED.reorder_point,
    is_active = TRUE,
    is_deleted = FALSE,
    deleted_at = NULL,
    deleted_by = NULL,
    updated_by = '00000000-0000-0000-0000-000000000000';
