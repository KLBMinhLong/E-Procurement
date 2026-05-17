CREATE EXTENSION IF NOT EXISTS pgcrypto;
CREATE SCHEMA IF NOT EXISTS iam;

CREATE OR REPLACE FUNCTION iam.touch_updated_at()
RETURNS TRIGGER
LANGUAGE plpgsql
AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$;

CREATE TABLE iam.departments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    code VARCHAR(50) NOT NULL,
    name VARCHAR(200) NOT NULL,
    parent_id UUID NULL REFERENCES iam.departments(id),
    head_user_id UUID NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NULL,
    created_by UUID NULL,
    updated_by UUID NULL,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at TIMESTAMPTZ NULL,
    deleted_by UUID NULL
);

CREATE UNIQUE INDEX ux_departments_code_active
    ON iam.departments (LOWER(code))
    WHERE is_deleted = FALSE;
CREATE INDEX ix_departments_parent_active
    ON iam.departments (parent_id)
    WHERE is_deleted = FALSE;

CREATE TRIGGER trg_departments_touch_updated_at
BEFORE UPDATE ON iam.departments
FOR EACH ROW
EXECUTE FUNCTION iam.touch_updated_at();

COMMENT ON TABLE iam.departments IS 'IAM department master data.';
COMMENT ON COLUMN iam.departments.is_deleted IS 'Soft delete flag; physical delete is not used.';

CREATE TABLE iam.org_nodes (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    code VARCHAR(50) NOT NULL,
    name VARCHAR(200) NOT NULL,
    department_id UUID NULL REFERENCES iam.departments(id),
    parent_id UUID NULL REFERENCES iam.org_nodes(id),
    path TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NULL,
    created_by UUID NULL,
    updated_by UUID NULL,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at TIMESTAMPTZ NULL,
    deleted_by UUID NULL
);

CREATE UNIQUE INDEX ux_org_nodes_code_active
    ON iam.org_nodes (LOWER(code))
    WHERE is_deleted = FALSE;
CREATE INDEX ix_org_nodes_parent_active
    ON iam.org_nodes (parent_id)
    WHERE is_deleted = FALSE;
CREATE INDEX ix_org_nodes_department_active
    ON iam.org_nodes (department_id)
    WHERE is_deleted = FALSE;

CREATE TRIGGER trg_org_nodes_touch_updated_at
BEFORE UPDATE ON iam.org_nodes
FOR EACH ROW
EXECUTE FUNCTION iam.touch_updated_at();

COMMENT ON TABLE iam.org_nodes IS 'Organization tree nodes used for approval routing.';

CREATE TABLE iam.users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    employee_code VARCHAR(20) NOT NULL,
    username VARCHAR(50) NOT NULL,
    email VARCHAR(255) NOT NULL,
    full_name VARCHAR(200) NOT NULL,
    phone VARCHAR(30) NULL,
    avatar_url TEXT NULL,
    department_id UUID NOT NULL REFERENCES iam.departments(id),
    org_node_id UUID NULL REFERENCES iam.org_nodes(id),
    keycloak_username VARCHAR(100) NOT NULL,
    password_hash TEXT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
    two_factor_enabled BOOLEAN NOT NULL DEFAULT FALSE,
    last_login_at TIMESTAMPTZ NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NULL,
    created_by UUID NULL,
    updated_by UUID NULL,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at TIMESTAMPTZ NULL,
    deleted_by UUID NULL,
    CONSTRAINT ck_users_status CHECK (status IN ('ACTIVE', 'INACTIVE', 'LOCKED', 'PENDING_VERIFY'))
);

CREATE UNIQUE INDEX ux_users_employee_code_active
    ON iam.users (LOWER(employee_code))
    WHERE is_deleted = FALSE;
CREATE UNIQUE INDEX ux_users_username_active
    ON iam.users (LOWER(username))
    WHERE is_deleted = FALSE;
CREATE UNIQUE INDEX ux_users_email_active
    ON iam.users (LOWER(email))
    WHERE is_deleted = FALSE;
CREATE INDEX ix_users_department_status_active
    ON iam.users (department_id, status)
    WHERE is_deleted = FALSE;
CREATE INDEX ix_users_org_node_active
    ON iam.users (org_node_id)
    WHERE is_deleted = FALSE;

CREATE TRIGGER trg_users_touch_updated_at
BEFORE UPDATE ON iam.users
FOR EACH ROW
EXECUTE FUNCTION iam.touch_updated_at();

