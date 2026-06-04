CREATE SCHEMA IF NOT EXISTS analytics;

CREATE OR REPLACE FUNCTION analytics.touch_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TABLE IF NOT EXISTS analytics.executive_dashboard_snapshots (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    fiscal_year INTEGER NOT NULL,
    quarter INTEGER NULL,
    currency VARCHAR(3) NOT NULL DEFAULT 'VND',
    total_spent NUMERIC(19,4) NOT NULL DEFAULT 0,
    approved_pr_count INTEGER NOT NULL DEFAULT 0,
    rfq_savings NUMERIC(19,4) NOT NULL DEFAULT 0,
    approval_on_time_percent NUMERIC(7,2) NOT NULL DEFAULT 0,
    approval_avg_cycle_hours NUMERIC(10,2) NOT NULL DEFAULT 0,
    approval_overdue_count INTEGER NOT NULL DEFAULT 0,
    cached_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by UUID NULL,
    updated_by UUID NULL,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at TIMESTAMPTZ NULL,
    deleted_by UUID NULL,
    CONSTRAINT ck_exec_dash_fiscal_year CHECK (fiscal_year >= 2000),
    CONSTRAINT ck_exec_dash_quarter CHECK (quarter IS NULL OR quarter BETWEEN 1 AND 4),
    CONSTRAINT ck_exec_dash_currency CHECK (char_length(currency) = 3),
    CONSTRAINT ck_exec_dash_amounts CHECK (total_spent >= 0 AND rfq_savings >= 0),
    CONSTRAINT ck_exec_dash_counts CHECK (approved_pr_count >= 0 AND approval_overdue_count >= 0)
);

CREATE INDEX IF NOT EXISTS ix_exec_dash_period_cached_active
    ON analytics.executive_dashboard_snapshots (fiscal_year, quarter, cached_at DESC)
    WHERE is_deleted = FALSE;

CREATE TABLE IF NOT EXISTS analytics.department_spend_snapshots (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    dashboard_id UUID NOT NULL REFERENCES analytics.executive_dashboard_snapshots(id),
    department_code VARCHAR(80) NOT NULL,
    department_name VARCHAR(255) NOT NULL,
    spent NUMERIC(19,4) NOT NULL DEFAULT 0,
    budget NUMERIC(19,4) NOT NULL DEFAULT 0,
    utilization NUMERIC(7,2) NOT NULL DEFAULT 0,
    status VARCHAR(30) NOT NULL DEFAULT 'GOOD',
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by UUID NULL,
    updated_by UUID NULL,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at TIMESTAMPTZ NULL,
    deleted_by UUID NULL,
    CONSTRAINT ck_department_spend_amounts CHECK (spent >= 0 AND budget >= 0),
    CONSTRAINT ck_department_spend_status CHECK (status IN ('GOOD','WARNING','CRITICAL'))
);

CREATE INDEX IF NOT EXISTS ix_department_spend_dashboard_active
    ON analytics.department_spend_snapshots (dashboard_id, spent DESC)
    WHERE is_deleted = FALSE;

CREATE TABLE IF NOT EXISTS analytics.category_spend_snapshots (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    dashboard_id UUID NOT NULL REFERENCES analytics.executive_dashboard_snapshots(id),
    category_code VARCHAR(80) NOT NULL,
    spent NUMERIC(19,4) NOT NULL DEFAULT 0,
    budget NUMERIC(19,4) NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by UUID NULL,
    updated_by UUID NULL,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at TIMESTAMPTZ NULL,
    deleted_by UUID NULL,
    CONSTRAINT ck_category_spend_amounts CHECK (spent >= 0 AND (budget IS NULL OR budget >= 0))
);

CREATE INDEX IF NOT EXISTS ix_category_spend_dashboard_active
    ON analytics.category_spend_snapshots (dashboard_id, spent DESC)
    WHERE is_deleted = FALSE;

