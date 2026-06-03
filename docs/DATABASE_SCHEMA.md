# DATABASE SCHEMA DOCUMENT
## eProcure Enterprise — PostgreSQL Schema

---

> **Version:** 1.0.0  
> **Engine:** PostgreSQL 15+  
> **Rule:** Không dùng schema `public`. Mỗi service có schema riêng.  
> **Soft Delete:** Mọi entity table đều có `is_deleted`, `deleted_at`, `deleted_by`.  
> **Timestamps:** Tất cả dùng `TIMESTAMPTZ` (UTC). Tài chính dùng `NUMERIC(19,4)`.  
> **Migration:** Flyway, naming `V{N}__{description}.sql`.

---

## 1. DATABASE LAYOUT

```
PostgreSQL Cluster
├── db_iam
│   └── schema: iam
│       ├── users
│       ├── roles
│       ├── permissions
│       ├── user_roles
│       ├── role_permissions
│       ├── departments
│       ├── org_nodes
│       ├── delegations
│       └── sessions
│
├── db_procurement
│   ├── schema: pr
│   │   ├── purchase_requests
│   │   ├── pr_line_items
│   │   ├── pr_attachments
│   │   ├── catalog_items
│   │   └── catalog_categories
│   └── schema: approval
│       ├── approval_processes
│       ├── approval_steps
│       ├── approval_rules
│       └── approval_rule_steps
│
├── db_finance
│   └── schema: finance
│       ├── budgets
│       ├── budget_transactions
│       ├── event_processing_log
│       ├── budget_transfers
│       ├── purchase_orders
│       ├── po_line_items
│       ├── invoices
│       ├── invoice_line_items
│       └── payments
│
├── db_inventory
│   └── schema: inventory
│       ├── warehouses
│       ├── items
│       ├── stock_entries
│       ├── purchase_order_snapshots
│       ├── purchase_order_line_snapshots
│       ├── goods_receipts
│       ├── goods_receipt_line_items
│       ├── stock_movements
│       └── event_processing_log
│
├── db_vendor
│   └── schema: vendor
│       ├── vendors
│       ├── vendor_contacts
│       ├── rfqs
│       ├── rfq_invitations
│       ├── vendor_quotes
│       ├── vendor_quote_line_items
│       └── vendor_scores
│
├── db_notification
│   └── schema: notification
│       ├── notifications
│       ├── notification_templates
│       ├── event_processing_log
│       └── email_dispatch_dead_letters
│
├── db_camunda
│   └── schema: camunda  (managed by Camunda auto-schema)
│
└── db_audit
    └── schema: audit   (append-only, immutable)
        └── audit_logs
```

---

## 2. db_iam — Schema IAM

### 2.1 users

```sql
CREATE TABLE iam.users (
    id                  UUID            PRIMARY KEY DEFAULT gen_random_uuid(),
    employee_code       VARCHAR(20)     NOT NULL UNIQUE,
    username            VARCHAR(50)     NOT NULL UNIQUE,
    password_hash       VARCHAR(255)    NOT NULL,           -- BCrypt(password + userId salt)
    email               VARCHAR(255)    NOT NULL UNIQUE,
    phone               VARCHAR(20),
    full_name           VARCHAR(200)    NOT NULL,
    avatar_url          TEXT,
    department_id       UUID            NOT NULL,
    org_node_id         UUID,
    status              VARCHAR(20)     NOT NULL DEFAULT 'PENDING_VERIFY',
    two_factor_enabled  BOOLEAN         NOT NULL DEFAULT FALSE,
    two_factor_secret   VARCHAR(255),                       -- AES encrypted at rest
    google_oauth_id     VARCHAR(255)    UNIQUE,
    last_login_at       TIMESTAMPTZ,
    last_login_ip       INET,
    failed_login_count  SMALLINT        NOT NULL DEFAULT 0,
    password_changed_at TIMESTAMPTZ,
    created_at          TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    created_by          UUID,
    is_deleted          BOOLEAN         NOT NULL DEFAULT FALSE,
    deleted_at          TIMESTAMPTZ,
    deleted_by          UUID,

    CONSTRAINT chk_users_status CHECK (status IN ('ACTIVE','INACTIVE','LOCKED','PENDING_VERIFY'))
);

-- Indexes
CREATE INDEX idx_users_email ON iam.users(email) WHERE is_deleted = FALSE;
CREATE INDEX idx_users_username ON iam.users(username) WHERE is_deleted = FALSE;
CREATE INDEX idx_users_department ON iam.users(department_id) WHERE is_deleted = FALSE;
CREATE INDEX idx_users_status ON iam.users(status) WHERE is_deleted = FALSE;
CREATE INDEX idx_users_google_oauth ON iam.users(google_oauth_id) WHERE google_oauth_id IS NOT NULL;
```

### 2.2 roles

```sql
CREATE TABLE iam.roles (
    code            VARCHAR(50)     PRIMARY KEY,            -- VD: 'MANAGER', 'PURCHASING'
    name            VARCHAR(100)    NOT NULL,
    description     TEXT,
    is_system_role  BOOLEAN         NOT NULL DEFAULT FALSE,
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    is_deleted      BOOLEAN         NOT NULL DEFAULT FALSE,
    deleted_at      TIMESTAMPTZ,
    deleted_by      UUID
);
```

### 2.3 permissions

```sql
CREATE TABLE iam.permissions (
    code        VARCHAR(100)    PRIMARY KEY,                -- VD: 'PR_APPROVE_L1'
    name        VARCHAR(200)    NOT NULL,
    description TEXT,
    service     VARCHAR(50)     NOT NULL,                   -- 'PR_SERVICE', 'FINANCE', etc.
    created_at  TIMESTAMPTZ     NOT NULL DEFAULT NOW()
);
```

### 2.4 user_roles / role_permissions

```sql
CREATE TABLE iam.user_roles (
    user_id     UUID            NOT NULL REFERENCES iam.users(id),
    role_code   VARCHAR(50)     NOT NULL REFERENCES iam.roles(code),
    granted_by  UUID,
    granted_at  TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    PRIMARY KEY (user_id, role_code)
);
CREATE INDEX idx_user_roles_user ON iam.user_roles(user_id);

CREATE TABLE iam.role_permissions (
    role_code       VARCHAR(50)     NOT NULL REFERENCES iam.roles(code),
    permission_code VARCHAR(100)    NOT NULL REFERENCES iam.permissions(code),
    PRIMARY KEY (role_code, permission_code)
);
CREATE INDEX idx_role_perm_role ON iam.role_permissions(role_code);
```

### 2.5 sessions

