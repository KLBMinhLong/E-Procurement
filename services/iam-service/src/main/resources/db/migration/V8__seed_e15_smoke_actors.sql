CREATE SCHEMA IF NOT EXISTS iam;

-- E15 runtime/API smoke actors. These are deterministic local/dev users only.
-- Password hashes intentionally use the IAM BCrypt(password + userId) strategy.

INSERT INTO iam.roles (id, code, name, description, is_system_role, created_by)
SELECT seed.id, seed.code, seed.name, seed.description, seed.is_system_role, seed.created_by
FROM (
    VALUES
        ('10000000-0000-0000-0000-000000000006'::UUID, 'PURCHASING', 'Purchasing Officer',
         'Can source vendors, run RFQ and issue purchase orders.', TRUE, '00000000-0000-0000-0000-000000000000'::UUID),
        ('10000000-0000-0000-0000-000000000007'::UUID, 'WAREHOUSE', 'Warehouse Keeper',
         'Can receive goods and manage stock movements.', TRUE, '00000000-0000-0000-0000-000000000000'::UUID),
        ('10000000-0000-0000-0000-000000000008'::UUID, 'ACCOUNTANT', 'Accountant',
         'Can manage invoices, matching and payment confirmation.', TRUE, '00000000-0000-0000-0000-000000000000'::UUID),
        ('10000000-0000-0000-0000-000000000009'::UUID, 'SUPER_ADMIN', 'Super Administrator',
         'Can administer platform configuration and view all operational data.', TRUE, '00000000-0000-0000-0000-000000000000'::UUID)
) AS seed(id, code, name, description, is_system_role, created_by)
WHERE NOT EXISTS (
    SELECT 1
    FROM iam.roles r
    WHERE r.is_deleted = FALSE
      AND UPPER(r.code) = UPPER(seed.code)
)
ON CONFLICT (id) DO NOTHING;

INSERT INTO iam.permissions (id, code, name, description, service, created_by)
SELECT seed.id, seed.code, seed.name, seed.description, seed.service, seed.created_by
FROM (
    VALUES
        ('20000000-0000-0000-0000-000000000906'::UUID, 'NOTIFICATION_VIEW_OWN',
         'View own notifications',
         'View personal in-app notifications and unread counts.',
         'NOTIFICATION_SERVICE', '00000000-0000-0000-0000-000000000000'::UUID)
) AS seed(id, code, name, description, service, created_by)
WHERE NOT EXISTS (
    SELECT 1
    FROM iam.permissions p
    WHERE p.is_deleted = FALSE
      AND UPPER(p.code) = UPPER(seed.code)
)
ON CONFLICT (id) DO NOTHING;

INSERT INTO iam.users (
    id, employee_code, username, email, full_name, department_id, org_node_id,
    keycloak_username, password_hash, status, two_factor_enabled, created_by
)
SELECT
    seed.id,
    seed.employee_code,
    seed.username,
    seed.email,
    seed.full_name,
    seed.department_id,
    seed.org_node_id,
    seed.username,
    seed.password_hash,
    'ACTIVE',
    FALSE,
    '00000000-0000-0000-0000-000000000000'::UUID
