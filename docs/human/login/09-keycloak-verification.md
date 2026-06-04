# 09 — Keycloak: Xử lý & Verify Credentials cho Login

> Tài liệu đi sâu vào cách Keycloak hoạt động trong luồng đăng nhập E-Procure — từ User Storage SPI, credential verification, đến tương tác giữa Keycloak Provider và IAM Service.

## Mục lục

- [Tổng quan vai trò của Keycloak](#tổng-quan-vai-trò-của-keycloak)
- [Kiến trúc User Storage SPI](#kiến-trúc-user-storage-spi)
- [Luồng verify credentials chi tiết](#luồng-verify-credentials-chi tiết)
- [EprocureIamClient — HTTP client nội bộ](#eprocureiamclient--http-client-nội-bộ)
- [IAM Internal API endpoints](#iam-internal-api-endpoints)
- [Password hashing strategy](#password-hashing-strategy)
- [Keycloak Realm Configuration](#keycloak-realm-configuration)
- [Docker & Deployment](#docker--deployment)
- [Security considerations](#security-considerations)
- [Troubleshooting](#troubleshooting)

---

## Tổng quan vai trò của Keycloak

Trong E-Procure, Keycloak **không** lưu trữ user data hay password. Keycloak đóng vai trò:

```
┌─────────────────────────────────────────────────────────┐
│                    Keycloak (port 8180)                  │
│                                                         │
│  ┌───────────────────────────────────────────────────┐  │
│  │  EprocureIamUserStorageProvider (User Storage SPI)│  │
│  │                                                   │  │
│  │  • Lookup user → gọi IAM internal API             │  │
│  │  • Verify password → gọi IAM internal API         │  │
│  │  • KHÔNG lưu user vào Keycloak DB                 │  │
│  │  • KHÔNG lưu password                             │  │
│  │  • Read-only, federated                           │  │
│  └───────────────────────────────────────────────────┘  │
│                                                         │
│  Realm: eprocure                                        │
│  Client: eprocure-iam (confidential, direct grant)      │
└─────────────────────────────────────────────────────────┘
         ▲ HTTP calls (X-Internal-Api-Key)
         │
         ▼
┌─────────────────────────────────────────────────────────┐
│              IAM Service (port 8081)                     │
│                                                         │
│  /internal/keycloak/users/{id}        → lookup by ID    │
│  /internal/keycloak/users?login=xxx   → lookup by login │
│  /internal/keycloak/credentials/verify → verify password│
│                                                         │
│  Password: BCrypt(password + userId), salt=12           │
└─────────────────────────────────────────────────────────┘
```

### Tại sao dùng User Storage SPI thay vì sync user?

| Approach | E-Procure chọn | Lý do |
|----------|----------------|-------|
| Sync user to Keycloak DB | ❌ | Duplicated data, sync complexity, stale data risk |
| **User Storage SPI (federated)** | ✅ | Single source of truth (IAM DB), no sync, real-time |
| Direct DB access from Keycloak | ❌ | Tight coupling, security risk |

---

## Kiến trúc User Storage SPI

### Các class chính

```
infra/keycloak/eprocure-keycloak-provider/
├── EprocureIamUserStorageProviderFactory.java   ← Entry point, tạo provider instance
├── EprocureIamUserStorageProvider.java          ← Core: lookup + credential validation
├── EprocureIamClient.java                       ← HTTP client gọi IAM internal API
├── EprocureIamUserAdapter.java                  ← Adapter chuyển data → Keycloak UserModel
├── EprocureUserRepresentation.java              ← Record: id, username, email, fullName, enabled
└── META-INF/services/
    └── org.keycloak.storage.UserStorageProviderFactory  ← SPI registration
```

### Interface implementation

```java
public final class EprocureIamUserStorageProvider implements
        UserStorageProvider,          // Base provider interface
        UserLookupProvider,           // getUserById, getUserByUsername, getUserByEmail
        CredentialInputValidator {    // supportsCredentialType, isValid
    // ...
}
```

| Interface | Method | Vai trò |
|-----------|--------|---------|
| `UserStorageProvider` | `close()` | Lifecycle cleanup |
| `UserLookupProvider` | `getUserById(RealmModel, String)` | Tìm user theo Keycloak StorageId |
| `UserLookupProvider` | `getUserByUsername(RealmModel, String)` | Tìm user theo username |
| `UserLookupProvider` | `getUserByEmail(RealmModel, String)` | Tìm user theo email |
| `CredentialInputValidator` | `supportsCredentialType(String)` | Chỉ hỗ trợ `password` |
| `CredentialInputValidator` | `isValid(RealmModel, UserModel, CredentialInput)` | Verify password qua IAM API |

### SPI Registration

**File:** `META-INF/services/org.keycloak.storage.UserStorageProviderFactory`

```
com.eprocure.keycloak.EprocureIamUserStorageProviderFactory
```

Keycloak sử dụng Java ServiceLoader để discover factory này khi khởi động.

---

## Luồng verify credentials chi tiết

### Bước 1: IAM Service nhận login request

```
POST /api/v1/auth/login
{
  "username": "nguyen.van.a",
  "password": "P@ssw0rd123"
}
```

`AuthController` → `LoginUseCase.execute()`:

```java
// 1. Tìm user trong IAM DB
User user = userRepository.findByUsernameOrEmail(command.username())
        .orElseThrow(() -> new BusinessException(ErrorCode.IAM_001));  // "Invalid credentials"

// 2. Kiểm tra account status
if (user.isLocked() || !user.canLogin()) {
    throw new BusinessException(ErrorCode.IAM_002);  // "Account locked"
}

// 3. Verify credentials qua Keycloak ← ĐÂY LÀ PHẦN CHÍNH
if (!credentialVerificationPort.verify(command.username(), command.password())) {
    throw new BusinessException(ErrorCode.IAM_001);  // "Invalid credentials"
}
```

### Bước 2: KeycloakCredentialVerificationAdapter gọi Keycloak

```java
// IAM Service gọi Keycloak bằng Resource Owner Password Grant
MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
form.add("grant_type", "password");
form.add("client_id", "eprocure-iam");
form.add("client_secret", clientSecret);
form.add("username", username);
form.add("password", password);

keycloakRestClient.post()
    .uri("/realms/{realm}/protocol/openid-connect/token", realm)
    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
    .body(form)
    .retrieve()
    .toBodilessEntity();
// 204 → true, 401 → false
```

**Quan trọng:** Adapter gửi `grant_type=password` nhưng **không cần access token**. Chỉ quan tâm response status:
- `204 No Content` → password đúng
- `401 Unauthorized` → password sai

### Bước 3: Keycloak xử lý User Storage SPI

Khi Keycloak nhận được token request với `grant_type=password`:

```
Keycloak nhận POST /realms/eprocure/protocol/openid-connect/token
│
├── 1. Parse form: username, password, client_id, client_secret
│
├── 2. Validate client (eprocure-iam)
│   ├── Client phải là confidential
│   ├── client_secret phải đúng
│   └── directAccessGrantsEnabled phải = true
│
├── 3. Tìm user — gọi User Storage SPI
│   ├── EprocureIamUserStorageProvider.getUserByUsername(realm, "nguyen.van.a")
│   │   └── EprocureIamClient.findByLogin("nguyen.van.a")
│   │       └── GET http://iam-service:8081/internal/keycloak/users?login=nguyen.van.a
│   │           Header: X-Internal-Api-Key: <shared-key>
│   │
│   └── Trả về EprocureIamUserAdapter (id, username, email, fullName, enabled)
│
├── 4. Verify password — gọi CredentialInputValidator
│   ├── EprocureIamUserStorageProvider.isValid(realm, userModel, credential)
│   │   ├── credential.getType() == "password" ✓
│   │   └── EprocureIamClient.verifyPassword("nguyen.van.a", "P@ssw0rd123")
│   │       └── POST http://iam-service:8081/internal/keycloak/credentials/verify
│   │           Header: X-Internal-Api-Key: <shared-key>
│   │           Body: {"username": "nguyen.van.a", "password": "P@ssw0rd123"}
│   │
│   └── IAM Service verify:
│       ├── Tìm user theo username
│       ├── BCrypt.matches(password + userId, storedHash)
│       └── Trả về {"data": {"valid": true/false}}
│
├── 5. Nếu password đúng → Keycloak tạo token response (200)
│   └── Nhưng IAM Service dùng toBodilessEntity() → chỉ cần 204
│
└── 6. Nếu password sai → Keycloak trả 401
    └── Adapter catch exception → return false
```

### Sequence Diagram đầy đủ

```
Browser        IAM Service       Keycloak           IAM Internal API
  │                │                  │                     │
  │─POST /login───▶│                  │                     │
  │                │                  │                     │
  │                │──find user───────┼─────────────────────▶│
  │                │◀─user data───────┼─────────────────────│
  │                │                  │                     │
  │                │─POST /token──────▶│                     │
  │                │  (grant=password) │                     │
  │                │                  │                     │
  │                │                  │─GET /users?login=───▶│
  │                │                  │◀─user representation│
  │                │                  │                     │
  │                │                  │─POST /credentials/──▶│
  │                │                  │   verify             │
  │                │                  │◀─{valid: true}──────│
  │                │                  │                     │
  │                │◀─204 No Content──│                     │
  │                │                  │                     │
  │                │──create session──┼─────────────────────▶│
  │                │──cache to Redis──┼─────────────────────▶│
  │                │                  │                     │
  │◀─200 + cookie──│                  │                     │
```

---

## EprocureIamClient — HTTP client nội bộ

### Khởi tạo

```java
// EprocureIamUserStorageProviderFactory.create()
EprocureIamClient client = new EprocureIamClient(
    iamBaseUrl,          // từ ENV IAM_PROVIDER_BASE_URL (e.g., http://iam-service:8081)
    internalApiKey,      // từ ENV IAM_INTERNAL_API_KEY
    Duration.ofSeconds(3) // timeout, từ ENV IAM_PROVIDER_TIMEOUT_SECONDS
);
```

### Config priority

```
1. Environment variable (luôn luôn ưu tiên cao nhất)
   ├── IAM_PROVIDER_BASE_URL
   ├── IAM_INTERNAL_API_KEY
   └── IAM_PROVIDER_TIMEOUT_SECONDS

2. Component model config (lưu trong Keycloak DB)
   ├── iamBaseUrl
   ├── internalApiKey
   └── timeoutSeconds

3. Default values
   └── timeoutSeconds = 3
```

**Tại sao env var ưu tiên?** Vì khi deploy Docker, chỉ cần thay `.env` rồi restart Keycloak — không cần update DB config.

### HTTP Client characteristics

```java
this.httpClient = HttpClient.newBuilder()
    .connectTimeout(timeout)   // Default 3s
    .build();
```

- Dùng Java 11+ `HttpClient` (không phải Spring RestClient)
- Mỗi request có header `X-Internal-Api-Key` để authenticate
- Timeout ngắn → fail closed khi IAM unavailable
- **Không retry** — fail fast để tránh cascading failure

### Error handling

```java
// findById / findByLogin
catch (IOException | InterruptedException exception) {
    if (exception instanceof InterruptedException) {
        Thread.currentThread().interrupt();  // Preserve interrupt flag
    }
    return Optional.empty();  // User not found → Keycloak treats as non-existent
}

// verifyPassword
catch (IOException | InterruptedException exception) {
    // ...
    return false;  // Network error → reject login (fail closed)
}
```

**Fail closed pattern:** Khi IAM Service không phản hồi, Keycloak trả về "user not found" / "password invalid" → từ chối login. An toàn hơn là cho phép login khi không verify được.

---

## IAM Internal API endpoints

### 1. GET /internal/keycloak/users/{userId}

Tìm user theo UUID.

```http
GET /internal/keycloak/users/550e8400-e29b-41d4-a716-446655440000
X-Internal-Api-Key: <shared-key>
```

**Response (200):**
```json
{
  "success": true,
  "data": {
    "id": "550e8400-e29b-41d4-a716-446655440000",
    "username": "nguyen.van.a",
    "email": "nguyen.van.a@company.com",
    "fullName": "Nguyễn Văn A",
    "enabled": true
  }
}
```

**Response (404):** User không tồn tại → Keycloak trả `null` từ `getUserById()`

### 2. GET /internal/keycloak/users?login={usernameOrEmail}

Tìm user theo username hoặc email.

```http
GET /internal/keycloak/users?login=nguyen.van.a
X-Internal-Api-Key: <shared-key>
```

**Sử dụng cho cả 2 method:**
- `getUserByUsername(realm, username)` → `findByLogin(username)`
- `getUserByEmail(realm, email)` → `findByLogin(email)`

### 3. POST /internal/keycloak/credentials/verify

Verify password.

```http
POST /internal/keycloak/credentials/verify
X-Internal-Api-Key: <shared-key>
Content-Type: application/json

{
  "username": "nguyen.van.a",
  "password": "P@ssw0rd123"
}
```

**Response (200):**
```json
{
  "success": true,
  "data": {
    "valid": true
  }
}
```

### Security: X-Internal-Api-Key

```java
// KeycloakInternalController — mỗi endpoint đều check
internalApiKeyGuard.verify(internalApiKey);
```

- Header `X-Internal-Api-Key` phải match với `IAM_INTERNAL_API_KEY` env var
- Nếu sai → 401 Unauthorized
- Chỉ internal service-to-service communication, không expose ra ngoài NGINX

---

## Password hashing strategy

### Công thức: BCrypt(password + userId)

```java
// PasswordHashService.java
private String salted(String rawPassword, UUID userId) {
    return rawPassword + userId;
    // Ví dụ: "P@ssw0rd123" + "550e8400-..." = "P@ssw0rd123550e8400-..."
}

public String hash(String rawPassword, UUID userId) {
    return encoder.encode(salted(rawPassword, userId));  // BCrypt rounds = 12
}

public boolean matches(String rawPassword, UUID userId, String passwordHash) {
    return encoder.matches(salted(rawPassword, userId), passwordHash);
}
```

### Tại sao salt bằng userId?

| Approach | Vấn đề |
|----------|--------|
| BCrypt(password) thuần | 2 users cùng password → cùng hash (rainbow table risk) |
| BCrypt(password + randomSalt) | Cần lưu salt riêng → thêm column |
| **BCrypt(password + userId)** | Mỗi user có salt riêng (userId unique), không cần lưu thêm |

**Lưu ý:** BCrypt đã có internal salt, nhưng việc thêm userId tạo thêm lớp phân biệt ngay cả khi 2 user dùng cùng password.

### Verify flow trong VerifyKeycloakCredentialUseCase

```java
// VerifyKeycloakCredentialUseCase.java
public boolean execute(String username, String password) {
    return userRepository.findByUsernameOrEmail(username)
            .filter(User::canLogin)                    // User phải active
            .flatMap(user -> userRepository.findPasswordHashById(user.getId())
                    .map(passwordHash -> passwordHashService.matches(
                            password,
                            user.getId(),              // ← salt = userId
                            passwordHash)))            // ← stored BCrypt hash
            .orElse(false);                            // User not found → false
}
```

---

## Keycloak Realm Configuration

### realm-eprocure.json

```json
{
  "realm": "eprocure",
  "enabled": true,
  "registrationAllowed": false,          // Không cho tự đăng ký
  "resetPasswordAllowed": true,          // Cho phép reset password (qua IAM flow)
  "loginWithEmailAllowed": true,         // Login được bằng email
  "duplicateEmailsAllowed": false,       // Email phải unique
  "sslRequired": "external",             // SSL required cho external access
  "clients": [
    {
      "clientId": "eprocure-iam",
      "publicClient": false,             // Confidential → cần client_secret
      "directAccessGrantsEnabled": true, // ← Cho phép Resource Owner Password Grant
      "standardFlowEnabled": true,       // Authorization code flow (cho OAuth)
      "serviceAccountsEnabled": true     // Service account cho backend
    }
  ],
  "components": {
    "org.keycloak.storage.UserStorageProvider": [
      {
        "name": "eprocure-iam-user-storage",
        "providerId": "eprocure-iam-user-storage",  // ← Match với Factory.getId()
        "config": {
          "enabled": ["true"],
          "priority": ["0"],                         // Highest priority
          "cachePolicy": ["NO_CACHE"],               // Không cache user data
          "iamBaseUrl": ["${IAM_PROVIDER_BASE_URL}"],
          "timeoutSeconds": ["${IAM_PROVIDER_TIMEOUT_SECONDS}"]
        }
      }
    ]
  }
}
```

### Client config quan trọng

| Field | Value | Ý nghĩa |
|-------|-------|---------|
| `publicClient` | `false` | Client phải có `client_secret` khi gọi token endpoint |
| `directAccessGrantsEnabled` | `true` | Cho phép `grant_type=password` (ROPG) |
| `serviceAccountsEnabled` | `true` | Cho phép client_credentials grant |
| `cachePolicy` | `NO_CACHE` | Không cache user lookup → always call IAM API |

### Tại sao NO_CACHE?

Nếu cache user data trong Keycloak:
- User bị khóa ở IAM → Keycloak vẫn cho login (stale cache)
- User đổi email → Keycloak trả email cũ
- **E-Procure quyết định:** IAM là single source of truth, luôn gọi real-time

---

## Docker & Deployment

### Dockerfile

```dockerfile
# infra/keycloak/Dockerfile
FROM quay.io/keycloak/keycloak:latest as builder
# ... build custom providers
COPY eprocure-keycloak-provider/target/*.jar /opt/keycloak/providers/
# ... import realm config
COPY realm-eprocure.json /opt/keycloak/data/import/
```

### Docker Compose

```yaml
keycloak:
  image: eprocure-keycloak:latest
  ports:
    - "8180:8080"          # Custom port (tránh conflict với Approval service)
  environment:
    KEYCLOAK_ADMIN: admin
    KEYCLOAK_ADMIN_PASSWORD: ${KEYCLOAK_ADMIN_PASSWORD}
    KEYCLOAK_CLIENT_SECRET: ${KEYCLOAK_CLIENT_SECRET}
    IAM_PROVIDER_BASE_URL: http://iam-service:8081
    IAM_INTERNAL_API_KEY: ${IAM_INTERNAL_API_KEY}
    IAM_PROVIDER_TIMEOUT_SECONDS: 3
  command: start --import-realm
```

### Startup flow

```
1. Keycloak container starts
2. Load provider JARs from /opt/keycloak/providers/
3. Discover EprocureIamUserStorageProviderFactory via ServiceLoader
4. Import realm-eprocure.json → register provider component
5. Resolve env vars: IAM_PROVIDER_BASE_URL, IAM_INTERNAL_API_KEY
6. Ready to handle requests
```

---

## Security considerations

### 1. Keycloak không biết password

```
Password flow:
User → IAM Service → Keycloak → IAM Internal API → IAM DB
                                       ↑
                              Keycloak chỉ "relay"
                              password đến IAM để verify
```

Keycloak **never** stores, caches, or logs user passwords. Password chỉ đi qua Keycloak để relay đến IAM Service.

### 2. Internal API key protection

- `X-Internal-Api-Key` header được check ở mỗi internal endpoint
- Không expose internal endpoints qua NGINX (chỉ service-to-service)
- Key rotation: thay env var → restart cả Keycloak lẫn IAM Service

### 3. Fail closed behavior

| Scenario | Behavior |
|----------|----------|
| IAM Service down | Keycloak trả "invalid credentials" → login fail |
| IAM Service timeout (3s) | Same as above → login fail |
| IAM Service trả 500 | Same as above → login fail |
| Network error | `IOException` → `return false` → login fail |

Không có fallback, không có cache → luôn fail closed.

### 4. Password không log

```java
// KeycloakCredentialVerificationAdapter
log.warn("[SECURITY] Keycloak credential rejected | status={}",
    exception.getStatusCode().value());
// ← Chỉ log status code, KHÔNG log password hay username
```

```java
// KeycloakInternalController
log.info("[CONTROLLER] POST /internal/keycloak/credentials/verify | userId=internal");
// ← Không log username/password trong request body
```

### 5. ReadOnly User Adapter

```java
// EprocureIamUserAdapter
@Override
public void setUsername(String username) {
    throw new ReadOnlyException("eProcure IAM users are read-only in Keycloak");
}
```

Không cho phép modify user data qua Keycloak. Mọi thay đổi phải qua IAM Service.

---

## Troubleshooting

### Common issues

| Symptom | Nguyên nhân | Cách kiểm tra |
|---------|-------------|---------------|
| Login fail, log "Keycloak credential rejected" | IAM internal API trả lỗi | Check IAM Service logs |
| Login fail, không có Keycloak log | Keycloak timeout | Check `IAM_PROVIDER_TIMEOUT_SECONDS` |
| 401 từ Keycloak | Sai `client_secret` hoặc `directAccessGrantsEnabled=false` | Check realm config |
| "User not found" nhưng user tồn tại | `X-Internal-Api-Key` sai hoặc IAM internal endpoint bị block | Check env vars, SecurityConfig |

### Debug checklist

```bash
# 1. Check Keycloak có nhận provider không
docker logs keycloak 2>&1 | grep -i "eprocure"

# 2. Test IAM internal API trực tiếp
curl -H "X-Internal-Api-Key: $IAM_INTERNAL_API_KEY" \
  http://localhost:8081/internal/keycloak/users?login=testuser

# 3. Test credential verify
curl -X POST \
  -H "X-Internal-Api-Key: $IAM_INTERNAL_API_KEY" \
  -H "Content-Type: application/json" \
  -d '{"username":"testuser","password":"testpass"}' \
  http://localhost:8081/internal/keycloak/credentials/verify

# 4. Check Keycloak realm config
docker exec keycloak cat /opt/keycloak/data/import/realm-eprocure.json | jq .
```

---

## Code References

| Component | Class | File |
|-----------|-------|------|
| Provider Factory | `EprocureIamUserStorageProviderFactory` | `infra/keycloak/.../EprocureIamUserStorageProviderFactory.java` |
| Provider (Core) | `EprocureIamUserStorageProvider` | `infra/keycloak/.../EprocureIamUserStorageProvider.java` |
| HTTP Client | `EprocureIamClient` | `infra/keycloak/.../EprocureIamClient.java` |
| User Adapter | `EprocureIamUserAdapter` | `infra/keycloak/.../EprocureIamUserAdapter.java` |
| User DTO | `EprocureUserRepresentation` | `infra/keycloak/.../EprocureUserRepresentation.java` |
| SPI Registration | `META-INF/services/...` | `infra/keycloak/.../META-INF/services/...` |
| Realm Config | `realm-eprocure.json` | `infra/keycloak/realm-eprocure.json` |
| IAM Credential Adapter | `KeycloakCredentialVerificationAdapter` | `services/iam-service/.../KeycloakCredentialVerificationAdapter.java` |
| IAM Internal Controller | `KeycloakInternalController` | `services/iam-service/.../KeycloakInternalController.java` |
| Verify Use Case | `VerifyKeycloakCredentialUseCase` | `services/iam-service/.../VerifyKeycloakCredentialUseCase.java` |
| Password Hashing | `PasswordHashService` | `services/iam-service/.../PasswordHashService.java` |
| Login Use Case | `LoginUseCase` | `services/iam-service/.../LoginUseCase.java` |
| Auth Controller | `AuthController` | `services/iam-service/.../AuthController.java` |
| Security Config | `SecurityConfig` | `services/iam-service/.../SecurityConfig.java` |
