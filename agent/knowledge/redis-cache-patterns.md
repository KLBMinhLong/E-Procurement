# Redis Cache Patterns (Session + Permission Cache)

## Key Design
- Prefix keys by domain and purpose.
- Examples:
  - session:{tokenHash}
  - user-session:{userId}
  - role-perm:{roleCode}
  - idempotent:{idempotencyKey}

## Cache-Aside for Token Lookup
1. Read from Redis by session token.
2. Session value stores identity plus role codes only; it must not store permission snapshots.
3. If session cache misses, load active session from DB, load current user roles from DB, and repopulate session cache.
4. Resolve permissions from role-perm:{roleCode}; if role-perm misses, load role permissions from DB and populate cache.
5. Slide session TTL on every request.

## Consistency Rules
- Redis is a cache, not the source of truth.
- Session keys use sliding TTL (default 8h) and are revoked on logout/new login.
- role-perm TTL: 15 minutes by `PERM_CACHE_TTL_MINUTES`.
- Session cache must not contain permission codes; this avoids stale permission after role-permission updates.
- Updating a user's roles must evict active session cache for that user so the next request loads current roles from DB.
- Updating a role's permissions must refresh or evict role-perm:{roleCode}; active sessions remain valid because they store roles only.
- idempotent key TTL: 24 hours.

## Suggested Fields
- tokenHash
- userId
- expiresAt
- roles

## Operations
- On login: revoke all active sessions in DB, delete session:{oldToken}, save session:{tokenHash} with roles.
- On logout: set session inactive in DB and delete session:{tokenHash}.
- On AssignUserRoles: update DB user_roles, then evict active session cache for that user.
- On UpdateRolePermissions: update DB role_permissions, then refresh role-perm:{roleCode}.

## Example JSON Value
```
{"tokenHash":"...","userId":"30000000-0000-0000-0000-000000000001","expiresAt":"2026-05-16T20:00:00Z","roles":["REQUESTER"]}
```
