# Redis Cache Patterns (Session + Permission Cache)

## Key Design
- Prefix keys by domain and purpose.
- Examples:
  - session:{token}
  - user-session:{userId}
  - user-perm:{userId}
  - role-perm:{roleCode}
  - idempotent:{idempotencyKey}

## Cache-Aside for Token Lookup
1. Read from Redis by session token.
2. If miss, load from DB and populate cache.
3. Slide TTL on every request.

## Consistency Rules
- Redis is a cache, not the source of truth.
- Session keys use sliding TTL (default 8h) and are revoked on logout/new login.
- user-perm TTL: 15 minutes. role-perm TTL: 1 hour.
- idempotent key TTL: 24 hours.

## Suggested Fields
- token
- userId
- username
- sessionId
- createdAt
- lastActivity

## Operations
- On login: revoke all active sessions in DB, delete session:{oldToken}, delete user-perm:{userId}.
- On logout: set session inactive in DB, delete session:{token}, delete user-perm:{userId}.

## Example JSON Value
```
{"token":"...","userId":"...","username":"...","sessionId":"...","createdAt":"2026-05-16T12:00:00Z","lastActivity":"2026-05-16T12:34:56Z"}
```