```sql
CREATE TABLE iam.sessions (
    token           VARCHAR(100)    PRIMARY KEY,            -- Opaque token
    user_id         UUID            NOT NULL REFERENCES iam.users(id),
    ip_address      INET,
    user_agent      TEXT,
    is_active       BOOLEAN         NOT NULL DEFAULT TRUE,
    last_activity   TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    invalidated_at  TIMESTAMPTZ,
    invalidated_by  VARCHAR(50)                             -- 'LOGOUT'|'NEW_LOGIN'|'ADMIN'
);
CREATE INDEX idx_sessions_user ON iam.sessions(user_id, is_active);
CREATE INDEX idx_sessions_active ON iam.sessions(is_active) WHERE is_active = TRUE;
```

### 2.6 departments

```sql
CREATE TABLE iam.departments (
    id              UUID            PRIMARY KEY DEFAULT gen_random_uuid(),
    code            VARCHAR(20)     NOT NULL UNIQUE,
    name            VARCHAR(200)    NOT NULL,
    parent_id       UUID            REFERENCES iam.departments(id),
    head_user_id    UUID,
    gl_account_prefix VARCHAR(10),
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    is_deleted      BOOLEAN         NOT NULL DEFAULT FALSE,
    deleted_at      TIMESTAMPTZ,
    deleted_by      UUID
);
CREATE INDEX idx_depts_parent ON iam.departments(parent_id);
```

### 2.7 delegations

```sql
CREATE TABLE iam.delegations (
    id                  UUID            PRIMARY KEY DEFAULT gen_random_uuid(),
    delegator_id        UUID            NOT NULL REFERENCES iam.users(id),
    delegate_id         UUID            NOT NULL REFERENCES iam.users(id),
    start_at            TIMESTAMPTZ     NOT NULL,
    end_at              TIMESTAMPTZ     NOT NULL,
    max_value           NUMERIC(19,4),
    currency            VARCHAR(3)      NOT NULL DEFAULT 'VND',
    allowed_categories  VARCHAR(50)[],                      -- NULL = tất cả
    scope               VARCHAR(20)     NOT NULL DEFAULT 'ALL',
    status              VARCHAR(20)     NOT NULL DEFAULT 'ACTIVE',
    revoked_at          TIMESTAMPTZ,
    revoked_by          UUID,
    created_at          TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    is_deleted          BOOLEAN         NOT NULL DEFAULT FALSE,
    deleted_at          TIMESTAMPTZ,
    deleted_by          UUID,

    CONSTRAINT chk_delegation_scope CHECK (scope IN ('ALL','OWN_TEAM')),
    CONSTRAINT chk_delegation_status CHECK (status IN ('ACTIVE','EXPIRED','REVOKED')),
    CONSTRAINT chk_delegation_dates CHECK (end_at > start_at),
    CONSTRAINT chk_delegation_no_self CHECK (delegator_id <> delegate_id)
);
CREATE INDEX idx_delegations_delegator ON iam.delegations(delegator_id, status);
CREATE INDEX idx_delegations_delegate ON iam.delegations(delegate_id, status);
CREATE INDEX idx_delegations_active_time ON iam.delegations(start_at, end_at) WHERE status = 'ACTIVE';
```

---

## 3. db_procurement — Schema PR

### 3.1 catalog_categories

```sql
CREATE TABLE pr.catalog_categories (
    code            VARCHAR(50)     PRIMARY KEY,            -- 'IT_HARDWARE', 'OFFICE_SUPPLIES'
    name            VARCHAR(200)    NOT NULL,
    parent_code     VARCHAR(50)     REFERENCES pr.catalog_categories(code),
    requires_special_approval BOOLEAN NOT NULL DEFAULT FALSE,
    special_approver_role VARCHAR(50),                     -- VD: 'CISO', 'IT_MANAGER'
    requires_rfq_above NUMERIC(19,4),                      -- Ngưỡng bắt buộc RFQ
    currency        VARCHAR(3)      NOT NULL DEFAULT 'VND',
    is_capex        BOOLEAN         NOT NULL DEFAULT FALSE,
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    is_deleted      BOOLEAN         NOT NULL DEFAULT FALSE
);
```

### 3.2 catalog_items

```sql
CREATE TABLE pr.catalog_items (
    id              UUID            PRIMARY KEY DEFAULT gen_random_uuid(),
    item_code       VARCHAR(20)     NOT NULL UNIQUE,
    name            VARCHAR(300)    NOT NULL,
    description     TEXT,
    category_code   VARCHAR(50)     NOT NULL REFERENCES pr.catalog_categories(code),
    unit            VARCHAR(20)     NOT NULL,
    unit_price      NUMERIC(19,4)   NOT NULL,
    currency        VARCHAR(3)      NOT NULL DEFAULT 'VND',
    preferred_vendor_id UUID,
    reorder_point   NUMERIC(10,2),
    is_active       BOOLEAN         NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    is_deleted      BOOLEAN         NOT NULL DEFAULT FALSE,
    deleted_at      TIMESTAMPTZ,
    deleted_by      UUID
);
CREATE INDEX idx_catalog_items_category ON pr.catalog_items(category_code) WHERE is_deleted = FALSE;
CREATE INDEX idx_catalog_items_code ON pr.catalog_items(item_code) WHERE is_deleted = FALSE;
-- Full-text search
CREATE INDEX idx_catalog_items_search ON pr.catalog_items USING GIN(to_tsvector('simple', name || ' ' || COALESCE(description,'')));
```

### 3.3 purchase_requests

