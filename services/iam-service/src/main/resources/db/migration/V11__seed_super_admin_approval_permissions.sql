CREATE SCHEMA IF NOT EXISTS iam;

-- Ensure Super Admin can be selected by permission-based approval routing.
-- This keeps approval routing permission-driven while preserving a global fallback approver.
INSERT INTO iam.role_permissions (id, role_id, permission_id, created_by)
SELECT gen_random_uuid(), r.id, p.id, '00000000-0000-0000-0000-000000000000'::UUID
FROM (
    VALUES
        ('PR_APPROVE_L1'),
        ('PR_APPROVE_L2'),
        ('PR_APPROVE_L3'),
        ('PR_APPROVE_FINANCE'),
        ('PR_APPROVE_EMERGENCY'),
        ('PR_REQUEST_CHANGES'),
        ('PR_FORWARD')
) AS seed(permission_code)
JOIN iam.roles r
    ON r.is_deleted = FALSE
   AND UPPER(r.code) = 'SUPER_ADMIN'
JOIN iam.permissions p
    ON p.is_deleted = FALSE
   AND UPPER(p.code) = seed.permission_code
ON CONFLICT (role_id, permission_id) DO UPDATE
    SET is_deleted = FALSE,
        deleted_at = NULL,
        deleted_by = NULL,
        updated_at = NOW(),
        updated_by = '00000000-0000-0000-0000-000000000000'::UUID;
