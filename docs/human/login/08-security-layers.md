# 08 — Tổng hợp các lớp bảo mật

## Tổng quan

E-Procure áp dụng **defense-in-depth** — nhiều lớp bảo mật chồng lên nhau. Nếu một lớp bị bypass, các lớp còn lại vẫn bảo vệ hệ thống.

## Các lớp bảo mật

```
┌───────────────────────────────────────────────────────┐
│                    Lớp 1: Network                      │
│  NGINX, CORS, HTTPS, Security Headers                 │
├───────────────────────────────────────────────────────┤
│                    Lớp 2: Gateway Auth                 │
│  NGINX auth_request subrequest                        │
├───────────────────────────────────────────────────────┤
│                    Lớp 3: Application Auth             │
│  Spring Security, SessionAuthenticationFilter          │
├───────────────────────────────────────────────────────┤
│                    Lớp 4: Authorization                │
│  RBAC, @PreAuthorize, Permission Codes                │
├───────────────────────────────────────────────────────┤
│                    Lớp 5: Credential Security          │
│  Keycloak delegation, TOTP 2FA                        │
├───────────────────────────────────────────────────────┤
│                    Lớp 6: Data Security                │
│  Opaque tokens, SHA-256 hashing, AES encryption       │
├───────────────────────────────────────────────────────┤
│                    Lớp 7: Transport Security           │
│  HttpOnly cookies, SameSite, CSRF protection          │
└───────────────────────────────────────────────────────┘
```

## Lớp 1: Network

### NGINX Security Headers

```nginx
add_header X-Content-Type-Options nosniff always;    # Chống MIME sniffing
add_header X-Frame-Options DENY always;              # Chống clickjacking
add_header Referrer-Policy no-referrer always;        # Không leak referrer
```

### CORS

- Chỉ cho phép origins được cấu hình (`CORS_ALLOWED_ORIGINS`)
- `Access-Control-Allow-Credentials: true` (cho phép cookie)
- Preflight cache: 86400 giây

### HTTPS (Production)

- NGINX terminate TLS
- Backend services giao tiếp nội bộ qua HTTP (Docker network)
- Cookie `Secure=true` ở production

## Lớp 2: Gateway Auth (NGINX auth_request)

- Mọi protected request đều qua subrequest `GET /api/v1/auth/verify`
- IAM Service xác thực session token
- User context (ID, permissions) inject vào headers
- Backend services **tin tưởng** headers từ Gateway

**Bảo vệ chống:**
- Truy cập trực tiếp vào backend service mà không qua Gateway
- Request không có session hợp lệ

## Lớp 3: Application Auth (Spring Security)

### SecurityFilterChain

```java
.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
.addFilterBefore(sessionAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
```

- **Stateless** — không tạo HTTP session
- **SessionAuthenticationFilter** — đọc cookie, xác thực qua SessionService
- **Public endpoints** — login, forgot-password, public-key, health

### Exception Handling

| Exception | HTTP Status | Response |
|-----------|-------------|----------|
| Not authenticated | 401 | `{"code": "IAM_003"}` |
| Access denied | 403 | `{"code": "IAM_004"}` |

## Lớp 4: Authorization (RBAC)

### Permission-based

```java
@PreAuthorize("hasAuthority('IAM_SESSION_REVOKE')")
```

### Permission Resolution Flow

```
User → UserRoles → Roles → RolePermissions → Permissions
                                                  ↓
                                        Set<String> permissionCodes
                                                  ↓
                                        UserPrincipal.authorities
                                                  ↓
                                        @PreAuthorize check
```

### Permission Cache

- Redis cache với TTL 15 phút
- Invalidate khi role assignment thay đổi

## Lớp 5: Credential Security

### Password Verification

- Delegate cho Keycloak (Resource Owner Password Grant)
- IAM Service **không lưu password** (chỉ backup hash)
- Keycloak xử lý brute-force protection

### Two-Factor Authentication

- TOTP (RFC 6238) — 6 digits, 30s period
- Backup codes — 8 codes, mỗi code dùng 1 lần
- Secret mã hóa bằng AES trước khi lưu DB
- Challenge token hết hạn sau 5 phút

### Account Lock

- `iam.users.status = 'LOCKED'`
- Locked user không thể login (`canLogin()` check)

## Lớp 6: Data Security

### Opaque Token

```
Raw token: 32 random bytes → 64 hex chars
    ↓ SHA-256
Token hash: 64 hex chars (lưu trong DB/Redis)
```

- Raw token **chỉ** nằm trong HttpOnly cookie
- DB chỉ lưu hash — nếu DB bị leak, attacker không có raw token
- Không thể reverse hash → raw token

### Session Token Properties

- 32 bytes entropy = 2^256 khả năng brute-force
- SHA-256 là one-way function
- Token ngẫu nhiên hoàn toàn (không predictable)

### Encryption (Production)

- RSA/AES hybrid encryption cho request/response payloads
- `EncryptionService` ở frontend
- `RsaAesCryptoService` ở backend
- Tắt ở local/dev để dễ debug

### Sensitive Data Masking

```java
LogMaskingUtil.maskEmail("user@example.com")  // "u***@e***.com"
LogMaskingUtil.maskId(uuid)                    // "550e****-****-****"
```

## Lớp 7: Transport Security (Cookie)

### Cookie Security Attributes

| Attribute | Giá trị | Tác dụng |
|-----------|---------|----------|
| `HttpOnly` | `true` | JavaScript không đọc được → chống XSS |
| `Secure` | `true` (prod) | Chỉ gửi qua HTTPS |
| `SameSite` | `Strict` | Chống CSRF (không gửi cross-site) |
| `Path` | `/` | Gửi với mọi request |
| `Max-Age` | `28800` (8h) | Session hết hạn tự động |

### SameSite cho OAuth

- `ep_oauth_state` dùng `SameSite=Lax` (vì redirect từ Google)
- `ep_session` dùng `SameSite=Strict`

## Tổng hợp mối đe dọa & Mitigation

| Mối đe dọa | Mitigation |
|------------|------------|
| **XSS** | HttpOnly cookie, CSP headers |
| **CSRF** | SameSite=Strict, Idempotency-Key |
| **Brute-force** | Keycloak protection, account lock |
| **Session hijacking** | Opaque token, SHA-256 hash, single session |
| **Token leak từ DB** | Chỉ lưu hash, không lưu raw token |
| **Replay attack** | Idempotency-Key, single-use tokens |
| **Clickjacking** | X-Frame-Options: DENY |
| **MIME sniffing** | X-Content-Type-Options: nosniff |
| **Credential stuffing** | 2FA, Keycloak rate limiting |
| **Man-in-the-middle** | HTTPS, Secure cookie |
| **Session fixation** | Mỗi login tạo token mới, revoke cũ |

## Security Checklist

- [x] HttpOnly cookies cho session token
- [x] SameSite=Strict cho session cookie
- [x] Opaque token (không dùng JWT)
- [x] SHA-256 hash trước khi lưu DB
- [x] Single session per user
- [x] Session TTL 8 giờ
- [x] 2FA support (TOTP + backup codes)
- [x] Credential delegation to Keycloak
- [x] RBAC permission-based authorization
- [x] Idempotency-Key cho write operations
- [x] Sensitive data masking trong logs
- [x] CORS restriction
- [x] Security headers (X-Frame-Options, etc.)
- [x] NGINX auth_request cho protected routes
- [x] Internal API key cho service-to-service
