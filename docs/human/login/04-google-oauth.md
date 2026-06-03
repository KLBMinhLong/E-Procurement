# 04 — Luồng đăng nhập Google OAuth 2.0

## Tổng quan

Hệ thống hỗ trợ đăng nhập qua **Google OAuth 2.0 Authorization Code Flow**. Người dùng có thể liên kết tài khoản Google với tài khoản E-Procure để đăng nhập nhanh.

## Prerequisites

- User đã liên kết Google account (có `google_id` trong `iam.users`)
- Hoặc admin đã tạo user với Google OAuth info

## Endpoints

| Endpoint | Method | Mô tả |
|----------|--------|-------|
| `/api/v1/auth/oauth/google` | GET | Bắt đầu OAuth flow → redirect đến Google |
| `/api/v1/auth/oauth/google/callback` | GET | Google callback → xử lý code, tạo session |

## Luồng chi tiết

### Bước 1: Frontend khởi tạo

```
User click "Đăng nhập với Google"
    ↓
Frontend redirect browser đến:
    GET /api/v1/auth/oauth/google
```

### Bước 2: IAM Service tạo authorization URL

```
StartGoogleOAuthUseCase.execute()
│
├── Tạo random state token (CSRF protection)
├── Lưu state vào cookie: ep_oauth_state (TTL 5 phút)
├── Tạo Google authorization URL:
│   https://accounts.google.com/o/oauth2/v2/auth?
│     client_id=<google_client_id>
│     &redirect_uri=<callback_url>
│     &response_type=code
│     &scope=openid email profile
│     &state=<random_state>
│     &access_type=offline
│
└── Trả về 302 redirect đến Google
```

**Set-Cookie:**
- `ep_oauth_state=<state-token>; HttpOnly; SameSite=Lax; Max-Age=300`

**File:** `StartGoogleOAuthUseCase.java`

### Bước 3: User xác nhận trên Google

- Google hiển thị consent screen (nếu lần đầu)
- User chọn account và đồng ý cấp quyền
- Google redirect về callback URL với `code` và `state`

### Bước 4: Xử lý callback

```
GET /api/v1/auth/oauth/google/callback?code=<auth_code>&state=<state>
```

```
HandleGoogleOAuthCallbackUseCase.execute(GoogleOAuthCallbackCommand)
│
├── 1. Validate state (CSRF protection)
│   ├── Lấy state từ cookie (ep_oauth_state)
│   ├── So sánh với state từ query param
│   └── Nếu không khớp → throw error
│
├── 2. Exchange authorization code lấy tokens
│   ├── POST https://oauth2.googleapis.com/token
│   │   grant_type=authorization_code
│   │   code=<auth_code>
│   │   client_id=<google_client_id>
│   │   client_secret=<google_client_secret>
│   │   redirect_uri=<callback_url>
│   │
│   └── Nhận: access_token, id_token
│
├── 3. Lấy user info từ Google
│   ├── Decode id_token hoặc gọi Google People API
│   └── Lấy: google_id, email, name, picture
│
├── 4. Tìm hoặc tạo user trong hệ thống
│   ├── Tìm theo google_id trong iam.users
│   ├── Nếu không có → tìm theo email
│   ├── Nếu vẫn không có → throw IAM_030 (Google user not found)
│   └── Nếu tìm thấy → liên kết google_id (nếu chưa có)
│
├── 5. Kiểm tra 2FA
│   ├── user.isTwoFactorEnabled()?
│   │   ├── YES → tạo 2FA challenge, redirect về frontend với ?requiresTwoFactor=true
│   │   └── NO → tạo session, redirect về frontend
│   │
│   └── Clear oauth state cookie
│
└── 6. Redirect về frontend
    ├── Nếu 2FA: http://localhost:4200/login?requiresTwoFactor=true
    ├── Nếu thành công: http://localhost:4200/
    └── Nếu lỗi: http://localhost:4200/login?error=<error_code>
```

**File:** `HandleGoogleOAuthCallbackUseCase.java`, `GoogleOAuthCallbackCommand.java`

## Sequence Diagram

```
Browser        IAM Service       Google           Redis         PostgreSQL
  │                │                │                │              │
  │─click "Google"─▶│                │                │              │
  │                │                │                │              │
  │◀─302 redirect──│                │                │              │
  │ Location:      │                │                │              │
  │ accounts.google│                │                │              │
  │ Set-Cookie:    │                │                │              │
  │ ep_oauth_state │                │                │              │
  │                │                │                │              │
  │─Google consent─▶│                │                │              │
  │                │                │                │              │
  │◀─redirect──────┼────────────────│                │              │
  │ /callback?code │                │                │              │
  │ &state=xxx     │                │                │              │
  │                │                │                │              │
  │─callback──────▶│                │                │              │
  │                │                │                │              │
  │                │─validate state─┼────────────────│              │
  │                │─exchange code──▶│                │              │
  │                │◀─tokens────────│                │              │
  │                │─get user info─▶│                │              │
  │                │◀─user info─────│                │              │
  │                │                │                │              │
  │                │─find user──────┼────────────────┼─────────────▶│
  │                │◀─user data─────┼────────────────┼──────────────│
  │                │                │                │              │
  │                │─create session─┼───────────────▶│              │
  │                │─save session───┼────────────────┼─────────────▶│
  │                │                │                │              │
  │◀─302 redirect──│                │                │              │
  │ /login or /    │                │                │              │
```

## Error Handling

| Code | Nguyên nhân | Redirect URL |
|------|-------------|--------------|
| `IAM_030` | Google account chưa liên kết với user nào | `/login?error=IAM_030` |
| `IAM_002` | Account bị khóa | `/login?error=IAM_002` |
| `SYS_001` | Lỗi hệ thống không xác định | `/login?error=SYS_001` |
| State mismatch | CSRF attack hoặc cookie hết hạn | `/login?error=<code>` |

## Cookie chi tiết

### ep_oauth_state

| Thuộc tính | Giá trị |
|-----------|---------|
| Name | `ep_oauth_state` |
| HttpOnly | `true` |
| Secure | tùy env |
| SameSite | `Lax` (vì là redirect flow) |
| Max-Age | `300` (5 phút) |

> **Lưu ý:** SameSite phải là `Lax` (không phải `Strict`) vì request đến từ redirect bên ngoài (Google).

## Google Cloud Console Configuration

- **Authorized redirect URIs:** `http://localhost:8081/api/v1/auth/oauth/google/callback`
- **Scopes:** `openid`, `email`, `profile`
- **Access type:** `offline`

## Code References

| Layer | Class | Method |
|-------|-------|--------|
| Controller | `AuthController` | `startGoogleOAuth()`, `handleGoogleOAuthCallback()` |
| Use Case | `StartGoogleOAuthUseCase` | `execute()` |
| Use Case | `HandleGoogleOAuthCallbackUseCase` | `execute()` |
| Command | `GoogleOAuthCallbackCommand` | record |
| Redirect | `GoogleOAuthRedirect` | record |
| Adapter | `GoogleOAuthAdapter` | `exchangeCode()`, `getUserInfo()` |