CREATE TABLE IF NOT EXISTS analytics.monthly_spend_snapshots (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    dashboard_id UUID NOT NULL REFERENCES analytics.executive_dashboard_snapshots(id),
    month_label VARCHAR(7) NOT NULL,
    spent NUMERIC(19,4) NOT NULL DEFAULT 0,
    budget NUMERIC(19,4) NOT NULL DEFAULT 0,
    pr_count INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by UUID NULL,
    updated_by UUID NULL,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at TIMESTAMPTZ NULL,
    deleted_by UUID NULL,
    CONSTRAINT ck_monthly_spend_month CHECK (month_label ~ '^[0-9]{4}-[0-9]{2}$'),
    CONSTRAINT ck_monthly_spend_amounts CHECK (spent >= 0 AND budget >= 0),
    CONSTRAINT ck_monthly_spend_pr_count CHECK (pr_count >= 0)
);

CREATE INDEX IF NOT EXISTS ix_monthly_spend_dashboard_active
    ON analytics.monthly_spend_snapshots (dashboard_id, month_label)
    WHERE is_deleted = FALSE;

CREATE TABLE IF NOT EXISTS analytics.top_vendor_snapshots (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    dashboard_id UUID NOT NULL REFERENCES analytics.executive_dashboard_snapshots(id),
    vendor_name VARCHAR(255) NOT NULL,
    total_spent NUMERIC(19,4) NOT NULL DEFAULT 0,
    order_count INTEGER NOT NULL DEFAULT 0,
    avg_score NUMERIC(7,2) NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by UUID NULL,
    updated_by UUID NULL,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at TIMESTAMPTZ NULL,
    deleted_by UUID NULL,
    CONSTRAINT ck_top_vendor_amounts CHECK (total_spent >= 0),
    CONSTRAINT ck_top_vendor_order_count CHECK (order_count >= 0)
);

CREATE INDEX IF NOT EXISTS ix_top_vendor_dashboard_active
    ON analytics.top_vendor_snapshots (dashboard_id, total_spent DESC)
    WHERE is_deleted = FALSE;

DROP TRIGGER IF EXISTS trg_exec_dash_touch_updated_at ON analytics.executive_dashboard_snapshots;
CREATE TRIGGER trg_exec_dash_touch_updated_at
BEFORE UPDATE ON analytics.executive_dashboard_snapshots
FOR EACH ROW
EXECUTE FUNCTION analytics.touch_updated_at();

DROP TRIGGER IF EXISTS trg_department_spend_touch_updated_at ON analytics.department_spend_snapshots;
CREATE TRIGGER trg_department_spend_touch_updated_at
BEFORE UPDATE ON analytics.department_spend_snapshots
FOR EACH ROW
EXECUTE FUNCTION analytics.touch_updated_at();

DROP TRIGGER IF EXISTS trg_category_spend_touch_updated_at ON analytics.category_spend_snapshots;
CREATE TRIGGER trg_category_spend_touch_updated_at
BEFORE UPDATE ON analytics.category_spend_snapshots
FOR EACH ROW
EXECUTE FUNCTION analytics.touch_updated_at();

DROP TRIGGER IF EXISTS trg_monthly_spend_touch_updated_at ON analytics.monthly_spend_snapshots;
CREATE TRIGGER trg_monthly_spend_touch_updated_at
BEFORE UPDATE ON analytics.monthly_spend_snapshots
FOR EACH ROW
EXECUTE FUNCTION analytics.touch_updated_at();

DROP TRIGGER IF EXISTS trg_top_vendor_touch_updated_at ON analytics.top_vendor_snapshots;
CREATE TRIGGER trg_top_vendor_touch_updated_at
BEFORE UPDATE ON analytics.top_vendor_snapshots
FOR EACH ROW
EXECUTE FUNCTION analytics.touch_updated_at();

COMMENT ON TABLE analytics.executive_dashboard_snapshots IS 'Executive dashboard aggregate snapshots for analytics-service read APIs.';
COMMENT ON TABLE analytics.department_spend_snapshots IS 'Department spend rows attached to an executive dashboard snapshot.';
COMMENT ON TABLE analytics.category_spend_snapshots IS 'Category spend rows attached to an executive dashboard snapshot.';
COMMENT ON TABLE analytics.monthly_spend_snapshots IS 'Monthly spend trend rows attached to an executive dashboard snapshot.';
COMMENT ON TABLE analytics.top_vendor_snapshots IS 'Top vendor spend rows attached to an executive dashboard snapshot.';