```sql
CREATE TABLE pr.purchase_requests (
    id                  UUID            PRIMARY KEY DEFAULT gen_random_uuid(),
    pr_number           VARCHAR(20)     NOT NULL UNIQUE,    -- PR-YYYY-MM-XXXXX
    requester_id        UUID            NOT NULL,
    department_id       UUID            NOT NULL,
    title               VARCHAR(500)    NOT NULL,
    justification       TEXT            NOT NULL,
    priority            VARCHAR(20)     NOT NULL DEFAULT 'NORMAL',
    urgency_reason      TEXT,
    status              VARCHAR(30)     NOT NULL DEFAULT 'DRAFT',
    total_amount        NUMERIC(19,4)   NOT NULL DEFAULT 0,
    currency            VARCHAR(3)      NOT NULL DEFAULT 'VND',
    fiscal_year         SMALLINT        NOT NULL,
    need_by_date        DATE,
    related_contract_id UUID,
    is_blanket_release  BOOLEAN         NOT NULL DEFAULT FALSE,

    -- Budget check snapshot
    budget_allocated    NUMERIC(19,4),
    budget_committed    NUMERIC(19,4),
    budget_spent        NUMERIC(19,4),
    budget_available    NUMERIC(19,4),
    budget_check_status VARCHAR(20),                        -- 'PASS','WARNING','FAIL'
    budget_warning_message TEXT,
    inventory_check     JSONB,                              -- Inventory check snapshot at submit

    -- Emergency tracking
    emergency_abuse_count SMALLINT      NOT NULL DEFAULT 0,
    emergency_report_submitted_at TIMESTAMPTZ,

    created_at          TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    submitted_at        TIMESTAMPTZ,
    created_by          UUID            NOT NULL,
    updated_by          UUID,
    is_deleted          BOOLEAN         NOT NULL DEFAULT FALSE,
    deleted_at          TIMESTAMPTZ,
    deleted_by          UUID,

    CONSTRAINT chk_pr_priority CHECK (priority IN ('NORMAL','URGENT','EMERGENCY')),
    CONSTRAINT chk_pr_status CHECK (status IN (
        'DRAFT','SUBMITTED','PENDING_APPROVAL','CHANGES_REQUESTED',
        'APPROVED','REJECTED','CONVERTED_TO_PO','CANCELLED','CLOSED'
    ))
);

CREATE INDEX idx_pr_requester ON pr.purchase_requests(requester_id) WHERE is_deleted = FALSE;
CREATE INDEX idx_pr_department ON pr.purchase_requests(department_id) WHERE is_deleted = FALSE;
CREATE INDEX idx_pr_status ON pr.purchase_requests(status) WHERE is_deleted = FALSE;
CREATE INDEX idx_pr_priority ON pr.purchase_requests(priority) WHERE is_deleted = FALSE;
CREATE INDEX idx_pr_dept_status ON pr.purchase_requests(department_id, status) WHERE is_deleted = FALSE;
CREATE INDEX idx_pr_fiscal_year ON pr.purchase_requests(fiscal_year);
CREATE INDEX idx_pr_created_at ON pr.purchase_requests(created_at DESC);
CREATE INDEX idx_pr_number ON pr.purchase_requests(pr_number);
```

### 3.4 pr_line_items

```sql
CREATE TABLE pr.pr_line_items (
    id                  UUID            PRIMARY KEY DEFAULT gen_random_uuid(),
    pr_id               UUID            NOT NULL REFERENCES pr.purchase_requests(id),
    line_number         SMALLINT        NOT NULL,
    item_code           VARCHAR(20)     REFERENCES pr.catalog_items(item_code),
    item_name           VARCHAR(300)    NOT NULL,
    description         TEXT,
    category_code       VARCHAR(50)     NOT NULL REFERENCES pr.catalog_categories(code),
    quantity            NUMERIC(10,2)   NOT NULL,
    unit                VARCHAR(20)     NOT NULL,
    unit_price          NUMERIC(19,4)   NOT NULL,
    total_price         NUMERIC(19,4)   NOT NULL GENERATED ALWAYS AS (quantity * unit_price) STORED,
    currency            VARCHAR(3)      NOT NULL DEFAULT 'VND',
    preferred_vendor_id UUID,
    specifications      TEXT,
    gl_account_code     VARCHAR(10)     NOT NULL,
    is_from_catalog     BOOLEAN         NOT NULL DEFAULT FALSE,
    is_deleted          BOOLEAN         NOT NULL DEFAULT FALSE,

    CONSTRAINT chk_pr_line_qty CHECK (quantity > 0),
    CONSTRAINT chk_pr_line_price CHECK (unit_price >= 0),
    UNIQUE (pr_id, line_number)
);

CREATE INDEX idx_pr_line_items_pr ON pr.pr_line_items(pr_id) WHERE is_deleted = FALSE;
CREATE INDEX idx_pr_line_items_category ON pr.pr_line_items(category_code);
```

### 3.5 pr_attachments

```sql
CREATE TABLE pr.pr_attachments (
    id              UUID            PRIMARY KEY DEFAULT gen_random_uuid(),
    pr_id           UUID            REFERENCES pr.purchase_requests(id),  -- nullable khi upload trước
    file_name       VARCHAR(255)    NOT NULL,
    file_path       TEXT            NOT NULL,
    file_size       BIGINT          NOT NULL,
    mime_type       VARCHAR(100)    NOT NULL,
    uploaded_by     UUID            NOT NULL,
    uploaded_at     TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    is_deleted      BOOLEAN         NOT NULL DEFAULT FALSE,
    deleted_at      TIMESTAMPTZ
);
CREATE INDEX idx_attachments_pr ON pr.pr_attachments(pr_id) WHERE is_deleted = FALSE;
```

---

## 4. db_procurement — Schema APPROVAL

### 4.1 approval_rules

```sql
CREATE TABLE approval.approval_rules (
    id              UUID            PRIMARY KEY DEFAULT gen_random_uuid(),
    rule_name       VARCHAR(100)    NOT NULL UNIQUE,
    priority        SMALLINT        NOT NULL DEFAULT 100,
    is_active       BOOLEAN         NOT NULL DEFAULT TRUE,
    rule_type       VARCHAR(30)     NOT NULL,               -- VALUE|CATEGORY|DEPARTMENT|DEFAULT
    
    -- Conditions
    min_value       NUMERIC(19,4),
    max_value       NUMERIC(19,4),
    categories      VARCHAR(50)[],
    department_ids  UUID[],
    priorities      VARCHAR(20)[],  -- ['NORMAL','URGENT','EMERGENCY']
    
    description     TEXT,
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    created_by      UUID            NOT NULL,
    is_deleted      BOOLEAN         NOT NULL DEFAULT FALSE,
    deleted_at      TIMESTAMPTZ,
    deleted_by      UUID,

    CONSTRAINT chk_rule_type CHECK (rule_type IN ('VALUE','CATEGORY','DEPARTMENT','DEFAULT'))
);
CREATE INDEX idx_rules_active ON approval.approval_rules(priority DESC) WHERE is_active = TRUE AND is_deleted = FALSE;
```

### 4.2 approval_rule_steps

```sql
CREATE TABLE approval.approval_rule_steps (
    id              UUID            PRIMARY KEY DEFAULT gen_random_uuid(),
    rule_id         UUID            NOT NULL REFERENCES approval.approval_rules(id),
    step_index      SMALLINT        NOT NULL,
    approver_role   VARCHAR(50)     NOT NULL,               -- 'MANAGER','DIRECTOR','C_LEVEL','FINANCE'
    step_type       VARCHAR(20)     NOT NULL DEFAULT 'SEQUENTIAL',
    sla_hours       SMALLINT        NOT NULL,               -- Giờ làm việc
    is_required     BOOLEAN         NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    created_by      UUID            NOT NULL,
    is_deleted      BOOLEAN         NOT NULL DEFAULT FALSE,
    deleted_at      TIMESTAMPTZ,
    deleted_by      UUID
);
```

### 4.3 approval_processes

