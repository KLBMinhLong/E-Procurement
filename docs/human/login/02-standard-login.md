# 02 — Luồng đăng nhập Username/Password

## Endpoint

```
POST /api/v1/auth/login
Content-Type: application/json
Idempotency-Key: <uuid>
```

## Request Body

```json
{
  "username": "nguyen.van.a",      // username hoặc email
  "password": "P@ssw0rd123"
}
```

> **Lưu ý:** Request có thể chứa encrypted payload (RSA/AES) khi bật encryption ở production. Ở local/dev, encryption tắt nên gửi plain text.

## Response

### Thành công (không có 2FA)

```json
{
  "success": true,
  "data": {
    "userId": "550e8400-e29b-41d4-a716-446655440000",
    "fullName": "Nguyễn Văn A",
    "avatarUrl": "https://...",
    "requiresTwoFactor": false
  },
  "meta": { "requestId": "..." }
}
```

**Set-Cookie headers:**
- `ep_session=<raw-token>; HttpOnly; SameSite=Strict; Path=/; Max-Age=28800`
- `ep_2fa=; Max-Age=0` (clear 2FA cookie nếu có)

### Thành công (yêu cầu 2FA)

```json
{
  "success": true,
  "data": {
    "userId": "550e8400-e29b-41d4-a716-446655440000",
    "fullName": "Nguyễn Văn A",
    "avatarUrl": null,
    "requiresTwoFactor": true
  },
  "meta": { "requestId": "..." }
}
```

**Set-Cookie headers:**
- `ep_session=; Max-Age=0` (clear session cookie)
- `ep_2fa=<challenge-token>; HttpOnly; SameSite=Strict; Path=/; Max-Age=300`

### Thất bại

| Code | HTTP Status | Nguyên nhân |
|------|-------------|-------------|
| `IAM_001` | 401 | Sai username/password |
| `IAM_002` | 423 | Account bị khóa hoặc không thể login |

## Chi tiết xử lý phía Backend

### Bước 1: AuthController nhận request

```
AuthController.login()
├── Validate request body (@Valid)
├── Resolve client IP (X-Forwarded-For / RemoteAddr)
├── Gọi LoginUseCase.execute(LoginCommand)
├── Nếu requiresTwoFactor → set ep_2fa cookie, clear ep_session
└── Nếu không → set ep_session cookie, clear ep_2fa
```

**File:** `AuthController.java` → method `login()`

### Bước 2: LoginUseCase xử lý logic

```
LoginUseCase.execute(LoginCommand)
│
├── 1. Tìm user trong DB
│   └── userRepository.findByUsernameOrEmail(username)
│       → Tìm theo username hoặc email, ignore case
│       → Nếu không tìm thấy → throw IAM_001
│
├── 2. Kiểm tra user status
│   ├── user.isLocked() → throw IAM_002
│   └── !user.canLogin() → throw IAM_002
│       (status phải là ACTIVE)
│
├── 3. Verify credentials qua Keycloak
│   └── credentialVerificationPort.verify(username, password)
│       → Gọi Keycloak Resource Owner Password Grant
│       → Nếu fail → throw IAM_001
│
├── 4. Kiểm tra 2FA
│   └── user.isTwoFactorEnabled()?
│       ├── YES → tạo TwoFactorChallenge, trả về yêu cầu OTP
│       └── NO → tạo session, trả về thành công
│
└── 5. Cập nhật lastLoginAt
```

**File:** `LoginUseCase.java`

### Bước 3: KeycloakCredentialVerificationAdapter

```
KeycloakCredentialVerificationAdapter.verify(username, password)
│
├── Tạo form data:
│   grant_type=password
│   client_id=eprocure-iam
│   client_secret=<secret>
│   username=<username>
│   password=<password>
│
├── POST /realms/eprocure/protocol/openid-connect/token
│   Content-Type: application/x-www-form-urlencoded
│
├── 204 No Content → return true (credentials hợp lệ)
└── 401 Unauthorized → return false (sai credentials)
```