COMMENT ON TABLE iam.users IS 'Application user profile mapped to Keycloak username.';
COMMENT ON COLUMN iam.users.password_hash IS 'Backup credential hash only; primary credential verification is delegated to Keycloak.';
COMMENT ON COLUMN iam.users.is_deleted IS 'Soft delete flag; physical delete is not used.';

CREATE TABLE iam.roles (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    code VARCHAR(80) NOT NULL,
    name VARCHAR(200) NOT NULL,
    description TEXT NULL,
    is_system_role BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NULL,
    created_by UUID NULL,
    updated_by UUID NULL,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at TIMESTAMPTZ NULL,
    deleted_by UUID NULL
);

CREATE UNIQUE INDEX ux_roles_code_active
    ON iam.roles (UPPER(code))
    WHERE is_deleted = FALSE;

CREATE TRIGGER trg_roles_touch_updated_at
BEFORE UPDATE ON iam.roles
FOR EACH ROW
EXECUTE FUNCTION iam.touch_updated_at();

COMMENT ON TABLE iam.roles IS 'RBAC role catalog.';

CREATE TABLE iam.permissions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    code VARCHAR(120) NOT NULL,
    name VARCHAR(200) NOT NULL,
    description TEXT NULL,
    service VARCHAR(80) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NULL,
    created_by UUID NULL,
    updated_by UUID NULL,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at TIMESTAMPTZ NULL,
    deleted_by UUID NULL
);

CREATE UNIQUE INDEX ux_permissions_code_active
    ON iam.permissions (UPPER(code))
    WHERE is_deleted = FALSE;
CREATE INDEX ix_permissions_service_active
    ON iam.permissions (service)
    WHERE is_deleted = FALSE;

CREATE TRIGGER trg_permissions_touch_updated_at
BEFORE UPDATE ON iam.permissions
FOR EACH ROW
EXECUTE FUNCTION iam.touch_updated_at();

COMMENT ON TABLE iam.permissions IS 'Permission codes consumed by @PreAuthorize.';

CREATE TABLE iam.user_roles (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES iam.users(id),
    role_id UUID NOT NULL REFERENCES iam.roles(id),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NULL,
    created_by UUID NULL,
    updated_by UUID NULL,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at TIMESTAMPTZ NULL,
    deleted_by UUID NULL,
    CONSTRAINT uq_user_roles_pair UNIQUE (user_id, role_id)
);

CREATE INDEX ix_user_roles_user_active
    ON iam.user_roles (user_id)
    WHERE is_deleted = FALSE;
CREATE INDEX ix_user_roles_role_active
    ON iam.user_roles (role_id)
    WHERE is_deleted = FALSE;

CREATE TRIGGER trg_user_roles_touch_updated_at
BEFORE UPDATE ON iam.user_roles
FOR EACH ROW
EXECUTE FUNCTION iam.touch_updated_at();

CREATE TABLE iam.role_permissions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    role_id UUID NOT NULL REFERENCES iam.roles(id),
    permission_id UUID NOT NULL REFERENCES iam.permissions(id),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NULL,
    created_by UUID NULL,
    updated_by UUID NULL,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at TIMESTAMPTZ NULL,
    deleted_by UUID NULL,
    CONSTRAINT uq_role_permissions_pair UNIQUE (role_id, permission_id)
);

CREATE INDEX ix_role_permissions_role_active
    ON iam.role_permissions (role_id)
    WHERE is_deleted = FALSE;
CREATE INDEX ix_role_permissions_permission_active
    ON iam.role_permissions (permission_id)
    WHERE is_deleted = FALSE;

CREATE TRIGGER trg_role_permissions_touch_updated_at
BEFORE UPDATE ON iam.role_permissions
FOR EACH ROW
EXECUTE FUNCTION iam.touch_updated_at();

CREATE TABLE iam.sessions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES iam.users(id),
    token_hash CHAR(64) NOT NULL,
    ip_address INET NULL,
    user_agent TEXT NULL,
    issued_at TIMESTAMPTZ NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    is_revoked BOOLEAN NOT NULL DEFAULT FALSE,
    revoked_at TIMESTAMPTZ NULL,
    revoked_by UUID NULL REFERENCES iam.users(id),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NULL,
    created_by UUID NULL,
    updated_by UUID NULL,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at TIMESTAMPTZ NULL,
    deleted_by UUID NULL,
    CONSTRAINT ck_sessions_token_hash CHECK (token_hash ~ '^[a-f0-9]{64}$')
);

CREATE UNIQUE INDEX ux_sessions_token_hash_active
    ON iam.sessions (token_hash)
    WHERE is_deleted = FALSE;
