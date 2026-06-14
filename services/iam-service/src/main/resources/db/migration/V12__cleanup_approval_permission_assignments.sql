CREATE SCHEMA IF NOT EXISTS iam;

-- Approval routing resolves users by permission, so each seeded role should only carry
-- the approval permissions it is meant to satisfy.
UPDATE iam.role_permissions rp
SET is_deleted = TRUE,
    deleted_at = NOW(),
    deleted_by = '00000000-0000-0000-0000-000000000000'::UUID,
    updated_at = NOW(),
    updated_by = '00000000-0000-0000-0000-000000000000'::UUID
FROM iam.roles r,
     iam.permissions p
WHERE rp.role_id = r.id
  AND rp.permission_id = p.id
  AND rp.is_deleted = FALSE
  AND r.is_deleted = FALSE
  AND p.is_deleted = FALSE
  AND (
      (UPPER(r.code) = 'MANAGER' AND UPPER(p.code) IN ('PR_APPROVE_FINANCE'))
      OR (UPPER(r.code) = 'DIRECTOR' AND UPPER(p.code) IN ('PR_APPROVE_EMERGENCY', 'PR_APPROVE_FINANCE', 'PR_APPROVE_L3'))
  );
