CREATE SCHEMA IF NOT EXISTS finance;

ALTER TABLE finance.budget_transactions
    DROP CONSTRAINT IF EXISTS ck_budget_transactions_type;

ALTER TABLE finance.budget_transactions
    ADD CONSTRAINT ck_budget_transactions_type
    CHECK (transaction_type IN (
        'COMMIT_TENTATIVE',
        'COMMIT_FIRM',
        'RELEASE',
        'SPEND',
        'TRANSFER_OUT',
        'TRANSFER_IN'
    ));

CREATE TABLE finance.budget_overrides (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    budget_id UUID NOT NULL REFERENCES finance.budgets(id),
    purchase_request_id UUID NOT NULL,
    override_amount NUMERIC(19,4) NOT NULL,
    currency VARCHAR(3) NOT NULL DEFAULT 'VND',
    override_reason TEXT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'APPROVED',
    approved_by UUID NOT NULL,
    approved_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    idempotency_key UUID NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by UUID NOT NULL,
    updated_by UUID NULL,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at TIMESTAMPTZ NULL,
    deleted_by UUID NULL,
    CONSTRAINT ck_budget_overrides_amount CHECK (override_amount > 0),
    CONSTRAINT ck_budget_overrides_currency CHECK (char_length(currency) = 3),
    CONSTRAINT ck_budget_overrides_reason CHECK (char_length(trim(override_reason)) >= 50),
    CONSTRAINT ck_budget_overrides_status CHECK (status IN ('APPROVED'))
);

CREATE UNIQUE INDEX ux_budget_overrides_idempotency_active
    ON finance.budget_overrides (idempotency_key)
    WHERE is_deleted = FALSE;
CREATE INDEX ix_budget_overrides_budget_active
    ON finance.budget_overrides (budget_id)
    WHERE is_deleted = FALSE;
CREATE INDEX ix_budget_overrides_pr_active
    ON finance.budget_overrides (purchase_request_id)
    WHERE is_deleted = FALSE;
CREATE INDEX ix_budget_overrides_approved_at_active
    ON finance.budget_overrides (approved_at DESC)
    WHERE is_deleted = FALSE;

CREATE TRIGGER trg_budget_overrides_touch_updated_at
BEFORE UPDATE ON finance.budget_overrides
FOR EACH ROW
EXECUTE FUNCTION finance.touch_updated_at();

COMMENT ON TABLE finance.budget_overrides IS 'Auditable approvals for purchase requests that are allowed to exceed an active budget line.';
COMMENT ON COLUMN finance.budget_overrides.override_amount IS 'Approved over-budget amount stored as NUMERIC(19,4).';
COMMENT ON COLUMN finance.budget_overrides.idempotency_key IS 'API Idempotency-Key used to prevent duplicate override approvals.';

CREATE TABLE finance.budget_transfers (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    source_budget_id UUID NOT NULL REFERENCES finance.budgets(id),
    target_budget_id UUID NOT NULL REFERENCES finance.budgets(id),
    amount NUMERIC(19,4) NOT NULL,
    currency VARCHAR(3) NOT NULL DEFAULT 'VND',
    reason TEXT NOT NULL,
    approved_by UUID NOT NULL,
    approved_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    idempotency_key UUID NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by UUID NOT NULL,
    updated_by UUID NULL,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at TIMESTAMPTZ NULL,
    deleted_by UUID NULL,
    CONSTRAINT ck_budget_transfers_amount CHECK (amount > 0),
    CONSTRAINT ck_budget_transfers_currency CHECK (char_length(currency) = 3),
    CONSTRAINT ck_budget_transfers_reason CHECK (char_length(trim(reason)) >= 20),
    CONSTRAINT ck_budget_transfers_distinct_budgets CHECK (source_budget_id <> target_budget_id)
);

CREATE UNIQUE INDEX ux_budget_transfers_idempotency_active
    ON finance.budget_transfers (idempotency_key)
    WHERE is_deleted = FALSE;
CREATE INDEX ix_budget_transfers_source_active
    ON finance.budget_transfers (source_budget_id)
    WHERE is_deleted = FALSE;
CREATE INDEX ix_budget_transfers_target_active
    ON finance.budget_transfers (target_budget_id)
    WHERE is_deleted = FALSE;
CREATE INDEX ix_budget_transfers_approved_at_active
    ON finance.budget_transfers (approved_at DESC)
    WHERE is_deleted = FALSE;

CREATE TRIGGER trg_budget_transfers_touch_updated_at
BEFORE UPDATE ON finance.budget_transfers
FOR EACH ROW
EXECUTE FUNCTION finance.touch_updated_at();

COMMENT ON TABLE finance.budget_transfers IS 'Auditable budget reallocations between active budget lines.';
COMMENT ON COLUMN finance.budget_transfers.amount IS 'Transferred amount stored as NUMERIC(19,4).';
COMMENT ON COLUMN finance.budget_transfers.idempotency_key IS 'API Idempotency-Key used to prevent duplicate budget transfers.';
COMMENT ON COLUMN finance.budget_transactions.transaction_type IS 'COMMIT_TENTATIVE, COMMIT_FIRM, RELEASE, SPEND, TRANSFER_OUT or TRANSFER_IN.';
