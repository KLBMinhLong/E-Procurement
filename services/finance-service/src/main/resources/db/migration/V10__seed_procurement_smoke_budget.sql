CREATE SCHEMA IF NOT EXISTS finance;

-- Active budget for the seeded requester/manager PROCUREMENT department.
-- This keeps the E15 runtime smoke path on the real finance budget check.

INSERT INTO finance.budgets (
    id, department_id, fiscal_year, quarter, gl_account_code, allocated_amount,
    currency, status, approved_by, approved_at, created_by
) VALUES (
    '70000000-0000-0000-0000-000000000104',
    '22222222-2222-2222-2222-222222222222',
    2026,
    NULL,
    '6002',
    500000000.0000,
    'VND',
    'ACTIVE',
    '30000000-0000-0000-0000-000000000008',
    NOW(),
    '00000000-0000-0000-0000-000000000000'
)
ON CONFLICT DO NOTHING;
