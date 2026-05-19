INSERT INTO pr.catalog_categories (
    code,
    name,
    requires_special_approval,
    special_approver_role,
    requires_rfq_above,
    currency,
    is_capex
) VALUES
    ('IT_HARDWARE', 'IT Hardware', FALSE, NULL, 50000000.0000, 'VND', TRUE),
    ('IT_SOFTWARE', 'IT Software', TRUE, 'CISO', 50000000.0000, 'VND', FALSE),
    ('OFFICE_SUPPLIES', 'Office Supplies', FALSE, NULL, NULL, 'VND', FALSE),
    ('FACILITIES', 'Facilities', FALSE, NULL, 50000000.0000, 'VND', FALSE)
ON CONFLICT (code) DO NOTHING;
