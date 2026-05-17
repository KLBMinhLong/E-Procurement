# AUTHENTICATION
## eProcure Enterprise — Xác thực & Phiên đăng nhập

---

## 1. TỔNG QUAN FLOW

```
┌──────────────┐   ①encrypt(RSA+AES)   ┌──────────────┐   ②verify cred   ┌──────────┐
│   Angular    │──── POST /auth/login ──▶│  IAM Service │──── CustomProv ──▶│ Keycloak │
│  (Frontend)  │                         │              │◀── OK ────────────│          │
│              │◀── Set-Cookie: token ──│   ③gen token │                   └──────────┘
│              │    (HttpOnly,Secure)    │   ④save Redis│
└──────────────┘                         └──────────────┘
                                                │
                                         ⑤save DB (backup)

─── Subsequent Requests ─────────────────────────────────────────────────────────

┌──────────────┐  Cookie: ep_session   ┌──────────────┐  X-Api-Key   ┌──────────────┐
│   Angular    │──── ANY Request ──────▶│ API Gateway  │──────────────▶│  Any Service │
│              │                         │ ①verify token│              │  ②check perm │
│              │◀── encrypted response ─│ ②inject roles│◀─────────────│  ③execute    │
└──────────────┘                         └──────────────┘              └──────────────┘
```

---

## 2. ENCRYPTION INIT (Bắt buộc trước login)

```http
### Step 0: Lấy RSA Public Key từ backend
GET /api/v1/auth/public-key

### Response 200
{
  "data": {
    "publicKey": "-----BEGIN PUBLIC KEY-----\nMIIBIjAN...\n-----END PUBLIC KEY-----",
    "keyVersion": "v2025-01",
    "algorithm": "RSA/ECB/OAEPWithSHA-256AndMGF1Padding"
  }
}
```

> FE cache public key (localStorage không được — dùng memory). Refresh khi nhận `SYS_003`.

---

## 3. LOGIN FLOW

### Step 1: FE chuẩn bị payload mã hóa

```typescript
// Angular — auth.service.ts
async login(username: string, password: string): Promise<void> {
  // 1. Lấy public key (từ cache hoặc fetch mới)
  const { publicKey } = await this.getPublicKey();

  // 2. Tạo AES-256-GCM key ngẫu nhiên
  const aesKey = await crypto.subtle.generateKey(
    { name: 'AES-GCM', length: 256 }, true, ['encrypt', 'decrypt']
  );

  // 3. Mã hóa payload bằng AES
  const payload = JSON.stringify({ username, password });
  const iv = crypto.getRandomValues(new Uint8Array(12));
  const encryptedPayload = await crypto.subtle.encrypt(
    { name: 'AES-GCM', iv }, aesKey, new TextEncoder().encode(payload)
  );

  // 4. Mã hóa AES key bằng RSA public key của backend
  const rawAesKey = await crypto.subtle.exportKey('raw', aesKey);
  const encryptedAesKey = await crypto.subtle.encrypt(
    { name: 'RSA-OAEP' }, await importPublicKey(publicKey), rawAesKey
  );

  // 5. Gửi lên backend
  const body = {
    encryptedPayload: btoa(String.fromCharCode(...new Uint8Array(encryptedPayload))),
    encryptedAesKey:  btoa(String.fromCharCode(...new Uint8Array(encryptedAesKey))),
    iv:               btoa(String.fromCharCode(...iv)),
    keyVersion:       'v2025-01'
  };
  await this.http.post('/api/v1/auth/login', body).toPromise();
  // Cookie được set tự động bởi browser (HttpOnly)
}
```

### Step 2: Backend xử lý

```java
// AuthController.java
@PostMapping("/login")
public ResponseEntity<ApiResponse<LoginResponse>> login(
        @RequestBody LoginRequest encryptedRequest,
        HttpServletResponse httpResponse) {

    // 1. Giải mã AES key bằng RSA private key
    byte[] aesKey = rsaService.decrypt(encryptedRequest.getEncryptedAesKey());

    // 2. Giải mã payload bằng AES key
    String plainJson = aesService.decrypt(
        encryptedRequest.getEncryptedPayload(), aesKey, encryptedRequest.getIv()
    );
    LoginCredential credential = objectMapper.readValue(plainJson, LoginCredential.class);

    // 3. Gọi Keycloak Custom Provider để verify
    keycloakService.verify(credential.getUsername(), credential.getPassword());

    // 4. Tạo opaque session token
    String token = tokenService.generateToken();

    // 5. Lưu session vào Redis + DB
    sessionService.save(token, credential.getUsername());

    // 6. Set HttpOnly Cookie
    ResponseCookie cookie = ResponseCookie.from("ep_session", token)
        .httpOnly(true)
        .secure(true)
        .sameSite("Strict")
        .path("/")
        .build();
    httpResponse.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());

    return ResponseEntity.ok(ApiResponse.success("LOGIN_SUCCESS",
        LoginResponse.of(user)));
}
```