FROM (
    VALUES
        ('30000000-0000-0000-0000-000000000006'::UUID, 'EMP-2026-00006', 'purchasing',
         'purchasing@eprocure.local', 'Purchasing User',
         '22222222-2222-2222-2222-222222222222'::UUID, 'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb'::UUID,
         '$2a$12$wPTc5x9Z2vRJN1QM3uFcuefvpANaXkHSFBWpijBQUZXxxOKc.Qd8W'),
        ('30000000-0000-0000-0000-000000000007'::UUID, 'EMP-2026-00007', 'warehouse',
         'warehouse@eprocure.local', 'Warehouse User',
         '44444444-4444-4444-4444-444444444444'::UUID, 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa'::UUID,
         '$2a$12$XXIUKpsiHe4S5vVd.hILXO1OZIZcwiaMzBVNii5GXaPbl.xCLX35e'),
        ('30000000-0000-0000-0000-000000000008'::UUID, 'EMP-2026-00008', 'accountant',
         'accountant@eprocure.local', 'Accountant User',
         '33333333-3333-3333-3333-333333333333'::UUID, 'cccccccc-cccc-cccc-cccc-cccccccccccc'::UUID,
         '$2a$12$ysRVmcFBwZuITgtucrkWvuBjjsTEIthP0MExkX8T3n3SPraRjEcBG'),
        ('30000000-0000-0000-0000-000000000009'::UUID, 'EMP-2026-00009', 'superadmin',
         'superadmin@eprocure.local', 'Super Admin User',
         '11111111-1111-1111-1111-111111111111'::UUID, 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa'::UUID,
         '$2a$12$wchzlURh80N0Mkcaj25MZexD0mRepU/wrnYlIoao1PAU4ZVG0iqAG')
) AS seed(id, employee_code, username, email, full_name, department_id, org_node_id, password_hash)
WHERE NOT EXISTS (
    SELECT 1
    FROM iam.users u
    WHERE u.is_deleted = FALSE
      AND (
          UPPER(u.username) = UPPER(seed.username)
          OR UPPER(u.email) = UPPER(seed.email)
          OR u.employee_code = seed.employee_code
      )
)
ON CONFLICT (id) DO NOTHING;

INSERT INTO iam.user_roles (id, user_id, role_id, created_by)
SELECT gen_random_uuid(), u.id, r.id, '00000000-0000-0000-0000-000000000000'::UUID
FROM (
    VALUES
        ('purchasing', 'PURCHASING'),
        ('warehouse', 'WAREHOUSE'),
        ('accountant', 'ACCOUNTANT'),
        ('superadmin', 'SUPER_ADMIN')
) AS seed(username, role_code)
JOIN iam.users u
    ON u.is_deleted = FALSE AND u.username = seed.username
JOIN iam.roles r
    ON r.is_deleted = FALSE AND r.code = seed.role_code
ON CONFLICT (user_id, role_id) DO UPDATE
    SET is_deleted = FALSE,
        deleted_at = NULL,
        deleted_by = NULL,
        updated_at = NOW(),
        updated_by = '00000000-0000-0000-0000-000000000000'::UUID;