```sql
CREATE TABLE approval.approval_processes (
    id                          UUID            PRIMARY KEY DEFAULT gen_random_uuid(),
    entity_type                 VARCHAR(50)     NOT NULL,   -- 'PURCHASE_REQUEST','INVOICE'
    entity_id                   UUID            NOT NULL,
    entity_number               VARCHAR(50)     NOT NULL,
    entity_title                VARCHAR(500)    NOT NULL,
    requester_id                UUID            NOT NULL,
    requester_department_id     UUID            NOT NULL,
    total_amount                NUMERIC(19,4)   NOT NULL,
    currency                    VARCHAR(3)      NOT NULL DEFAULT 'VND',
    priority                    VARCHAR(20)     NOT NULL,
    camunda_process_instance_id VARCHAR(100)    UNIQUE,
    status                      VARCHAR(30)     NOT NULL DEFAULT 'RUNNING',
    current_step_index          SMALLINT        NOT NULL DEFAULT 1,
    entity_snapshot             JSONB,
    started_at                  TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    completed_at                TIMESTAMPTZ,
    created_at                  TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at                  TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    created_by                  UUID            NOT NULL,
    is_deleted                  BOOLEAN         NOT NULL DEFAULT FALSE,
    deleted_at                  TIMESTAMPTZ,
    deleted_by                  UUID,
    
    CONSTRAINT chk_ap_status CHECK (status IN ('RUNNING','COMPLETED','CANCELLED')),
    CONSTRAINT chk_ap_priority CHECK (priority IN ('NORMAL','URGENT','EMERGENCY'))
);
CREATE INDEX idx_ap_entity ON approval.approval_processes(entity_type, entity_id) WHERE is_deleted = FALSE;
CREATE INDEX idx_ap_status ON approval.approval_processes(status) WHERE status = 'RUNNING' AND is_deleted = FALSE;
```

### 4.4 approval_steps

```sql
CREATE TABLE approval.approval_steps (
    id                  UUID            PRIMARY KEY DEFAULT gen_random_uuid(),
    process_id          UUID            NOT NULL REFERENCES approval.approval_processes(id),
    step_index          SMALLINT        NOT NULL,
    step_type           VARCHAR(20)     NOT NULL DEFAULT 'SEQUENTIAL',
    approver_role       VARCHAR(50)     NOT NULL,
    approver_id         UUID            NOT NULL,
    delegate_id         UUID,                               -- Nếu đang uỷ quyền
    status              VARCHAR(20)     NOT NULL DEFAULT 'PENDING',
    action              VARCHAR(30),
    comment             TEXT,
    sla_deadline        TIMESTAMPTZ     NOT NULL,
    assigned_at         TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    acted_at            TIMESTAMPTZ,
    is_escalated        BOOLEAN         NOT NULL DEFAULT FALSE,
    escalated_from      UUID,
    reminder_1_sent     BOOLEAN         NOT NULL DEFAULT FALSE,
    reminder_2_sent     BOOLEAN         NOT NULL DEFAULT FALSE,
    camunda_task_id     VARCHAR(100),
    created_at          TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    created_by          UUID            NOT NULL,
    is_deleted          BOOLEAN         NOT NULL DEFAULT FALSE,
    deleted_at          TIMESTAMPTZ,
    deleted_by          UUID,
    
    CONSTRAINT chk_step_status CHECK (status IN ('PENDING','APPROVED','REJECTED','ESCALATED','SKIPPED','FORWARDED')),
    CONSTRAINT chk_step_action CHECK (action IN ('APPROVE','REJECT','REQUEST_CHANGES','FORWARD') OR action IS NULL)
);

CREATE INDEX idx_ap_steps_approver ON approval.approval_steps(approver_id, status) WHERE status = 'PENDING' AND is_deleted = FALSE;
CREATE INDEX idx_ap_steps_sla ON approval.approval_steps(sla_deadline) WHERE status = 'PENDING' AND is_deleted = FALSE;
CREATE INDEX idx_ap_steps_process ON approval.approval_steps(process_id) WHERE is_deleted = FALSE;
```

### 4.5 event_processing_log

```sql
CREATE TABLE approval.event_processing_log (
    event_id        VARCHAR(100)    PRIMARY KEY,
    topic           VARCHAR(150)    NOT NULL,
    partition_id    INTEGER,
    offset_value    BIGINT,
    processed_at    TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    handler_name    VARCHAR(150)    NOT NULL,
    status          VARCHAR(20)     NOT NULL DEFAULT 'PROCESSED',
    error_code      VARCHAR(50),
    error_message   TEXT,

    CONSTRAINT chk_event_processing_status CHECK (status IN ('PROCESSED','FAILED','SKIPPED'))
);
CREATE INDEX idx_event_processing_processed ON approval.event_processing_log(processed_at DESC);
```

---

## 5. db_finance — Schema FINANCE

### 5.1 budgets

```sql
CREATE TABLE finance.budgets (
    id                  UUID            PRIMARY KEY DEFAULT gen_random_uuid(),
    department_id       UUID            NOT NULL,
    fiscal_year         SMALLINT        NOT NULL,
    quarter             SMALLINT,                           -- 1-4, NULL nếu annual
    gl_account_code     VARCHAR(10)     NOT NULL,
    allocated_amount    NUMERIC(19,4)   NOT NULL DEFAULT 0,
    currency            VARCHAR(3)      NOT NULL DEFAULT 'VND',
    status              VARCHAR(20)     NOT NULL DEFAULT 'PLANNING',
    approved_by         UUID,
    approved_at         TIMESTAMPTZ,
    created_at          TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    created_by          UUID            NOT NULL,
    is_deleted          BOOLEAN         NOT NULL DEFAULT FALSE,
    deleted_at          TIMESTAMPTZ,
    deleted_by          UUID,

    CONSTRAINT chk_budget_status CHECK (status IN ('PLANNING','SUBMITTED','APPROVED','ACTIVE','CLOSED')),
    CONSTRAINT chk_budget_quarter CHECK (quarter BETWEEN 1 AND 4 OR quarter IS NULL),
    UNIQUE (department_id, fiscal_year, quarter, gl_account_code)
);
CREATE INDEX idx_budgets_dept_year ON finance.budgets(department_id, fiscal_year) WHERE is_deleted = FALSE;
```

### 5.2 budget_transactions

