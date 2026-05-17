# API Authentication

## Auth flow
1) FE fetches RSA public key: GET /api/v1/auth/public-key
2) FE encrypts login payload (RSA+AES) and POST /api/v1/auth/login
3) IAM verifies with Keycloak, creates opaque token
4) Token stored in Redis (primary) and DB (backup)
5) Response sets HttpOnly cookie ep_session

## Encryption init
```http
GET /api/v1/auth/public-key
```
Response data includes publicKey, keyVersion, algorithm.

## Login response
- Set-Cookie: ep_session={token}; HttpOnly; Secure; SameSite=Strict; Path=/
- Response envelope with user data

## Token format
- Opaque 64-char string (UUID + random)
- Not JWT, not decodable

## Session data in Redis
Key: session:{token}
Value includes userId, username, sessionId, createdAt, lastActivity
TTL: sliding window, default 8h, refresh on each request

## Permission cache
- user-perm:{userId} TTL 15 minutes
- role-perm:{roleCode} TTL 1 hour

## Single session enforcement
On new login:
- Mark old sessions inactive in DB
- Delete session:{oldToken}
- Delete user-perm:{userId}
- Create new session and cookie

## Two-factor auth
- If requiresTwoFactor: true -> POST /api/v1/auth/two-factor/verify
- Uses temporary cookie ep_session_pending

## Google OAuth
- GET /api/v1/auth/oauth/google -> redirect flow
- On success, issue opaque session token

## Logout
POST /api/v1/auth/logout
- Invalidate session in DB
- Delete session:{token} and user-perm:{userId}
- Clear ep_session cookie
