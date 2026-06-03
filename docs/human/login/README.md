# Login Flow — Documentation Index

> Tài liệu chi tiết luồng đăng nhập hệ thống E-Procure Enterprise.

## Mục lục

| # | File | Nội dung |
|---|------|----------|
| 1 | [01-overview.md](01-overview.md) | Tổng quan kiến trúc đăng nhập |
| 2 | [02-standard-login.md](02-standard-login.md) | Luồng đăng nhập Username/Password |
| 3 | [03-two-factor-auth.md](03-two-factor-auth.md) | Luồng xác thực 2 yếu tố (TOTP) |
| 4 | [04-google-oauth.md](04-google-oauth.md) | Luồng đăng nhập Google OAuth 2.0 |
| 5 | [05-session-management.md](05-session-management.md) | Quản lý vòng đời Session |
| 6 | [06-nginx-gateway.md](06-nginx-gateway.md) | Cơ chế auth_request tại NGINX Gateway |
| 7 | [07-frontend-flow.md](07-frontend-flow.md) | Luồng đăng nhập phía Angular Frontend |
| 8 | [08-security-layers.md](08-security-layers.md) | Tổng hợp các lớp bảo mật |

## Dịch vụ liên quan

| Service | Vai trò trong login |
|---------|---------------------|
| **IAM Service** (8081) | Xác thực, phân quyền, quản lý session, 2FA, OAuth |
| **Keycloak** (8180) | Credential verification (delegate password check) |
| **Redis** (6379) | Session cache, 2FA challenge cache, permission cache |
| **PostgreSQL** (5432) | Persist session, user, roles, permissions |
| **NGINX** (443/80) | API Gateway + auth_request subrequest |
| **Angular Frontend** (4200) | UI login, route guards, token cookie handling |

## Stack liên quan

- **Spring Security** — Stateless session, filter chain
- **Opaque Token** — Random 32-byte hex, SHA-256 hash stored in DB/Redis
- **TOTP** — Time-based One-Time Password cho 2FA (RFC 6238)
- **HttpOnly Cookie** — Session token không expose cho JavaScript
- **NGINX auth_request** — Subrequest xác thực cho mọi protected route