### Step 3: Response

```http
HTTP/1.1 200 OK
Set-Cookie: ep_session=a1b2c3d4e5f6...xyz; HttpOnly; Secure; SameSite=Strict; Path=/

{
  "success": true,
  "code": "LOGIN_SUCCESS",
  "data": {
    "userId": "uuid",
    "fullName": "Nguyễn Văn A",
    "avatarUrl": "https://...",
    "requiresTwoFactor": false
  }
}
```

---

## 4. TWO-FACTOR AUTHENTICATION (2FA)

Nếu `requiresTwoFactor: true` trong login response:

```http
### Bước tiếp theo bắt buộc
POST /api/v1/auth/two-factor/verify
Content-Type: application/json
Cookie: ep_session_pending={pending_token}   ← Temporary token, chưa có quyền

{ "code": "123456" }   ← TOTP code từ Google Authenticator

### Thành công → Đổi sang full session token (Set-Cookie lại)
HTTP 200
Set-Cookie: ep_session={full_token}; HttpOnly; Secure; SameSite=Strict

### Sai code
HTTP 401
{ "success": false, "code": "IAM_006", "message": "Mã 2FA không đúng hoặc đã hết hạn" }
```

**Bật 2FA:**
```http
PUT /api/v1/users/me/two-factor/enable
→ Response: { "qrCodeUrl": "otpauth://totp/eProcure:user@co.com?secret=BASE32..." }
→ FE hiển thị QR code để scan bằng Authenticator app

PUT /api/v1/users/me/two-factor/confirm
{ "code": "123456" }   ← Xác nhận lần đầu sau khi scan QR
→ Response: { "backupCodes": ["12345678", "87654321", ...] }  ← 8 backup codes một lần dùng
```

---

## 5. GOOGLE OAUTH LOGIN

```
① FE click "Đăng nhập với Google"
② FE redirect → GET /api/v1/auth/oauth/google
③ Backend redirect → Google OAuth consent screen
④ User đồng ý → Google redirect → GET /api/v1/auth/oauth/google/callback?code=...
⑤ Backend:
   - Exchange code → Google access token
   - Lấy email từ Google
   - Tìm user trong DB bằng email hoặc google_oauth_id
   - Nếu chưa có: tạo account mới (cần department assignment bởi Admin)
   - Tạo opaque token, Set-Cookie
⑥ Backend redirect → FE /dashboard (hoặc /pending-setup nếu account mới)
```

---

## 6. TOKEN MANAGEMENT

### 6.1 Token Format

```
Token = UUID(32 chars without hyphens) + SecureRandom(32 chars alphanumeric)
      = 64 chars tổng

Ví dụ: a1b2c3d4e5f6789012345678901234566f7a8b9c0d1e2f3a4b5c6d7e8f9a0b1c2

KHÔNG phải JWT — không decode được — không chứa thông tin.
```

### 6.2 Session Data trong Redis

```
Key:   session:{token}
Value: {
  "userId": "uuid",
  "username": "nguyenvana",
  "sessionId": "uuid",    ← ID của session trong DB
  "createdAt": "...",
  "lastActivity": "..."
}
TTL: Sliding window — reset mỗi request (mặc định 8h không hoạt động → expire)
```

### 6.3 Role-Permission Cache

```
Key:   user-perm:{userId}
Value: ["PR_CREATE", "PR_APPROVE_L1", "PO_VIEW_OWN", ...]
TTL:   15 phút (refresh khi role thay đổi)

Key:   role-perm:{roleCode}
Value: ["PR_CREATE", "PR_EDIT_OWN_DRAFT", ...]
TTL:   1 giờ
```

### 6.4 Single Session Enforcement