CREATE INDEX ix_sessions_user_active
    ON iam.sessions (user_id, expires_at DESC)
    WHERE is_deleted = FALSE AND is_revoked = FALSE;

CREATE TRIGGER trg_sessions_touch_updated_at
BEFORE UPDATE ON iam.sessions
FOR EACH ROW
EXECUTE FUNCTION iam.touch_updated_at();

COMMENT ON TABLE iam.sessions IS 'Opaque session token hashes. Raw tokens are only sent via HttpOnly cookie.';
COMMENT ON COLUMN iam.sessions.token_hash IS 'SHA-256 hex of opaque 64-char token.';

CREATE TABLE iam.delegations (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    delegator_id UUID NOT NULL REFERENCES iam.users(id),
    delegate_id UUID NOT NULL REFERENCES iam.users(id),
    start_at TIMESTAMPTZ NOT NULL,
    end_at TIMESTAMPTZ NOT NULL,
    max_value NUMERIC(19,4) NULL,
    currency CHAR(3) NOT NULL DEFAULT 'VND',
    allowed_categories JSONB NULL,
    scope VARCHAR(20) NOT NULL DEFAULT 'ALL',
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NULL,
    created_by UUID NULL,
    updated_by UUID NULL,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at TIMESTAMPTZ NULL,
    deleted_by UUID NULL,
    CONSTRAINT ck_delegations_scope CHECK (scope IN ('ALL', 'OWN_TEAM')),
    CONSTRAINT ck_delegations_status CHECK (status IN ('ACTIVE', 'EXPIRED', 'REVOKED')),
    CONSTRAINT ck_delegations_period CHECK (end_at > start_at),
    CONSTRAINT ck_delegations_distinct_users CHECK (delegator_id <> delegate_id)
);

CREATE INDEX ix_delegations_delegator_status_active
    ON iam.delegations (delegator_id, status, start_at DESC)
    WHERE is_deleted = FALSE;
CREATE INDEX ix_delegations_delegate_active
    ON iam.delegations (delegate_id, start_at, end_at)
    WHERE is_deleted = FALSE AND status = 'ACTIVE';

CREATE TRIGGER trg_delegations_touch_updated_at
BEFORE UPDATE ON iam.delegations
FOR EACH ROW
EXECUTE FUNCTION iam.touch_updated_at();

COMMENT ON TABLE iam.delegations IS 'Approval delegation rules.';
COMMENT ON COLUMN iam.delegations.max_value IS 'Maximum delegated approval amount, NUMERIC(19,4).';

INSERT INTO iam.departments (id, code, name, created_by)
VALUES
    ('11111111-1111-1111-1111-111111111111', 'IT', 'Information Technology', '00000000-0000-0000-0000-000000000000'),
    ('22222222-2222-2222-2222-222222222222', 'PROCUREMENT', 'Procurement', '00000000-0000-0000-0000-000000000000'),
    ('33333333-3333-3333-3333-333333333333', 'FINANCE', 'Finance', '00000000-0000-0000-0000-000000000000'),
    ('44444444-4444-4444-4444-444444444444', 'OPERATIONS', 'Operations', '00000000-0000-0000-0000-000000000000')
ON CONFLICT (id) DO NOTHING;

