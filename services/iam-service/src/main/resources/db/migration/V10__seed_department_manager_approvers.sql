CREATE SCHEMA IF NOT EXISTS iam;

-- Ensure deterministic local/dev departments have a level-1 manager approver.
-- Without these seed assignments, PRs created from non-Procurement departments can
-- be submitted but approval-service cannot start the first MANAGER step.
INSERT INTO iam.user_roles (id, user_id, role_id, created_by)
SELECT gen_random_uuid(),
       u.id,
       r.id,
       '00000000-0000-0000-0000-000000000000'::UUID
FROM (
    VALUES
        ('admin', 'MANAGER'),
        ('warehouse', 'MANAGER'),
        ('accountant', 'MANAGER')
) AS seed(username, role_code)
JOIN iam.users u
    ON u.is_deleted = FALSE
   AND u.username = seed.username
JOIN iam.roles r
    ON r.is_deleted = FALSE
   AND UPPER(r.code) = seed.role_code
ON CONFLICT (user_id, role_id) DO UPDATE
    SET is_deleted = FALSE,
        deleted_at = NULL,
        deleted_by = NULL,
        updated_at = NOW(),
        updated_by = '00000000-0000-0000-0000-000000000000'::UUID;
