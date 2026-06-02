# 07 — Luồng đăng nhập phía Angular Frontend

## Tổng quan

Frontend Angular xử lý đăng nhập thông qua `AuthService`, `LoginComponent`, và các route guards. Cookie-based authentication — frontend không lưu token, chỉ quản lý cookie qua HTTP response.

## Components & Services

### LoginComponent

**Path:** `features/auth/login/login.component.ts`

Trang login UI xử lý:
- Form username/password
- 2FA OTP input (hiển thị sau khi server trả `requiresTwoFactor: true`)
- Google OAuth redirect
- Error handling (account locked, invalid credentials, etc.)

### AuthService

**Path:** `core/auth/auth.service.ts`

Service quản lý authentication state:
- `currentUser` signal — user context hiện tại
- `isHydrating` / `isFullyHydrated` signals — trạng thái hydrate
- `login()`, `logout()`, `verifyTwoFactor()`, `hydrateUserContext()`

### Route Guards

| Guard | File | Vai trò |
|-------|------|---------|
| `authGuard` | `auth.guard.ts` | Bảo vệ protected routes — redirect về `/login` nếu chưa đăng nhập |
| `loginGuard` | `login.guard.ts` | Bảo vệ login page — redirect về `/dashboard` nếu đã đăng nhập |

## Luồng chi tiết

### Standard Login (không 2FA)

```
1. User mở /login
   └── loginGuard kiểm tra session
       ├── Đã đăng nhập → redirect /dashboard
       └── Chưa đăng nhập → hiển thị form

2. User nhập username + password, click "Đăng nhập"
   │
   ├── LoginComponent.submit()
   │   ├── Validate form
   │   ├── Set isSubmitting = true
   │   └── Gọi authService.login({username, password})
   │
   ├── AuthService.login()
   │   ├── POST /api/v1/auth/login
   │   │   Content-Type: application/json
   │   │   Idempotency-Key: <uuid>
   │   │   Body: {username, password}
   │   │
   │   └── Response:
   │       {userId, fullName, avatarUrl, requiresTwoFactor: false}
   │
   │   → Backend set ep_session cookie trong response
   │
   ├── Nếu success:
   │   ├── Set currentUser signal (tạm thời, chưa có permissions)
   │   └── router.navigate(['/dashboard'])
   │
   └── Nếu error:
       ├── 423 / IAM_002 → errorKey = 'auth.login.accountLocked'
       └── Other → errorKey = 'auth.login.failed'

3. Đến /dashboard
   └── authGuard canActivate
       ├── isFullyHydrated = false → gọi hydrateUserContext()
       │
       ├── AuthService.hydrateUserContext()
       │   ├── GET /api/v1/users/me
       │   │   Cookie: ep_session=...
       │   │
       │   └── Response: UserContext đầy đủ
       │       {id, username, fullName, roles, permissions, department, ...}
       │
       ├── Set currentUser = response.data
       ├── Set isFullyHydrated = true
       └── return true → cho phép truy cập
```

### Login với 2FA

```
1. POST /api/v1/auth/login → response có requiresTwoFactor: true
   │
   ├── LoginComponent nhận response
   │   ├── Set requiresTwoFactor = true
   │   ├── Hiển thị OTP form
   │   └── Backend đã set ep_2fa cookie
   │
2. User nhập OTP code (6 digits hoặc backup code XXXX-XXXX)
   │
   ├── LoginComponent.verifyOtp()
   │   ├── Validate form (pattern: 6 digits hoặc XXXX-XXXX)
   │   └── Gọi authService.verifyTwoFactor(code)
   │
   ├── AuthService.verifyTwoFactor()
   │   ├── POST /api/v1/auth/two-factor/verify
   │   │   Cookie: ep_2fa=<challenge-token>
   │   │   Body: {code: "123456"}
   │   │
   │   └── Response: UserSummaryView
   │       {id, fullName, roles, ...}
   │
   │   → Backend set ep_session cookie, clear ep_2fa cookie
   │
   ├── Nếu success:
   │   ├── Set currentUser (từ 2FA response)
   │   └── router.navigate(['/dashboard'])
   │
   └── Nếu error:
       └── errorKey = 'auth.login.twoFactorInvalid'

3. User có thể cancel 2FA → quay lại form login
   └── cancelTwoFactor()
       ├── Set requiresTwoFactor = false
       └── Reset OTP form
```

