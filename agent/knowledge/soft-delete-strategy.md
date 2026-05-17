# Soft Delete Strategy

## Rules
- HTTP DELETE is not used.
- Use soft delete fields: is_deleted, deleted_at, deleted_by.
- Default queries must filter is_deleted = false.
- Use PATCH /{resource}/{id}/cancel or /deactivate.

## SQL example
```sql
UPDATE pr.purchase_requests
SET is_deleted = true,
    deleted_at = NOW(),
    deleted_by = :userId
WHERE id = :id;

SELECT *
FROM pr.purchase_requests
WHERE is_deleted = false;
```

## Data retention
- Keep audit history, archive old data if needed (e.g. > 5 years).