INSERT INTO iam.org_nodes (id, code, name, department_id, parent_id, path, created_by)
VALUES
    ('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa', 'HQ', 'Headquarters', NULL, NULL, '/HQ', '00000000-0000-0000-0000-000000000000'),
    ('bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb', 'PROCUREMENT', 'Procurement Node', '22222222-2222-2222-2222-222222222222', 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa', '/HQ/PROCUREMENT', '00000000-0000-0000-0000-000000000000'),
    ('cccccccc-cccc-cccc-cccc-cccccccccccc', 'FINANCE', 'Finance Node', '33333333-3333-3333-3333-333333333333', 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa', '/HQ/FINANCE', '00000000-0000-0000-0000-000000000000')
ON CONFLICT (id) DO NOTHING;

INSERT INTO iam.roles (id, code, name, description, is_system_role, created_by)
VALUES
    ('10000000-0000-0000-0000-000000000001', 'REQUESTER', 'Requester', 'Can create and submit purchase requests.', TRUE, '00000000-0000-0000-0000-000000000000'),
    ('10000000-0000-0000-0000-000000000002', 'MANAGER', 'Manager', 'Can approve department purchase requests.', TRUE, '00000000-0000-0000-0000-000000000000'),
    ('10000000-0000-0000-0000-000000000003', 'DIRECTOR', 'Director', 'Can approve high value purchase requests.', TRUE, '00000000-0000-0000-0000-000000000000'),
    ('10000000-0000-0000-0000-000000000004', 'FINANCE', 'Finance', 'Can manage budget, PO and invoices.', TRUE, '00000000-0000-0000-0000-000000000000'),
    ('10000000-0000-0000-0000-000000000005', 'ADMIN', 'Administrator', 'Can administer IAM and platform configuration.', TRUE, '00000000-0000-0000-0000-000000000000')
ON CONFLICT (id) DO NOTHING;

INSERT INTO iam.permissions (id, code, name, description, service, created_by)
VALUES
    ('20000000-0000-0000-0000-000000000001', 'IAM_PROFILE_READ', 'Read own profile', 'Read current authenticated user profile.', 'IAM_SERVICE', '00000000-0000-0000-0000-000000000000'),
    ('20000000-0000-0000-0000-000000000002', 'IAM_SESSION_REVOKE', 'Revoke own session', 'Logout and revoke current opaque session.', 'IAM_SERVICE', '00000000-0000-0000-0000-000000000000'),
    ('20000000-0000-0000-0000-000000000003', 'ADMIN_USER_VIEW', 'View users', 'View IAM users.', 'IAM_SERVICE', '00000000-0000-0000-0000-000000000000'),
    ('20000000-0000-0000-0000-000000000004', 'ADMIN_USER_MANAGE', 'Manage users', 'Create and update IAM users.', 'IAM_SERVICE', '00000000-0000-0000-0000-000000000000'),
    ('20000000-0000-0000-0000-000000000005', 'ADMIN_ROLE_MANAGE', 'Manage roles', 'Manage roles and permissions.', 'IAM_SERVICE', '00000000-0000-0000-0000-000000000000'),
    ('20000000-0000-0000-0000-000000000006', 'ADMIN_DEPARTMENT_MANAGE', 'Manage departments', 'Manage organization departments.', 'IAM_SERVICE', '00000000-0000-0000-0000-000000000000'),
    ('20000000-0000-0000-0000-000000000007', 'PR_CREATE', 'Create purchase request', 'Create purchase requests.', 'PR_SERVICE', '00000000-0000-0000-0000-000000000000'),
    ('20000000-0000-0000-0000-000000000008', 'PR_VIEW_OWN', 'View own purchase requests', 'View own purchase requests.', 'PR_SERVICE', '00000000-0000-0000-0000-000000000000'),
    ('20000000-0000-0000-0000-000000000009', 'PR_APPROVE_L1', 'Approve level 1', 'Approve level 1 purchase requests.', 'APPROVAL_SERVICE', '00000000-0000-0000-0000-000000000000'),
    ('20000000-0000-0000-0000-000000000010', 'PR_APPROVE_L2', 'Approve level 2', 'Approve level 2 purchase requests.', 'APPROVAL_SERVICE', '00000000-0000-0000-0000-000000000000'),
    ('20000000-0000-0000-0000-000000000011', 'DELEGATION_MANAGE', 'Manage delegation', 'Create and revoke approval delegations.', 'IAM_SERVICE', '00000000-0000-0000-0000-000000000000'),
    ('20000000-0000-0000-0000-000000000012', 'BUDGET_VIEW_ALL', 'View all budgets', 'View organization budgets.', 'FINANCE_SERVICE', '00000000-0000-0000-0000-000000000000'),
    ('20000000-0000-0000-0000-000000000013', 'PO_ISSUE', 'Issue purchase order', 'Issue purchase orders.', 'FINANCE_SERVICE', '00000000-0000-0000-0000-000000000000'),
    ('20000000-0000-0000-0000-000000000014', 'INVOICE_MATCH', 'Match invoices', 'Run invoice matching.', 'FINANCE_SERVICE', '00000000-0000-0000-0000-000000000000')
ON CONFLICT (id) DO NOTHING;

INSERT INTO iam.users (
    id, employee_code, username, email, full_name, department_id, org_node_id,
    keycloak_username, password_hash, status, two_factor_enabled, created_by
)
VALUES
    ('30000000-0000-0000-0000-000000000001', 'EMP-2025-00001', 'requester', 'requester@eprocure.local', 'Request User', '22222222-2222-2222-2222-222222222222', 'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb', 'requester', '$2a$12$placeholder', 'ACTIVE', FALSE, '00000000-0000-0000-0000-000000000000'),
    ('30000000-0000-0000-0000-000000000002', 'EMP-2025-00002', 'manager', 'manager@eprocure.local', 'Manager User', '22222222-2222-2222-2222-222222222222', 'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb', 'manager', '$2a$12$placeholder', 'ACTIVE', FALSE, '00000000-0000-0000-0000-000000000000'),
    ('30000000-0000-0000-0000-000000000003', 'EMP-2025-00003', 'director', 'director@eprocure.local', 'Director User', '44444444-4444-4444-4444-444444444444', 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa', 'director', '$2a$12$placeholder', 'ACTIVE', FALSE, '00000000-0000-0000-0000-000000000000'),
    ('30000000-0000-0000-0000-000000000004', 'EMP-2025-00004', 'finance', 'finance@eprocure.local', 'Finance User', '33333333-3333-3333-3333-333333333333', 'cccccccc-cccc-cccc-cccc-cccccccccccc', 'finance', '$2a$12$placeholder', 'ACTIVE', FALSE, '00000000-0000-0000-0000-000000000000'),
    ('30000000-0000-0000-0000-000000000005', 'EMP-2025-00005', 'admin', 'admin@eprocure.local', 'Admin User', '11111111-1111-1111-1111-111111111111', 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa', 'admin', '$2a$12$placeholder', 'ACTIVE', FALSE, '00000000-0000-0000-0000-000000000000')
ON CONFLICT (id) DO NOTHING;

INSERT INTO iam.user_roles (id, user_id, role_id, created_by)
VALUES
    (gen_random_uuid(), '30000000-0000-0000-0000-000000000001', '10000000-0000-0000-0000-000000000001', '00000000-0000-0000-0000-000000000000'),
    (gen_random_uuid(), '30000000-0000-0000-0000-000000000002', '10000000-0000-0000-0000-000000000002', '00000000-0000-0000-0000-000000000000'),
    (gen_random_uuid(), '30000000-0000-0000-0000-000000000003', '10000000-0000-0000-0000-000000000003', '00000000-0000-0000-0000-000000000000'),
    (gen_random_uuid(), '30000000-0000-0000-0000-000000000004', '10000000-0000-0000-0000-000000000004', '00000000-0000-0000-0000-000000000000'),
    (gen_random_uuid(), '30000000-0000-0000-0000-000000000005', '10000000-0000-0000-0000-000000000005', '00000000-0000-0000-0000-000000000000')
ON CONFLICT (user_id, role_id) DO NOTHING;

INSERT INTO iam.role_permissions (id, role_id, permission_id, created_by)
SELECT gen_random_uuid(), r.id, p.id, '00000000-0000-0000-0000-000000000000'::UUID
FROM (
    VALUES
        ('REQUESTER', 'IAM_PROFILE_READ'),
        ('REQUESTER', 'IAM_SESSION_REVOKE'),
        ('REQUESTER', 'PR_CREATE'),
        ('REQUESTER', 'PR_VIEW_OWN'),
        ('MANAGER', 'IAM_PROFILE_READ'),
        ('MANAGER', 'IAM_SESSION_REVOKE'),
        ('MANAGER', 'PR_APPROVE_L1'),
        ('MANAGER', 'DELEGATION_MANAGE'),
        ('DIRECTOR', 'IAM_PROFILE_READ'),
        ('DIRECTOR', 'IAM_SESSION_REVOKE'),
        ('DIRECTOR', 'PR_APPROVE_L2'),
        ('DIRECTOR', 'DELEGATION_MANAGE'),
        ('FINANCE', 'IAM_PROFILE_READ'),
        ('FINANCE', 'IAM_SESSION_REVOKE'),
        ('FINANCE', 'BUDGET_VIEW_ALL'),
        ('FINANCE', 'PO_ISSUE'),
        ('FINANCE', 'INVOICE_MATCH'),
        ('ADMIN', 'IAM_PROFILE_READ'),
        ('ADMIN', 'IAM_SESSION_REVOKE'),
        ('ADMIN', 'ADMIN_USER_VIEW'),
        ('ADMIN', 'ADMIN_USER_MANAGE'),
        ('ADMIN', 'ADMIN_ROLE_MANAGE'),
        ('ADMIN', 'ADMIN_DEPARTMENT_MANAGE')
) AS seed(role_code, permission_code)
JOIN iam.roles r ON r.code = seed.role_code AND r.is_deleted = FALSE
JOIN iam.permissions p ON p.code = seed.permission_code AND p.is_deleted = FALSE
ON CONFLICT (role_id, permission_id) DO NOTHING;