**File:** `KeycloakCredentialVerificationAdapter.java`

> **Quan trọng:** Adapter chỉ dùng để **verify** credentials, không lấy access token. Keycloak đóng vai trò "password validator" thuần túy.

### Bước 4: SessionService.issueFor() — Tạo session

```
SessionService.issueFor(user, clientContext)
│
├── 1. Tạo opaque token
│   ├── rawToken = OpaqueTokenService.generate()  // 32 random bytes → 64 hex chars
│   └── tokenHash = OpaqueTokenService.hash(rawToken)  // SHA-256
│
├── 2. Revoke session cũ của user
│   └── revokeActiveForUser(userId)  // mỗi user chỉ có 1 session active
│
├── 3. Lưu vào PostgreSQL
│   └── iam.sessions table:
│       id, user_id, token_hash, ip_address, user_agent, issued_at, expires_at
│
├── 4. Cache vào Redis
│   └── key: iam:session:<token_hash>
│       value: {tokenHash, userId, expiresAt, roles}
│       TTL: 8 hours
│
└── 5. Trả về CreatedSession(rawToken, tokenHash, expiresAt, roles)
```

**File:** `SessionService.java`, `OpaqueTokenService.java`

## Sequence Diagram

```
Browser          NGINX           IAM Service        Keycloak         Redis         PostgreSQL
  │                │                  │                 │               │              │
  │─POST /login───▶│                  │                 │               │              │
  │                │─proxy_pass──────▶│                 │               │              │
  │                │                  │                 │               │              │
  │                │                  │─find user───────┼───────────────┼─────────────▶│
  │                │                  │◀─user data──────┼───────────────┼──────────────│
  │                │                  │                 │               │              │
  │                │                  │─verify pass─────▶│               │              │
  │                │                  │◀─204/401────────│               │              │
  │                │                  │                 │               │              │
  │                │                  │─revoke old──────┼───────────────┼─────────────▶│
  │                │                  │                 │               │              │
  │                │                  │─save session────┼───────────────┼─────────────▶│
  │                │                  │                 │               │              │
  │                │                  │─cache session───┼──────────────▶│              │
  │                │                  │                 │               │              │
  │◀─200 + cookie──│◀─200 + cookie───│                 │               │              │
```

## Cookie chi tiết

### ep_session (Session Token)

| Thuộc tính | Giá trị | Giải thích |
|-----------|---------|-------------|
| Name | `ep_session` (configurable) | Tên cookie |
| Value | 64 hex chars | Opaque token ngẫu nhiên |
| HttpOnly | `true` | JavaScript không đọc được |
| Secure | `false` (local) / `true` (prod) | Chỉ gửi qua HTTPS ở production |
| SameSite | `Strict` | Chống CSRF |
| Path | `/` | Gửi với mọi request |
| Max-Age | `28800` (8 hours) | Session TTL |

### ep_2fa (2FA Challenge Token)

| Thuộc tính | Giá trị | Giải thích |
|-----------|---------|-------------|
| Name | `ep_2fa` | Cookie 2FA challenge |
| Max-Age | `300` (5 minutes) | Challenge hết hạn sau 5 phút |
| Còn lại | Giống ep_session | HttpOnly, Strict, Path=/ |

## Code References

| Layer | Class | Method |
|-------|-------|--------|
| Controller | `AuthController` | `login()` |
| Use Case | `LoginUseCase` | `execute()` |
| Command | `LoginCommand` | record |
| Credential | `KeycloakCredentialVerificationAdapter` | `verify()` |
| Session | `SessionService` | `issueFor()` |
| Token | `OpaqueTokenService` | `generate()`, `hash()` |
| Domain | `SessionRecord` | `issue()` |
| Cache | `RedisSessionCacheAdapter` | `store()` |
| Request DTO | `LoginRequest` | record |
| Response DTO | `LoginResponse` | record |