```
Khi user đăng nhập từ thiết bị/trình duyệt mới:
1. Backend tìm tất cả session active của userId trong DB
2. Set is_active = false, invalidated_by = 'NEW_LOGIN' cho session cũ
3. Xóa key Redis của token cũ: DEL session:{old_token}
4. Xóa user-perm cache: DEL user-perm:{userId}
5. Tạo session mới → Set-Cookie token mới

Effect: Device cũ sẽ nhận IAM_005 trong request tiếp theo.
```

### 6.5 Token Verification Flow (mỗi request)

```java
// Gateway / TokenVerificationFilter
public void verify(HttpServletRequest request) {
    // 1. Đọc token từ cookie
    String token = cookieUtils.extract(request, "ep_session");
    if (token == null) throw new UnauthorizedException("IAM_003");

    // 2. Lookup Redis (fast path)
    SessionData session = redisClient.get("session:" + token);
    if (session == null) {
        // 3. Fallback: kiểm tra DB (slow path, hiếm khi xảy ra)
        session = sessionRepo.findByToken(token)
            .filter(s -> s.isActive())
            .orElseThrow(() -> new UnauthorizedException("IAM_005"));
        // Restore Redis cache
        redisClient.set("session:" + token, session, Duration.ofHours(8));
    }

    // 4. Lấy permissions từ cache
    Set<String> permissions = permissionCache.get(session.getUserId());

    // 5. Inject vào SecurityContext
    SecurityContextHolder.getContext().setAuthentication(
        new EprocureAuthentication(session, permissions)
    );

    // 6. Slide TTL
    redisClient.expire("session:" + token, Duration.ofHours(8));
}
```

---

## 7. LOGOUT

```http
POST /api/v1/auth/logout
Cookie: ep_session={token}

### Backend:
# 1. Đọc token từ cookie
# 2. Set session.is_active = false, invalidated_by = 'LOGOUT' trong DB
# 3. DEL session:{token} khỏi Redis
# 4. DEL user-perm:{userId} khỏi Redis
# 5. Clear cookie

HTTP 200
Set-Cookie: ep_session=; HttpOnly; Secure; Max-Age=0; Path=/
{ "success": true, "code": "LOGOUT_SUCCESS" }
```

---

## 8. FORGOT PASSWORD

```http
### Bước 1: Gửi email reset
POST /api/v1/auth/forgot-password
{ "email": "van.a@company.com" }

→ HTTP 200 (luôn trả 200 dù email có tồn tại hay không — tránh user enumeration)
{ "success": true, "code": "RESET_EMAIL_SENT",
  "message": "Nếu email tồn tại, bạn sẽ nhận được hướng dẫn trong vài phút" }

### Backend tạo reset token (UUID, TTL 15 phút) → gửi email qua Brevo SMTP

### Bước 2: Đặt lại mật khẩu
POST /api/v1/auth/reset-password
{
  "resetToken": "token-from-email",
  "newPassword": "StrongPass123!",
  "confirmPassword": "StrongPass123!"
}

→ Thành công: HTTP 200 + invalidate tất cả session cũ
→ Token hết hạn: HTTP 401, code: "IAM_007"
```

---

## 9. PASSWORD POLICY

```
Tối thiểu 8 ký tự
Chứa ít nhất: 1 chữ hoa, 1 chữ thường, 1 số, 1 ký tự đặc biệt
Không chứa username
Không được trùng 3 mật khẩu gần nhất
Hash: BCrypt(password + userId_salt, cost=12)
      userId_salt = userId.toString() (đảm bảo cùng password ≠ cùng hash cross-user)
```

---

## 10. SECURITY HEADERS (NGINX)

```nginx
# nginx.conf — security headers
add_header Strict-Transport-Security "max-age=31536000; includeSubDomains; preload" always;
add_header X-Frame-Options "DENY" always;
add_header X-Content-Type-Options "nosniff" always;
add_header X-XSS-Protection "1; mode=block" always;
add_header Referrer-Policy "strict-origin-when-cross-origin" always;
add_header Content-Security-Policy "default-src 'self'; script-src 'self'; style-src 'self' 'unsafe-inline'; img-src 'self' data: https:; connect-src 'self' wss:;" always;
add_header Permissions-Policy "geolocation=(), microphone=(), camera=()" always;

# Disable weak protocols
ssl_protocols TLSv1.2 TLSv1.3;
ssl_prefer_server_ciphers on;
ssl_ciphers ECDHE-ECDSA-AES256-GCM-SHA384:ECDHE-RSA-AES256-GCM-SHA384:...;

# Disable server version disclosure
server_tokens off;
```
