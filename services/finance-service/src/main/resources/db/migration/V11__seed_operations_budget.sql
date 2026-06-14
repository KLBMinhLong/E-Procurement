CREATE SCHEMA IF NOT EXISTS finance;

-- Local/dev runtime budget for Operations requesters.
-- Without this, PR submit for users in department OPERATIONS fails at the real
-- Finance budget check before the approval workflow can be created.

INSERT INTO finance.budgets (
    id, department_id, fiscal_year, quarter, gl_account_code, allocated_amount,
    currency, status, approved_by, approved_at, created_by
) VALUES (
    '70000000-0000-0000-0000-000000000105',
    '44444444-4444-4444-4444-444444444444',
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
