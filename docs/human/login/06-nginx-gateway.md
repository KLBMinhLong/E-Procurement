# 06 — Cơ chế auth_request tại NGINX Gateway

## Tổng quan

NGINX đóng vai trò **API Gateway** và sử dụng `auth_request` module để bảo vệ các protected routes. Mọi request đến protected endpoint đều phải qua một subrequest xác thực trước khi được forward đến backend service.

## Cấu trúc

```
Client Request
    │
    ▼
┌─────────────────────────────┐
│         NGINX               │
│                             │
│  location /api/v1/auth ─────┼──▶ IAM Service (không cần auth)
│  (login, public-key, etc.)  │
│                             │
│  location /api/v1/users ────┼──▶ auth_request ──▶ IAM Service
│  (protected routes)         │        │              /auth/verify
│                             │        │
│                             │        ├── 200 → proxy_pass + inject headers
│                             │        └── 401 → return 401
│                             │
│  location / { return 404 }  │
└─────────────────────────────┘
```

## auth_request flow

### Bước 1: Client gửi request

```
GET /api/v1/users/me
Cookie: ep_session=a1b2c3d4e5f6...
```

### Bước 2: NGINX nhận request, match location

```nginx
location /api/v1/users {
    set $upstream "${IAM_SERVICE_URL}";
    proxy_pass $upstream;
    include /etc/nginx/conf.d/proxy-protected;
}
```

File `proxy-protected.template` chứa directive `auth_request`.

### Bước 3: NGINX tạo subrequest

```nginx
auth_request /auth_verify;
```

NGINX tự động tạo một **subrequest** nội bộ:
```
GET /auth_verify
Cookie: ep_session=a1b2c3d4e5f6...
```

### Bước 4: Xử lý subrequest

```nginx
location = /auth_verify {
    internal;
    set $auth_upstream "${IAM_SERVICE_URL}/api/v1/auth/verify";
    proxy_pass $auth_upstream;
    proxy_pass_request_body off;        # Không forward body
    proxy_set_header Content-Length "";
    proxy_set_header X-Original-URI $request_uri;
    proxy_set_header Host $host;
    proxy_set_header X-Real-IP $remote_addr;
    proxy_set_header Cookie $http_cookie;  # Forward cookie để IAM đọc session
}
```

### Bước 5: IAM Service xử lý /auth/verify

```java
@GetMapping("/verify")
public ResponseEntity<Void> verify(@AuthenticationPrincipal UserPrincipal principal) {
    if (principal == null) {
        return ResponseEntity.status(401).build();  // Unauthorized
    }
    
    CurrentUserView userView = getCurrentUserUseCase.execute(principal.getId());
    
    return ResponseEntity.ok()
            .header("X-User-ID", userView.id().toString())
            .header("X-Department-ID", deptId)
            .header("X-Username", userView.username())
            .header("X-Full-Name", userView.fullName())
            .header("X-Permissions", permissions)
            .build();
}
```

**Flow nội bộ:**
1. `SessionAuthenticationFilter` đọc cookie `ep_session`
2. `SessionService.authenticate()` xác thực token
3. Nếu hợp lệ → `UserPrincipal` được set vào SecurityContext
4. `@AuthenticationPrincipal` inject UserPrincipal
5. Trả 200 + headers chứa user info

### Bước 6: NGINX xử lý kết quả

**Nếu 200:**
```nginx
# Lưu headers từ subresponse
auth_request_set $auth_user_id $upstream_http_x_user_id;
auth_request_set $auth_department_id $upstream_http_x_department_id;
auth_request_set $auth_username $upstream_http_x_username;
auth_request_set $auth_full_name $upstream_http_x_full_name;
auth_request_set $auth_permissions $upstream_http_x_permissions;

# Inject headers vào request gốc trước khi forward
proxy_set_header X-User-ID $auth_user_id;
proxy_set_header X-Department-ID $auth_department_id;
proxy_set_header X-Username $auth_username;
proxy_set_header X-Full-Name $auth_full_name;
proxy_set_header X-Permissions $auth_permissions;
proxy_set_header X-Api-Key "${NGINX_INTERNAL_API_KEY}";
```

