CREATE SCHEMA IF NOT EXISTS iam;

INSERT INTO iam.permissions (id, code, name, description, service, created_by)
SELECT '20000000-0000-0000-0000-000000000015'::UUID,
       'ORG_VIEW',
       'View organization',
       'View organization departments and department members.',
       'IAM_SERVICE',
       '00000000-0000-0000-0000-000000000000'::UUID
WHERE NOT EXISTS (
    SELECT 1 FROM iam.permissions WHERE is_deleted = FALSE AND UPPER(code) = 'ORG_VIEW'
);

INSERT INTO iam.permissions (id, code, name, description, service, created_by)
SELECT '20000000-0000-0000-0000-000000000016'::UUID,
       'ORG_APPROVER_RESOLVE',
       'Resolve approvers',
       'Resolve approver candidates by role and department.',
       'IAM_SERVICE',
       '00000000-0000-0000-0000-000000000000'::UUID
WHERE NOT EXISTS (
    SELECT 1 FROM iam.permissions WHERE is_deleted = FALSE AND UPPER(code) = 'ORG_APPROVER_RESOLVE'
);

INSERT INTO iam.role_permissions (id, role_id, permission_id, created_by)
SELECT gen_random_uuid(), r.id, p.id, '00000000-0000-0000-0000-000000000000'::UUID
FROM (
    VALUES
        ('ADMIN', 'ORG_VIEW'),
        ('ADMIN', 'ORG_APPROVER_RESOLVE'),
        ('REQUESTER', 'ORG_APPROVER_RESOLVE')
) AS seed(role_code, permission_code)
JOIN iam.roles r ON r.is_deleted = FALSE AND UPPER(r.code) = seed.role_code
JOIN iam.permissions p ON p.is_deleted = FALSE AND UPPER(p.code) = seed.permission_code
ON CONFLICT (role_id, permission_id) DO UPDATE
SET is_deleted = FALSE,
    deleted_at = NULL,
    deleted_by = NULL,
    updated_at = NOW(),
    updated_by = '00000000-0000-0000-0000-000000000000'::UUID;