```sql
CREATE TABLE finance.budget_transactions (
    id              UUID            PRIMARY KEY DEFAULT gen_random_uuid(),
    budget_id       UUID            NOT NULL REFERENCES finance.budgets(id),
    transaction_type VARCHAR(30)    NOT NULL,               -- COMMIT_TENTATIVE|COMMIT_FIRM|RELEASE|SPEND|TRANSFER_OUT|TRANSFER_IN
    amount          NUMERIC(19,4)   NOT NULL,
    currency        VARCHAR(3)      NOT NULL DEFAULT 'VND',
    reference_type  VARCHAR(50)     NOT NULL,               -- 'PURCHASE_REQUEST'|'PURCHASE_ORDER'|'INVOICE'
    reference_id    UUID            NOT NULL,
    source_event_id VARCHAR(80),
    description     TEXT,
    performed_by    UUID            NOT NULL,
    performed_at    TIMESTAMPTZ     NOT NULL DEFAULT NOW()
    -- Immutable: không có is_deleted
);
CREATE INDEX idx_budget_tx_budget ON finance.budget_transactions(budget_id);
CREATE INDEX idx_budget_tx_ref ON finance.budget_transactions(reference_type, reference_id);
CREATE INDEX idx_budget_tx_source_event ON finance.budget_transactions(source_event_id) WHERE source_event_id IS NOT NULL;
```

### 5.3 event_processing_log

```sql
CREATE TABLE finance.event_processing_log (
    event_id      VARCHAR(80)     PRIMARY KEY,
    topic         VARCHAR(120)    NOT NULL,
    partition_id  INTEGER,
    offset_value  BIGINT,
    handler_name  VARCHAR(120)    NOT NULL,
    status        VARCHAR(20)     NOT NULL,                  -- PROCESSED|SKIPPED
    processed_at  TIMESTAMPTZ     NOT NULL DEFAULT NOW()
);
```

### 5.4 budget_overrides

```sql
CREATE TABLE finance.budget_overrides (
    id                  UUID            PRIMARY KEY DEFAULT gen_random_uuid(),
    budget_id           UUID            NOT NULL REFERENCES finance.budgets(id),
    purchase_request_id UUID            NOT NULL,
    override_amount     NUMERIC(19,4)   NOT NULL,
    currency            VARCHAR(3)      NOT NULL DEFAULT 'VND',
    override_reason     TEXT            NOT NULL,
    status              VARCHAR(20)     NOT NULL DEFAULT 'APPROVED',
    approved_by         UUID            NOT NULL,
    approved_at         TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    idempotency_key     UUID            NOT NULL,
    created_at          TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    created_by          UUID            NOT NULL,
    updated_by          UUID,
    is_deleted          BOOLEAN         NOT NULL DEFAULT FALSE,
    deleted_at          TIMESTAMPTZ,
    deleted_by          UUID,

    CONSTRAINT chk_budget_overrides_amount CHECK (override_amount > 0),
    CONSTRAINT chk_budget_overrides_reason CHECK (char_length(trim(override_reason)) >= 50),
    CONSTRAINT chk_budget_overrides_status CHECK (status IN ('APPROVED'))
);
CREATE UNIQUE INDEX idx_budget_overrides_idempotency
    ON finance.budget_overrides(idempotency_key) WHERE is_deleted = FALSE;
CREATE INDEX idx_budget_overrides_budget ON finance.budget_overrides(budget_id) WHERE is_deleted = FALSE;
CREATE INDEX idx_budget_overrides_pr ON finance.budget_overrides(purchase_request_id) WHERE is_deleted = FALSE;
```

### 5.5 budget_transfers

```sql
CREATE TABLE finance.budget_transfers (
    id                  UUID            PRIMARY KEY DEFAULT gen_random_uuid(),
    source_budget_id    UUID            NOT NULL REFERENCES finance.budgets(id),
    target_budget_id    UUID            NOT NULL REFERENCES finance.budgets(id),
    amount              NUMERIC(19,4)   NOT NULL,
    currency            VARCHAR(3)      NOT NULL DEFAULT 'VND',
    reason              TEXT            NOT NULL,
    approved_by         UUID            NOT NULL,
    approved_at         TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    idempotency_key     UUID            NOT NULL,
    created_at          TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    created_by          UUID            NOT NULL,
    updated_by          UUID,
    is_deleted          BOOLEAN         NOT NULL DEFAULT FALSE,
    deleted_at          TIMESTAMPTZ,
    deleted_by          UUID,

    CONSTRAINT chk_budget_transfers_amount CHECK (amount > 0),
    CONSTRAINT chk_budget_transfers_reason CHECK (char_length(trim(reason)) >= 20),
    CONSTRAINT chk_budget_transfers_distinct_budgets CHECK (source_budget_id <> target_budget_id)
);
CREATE UNIQUE INDEX idx_budget_transfers_idempotency
    ON finance.budget_transfers(idempotency_key) WHERE is_deleted = FALSE;
CREATE INDEX idx_budget_transfers_source ON finance.budget_transfers(source_budget_id) WHERE is_deleted = FALSE;
CREATE INDEX idx_budget_transfers_target ON finance.budget_transfers(target_budget_id) WHERE is_deleted = FALSE;
```

### 5.6 purchase_orders

```sql
CREATE TABLE finance.purchase_orders (
    id                      UUID            PRIMARY KEY DEFAULT gen_random_uuid(),
    po_number               VARCHAR(30)     NOT NULL UNIQUE,
    pr_id                   UUID            NOT NULL,
    pr_number               VARCHAR(40)     NOT NULL,
    rfq_id                  UUID,
    rfq_number              VARCHAR(40),
    awarded_quote_id        UUID,
    vendor_id               UUID            NOT NULL,
    vendor_name             VARCHAR(255)    NOT NULL,
    vendor_email            VARCHAR(255),
    vendor_tax_code         VARCHAR(50),
    purchasing_officer_id   UUID            NOT NULL,
    purchasing_officer_full_name VARCHAR(255),
    status                  VARCHAR(30)     NOT NULL DEFAULT 'DRAFT',
    total_amount            NUMERIC(19,4)   NOT NULL DEFAULT 0,
    currency                VARCHAR(3)      NOT NULL DEFAULT 'VND',
    delivery_address        TEXT,
    delivery_deadline       DATE,
    payment_terms           VARCHAR(100),
    vendor_note             TEXT,
    is_blanket_release      BOOLEAN         NOT NULL DEFAULT FALSE,
    issued_at               TIMESTAMPTZ,
    sent_to_vendor_at       TIMESTAMPTZ,
    cancelled_at            TIMESTAMPTZ,
    cancelled_by            UUID,
    cancel_reason           TEXT,
    source_event_id         VARCHAR(80),
    created_at              TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at              TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    created_by              UUID            NOT NULL,
    updated_by              UUID,
    is_deleted              BOOLEAN         NOT NULL DEFAULT FALSE,
    deleted_at              TIMESTAMPTZ,
    deleted_by              UUID,

    CONSTRAINT chk_po_status CHECK (status IN (
        'DRAFT','PENDING_APPROVAL','APPROVED','SENT_TO_VENDOR',
        'PARTIALLY_RECEIVED','FULLY_RECEIVED','INVOICED','PAID','CLOSED','CANCELLED'
    ))
);
CREATE UNIQUE INDEX ux_purchase_orders_rfq_active
    ON finance.purchase_orders(rfq_id)
    WHERE rfq_id IS NOT NULL AND is_deleted = FALSE;
CREATE UNIQUE INDEX ux_purchase_orders_source_event_active
    ON finance.purchase_orders(source_event_id)
    WHERE source_event_id IS NOT NULL AND is_deleted = FALSE;
CREATE INDEX idx_po_pr ON finance.purchase_orders(pr_id) WHERE is_deleted = FALSE;
CREATE INDEX idx_po_vendor ON finance.purchase_orders(vendor_id) WHERE is_deleted = FALSE;
CREATE INDEX idx_po_status ON finance.purchase_orders(status) WHERE is_deleted = FALSE;
```