→ Request được forward đến backend service với user context headers.

**Nếu 401:**
```nginx
# Không forward, trả 401 cho client
```

IAM Service xử lý 401 response:
```json
{"success": false, "code": "IAM_003", "message": "Session is invalid or expired"}
```

## Protected vs Public Routes

### Public Routes (không cần auth_request)

```nginx
# Auth endpoints — ai cũng gọi được
location /api/v1/auth {
    set $upstream "${IAM_SERVICE_URL}";
    proxy_pass $upstream;
    include /etc/nginx/conf.d/proxy-common;  # Không có auth_request
}
```

Bao gồm:
- `POST /api/v1/auth/login`
- `POST /api/v1/auth/forgot-password`
- `POST /api/v1/auth/reset-password`
- `GET /api/v1/auth/public-key`
- `GET /api/v1/auth/oauth/google`
- `GET /api/v1/auth/oauth/google/callback`
- `POST /api/v1/auth/two-factor/verify`

### Protected Routes (cần auth_request)

Tất cả các routes khác, bao gồm:
- `/api/v1/users/*`
- `/api/v1/roles/*`
- `/api/v1/permissions/*`
- `/api/v1/delegations/*`
- `/api/v1/org/*`
- `/api/v1/purchase-requests/*`
- `/api/v1/approvals/*`
- `/api/v1/budgets/*`
- `/api/v1/vendors/*`
- `/api/v1/notifications/*`
- `/api/v1/admin/*`
- ... và tất cả business routes

## Headers được inject bởi Gateway

| Header | Nguồn | Giải thích |
|--------|--------|-------------|
| `X-User-ID` | IAM /auth/verify | UUID của user đang đăng nhập |
| `X-Department-ID` | IAM /auth/verify | UUID của department |
| `X-Username` | IAM /auth/verify | Username |
| `X-Full-Name` | IAM /auth/verify | Họ tên đầy đủ |
| `X-Permissions` | IAM /auth/verify | Danh sách permission codes (comma-separated) |
| `X-Api-Key` | NGINX env | Internal API key cho service-to-service |
| `X-Request-ID` | NGINX generated | Request tracing ID |
| `X-Real-IP` | NGINX | Client IP thực |
| `X-Forwarded-For` | NGINX | Proxy chain |

## CORS Configuration

```nginx
# Preflight (OPTIONS)
Access-Control-Allow-Origin: ${CORS_ALLOWED_ORIGINS}
Access-Control-Allow-Credentials: true
Access-Control-Allow-Methods: GET, POST, PUT, PATCH, DELETE, OPTIONS
Access-Control-Allow-Headers: Content-Type, Authorization, X-Request-ID, Idempotency-Key, X-API-Key
Access-Control-Max-Age: 86400

# Actual requests — same headers
Access-Control-Expose-Headers: X-Request-ID, X-Correlation-ID
```

## Security Headers

```nginx
add_header X-Content-Type-Options nosniff always;
add_header X-Frame-Options DENY always;
add_header Referrer-Policy no-referrer always;
```

## Debug Headers (Development only)

```nginx
add_header X-Auth-Status $auth_status always;
add_header X-Auth-User $auth_user_id always;
add_header X-Auth-Dept $auth_department_id always;
add_header X-Proxy-Target $proxy_host always;
```

> **Production:** Xóa các debug headers này.

## Code References

| File | Vai trò |
|------|---------|
| `infra/nginx/templates/default.conf.template` | Server config, location routing |
| `infra/nginx/templates/proxy-protected.template` | auth_request + headers inject |
| `infra/nginx/templates/proxy-common.template` | CORS + proxy headers (không auth) |
| `AuthController.verify()` | Xử lý subrequest, trả user info headers |
| `SessionAuthenticationFilter` | Đọc cookie, xác thực token |
