# API Authentication

## Auth flow
1) FE fetches RSA public key: GET /api/v1/auth/public-key
2) FE encrypts login payload (RSA+AES) and POST /api/v1/auth/login
3) IAM asks Keycloak to verify credentials; Keycloak User Storage SPI calls IAM internal credential APIs
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
Key: session:{tokenHash}
Value includes userId, expiresAt, and role codes only. Do not store permission snapshots in session data.
TTL: sliding window, default 8h, refresh on each request

## Permission cache
- role-perm:{roleCode} TTL PERM_CACHE_TTL_MINUTES
- Permissions are resolved from session roles via role-perm cache, with DB fallback.
- When user roles change, evict that user's active session cache so the next request loads current roles.
- When role permissions change, refresh or evict role-perm:{roleCode}; active sessions remain valid.

## Single session enforcement
On new login:
- Mark old sessions inactive in DB
- Delete session:{oldToken}
- Create new session and cookie

## Two-factor auth
- If requiresTwoFactor: true -> POST /api/v1/auth/two-factor/verify
- Uses temporary cookie ep_2fa

## Google OAuth
- GET /api/v1/auth/oauth/google -> redirect flow
- Sets ep_oauth_state HttpOnly cookie and redirects to Google consent
- Callback validates state/code and existing IAM user by googleOauthId or email
- On success, issue opaque session token, or ep_2fa if local 2FA is enabled

## Forgot/reset password
- POST /api/v1/auth/forgot-password requires Idempotency-Key and always returns 200 to avoid user enumeration
- Reset token is opaque 64 chars; only SHA-256 token_hash is stored in iam.password_reset_tokens
- POST /api/v1/auth/reset-password validates token, password policy, updates the IAM BCrypt(password + userId) credential hash, records password history, and revokes active sessions
- Email delivery is behind PasswordResetDeliveryPort; notification-service/Brevo integration is the production adapter target

## Logout
POST /api/v1/auth/logout
- Invalidate session in DB
- Delete session:{tokenHash}
- Clear ep_session cookie