### Google OAuth Login

```
1. User click "Đăng nhập với Google"
   │
   ├── LoginComponent.loginWithGoogle()
   │   └── window.location.assign(authService.googleLoginUrl())
   │       → Redirect đến: /api/v1/auth/oauth/google
   │
2. Browser redirect đến Google → user chọn account
   │
3. Google redirect về: /api/v1/auth/oauth/google/callback?code=xxx&state=yyy
   │
   ├── Backend xử lý callback
   │   ├── Exchange code → tokens
   │   ├── Tìm user trong DB
   │   ├── Nếu có 2FA → redirect: /login?requiresTwoFactor=true
   │   └── Nếu không → redirect: / (homepage)
   │
4. Nếu redirect về /login?requiresTwoFactor=true
   └── LoginComponent.ngOnInit()
       ├── Đọc query param requiresTwoFactor=true
       ├── Set requiresTwoFactor = true
       └── Hiển thị OTP form

5. Nếu redirect về / (homepage)
   └── authGuard → hydrateUserContext → vào dashboard

6. Nếu redirect về /login?error=IAM_030
   └── LoginComponent.ngOnInit()
       └── errorKey = 'auth.login.googleUserNotFound'
```

## Route Configuration

```typescript
const routes: Routes = [
  {
    path: 'login',
    canActivate: [loginGuard],  // Đã login → redirect dashboard
    component: LoginComponent
  },
  {
    path: 'dashboard',
    canActivate: [authGuard],   // Chưa login → redirect login
    component: DashboardComponent
  },
  // ... other protected routes
];
```

## Cookie Handling

Frontend **không trực tiếp quản lý cookie**. Cookie được set bởi backend qua `Set-Cookie` header và browser tự động gửi lại trong mỗi request.

```
┌─────────────┐                    ┌─────────────┐
│   Browser   │                    │   Backend   │
│             │                    │             │
│ POST /login─┼───────────────────▶│             │
│             │                    │             │
│◀────────────┼────────────────────│ Set-Cookie: │
│             │                    │ ep_session= │
│             │                    │ (HttpOnly)  │
│             │                    │             │
│ GET /users  │ Cookie: ep_session │             │
│ ────────────┼───────────────────▶│             │
│ (tự động)   │                    │             │
```

## API Service & Interceptors

Frontend sử dụng `ApiService` wrapper với các features:
- Auto-prefix API base URL
- Error interceptor (handle 401 → redirect login)
- `BYPASS_ERROR_INTERCEPTOR` context cho một số requests (hydrate, reset password)
- Idempotency-Key header generation

## Error Handling

| Scenario | Frontend behavior |
|----------|-------------------|
| 401 từ API | Redirect về `/login` (qua error interceptor) |
| 423 (locked) | Hiển thị "Account locked" message |
| Network error | Hiển thị generic error message |
| 2FA code sai | Hiển thị "Invalid code" message |
| Google OAuth fail | Hiển thị error từ query param |

## Code References

| File | Vai trò |
|------|---------|
| `features/auth/login/login.component.ts` | Login page UI + logic |
| `features/auth/login/login.component.html` | Login page template |
| `core/auth/auth.service.ts` | Authentication state management |
| `core/auth/auth.guard.ts` | Protected route guard |
| `core/auth/login.guard.ts` | Login page guard |
| `core/models/user-context.model.ts` | TypeScript interfaces |
| `core/http/api.service.ts` | HTTP client wrapper |
| `core/http/encryption.service.ts` | RSA/AES encryption (production) |
