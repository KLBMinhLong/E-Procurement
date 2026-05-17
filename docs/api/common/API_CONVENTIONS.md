# API CONVENTIONS
## eProcure Enterprise — Quy ước chung cho toàn bộ REST API

---

> **Version:** 1.0.0  
> Tài liệu này là **nguồn chân lý duy nhất** cho mọi quy ước API.  
> Mọi service PHẢI tuân thủ — không có ngoại lệ nếu không có ADR kèm theo.

---

## 1. BASE URL & VERSIONING

```
https://{host}/api/v1/{resource}

Ví dụ:
  https://api.eprocure.internal/api/v1/purchase-requests
  https://api.eprocure.internal/api/v1/approvals/tasks/{id}/approve
```

- **Versioning**: URL path (`/v1/`). Khi breaking change → tạo `/v2/`, giữ `/v1/` song song tối thiểu 3 tháng.
- **Host**: Cấu hình qua biến môi trường `API_BASE_URL`.
- **Protocol**: HTTPS bắt buộc cho mọi môi trường (kể cả dev nội bộ nếu có thể).

---

## 2. HTTP METHODS & SOFT DELETE

| Method | Mục đích | Idempotent | Body |
|---|---|---|---|
| `GET` | Lấy dữ liệu | ✅ | ❌ |
| `POST` | Tạo mới / Action | ❌ (cần Idempotency-Key) | ✅ |
| `PUT` | Cập nhật toàn bộ | ✅ (với Idempotency-Key) | ✅ |
| `PATCH` | Cập nhật một phần / đổi trạng thái | ❌ (cần Idempotency-Key) | ✅ |

> ❌ **`DELETE` method KHÔNG ĐƯỢC DÙNG** trong bất kỳ service nào.  
> Thay thế bằng `PATCH /{resource}/{id}/cancel` hoặc `PATCH /{resource}/{id}/deactivate`.

### Soft Delete Pattern

```http
# Hủy/Xóa mềm một PR
PATCH /api/v1/purchase-requests/{id}/cancel
Content-Type: application/json

{ "reason": "Không còn nhu cầu" }

# Response 200
{ "success": true, "code": "PR_CANCELLED" }
```

---

## 3. URL NAMING RULES

```
✅ Danh từ số nhiều cho resource
GET /api/v1/purchase-requests          ✅
GET /api/v1/purchaseRequest            ❌
GET /api/v1/getPurchaseRequests        ❌

✅ Lowercase, hyphen-separated
GET /api/v1/vendor-quotes              ✅
GET /api/v1/vendorQuotes               ❌
GET /api/v1/vendor_quotes              ❌

✅ Hành động = verb ở cuối resource path
POST /api/v1/purchase-requests/{id}/submit          ✅
POST /api/v1/purchase-requests/{id}/cancel          ✅
POST /api/v1/approvals/tasks/{id}/approve           ✅
POST /api/v1/approvals/tasks/{id}/reject            ✅
POST /api/v1/users/{id}/roles/assign                ✅

✅ Sub-resource cho quan hệ rõ ràng
GET /api/v1/purchase-requests/{id}/line-items       ✅
GET /api/v1/purchase-requests/{id}/attachments      ✅
GET /api/v1/approvals/processes/{id}/steps          ✅
```

---

## 4. REQUEST FORMAT

### 4.1 Headers bắt buộc

```http
Content-Type: application/json
Accept: application/json
Cookie: ep_session={opaque_token}        (bắt buộc cho mọi request cần auth)
Idempotency-Key: {uuid-v4}              (bắt buộc cho POST, PUT, PATCH)
X-Request-ID: {uuid-v4}                 (optional, dùng để trace nếu client muốn set)
Accept-Language: vi                      (vi | en, mặc định vi)
```

### 4.2 Body (Prod với encryption)

