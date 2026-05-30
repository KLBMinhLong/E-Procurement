CREATE EXTENSION IF NOT EXISTS pgcrypto;
CREATE SCHEMA IF NOT EXISTS finance;

CREATE OR REPLACE FUNCTION finance.touch_updated_at()
RETURNS TRIGGER
LANGUAGE plpgsql
AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$;

CREATE TABLE finance.budgets (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    department_id UUID NOT NULL,
    fiscal_year SMALLINT NOT NULL,
    quarter SMALLINT NULL,
    gl_account_code VARCHAR(10) NOT NULL,
    allocated_amount NUMERIC(19,4) NOT NULL DEFAULT 0,
    currency VARCHAR(3) NOT NULL DEFAULT 'VND',
    status VARCHAR(20) NOT NULL DEFAULT 'PLANNING',
    approved_by UUID NULL,
    approved_at TIMESTAMPTZ NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by UUID NOT NULL,
    updated_by UUID NULL,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at TIMESTAMPTZ NULL,
    deleted_by UUID NULL,
    CONSTRAINT ck_budgets_status CHECK (status IN ('PLANNING','SUBMITTED','APPROVED','ACTIVE','CLOSED')),
    CONSTRAINT ck_budgets_quarter CHECK (quarter BETWEEN 1 AND 4 OR quarter IS NULL),
    CONSTRAINT ck_budgets_allocated_amount CHECK (allocated_amount >= 0),
    CONSTRAINT ck_budgets_currency CHECK (char_length(currency) = 3)
);

CREATE UNIQUE INDEX ux_budgets_dept_period_gl_active
    ON finance.budgets (department_id, fiscal_year, COALESCE(quarter, 0), gl_account_code)
    WHERE is_deleted = FALSE;
CREATE INDEX ix_budgets_dept_year_active
    ON finance.budgets (department_id, fiscal_year)
    WHERE is_deleted = FALSE;
CREATE INDEX ix_budgets_status_active
    ON finance.budgets (status)
    WHERE is_deleted = FALSE;

CREATE TRIGGER trg_budgets_touch_updated_at
BEFORE UPDATE ON finance.budgets
FOR EACH ROW
EXECUTE FUNCTION finance.touch_updated_at();

COMMENT ON TABLE finance.budgets IS 'Budget lines by department, fiscal period and GL account.';
COMMENT ON COLUMN finance.budgets.allocated_amount IS 'Approved allocated amount for the budget line, stored as NUMERIC(19,4).';
COMMENT ON COLUMN finance.budgets.gl_account_code IS 'General ledger account code used to group purchase request spending.';

CREATE TABLE finance.budget_transactions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    budget_id UUID NOT NULL REFERENCES finance.budgets(id),
    transaction_type VARCHAR(30) NOT NULL,
    amount NUMERIC(19,4) NOT NULL,
    currency VARCHAR(3) NOT NULL DEFAULT 'VND',
    reference_type VARCHAR(50) NOT NULL,
    reference_id UUID NOT NULL,
    description TEXT NULL,
    performed_by UUID NOT NULL,
    performed_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT ck_budget_transactions_type CHECK (transaction_type IN ('COMMIT_TENTATIVE','COMMIT_FIRM','RELEASE','SPEND')),
    CONSTRAINT ck_budget_transactions_amount CHECK (amount >= 0),
    CONSTRAINT ck_budget_transactions_currency CHECK (char_length(currency) = 3)
);

CREATE INDEX ix_budget_transactions_budget
    ON finance.budget_transactions (budget_id);
CREATE INDEX ix_budget_transactions_ref
    ON finance.budget_transactions (reference_type, reference_id);
CREATE UNIQUE INDEX ux_budget_transactions_idempotent_effect
    ON finance.budget_transactions (budget_id, transaction_type, reference_type, reference_id);

COMMENT ON TABLE finance.budget_transactions IS 'Immutable budget ledger transactions. Rows are append-only and are never soft deleted.';
COMMENT ON COLUMN finance.budget_transactions.transaction_type IS 'COMMIT_TENTATIVE, COMMIT_FIRM, RELEASE or SPEND.';

INSERT INTO finance.budgets (
    id, department_id, fiscal_year, quarter, gl_account_code, allocated_amount,
    currency, status, approved_by, approved_at, created_by
) VALUES
    (
        '70000000-0000-0000-0000-000000000101',
        '11111111-1111-1111-1111-111111111111',
        2026,
        NULL,
        '6002',
        500000000.0000,
        'VND',
        'ACTIVE',
        '30000000-0000-0000-0000-000000000004',
        NOW(),
        '00000000-0000-0000-0000-000000000000'
    ),
    (
        '70000000-0000-0000-0000-000000000102',
        '33333333-3333-3333-8333-333333333333',
        2026,
        NULL,
        '6002',
        500000000.0000,
        'VND',
        'ACTIVE',
        '30000000-0000-0000-0000-000000000004',
        NOW(),
        '00000000-0000-0000-0000-000000000000'
    ),
    (
        '70000000-0000-0000-0000-000000000103',
        '33333333-3333-3333-3333-333333333333',
        2026,
        NULL,
        '6002',
        800000000.0000,
        'VND',
        'ACTIVE',
        '30000000-0000-0000-0000-000000000004',
        NOW(),
        '00000000-0000-0000-0000-000000000000'
    )
ON CONFLICT DO NOTHING;
