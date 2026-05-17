# API Pagination

## Request params (offset pagination)
```
GET /api/v1/purchase-requests?page=1&size=20&sort=createdAt,desc

page: 1-based, default 1
size: default 20, max 100 (some endpoints override)
sort: field,direction (asc|desc). Multiple sort allowed.
```

### Default sizes by endpoint
| Endpoint | Default size | Max size | Default sort |
|---|---|---|---|
| Approval inbox | 20 | 50 | slaDeadline,asc |
| Purchase requests | 20 | 100 | createdAt,desc |
| Vendors | 20 | 100 | name,asc |
| Notifications | 30 | 100 | createdAt,desc |
| Audit logs | 50 | 200 | occurredAt,desc |
| Reports/Analytics | 50 | 500 | createdAt,desc |

## Response meta
```json
{
  "meta": {
    "page": 1,
    "size": 20,
    "totalElements": 156,
    "totalPages": 8,
    "isFirst": true,
    "isLast": false,
    "sort": "createdAt,desc"
  }
}
```

## Cursor pagination (notifications/feed)
```
GET /api/v1/notifications?cursor=base64(...)&size=30
```

```json
{
  "meta": {
    "size": 30,
    "hasMore": true,
    "nextCursor": "...",
    "prevCursor": null
  }
}
```

## Empty result
```json
{
  "data": [],
  "meta": {
    "page": 1,
    "size": 20,
    "totalElements": 0,
    "totalPages": 0,
    "isFirst": true,
    "isLast": true,
    "sort": "createdAt,desc"
  }
}
```

## Backend rules
- Page index is 1-based.
- Whitelist sortable fields to avoid SQL injection.
- Reject invalid params with VAL_003.