INSERT INTO iam.role_permissions (id, role_id, permission_id, created_by)
SELECT gen_random_uuid(), r.id, p.id, '00000000-0000-0000-0000-000000000000'::UUID
FROM (
    VALUES
        ('REQUESTER', 'NOTIFICATION_VIEW_OWN'),
        ('MANAGER', 'NOTIFICATION_VIEW_OWN'),
        ('DIRECTOR', 'NOTIFICATION_VIEW_OWN'),
        ('FINANCE', 'NOTIFICATION_VIEW_OWN'),
        ('ADMIN', 'NOTIFICATION_VIEW_OWN'),

        ('PURCHASING', 'IAM_PROFILE_READ'),
        ('PURCHASING', 'IAM_SESSION_REVOKE'),
        ('PURCHASING', 'PR_VIEW_ALL'),
        ('PURCHASING', 'PO_CREATE'),
        ('PURCHASING', 'PO_VIEW_OWN'),
        ('PURCHASING', 'PO_VIEW_ALL'),
        ('PURCHASING', 'PO_EDIT'),
        ('PURCHASING', 'PO_SEND_TO_VENDOR'),
        ('PURCHASING', 'PO_CANCEL'),
        ('PURCHASING', 'RFQ_CREATE'),
        ('PURCHASING', 'RFQ_VIEW'),
        ('PURCHASING', 'RFQ_EVALUATE'),
        ('PURCHASING', 'RFQ_AWARD'),
        ('PURCHASING', 'VENDOR_CREATE'),
        ('PURCHASING', 'VENDOR_VIEW'),
        ('PURCHASING', 'VENDOR_APPROVE'),
        ('PURCHASING', 'GR_VIEW'),
        ('PURCHASING', 'INVOICE_VIEW'),
        ('PURCHASING', 'REPORT_VIEW'),
        ('PURCHASING', 'NOTIFICATION_VIEW_OWN'),

        ('WAREHOUSE', 'IAM_PROFILE_READ'),
        ('WAREHOUSE', 'IAM_SESSION_REVOKE'),
        ('WAREHOUSE', 'PO_VIEW_ALL'),
        ('WAREHOUSE', 'GR_CREATE'),
        ('WAREHOUSE', 'GR_VIEW'),
        ('WAREHOUSE', 'GR_ISSUE_OUT'),
        ('WAREHOUSE', 'NOTIFICATION_VIEW_OWN'),

        ('ACCOUNTANT', 'IAM_PROFILE_READ'),
        ('ACCOUNTANT', 'IAM_SESSION_REVOKE'),
        ('ACCOUNTANT', 'PO_VIEW_ALL'),
        ('ACCOUNTANT', 'GR_VIEW'),
        ('ACCOUNTANT', 'INVOICE_CREATE'),
        ('ACCOUNTANT', 'INVOICE_VIEW'),
        ('ACCOUNTANT', 'INVOICE_MATCH'),
        ('ACCOUNTANT', 'INVOICE_APPROVE'),
        ('ACCOUNTANT', 'PAYMENT_CONFIRM'),
        ('ACCOUNTANT', 'BUDGET_VIEW_ALL'),
        ('ACCOUNTANT', 'REPORT_VIEW'),
        ('ACCOUNTANT', 'REPORT_EXPORT'),
        ('ACCOUNTANT', 'NOTIFICATION_VIEW_OWN'),

        ('SUPER_ADMIN', 'IAM_PROFILE_READ'),
        ('SUPER_ADMIN', 'IAM_SESSION_REVOKE'),
        ('SUPER_ADMIN', 'ADMIN_USER_VIEW'),
        ('SUPER_ADMIN', 'ADMIN_USER_MANAGE'),
        ('SUPER_ADMIN', 'ADMIN_ROLE_MANAGE'),
        ('SUPER_ADMIN', 'ADMIN_DEPARTMENT_MANAGE'),
        ('SUPER_ADMIN', 'ADMIN_APPROVAL_RULE'),
        ('SUPER_ADMIN', 'ADMIN_CATALOG_MANAGE'),
        ('SUPER_ADMIN', 'ADMIN_DELEGATION_MANAGE'),
        ('SUPER_ADMIN', 'SYSTEM_CONFIG'),
        ('SUPER_ADMIN', 'SYSTEM_AUDIT_VIEW'),
        ('SUPER_ADMIN', 'ORG_VIEW'),
        ('SUPER_ADMIN', 'ORG_APPROVER_RESOLVE'),
        ('SUPER_ADMIN', 'PR_VIEW_ALL'),
        ('SUPER_ADMIN', 'PO_VIEW_ALL'),
        ('SUPER_ADMIN', 'RFQ_VIEW'),
        ('SUPER_ADMIN', 'GR_VIEW'),
        ('SUPER_ADMIN', 'INVOICE_VIEW'),
        ('SUPER_ADMIN', 'BUDGET_VIEW_ALL'),
        ('SUPER_ADMIN', 'VENDOR_VIEW'),
        ('SUPER_ADMIN', 'REPORT_VIEW'),
        ('SUPER_ADMIN', 'REPORT_EXPORT'),
        ('SUPER_ADMIN', 'NOTIFICATION_VIEW_OWN')
) AS seed(role_code, permission_code)
JOIN iam.roles r
    ON r.is_deleted = FALSE AND UPPER(r.code) = seed.role_code
JOIN iam.permissions p
    ON p.is_deleted = FALSE AND UPPER(p.code) = seed.permission_code
ON CONFLICT (role_id, permission_id) DO UPDATE
    SET is_deleted = FALSE,
        deleted_at = NULL,
        deleted_by = NULL,
        updated_at = NOW(),
        updated_by = '00000000-0000-0000-0000-000000000000'::UUID;