```jsonc
// Khi ENCRYPTION_ENABLED=true (production)
{
  "encryptedPayload": "base64-encoded-AES-encrypted-body",
  "encryptedAesKey":  "base64-encoded-RSA-encrypted-AES-key",
  "keyVersion":       "v2025-01"         // version của RSA public key đã dùng
}

// Khi ENCRYPTION_ENABLED=false (local/dev)
// Gửi plain JSON trực tiếp
{
  "title": "Mua laptop",
  ...
}
```

### 4.3 Query Parameters

```
Naming: camelCase cho query params
?pageNumber=1&pageSize=20               ❌ (inconsistent)
?page=1&size=20&sort=createdAt,desc     ✅

Filter params: snake_case hoặc camelCase nhất quán trong 1 service
?department_id=uuid&status=ACTIVE       ✅ (snake_case — dùng thống nhất toàn hệ thống)
?min_amount=5000000&max_amount=200000000 ✅

Date/time filter:
?from_date=2025-01-01&to_date=2025-03-31   ✅ (ISO 8601 date)
?from_time=2025-01-01T00:00:00Z&to_time=...  ✅ (ISO 8601 datetime UTC)

Search:
?q=laptop+dell                           ✅ (full-text search)
```

---

## 5. RESPONSE FORMAT

### 5.1 Standard Response Envelope

**Mọi response đều được bọc trong envelope này** — không có ngoại lệ.

```jsonc
// ✅ Thành công — có data
{
  "success": true,
  "code": "PR_CREATED",           // Business success code
  "message": null,                 // null khi success, hoặc message mô tả
  "data": { ... },                 // Payload chính
  "meta": null,                    // null khi không phân trang
  "timestamp": "2025-01-15T08:30:00.000Z",
  "requestId": "550e8400-e29b-41d4-a716-446655440000"
}

// ✅ Thành công — có phân trang
{
  "success": true,
  "code": "OK",
  "data": [ ... ],
  "meta": {
    "page": 1,
    "size": 20,
    "totalElements": 156,
    "totalPages": 8,
    "isFirst": true,
    "isLast": false,
    "sort": "createdAt,desc"
  },
  "timestamp": "...",
  "requestId": "..."
}

// ✅ Thành công — không có data (action)
{
  "success": true,
  "code": "PR_SUBMITTED",
  "message": "Yêu cầu mua sắm đã được gửi thành công",
  "data": null,
  "meta": null,
  "timestamp": "...",
  "requestId": "..."
}

// ❌ Lỗi
{
  "success": false,
  "code": "PR_002",
  "message": "Ngân sách phòng ban không đủ. Còn lại: 42,000,000 VND, cần: 70,000,000 VND",
  "data": null,
  "details": [                     // Chỉ có khi validation error
    { "field": "lineItems[0].quantity", "reason": "Số lượng phải lớn hơn 0" }
  ],
  "meta": null,
  "timestamp": "...",
  "requestId": "..."
}
```

### 5.2 HTTP Status Codes

| Code | Khi nào | Ví dụ |
|---|---|---|
| `200 OK` | GET thành công, action thành công | Lấy danh sách PR, duyệt PR |
| `201 Created` | POST tạo mới thành công | Tạo PR mới |
| `202 Accepted` | Request nhận được, xử lý async | Submit RFQ (Kafka) |
| `204 No Content` | **KHÔNG DÙNG** — luôn trả envelope | — |
| `400 Bad Request` | Request malformed, field sai kiểu | JSON parse error |
| `401 Unauthorized` | Chưa đăng nhập / token invalid | Cookie thiếu |
| `403 Forbidden` | Đã đăng nhập nhưng không có quyền | Thiếu permission |
| `404 Not Found` | Resource không tồn tại | PR không tìm thấy |
| `409 Conflict` | Trùng lặp hoặc state conflict | Submit PR đã submit |
| `422 Unprocessable Entity` | Validation nghiệp vụ thất bại | Budget không đủ |
| `423 Locked` | Tài khoản bị khóa | 5 lần sai mật khẩu |
| `429 Too Many Requests` | Rate limit | > 100 req/min |
| `500 Internal Server Error` | Lỗi hệ thống | DB connection fail |
| `503 Service Unavailable` | Circuit breaker open | Finance service down |