### 5.7 po_line_items

```sql
CREATE TABLE finance.po_line_items (
    id              UUID            PRIMARY KEY DEFAULT gen_random_uuid(),
    po_id           UUID            NOT NULL REFERENCES finance.purchase_orders(id),
    line_number     INTEGER         NOT NULL,
    rfq_line_item_id UUID,
    pr_line_item_id UUID            NOT NULL,
    item_name       VARCHAR(255)    NOT NULL,
    category_code   VARCHAR(80)     NOT NULL,
    quantity        NUMERIC(19,4)   NOT NULL,
    unit            VARCHAR(30)     NOT NULL,
    unit_price      NUMERIC(19,4)   NOT NULL,
    total_price     NUMERIC(19,4)   NOT NULL,
    currency        VARCHAR(3)      NOT NULL DEFAULT 'VND',
    delivery_days   INTEGER,
    warranty        TEXT,
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    created_by      UUID            NOT NULL,
    updated_by      UUID,
    is_deleted      BOOLEAN         NOT NULL DEFAULT FALSE,
    deleted_at      TIMESTAMPTZ,
    deleted_by      UUID
);
CREATE UNIQUE INDEX ux_po_line_items_line_active
    ON finance.po_line_items(po_id, line_number)
    WHERE is_deleted = FALSE;
CREATE INDEX idx_po_line_items_po_active ON finance.po_line_items(po_id) WHERE is_deleted = FALSE;
```

### 5.8 invoices

```sql
CREATE TABLE finance.invoices (
    id                  UUID            PRIMARY KEY DEFAULT gen_random_uuid(),
    invoice_number      VARCHAR(100)    NOT NULL,
    vendor_id           UUID            NOT NULL,
    po_id               UUID            REFERENCES finance.purchase_orders(id),
    subtotal            NUMERIC(19,4)   NOT NULL,
    tax_amount          NUMERIC(19,4)   NOT NULL DEFAULT 0,
    total_amount        NUMERIC(19,4)   NOT NULL,
    currency            VARCHAR(3)      NOT NULL DEFAULT 'VND',
    invoice_date        DATE            NOT NULL,
    due_date            DATE            NOT NULL,
    status              VARCHAR(30)     NOT NULL DEFAULT 'PENDING_MATCH',
    
    -- 3-way match result
    po_match_status     VARCHAR(20),
    gr_match_status     VARCHAR(20),
    qty_variance        NUMERIC(10,4),
    price_variance      NUMERIC(19,4),
    matched_at          TIMESTAMPTZ,
    matched_by          UUID,
    
    approved_by         UUID,
    approved_at         TIMESTAMPTZ,
    created_at          TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    created_by          UUID            NOT NULL,
    is_deleted          BOOLEAN         NOT NULL DEFAULT FALSE,
    deleted_at          TIMESTAMPTZ,
    deleted_by          UUID,

    CONSTRAINT chk_invoice_status CHECK (status IN (
        'PENDING_MATCH','MATCHED','MISMATCHED','APPROVED','DISPUTED','PAID','CANCELLED'
    ))
);
CREATE INDEX idx_invoices_po ON finance.invoices(po_id);
CREATE INDEX idx_invoices_vendor ON finance.invoices(vendor_id);
CREATE INDEX idx_invoices_status ON finance.invoices(status) WHERE is_deleted = FALSE;
CREATE INDEX idx_invoices_due_date ON finance.invoices(due_date) WHERE status NOT IN ('PAID','CANCELLED');
```

Implementation note:
- `finance.invoices` adds `idempotency_key UUID NOT NULL`, `match_idempotency_key UUID`, `updated_by UUID`, and partial unique indexes:
  - `ux_invoices_vendor_number_active(vendor_id, invoice_number) WHERE is_deleted = FALSE`
  - `ux_invoices_idempotency_active(idempotency_key) WHERE is_deleted = FALSE`
  - `ux_invoices_match_idempotency_active(match_idempotency_key) WHERE match_idempotency_key IS NOT NULL AND is_deleted = FALSE`
- `finance.invoice_line_items` stores invoice line snapshots with `po_line_item_id`, `quantity`, `unit_price`, `tax_rate`, `tax_amount`, `total_price`, all as `NUMERIC(19,4)` except `tax_rate NUMERIC(7,4)`.

### 5.9 goods_receipt_snapshots

Finance stores a local read model from `inventory.gr.created` for invoice 3-way match. It does not query inventory-service tables directly.

```sql
finance.goods_receipt_snapshots
  id UUID PK, gr_number VARCHAR(50), po_id UUID FK purchase_orders, status VARCHAR(30),
  received_at TIMESTAMPTZ, completed_at TIMESTAMPTZ, source_event_id VARCHAR(80)
  ux_goods_receipt_snapshots_source_event_active(source_event_id) WHERE is_deleted = FALSE

finance.goods_receipt_line_snapshots
  gr_line_item_id UUID PK, gr_id UUID FK goods_receipt_snapshots,
  po_line_item_id UUID FK po_line_items, received_quantity NUMERIC(19,4),
  rejected_quantity NUMERIC(19,4), unit VARCHAR(50)
  ix_goods_receipt_line_snapshots_po_line_active(po_line_item_id) WHERE is_deleted = FALSE
```

---

## 6. db_inventory — Schema INVENTORY

### 6.1 V1 foundation overview

