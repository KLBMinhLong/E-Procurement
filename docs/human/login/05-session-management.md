# 05 — Quản lý vòng đời Session

## Tổng quan

Session trong E-Procure sử dụng **Opaque Token** pattern — token ngẫu nhiên, hash bằng SHA-256, lưu ở cả Redis (cache) và PostgreSQL (persistent).

## Tạo Session (Issue)

### Khi nào tạo session mới?

1. **Standard login** — user login thành công, không có 2FA
2. **2FA verify** — user xác thực OTP thành công
3. **Google OAuth** — user login qua Google, không có 2FA

### Quy trình tạo session

```
SessionService.issueFor(user, clientContext)
│
├── 1. Tạo opaque token
│   ├── rawToken = SecureRandom(32 bytes) → 64 hex chars
│   └── tokenHash = SHA-256(rawToken) → 64 hex chars
│
├── 2. Revoke tất cả session cũ của user
│   ├── Tìm active sessions trong DB
│   ├── Xóa cache Redis cho mỗi session
│   └── Đánh dấu revoked trong DB
│   → Mỗi user chỉ có MỘT session active tại một thời điểm
│
├── 3. Tạo SessionRecord mới
│   SessionRecord {
│     id: UUID,
│     userId: UUID,
│     tokenHash: "a1b2c3..." (64 hex),
│     ipAddress: "192.168.1.1",
│     userAgent: "Mozilla/5.0...",
│     issuedAt: Instant,
│     expiresAt: Instant.now() + 8h
│   }
│
├── 4. Lưu vào PostgreSQL (iam.sessions)
│
├── 5. Cache vào Redis
│   key: iam:session:<token_hash>
│   value: {tokenHash, userId, expiresAt, roles}
│   TTL: 8 hours
│
└── 6. Trả về CreatedSession(rawToken, tokenHash, expiresAt, roles)
```

### Database Schema (iam.sessions)

```sql
CREATE TABLE iam.sessions (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES iam.users(id),
    token_hash CHAR(64) NOT NULL,        -- SHA-256 hex
    ip_address INET NULL,
    user_agent TEXT NULL,
    issued_at TIMESTAMPTZ NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    is_revoked BOOLEAN NOT NULL DEFAULT FALSE,
    revoked_at TIMESTAMPTZ NULL,
    revoked_by UUID NULL,
    -- soft delete columns...
);
```

### Redis Cache

| Key | Value | TTL |
|-----|-------|-----|
| `iam:session:<hash>` | `SessionData{tokenHash, userId, expiresAt, roles}` | 8h |

## Xác thực Session (Authenticate)

### Khi nào cần xác thực?

- Mọi request đến **protected routes** (qua NGINX auth_request)
- Frontend gọi `GET /api/v1/users/me` để hydrate user context

### Quy trình xác thực

```
SessionService.authenticate(rawToken)
│
├── 1. Hash rawToken → tokenHash
│
├── 2. Check Redis (L1 cache)
│   ├── sessionCachePort.findByTokenHash(tokenHash)
│   ├── Nếu tìm thấy và còn active → trả về UserPrincipal
│   └── Nếu miss hoặc expired → fallback xuống DB
│
├── 3. Check PostgreSQL (L2 persistent)
│   ├── sessionRepository.findActiveByTokenHash(tokenHash, now)
│   ├── Nếu tìm thấy:
│   │   ├── Load roles từ user_roles
│   │   ├── Cache lại vào Redis
│   │   └── Trả về UserPrincipal
│   └── Nếu không tìm thấy → trả về Optional.empty()
│
└── 4. Tạo UserPrincipal
    UserPrincipal {
      id: UUID,
      username: String,
      fullName: String,
      tokenHash: String,
      permissions: Set<String>  // resolved từ roles
    }
```

### Session Authentication Filter

```
SessionAuthenticationFilter (extends OncePerRequestFilter)
│
├── Chạy MỖI request (trước UsernamePasswordAuthenticationFilter)
│
├── Lấy cookie "ep_session" từ request
│
├── Nếu có cookie:
│   └── sessionService.authenticate(rawToken)
│       ├── Nếu hợp lệ → set SecurityContext authentication
│       └── Nếu không hợp lệ → tiếp tục (để SecurityConfig xử lý 401)
│
└── Nếu không có cookie → tiếp tục (anonymous)
```

**File:** `SessionAuthenticationFilter.java`

## Hủy Session (Revoke)

### Khi nào revoke?

1. **User logout** — explicit logout
2. **User login lần sau** — revoke session cũ tự động
3. **Admin revoke** — admin force logout user
4. **Password change** — revoke tất cả sessions

### Quy trình revoke

```
SessionService.revoke(rawToken, revokedBy)
│
├── 1. Hash rawToken → tokenHash
│
├── 2. Xóa cache Redis
│   └── sessionCachePort.evict(tokenHash)
│
└── 3. Đánh dấu revoked trong DB
    └── sessionRepository.revokeByTokenHash(tokenHash, revokedBy, now)
        SET is_revoked = TRUE,
            revoked_at = now,
            revoked_by = revokedBy
```

### Revoke tất cả sessions của user

```
SessionService.revokeActiveForUser(userId, revokedBy, now)
│
├── 1. Tìm tất cả active token hashes của user
│   └── sessionRepository.findActiveTokenHashesByUserId(userId, now)
│
├── 2. Xóa cache Redis cho mỗi session
│
└── 3. Đánh dấu revoked trong DB
    └── sessionRepository.revokeActiveByUserId(userId, revokedBy, now)
```

## Session TTL

| Config | Default | Giải thích |
|--------|---------|-------------|
| `eprocure.session.ttl-hours` | `8` | Session hết hạn sau 8 giờ |

## Single Session Policy

Hệ thống áp dụng **single session per user**:
- Khi user login mới → tất cả session cũ bị revoke
- User chỉ có thể login từ 1 thiết bị/browser tại một thời điểm
- Login từ thiết bị mới sẽ tự động kick session cũ

## Code References

| Layer | Class | Method |
|-------|-------|--------|
| Service | `SessionService` | `issueFor()`, `authenticate()`, `revoke()` |
| Token | `OpaqueTokenService` | `generate()`, `hash()` |
| Domain | `SessionRecord` | `issue()`, `isActive()` |
| Cache | `RedisSessionCacheAdapter` | `store()`, `findByTokenHash()`, `evict()` |
| Repository | `SessionRepository` (MyBatis) | `save()`, `findActiveByTokenHash()` |
| Filter | `SessionAuthenticationFilter` | `doFilterInternal()` |
| Principal | `UserPrincipal` | `from()` |
