# 03 — Luồng xác thực 2 yếu tố (TOTP)

## Tổng quan

Hệ thống hỗ trợ **TOTP (Time-based One-Time Password)** theo RFC 6238 cho xác thực 2 yếu tố. Người dùng có thể dùng app authenticator (Google Authenticator, Authy, etc.) hoặc backup code.

## Điều kiện kích hoạt

- User phải enable 2FA trước (qua `PUT /api/v1/users/me/two-factor/enable`).
- `iam.users.two_factor_enabled = TRUE`
- `iam.users.two_factor_secret_encrypted` đã được lưu (mã hóa AES).

## Luồng 2FA

### Phase 1: Đăng nhập ban đầu

```
POST /api/v1/auth/login
{
  "username": "nguyen.van.a",
  "password": "P@ssw0rd123"
}
```

**Response (yêu cầu 2FA):**
```json
{
  "success": true,
  "data": {
    "userId": "...",
    "fullName": "Nguyễn Văn A",
    "avatarUrl": null,
    "requiresTwoFactor": true
  }
}
```

**Set-Cookie:**
- `ep_2fa=<challenge-token>; HttpOnly; Max-Age=300` (5 phút)
- `ep_session=; Max-Age=0` (clear session cookie)

### Phase 2: Xác thực OTP

```
POST /api/v1/auth/two-factor/verify
Content-Type: application/json
Cookie: ep_2fa=<challenge-token>

{
  "code": "123456"           // 6-digit TOTP code
}
```

**Hoặc dùng backup code:**
```json
{
  "code": "ABCD-EFGH"       // Backup code format: XXXX-XXXX
}
```

**Response (thành công):**
```json
{
  "success": true,
  "data": {
    "id": "...",
    "employeeCode": "EMP001",
    "username": "nguyen.van.a",
    "fullName": "Nguyễn Văn A",
    "email": "nguyen.van.a@company.com",
    "avatarUrl": null,
    "department": { "id": "...", "code": "IT", "name": "IT Department" },
    "roles": ["PROCUREMENT_STAFF"],
    "status": "ACTIVE"
  }
}
```

**Set-Cookie:**
- `ep_session=<session-token>; HttpOnly; Max-Age=28800`
- `ep_2fa=; Max-Age=0` (clear challenge cookie)

## Chi tiết xử lý phía Backend

### VerifyTwoFactorUseCase

```
VerifyTwoFactorUseCase.execute(VerifyTwoFactorCommand)
│
├── 1. Idempotency check
│   └── idempotencyGuard.verify(idempotencyKey)
│
├── 2. Tìm 2FA challenge từ cookie
│   └── challengeService.findByRawToken(challengeToken)
│       ├── Hash challenge token → tìm trong Redis
│       ├── Kiểm tra challenge còn active (chưa hết hạn)
│       └── Nếu không tìm thấy → throw IAM_006
│
├── 3. Tìm user
│   └── userRepository.findById(challenge.userId())
│       ├── Kiểm tra user.canLogin()
│       └── Nếu không → throw IAM_003
│
├── 4. Xác thực OTP
│   ├── Kiểm tra code format:
│   │   ├── Có dấu "-" → Backup code
│   │   └── 6 digits → TOTP code
│   │
│   ├── Nếu là TOTP code:
│   │   ├── Giải mã encrypted secret (AES)
│   │   ├── totpService.verifyCode(secret, code)
│   │   └── So sánh với window ±1 step (30s mỗi step)
│   │
│   └── Nếu là Backup code:
│       ├── Hash backup code
│       ├── So sánh với danh sách backup codes đã hash
│       ├── Nếu match → consume (xóa khỏi danh sách)
│       └── Lưu lại danh sách backup codes còn lại
│
├── 5. Tạo session
│   └── sessionService.issueFor(user, clientContext)
│       (giống luồng standard login)
│
├── 6. Cập nhật lastLoginAt
│
├── 7. Evict 2FA challenge
│   └── challengeService.evict(challengeToken)
│
└── 8. Trả về TwoFactorVerificationResult(userView, rawToken)
```