```sql
-- All mutable inventory tables include:
-- created_at, updated_at, created_by, updated_by, is_deleted, deleted_at, deleted_by.
-- Money/quantity columns use NUMERIC(19,4), timestamps use TIMESTAMPTZ.

inventory.warehouses
  id UUID PK, code VARCHAR(20), name VARCHAR(200), address TEXT, is_active BOOLEAN
  ux_warehouses_code_active(code) WHERE is_deleted = FALSE

inventory.items
  id UUID PK, item_code VARCHAR(20), name VARCHAR(300), category_code VARCHAR(50),
  unit VARCHAR(20), unit_price NUMERIC(19,4), currency VARCHAR(3),
  preferred_vendor_id UUID, reorder_point NUMERIC(19,4), is_active BOOLEAN
  ux_items_code_active(item_code) WHERE is_deleted = FALSE

inventory.stock_entries
  id UUID PK, item_code VARCHAR(20), warehouse_id UUID FK warehouses(id),
  quantity_on_hand NUMERIC(19,4), unit VARCHAR(20), last_updated TIMESTAMPTZ
  ux_stock_entries_item_warehouse_active(item_code, warehouse_id) WHERE is_deleted = FALSE

inventory.purchase_order_snapshots
  id UUID PK, po_id UUID, po_number VARCHAR(30), pr_id UUID, pr_number VARCHAR(30),
  vendor_id UUID, vendor_name VARCHAR(300), vendor_email VARCHAR(320),
  purchasing_officer_id UUID, total_amount NUMERIC(19,4), currency VARCHAR(3),
  delivery_address TEXT, delivery_deadline DATE, payment_terms VARCHAR(200),
  issued_at TIMESTAMPTZ, sent_to_vendor_at TIMESTAMPTZ, source_event_id VARCHAR(100)
  ux_po_snapshots_po_active(po_id) WHERE is_deleted = FALSE
  ux_po_snapshots_source_event_active(source_event_id) WHERE is_deleted = FALSE

inventory.purchase_order_line_snapshots
  id UUID PK, snapshot_id UUID FK purchase_order_snapshots(id), po_id UUID,
  po_line_item_id UUID, pr_line_item_id UUID, item_name VARCHAR(300),
  category_code VARCHAR(50), quantity NUMERIC(19,4), unit VARCHAR(20),
  unit_price NUMERIC(19,4), total_price NUMERIC(19,4), currency VARCHAR(3)
  ux_po_line_snapshots_line_active(po_line_item_id) WHERE is_deleted = FALSE

inventory.goods_receipts
  id UUID PK, gr_number VARCHAR(30), po_id UUID, warehouse_id UUID FK warehouses(id),
  warehouse_keeper_id UUID, warehouse_keeper_full_name VARCHAR(200),
  received_at TIMESTAMPTZ, status VARCHAR(30), notes TEXT, idempotency_key UUID,
  completed_at TIMESTAMPTZ, completed_by UUID, completed_idempotency_key UUID
  status IN ('DRAFT','PARTIAL','COMPLETE','DISCREPANCY')
  ux_goods_receipts_idempotency_active(idempotency_key) WHERE idempotency_key IS NOT NULL AND is_deleted = FALSE
  ux_goods_receipts_completed_idempotency_active(completed_idempotency_key) WHERE completed_idempotency_key IS NOT NULL AND is_deleted = FALSE
  ix_goods_receipts_received_at_active(received_at) WHERE is_deleted = FALSE
  ix_goods_receipts_created_at_active(created_at DESC, gr_number DESC) WHERE is_deleted = FALSE
  ix_goods_receipts_completed_at_active(completed_at DESC) WHERE completed_at IS NOT NULL AND is_deleted = FALSE
  inventory.gr_number_seq generates GR numbers in GR-YYYY-000001 format

inventory.goods_receipt_line_items
  id UUID PK, goods_receipt_id UUID FK goods_receipts(id), po_line_item_id UUID,
  item_code VARCHAR(20), item_name VARCHAR(300), ordered_quantity NUMERIC(19,4),
  received_quantity NUMERIC(19,4), rejected_quantity NUMERIC(19,4), unit VARCHAR(20),
  rejection_reason TEXT, lot_number VARCHAR(100)

inventory.stock_movements -- immutable, no soft delete
  id UUID PK, item_code VARCHAR(20), warehouse_id UUID FK warehouses(id),
  movement_type VARCHAR(30), quantity NUMERIC(19,4), unit VARCHAR(20),
  balance_after NUMERIC(19,4), source_ref_type VARCHAR(50), source_ref_id UUID,
  performed_by UUID, performed_at TIMESTAMPTZ, notes TEXT
  movement_type IN ('RECEIPT_IN','ISSUE_OUT','ADJUSTMENT','TRANSFER')
  Complete GR writes RECEIPT_IN rows with source_ref_type = 'GOODS_RECEIPT'
  Issue-out writes ISSUE_OUT rows with negative quantity and source_ref_type = 'STOCK_ISSUE_OUT'
  ix_stock_movements_type_performed(movement_type, performed_at DESC)

inventory.stock_issue_out_requests
  id UUID PK, idempotency_key UUID, warehouse_id UUID FK warehouses(id),
  pr_id UUID, recipient_id UUID, issued_by UUID, issued_at TIMESTAMPTZ, notes TEXT
  ux_stock_issue_out_idempotency_active(idempotency_key) WHERE is_deleted = FALSE
  ix_stock_issue_out_warehouse_issued_active(warehouse_id, issued_at DESC) WHERE is_deleted = FALSE
  ix_stock_issue_out_recipient_issued_active(recipient_id, issued_at DESC) WHERE is_deleted = FALSE

inventory.event_processing_log -- immutable Kafka idempotency log
  event_id VARCHAR(100) PK, topic VARCHAR(200), partition_id INTEGER,
  offset_value BIGINT, handler_name VARCHAR(100), status VARCHAR(30),
  processed_at TIMESTAMPTZ
```

---

## 7. db_audit — Schema AUDIT (Immutable)

### 7.1 audit_logs

```sql
CREATE TABLE audit.audit_logs (
    id              BIGSERIAL       PRIMARY KEY,            -- Sequential, không dùng UUID
    -- WHO
    actor_id        UUID            NOT NULL,
    actor_name      VARCHAR(200)    NOT NULL,
    actor_roles     VARCHAR(50)[],
    actor_ip        INET,
    session_id      VARCHAR(100),
    -- WHAT
    action          VARCHAR(100)    NOT NULL,               -- VD: 'PR.SUBMITTED', 'APPROVAL.ESCALATED'
    entity_type     VARCHAR(50)     NOT NULL,
    entity_id       UUID,
    entity_number   VARCHAR(50),                            -- VD: 'PR-2025-01-00001'
    -- WHEN
    occurred_at     TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    -- HOW
    http_method     VARCHAR(10),
    endpoint        VARCHAR(500),
    request_id      VARCHAR(100),
    -- RESULT
    is_success      BOOLEAN         NOT NULL,
    error_code      VARCHAR(50),
    -- CHANGE
    old_value       JSONB,
    new_value       JSONB,
    description     TEXT,
    service_name    VARCHAR(50)     NOT NULL
    -- Không có UPDATE/DELETE permission trên table này
);

-- Phân vùng theo năm
CREATE TABLE audit.audit_logs_2025 PARTITION OF audit.audit_logs
    FOR VALUES FROM ('2025-01-01') TO ('2026-01-01');

CREATE INDEX idx_audit_actor ON audit.audit_logs(actor_id, occurred_at DESC);
CREATE INDEX idx_audit_entity ON audit.audit_logs(entity_type, entity_id);
CREATE INDEX idx_audit_action ON audit.audit_logs(action, occurred_at DESC);
CREATE INDEX idx_audit_occurred ON audit.audit_logs(occurred_at DESC);
```

