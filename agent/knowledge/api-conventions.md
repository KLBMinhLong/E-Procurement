# API Conventions

## Base URL and versioning
- Base URL: /api/v1/{resource}
- Breaking change -> /api/v2. Keep /api/v1 for at least 3 months.

## HTTP methods and soft delete
- GET: read
- POST: create/action (Idempotency-Key required)
- PUT: full update (Idempotency-Key required)
- PATCH: partial/state change (Idempotency-Key required)
- DELETE is not used. Use PATCH /{resource}/{id}/cancel or /deactivate.
- Soft delete fields: is_deleted, deleted_at, deleted_by.

## URL naming
- plural nouns, lowercase, hyphen-separated
- action verbs at end
- sub-resources for relations

Examples:
GET /api/v1/purchase-requests
POST /api/v1/purchase-requests/{id}/submit
GET /api/v1/purchase-requests/{id}/line-items

## Required headers
Content-Type: application/json
Accept: application/json
Cookie: ep_session={opaque_token}
Idempotency-Key: {uuid-v4}   (POST/PUT/PATCH)
X-Request-ID: {uuid-v4}      (optional)
Accept-Language: vi|en       (default vi)

## Request body (encryption enabled)
When ENCRYPTION_ENABLED=true:

```json
{
  "encryptedPayload": "...",
  "encryptedAesKey": "...",
  "iv": "...",
  "keyVersion": "v2025-01"
}
```

When ENCRYPTION_ENABLED=false:

```json
{
  "title": "Mua laptop",
  "...": "..."
}
```

## Query params
- page (1-based), size, sort=field,dir
- filter params: snake_case or camelCase (consistent per service)
- dates: ISO 8601 (date or datetime UTC)
- search: q=...

## Response envelope

```json
{
  "success": true,
  "code": "PR_CREATED",
  "message": null,
  "data": {},
  "meta": null,
  "timestamp": "2025-01-15T08:30:00.000Z",
  "requestId": "uuid"
}
```

## HTTP status codes
200, 201, 202, 400, 401, 403, 404, 409, 422, 423, 429, 500, 503
- 204 No Content is not used (always return envelope).

## Data types
- UUID: lowercase, hyphenated string
- Money: string with 4 decimals (e.g. "70000000.0000")
- Timestamp: ISO 8601 UTC with milliseconds
- Enum: UPPER_SNAKE_CASE string
- Page index: 1-based

## Field naming
- JSON fields use camelCase.

## Inter-service headers
X-Api-Key, X-Request-ID, X-User-ID, X-User-Roles

## File upload
- multipart/form-data
- Max size: 10MB
- Allowed types: pdf, xlsx, docx, jpeg, png

## Rate limiting
- Response headers: X-RateLimit-Limit, X-RateLimit-Remaining, X-RateLimit-Reset
- Defaults: user 100 req/min, upload 10 req/min, auth/login 10 req/min per IP,
  inter-service 1000 req/min
