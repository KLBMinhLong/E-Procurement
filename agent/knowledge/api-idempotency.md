# API Idempotency

## When required
Idempotency-Key is required for POST, PUT, PATCH.

## Key format
- UUID v4, lowercase, hyphenated
- FE must keep the same key for the same user intent (retry safe)

## Backend flow
1) Check Redis key idempotent:{key}
2) If hit -> return cached response (HTTP 200)
3) If miss -> process, then cache response (TTL 24h)

## Redis value
```json
{
  "statusCode": 201,
  "body": { "success": true, "code": "PR_CREATED", "data": {} },
  "createdAt": "2025-01-15T08:30:00Z"
}
```

## Headers on replay
Idempotency-Replayed: true

## Missing key behavior
- If required and missing -> 400 with code SYS_005
- Some internal endpoints may skip (e.g. auth/login)

## Concurrency
- If a request with the same key is still processing -> return 409 Conflict