---

## 8. db_notification — Schema NOTIFICATION

### 8.1 notification_templates

```sql
CREATE TABLE notification.notification_templates (
    code            VARCHAR(100)    PRIMARY KEY,
    event_type      VARCHAR(100)    NOT NULL,               -- VD: 'PR_SUBMITTED'
    channel         VARCHAR(20)     NOT NULL,               -- 'EMAIL'|'IN_APP'|'PUSH'
    language        VARCHAR(5)      NOT NULL DEFAULT 'vi',
    subject_template TEXT,
    body_template   TEXT            NOT NULL,               -- Handlebars/Thymeleaf template
    is_active       BOOLEAN         NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    created_by      UUID,
    is_deleted      BOOLEAN         NOT NULL DEFAULT FALSE,
    deleted_at      TIMESTAMPTZ,
    deleted_by      UUID
);
```

### 8.2 notifications

```sql
CREATE TABLE notification.notifications (
    id              UUID            PRIMARY KEY DEFAULT gen_random_uuid(),
    recipient_id    UUID            NOT NULL,
    event_type      VARCHAR(100)    NOT NULL,
    channel         VARCHAR(20)     NOT NULL,
    subject         VARCHAR(500),
    body            TEXT            NOT NULL,
    reference_type  VARCHAR(50),
    reference_id    UUID,
    reference_number VARCHAR(100),
    action_url      VARCHAR(500),                        -- relative FE route
    email_to        VARCHAR(320),                        -- EMAIL channel recipient
    provider_message_id VARCHAR(200),
    status          VARCHAR(20)     NOT NULL DEFAULT 'PENDING',
    is_read         BOOLEAN         NOT NULL DEFAULT FALSE,
    read_at         TIMESTAMPTZ,
    sent_at         TIMESTAMPTZ,
    retry_count     SMALLINT        NOT NULL DEFAULT 0,
    last_error      TEXT,
    last_attempt_at TIMESTAMPTZ,
    next_attempt_at TIMESTAMPTZ,
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    created_by      UUID,
    is_deleted      BOOLEAN         NOT NULL DEFAULT FALSE,
    deleted_at      TIMESTAMPTZ,
    deleted_by      UUID,

    CONSTRAINT chk_notif_status CHECK (status IN ('PENDING','SENT','FAILED','CANCELLED')),
    CONSTRAINT chk_notifications_email_to_required CHECK (channel <> 'EMAIL' OR email_to IS NOT NULL)
);
CREATE INDEX idx_notif_recipient
    ON notification.notifications(recipient_id, is_read, created_at DESC)
    WHERE is_deleted = FALSE;
CREATE INDEX idx_notif_status
    ON notification.notifications(status)
    WHERE status = 'PENDING' AND is_deleted = FALSE;
CREATE INDEX idx_notifications_email_dispatch_due
    ON notification.notifications((COALESCE(next_attempt_at, created_at)), created_at)
    WHERE channel = 'EMAIL' AND status = 'PENDING' AND is_deleted = FALSE;
```

### 8.3 event_processing_log

```sql
CREATE TABLE notification.event_processing_log (
    event_id        VARCHAR(100)    PRIMARY KEY,
    event_type      VARCHAR(100)    NOT NULL,
    source          VARCHAR(100)    NOT NULL,
    topic           VARCHAR(150)    NOT NULL,
    partition_id    INTEGER,
    offset_value    BIGINT,
    handler_name    VARCHAR(100)    NOT NULL,
    status          VARCHAR(20)     NOT NULL DEFAULT 'PROCESSED',
    processed_at    TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    created_by      UUID,
    is_deleted      BOOLEAN         NOT NULL DEFAULT FALSE,
    deleted_at      TIMESTAMPTZ,
    deleted_by      UUID,

    CONSTRAINT chk_notification_event_log_status
        CHECK (status IN ('PROCESSED','SKIPPED','FAILED'))
);
CREATE INDEX idx_notification_event_log_topic_processed
    ON notification.event_processing_log(topic, processed_at DESC)
    WHERE is_deleted = FALSE;
```

### 8.4 email_dispatch_dead_letters

```sql
CREATE TABLE notification.email_dispatch_dead_letters (
    id              UUID            PRIMARY KEY DEFAULT gen_random_uuid(),
    notification_id UUID            NOT NULL UNIQUE REFERENCES notification.notifications(id),
    recipient_email VARCHAR(320)    NOT NULL,
    event_type      VARCHAR(100)    NOT NULL,
    failure_reason  TEXT            NOT NULL,
    retry_count     SMALLINT        NOT NULL,
    failed_at       TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    created_by      UUID,
    is_deleted      BOOLEAN         NOT NULL DEFAULT FALSE,
    deleted_at      TIMESTAMPTZ,
    deleted_by      UUID
);
CREATE INDEX idx_email_dispatch_dead_letters_failed_at
    ON notification.email_dispatch_dead_letters(failed_at DESC)
    WHERE is_deleted = FALSE;
```

---

## 9. FLYWAY MIGRATION NAMING CONVENTION

```
resources/
└── db/
    └── migration/
        ├── V1__create_schema_and_extensions.sql
        ├── V2__create_users_roles.sql
        ├── V3__create_departments_org.sql
        ├── V4__create_sessions_delegations.sql
        ├── V5__seed_permissions.sql
        ├── V6__seed_default_roles.sql
        └── V7__seed_super_admin.sql
```

**Conventions:**
- V{N}\_\_{description}.sql — forward migration
- U{N}\_\_{description}.sql — undo migration (nếu cần)
- R\_\_{description}.sql — repeatable migration (data seed, views)
- Mỗi file migration chỉ làm 1 việc duy nhất
- Không sửa file migration đã commit — tạo file mới để alter

---

## 10. RESOURCE LIMITS (Docker — 8GB RAM machine)

| Service DB | CPU | RAM |
|---|---|---|
| db_iam | 0.25 | 256MB |
| db_procurement | 0.5 | 512MB |
| db_finance | 0.25 | 256MB |
| db_inventory | 0.25 | 256MB |
| db_vendor | 0.25 | 128MB |
| db_notification | 0.1 | 128MB |
| db_audit | 0.25 | 256MB |

> Dev mode: dùng 1 PostgreSQL instance duy nhất với nhiều database.  
> Prod mode: tách thành separate instances hoặc dùng connection pooler (PgBouncer).