---

## 6. DATA TYPES

```
UUID:          "550e8400-e29b-41d4-a716-446655440000"  (lowercase, with hyphens)
Money:         "70000000.0000"                          (string, 4 decimal places)
Currency:      "VND"                                    (ISO 4217, 3 chars)
Timestamp:     "2025-01-15T08:30:00.000Z"              (ISO 8601, UTC, milliseconds)
Date:          "2025-01-15"                             (ISO 8601 date)
Enum values:   "PENDING_APPROVAL"                       (UPPER_SNAKE_CASE string)
Boolean:       true / false                             (JSON native)
Page index:    1-based                                  (page=1 là trang đầu)
```

> ⚠️ **Tiền tệ luôn là string trong JSON**, không phải number. Lý do: tránh floating point precision loss khi parse.

---

## 7. FIELD NAMING

```jsonc
// ✅ camelCase cho mọi JSON field
{
  "prNumber": "PR-2025-01-00001",     ✅
  "createdAt": "2025-01-15T...",      ✅
  "lineItems": [ ... ],               ✅
  "totalAmount": "70000000.0000",     ✅
  "isDeleted": false                  ✅
}

// ❌ Không dùng
{
  "pr_number": "...",                 ❌ snake_case
  "PRNumber": "...",                  ❌ PascalCase
  "created-at": "..."                 ❌ kebab-case
}
```

---

## 8. INTER-SERVICE COMMUNICATION

### 8.1 Đồng bộ (REST qua API Gateway)

```http
# Gateway → Service: x-api-key được gắn tự động
GET /api/v1/budgets/{id}
X-Api-Key: {internal-api-key}         (Gateway inject, service validate)
X-Request-ID: {trace-id}             (propagate cho distributed tracing)
X-User-ID: {userId}                  (Gateway inject sau khi verify token)
X-User-Roles: MANAGER,REQUESTER     (Gateway inject)
```

### 8.2 Bất đồng bộ (Kafka Events)

```json
// Kafka message envelope
{
  "eventId":     "uuid",
  "eventType":   "PR_SUBMITTED",
  "version":     "1.0",
  "source":      "pr-service",
  "timestamp":   "2025-01-15T08:30:00.000Z",
  "traceId":     "otel-trace-id",
  "payload":     { ... }
}
```

---

## 9. FILE UPLOAD

```http
POST /api/v1/purchase-requests/attachments/upload
Content-Type: multipart/form-data

# Fields
file:        <binary>          (max 10MB)
description: "Báo giá từ Dell" (optional)

# Allowed MIME types
application/pdf
application/vnd.openxmlformats-officedocument.spreadsheetml.sheet   (xlsx)
application/vnd.openxmlformats-officedocument.wordprocessingml.document (docx)
image/jpeg
image/png

# Response 201
{
  "data": {
    "attachmentId": "uuid",
    "fileName": "bao-gia-dell.pdf",
    "fileSize": 204800,
    "mimeType": "application/pdf",
    "uploadedAt": "2025-01-15T..."
  }
}
```

---

## 10. RATE LIMITING

```http
# Response headers luôn có
X-RateLimit-Limit:     100          (tối đa req/phút)
X-RateLimit-Remaining: 87           (còn lại trong cửa sổ hiện tại)
X-RateLimit-Reset:     1705300200   (Unix timestamp khi reset)

# Khi vượt rate limit — 429
{
  "success": false,
  "code": "GW_001",
  "message": "Vượt giới hạn request. Vui lòng thử lại sau 30 giây",
  "data": { "retryAfterSeconds": 30 }
}
```

Giới hạn:
- User endpoint: 100 req/phút/user
- Upload endpoint: 10 req/phút/user
- Auth/login: 10 req/phút/IP
- Inter-service: 1000 req/phút/service
