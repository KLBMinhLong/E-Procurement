# 01 — Tổng quan kiến trúc đăng nhập

## Mô hình tổng thể

```
┌─────────────┐     ┌─────────────┐     ┌─────────────────┐
│   Browser   │────▶│   NGINX     │────▶│  IAM Service    │
│  (Angular)  │     │  (Gateway)  │     │   (port 8081)   │
└─────────────┘     └──────┬──────┘     └────────┬────────┘
                           │                     │
                    auth_request          ┌──────┴──────┐
                    subrequest            │             │
                                     ┌────▼───┐   ┌────▼───┐
                                     │Keycloak│   │ Redis  │
                                     │(8180)  │   │ (6379) │
                                     └────────┘   └────────┘
                                          │
                                     ┌────▼────────┐
                                     │ PostgreSQL   │
                                     │ (db_iam)     │
                                     └─────────────┘
```

## Nguyên tắc cốt lõi

### 1. Stateless Authentication
- Hệ thống **không dùng server-side session** (Spring Session).
- Thay vào đó dùng **Opaque Token** — token ngẫu nhiên 32 bytes, hash bằng SHA-256.
- Token gốc (raw token) chỉ nằm trong **HttpOnly cookie**, không bao giờ expose cho JavaScript.

### 2. Credential Delegation
- IAM Service **không tự verify password**.
- Delegate việc verify credentials cho **Keycloak** qua `Resource Owner Password Grant`.
- IAM chỉ quản lý: user profile, session, roles, permissions, 2FA.

### 3. Two-Layer Session Store
- **Redis** (L1 cache): Session data cho tra cứu nhanh (~ms).
- **PostgreSQL** (L2 persistent): `iam.sessions` table cho durability.
- Khi authenticate: check Redis trước → miss thì query DB → cache lại vào Redis.

### 4. Gateway-Level Protection
- Mọi request đến protected routes đều qua **NGINX auth_request** subrequest.
- NGINX gọi `GET /api/v1/auth/verify` trên IAM Service.
- Nếu 200 → forward request + inject headers (X-User-ID, X-Permissions...).
- Nếu 401 → trả 401 cho client.

## Các phương thức đăng nhập

| Method | Mô tả | 2FA Support |
|--------|-------|-------------|
| **Username/Password** | Form login truyền thống | ✅ |
| **Google OAuth 2.0** | Đăng nhập qua Google account | ✅ |

## Luồng tổng quát

```
1. User nhập credentials
2. Frontend gửi POST /api/v1/auth/login
3. IAM Service:
   a. Tìm user trong DB (by username hoặc email)
   b. Kiểm tra user status (ACTIVE, không LOCKED)
   c. Verify credentials qua Keycloak
   d. Nếu 2FA enabled → trả challenge token, chờ OTP
   e. Nếu không 2FA → tạo session, trả session cookie
4. Frontend nhận cookie → redirect đến dashboard
5. Mọi request tiếp theo → NGINX auth_request xác thực session
```

## Xem thêm

- [02-standard-login.md](02-standard-login.md) — Chi tiết luồng Username/Password
- [03-two-factor-auth.md](03-two-factor-auth.md) — Chi tiết luồng 2FA
- [04-google-oauth.md](04-google-oauth.md) — Chi tiết luồng Google OAuth