**File:** `VerifyTwoFactorUseCase.java`

### TwoFactorChallengeService

```
TwoFactorChallengeService
│
├── createFor(user, clientContext)
│   ├── Tạo opaque token (32 bytes random)
│   ├── Hash token → SHA-256
│   ├── Tạo TwoFactorChallengeData:
│   │   {tokenHash, userId, ipAddress, userAgent, expiresAt}
│   ├── Lưu vào Redis: key=iam:2fa:<hash>, TTL=5 phút
│   └── Trả về rawToken
│
├── findByRawToken(rawToken)
│   ├── Hash rawToken → SHA-256
│   ├── Tìm trong Redis
│   ├── Kiểm tra isActive (chưa hết hạn)
│   └── Trả về TwoFactorChallengeData
│
└── evict(rawToken)
    └── Xóa key Redis
```

**File:** `TwoFactorChallengeService.java`

## Backup Codes

Khi enable 2FA, hệ thống tạo **8 backup codes**:
- Format: `XXXX-XXXX` (8 ký tự alphanumeric, loại bỏ ambiguous chars)
- Hash bằng SHA-256 trước khi lưu vào DB
- Mỗi backup code chỉ dùng được **1 lần** (consume sau khi verify)
- Lưu trong `iam.users.two_factor_backup_codes_hash` (JSON array)

## TOTP Configuration

| Parameter | Giá trị | Giải thích |
|-----------|---------|-------------|
| Algorithm | SHA-1 | Mặc định TOTP (Google Authenticator compatible) |
| Digits | 6 | 6-digit code |
| Period | 30 seconds | Mỗi code hợp lệ 30 giây |
| Window | ±1 step | Chấp nhận code của step trước/sau |
| Issuer | `eProcure` | Tên hiển thị trong authenticator app |

## 2FA Challenge Cache (Redis)

| Key pattern | Value | TTL |
|-------------|-------|-----|
| `iam:2fa:<token_hash>` | TwoFactorChallengeData JSON | 5 phút |

## Error Codes

| Code | HTTP | Nguyên nhân |
|------|------|-------------|
| `IAM_006` | 401 | 2FA code không hợp lệ hoặc challenge hết hạn |
| `IAM_003` | 401 | Session không hợp lệ |
| `IAM_002` | 423 | Account bị khóa |

## Sequence Diagram

```
Browser          IAM Service          Redis           PostgreSQL
  │                  │                   │                │
  │─POST /login─────▶│                   │                │
  │ (user có 2FA)    │                   │                │
  │◀─200 + 2FA cookie│                   │                │
  │                  │                   │                │
  │─POST /2fa/verify▶│                   │                │
  │ Cookie: ep_2fa   │                   │                │
  │                  │─find challenge────▶│                │
  │                  │◀─challenge data───│                │
  │                  │                   │                │
  │                  │─get user secret───┼───────────────▶│
  │                  │◀─encrypted secret─┼───────────────│
  │                  │                   │                │
  │                  │─verify TOTP code  │                │
  │                  │ (internal)        │                │
  │                  │                   │                │
  │                  │─create session───▶│                │
  │                  │─save session──────┼───────────────▶│
  │                  │                   │                │
  │                  │─evict challenge──▶│                │
  │                  │                   │                │
  │◀─200 + session───│                   │                │
```

## Code References

| Layer | Class | Method |
|-------|-------|--------|
| Controller | `AuthController` | `verifyTwoFactor()` |
| Use Case | `VerifyTwoFactorUseCase` | `execute()` |
| Command | `VerifyTwoFactorCommand` | record |
| Challenge | `TwoFactorChallengeService` | `createFor()`, `findByRawToken()` |
| TOTP | `TotpService` | `verifyCode()`, `hashBackupCode()` |
| Cipher | `TotpSecretCipher` | `decrypt()` |
| Cache | `TwoFactorChallengeCachePort` | `store()`, `findByTokenHash()` |
| Session | `SessionService` | `issueFor()` |
