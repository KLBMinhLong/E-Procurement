# API SPECIFICATION
## eProcure Enterprise — REST API Design

---

> **Version:** 1.0.0  
> **Base URL:** `https://{host}/api/v1`  
> **Auth:** HttpOnly Cookie `ep_session` (opaque token)  
> **Encryption:** Tất cả request/response body được mã hoá (RSA+AES) trong môi trường prod  
> **No HTTP DELETE:** Sử dụng soft delete qua PATCH endpoint  
> **Idempotency:** POST/PUT phải gửi header `Idempotency-Key: <uuid>`

---

## 1. CONVENTIONS

### 1.1 Request/Response Format

```jsonc
// Request (encrypted in prod):
POST /api/v1/purchase-requests
Content-Type: application/json
Idempotency-Key: 550e8400-e29b-41d4-a716-446655440000

{
  "encryptedPayload": "base64...",
  "encryptedAesKey":  "base64..."
}

// Dev (ENCRYPTION_ENABLED=false): plain JSON
{
  "title": "Mua máy tính xách tay",
  "priority": "NORMAL",
  ...
}
```

### 1.2 Standard Response Envelope

```jsonc
// Thành công
{
  "success": true,
  "code": "PR_CREATED",           // Business code, không phải HTTP status
  "data": { ... },
  "meta": {                        // Chỉ có khi phân trang
    "page": 1,
    "size": 20,
    "totalElements": 156,
    "totalPages": 8
  },
  "timestamp": "2025-01-15T08:30:00.000Z",
  "requestId": "trace-id-here"
}

// Lỗi nghiệp vụ (HTTP 4xx)
{
  "success": false,
  "code": "PR_001",               // Mã lỗi theo service (xem Coding Guide)
  "message": "Ngân sách phòng ban không đủ để tạo yêu cầu mua sắm",
  "details": [                    // Validation errors (nullable)
    { "field": "lineItems[0].quantity", "reason": "Số lượng phải lớn hơn 0" }
  ],
  "timestamp": "2025-01-15T08:30:00.000Z",
  "requestId": "trace-id-here"
}
```

### 1.3 Pagination

```
Query params: ?page=1&size=20&sort=createdAt,desc
```

### 1.4 Mã lỗi theo service

| Service | Prefix | Dải mã |
|---|---|---|
| IAM | IAM_ | IAM_001–IAM_099 |
| PR Service | PR_ | PR_001–PR_099 |
| Approval | APR_ | APR_001–APR_099 |
| Finance | FIN_ | FIN_001–FIN_099 |
| Inventory | INV_ | INV_001–INV_099 |
| Vendor | VND_ | VND_001–VND_099 |
| Gateway | GW_ | GW_001–GW_099 |
| System | SYS_ | SYS_001–SYS_099 |

---

## 2. IAM SERVICE APIs

### 2.1 Auth Endpoints

#### POST /api/v1/auth/init-encryption
Lấy RSA Public Key của backend để bắt đầu mã hoá.

```jsonc
// Response 200
{
  "success": true,
  "code": "OK",
  "data": {
    "publicKey": "-----BEGIN PUBLIC KEY-----\n...\n-----END PUBLIC KEY-----",
    "keyVersion": "v2025-01",
    "algorithm": "RSA-2048"
  }
}
```

#### POST /api/v1/auth/login
```jsonc
// Request
{
  "username": "nguyenvana",
  "password": "encrypted_password_here"  // AES encrypted
}

// Response 200 — Set-Cookie: ep_session=...; HttpOnly; Secure; SameSite=Strict
{
  "success": true,
  "code": "LOGIN_SUCCESS",
  "data": {
    "userId": "uuid",
    "fullName": "Nguyễn Văn A",
    "avatarUrl": "https://...",
    "requiresTwoFactor": false   // nếu true → tiếp tục flow 2FA
  }
}

// Response 401 — sai mật khẩu
{ "success": false, "code": "IAM_001", "message": "Thông tin đăng nhập không đúng" }

// Response 423 — tài khoản bị khoá
{ "success": false, "code": "IAM_002", "message": "Tài khoản đã bị khoá sau 5 lần sai" }
```

#### POST /api/v1/auth/two-factor/verify
```jsonc
// Request
{ "code": "123456" }  // TOTP code

// Response 200
{
  "success": true,
  "code": "TWO_FACTOR_OK",
  "data": { "userId": "uuid", "fullName": "..." }
}
```

#### POST /api/v1/auth/logout
```jsonc
// No body needed — reads token from Cookie
// Response 200
{ "success": true, "code": "LOGOUT_SUCCESS" }
// Clears HttpOnly cookie, invalidates token in Redis+DB
```

#### POST /api/v1/auth/refresh
*Token không hết hạn theo thời gian, nhưng sliding window refresh để duy trì session active.*

#### POST /api/v1/auth/forgot-password
Header: `Idempotency-Key`

```jsonc
{ "email": "user@company.com" }
```

#### POST /api/v1/auth/reset-password
Header: `Idempotency-Key`

```jsonc
{ "resetToken": "reset-token-from-email", "newPassword": "...", "confirmPassword": "..." }
```

#### GET /api/v1/auth/oauth/google
*Redirect đến Google OAuth.*

#### GET /api/v1/auth/oauth/google/callback
*Google redirect back — tạo session.*

### 2.2 User Endpoints

#### GET /api/v1/users/me
*Thông tin user hiện tại từ token.*
```jsonc
// Response 200
{
  "data": {
    "id": "uuid",
    "employeeCode": "EMP-2025-00123",
    "username": "nguyenvana",
    "fullName": "Nguyễn Văn A",
    "email": "van.a@company.com",
    "phone": "0912345678",
    "department": { "id": "uuid", "code": "IT", "name": "Phòng CNTT" },
    "roles": ["REQUESTER", "MANAGER"],
    "permissions": ["PR_CREATE", "PR_APPROVE_L1", "..."],
    "avatarUrl": "https://...",
    "twoFactorEnabled": true
  }
}
```

#### GET /api/v1/users?page=1&size=20&dept=IT&status=ACTIVE
*Requires: ADMIN_USER_VIEW*

#### POST /api/v1/users
*Requires: ADMIN_USER_MANAGE*
```jsonc
{
  "employeeCode": "EMP-2025-00124",
  "username": "tranthib",
  "email": "thi.b@company.com",
  "fullName": "Trần Thị B",
  "departmentId": "uuid",
  "orgNodeId": "uuid",
  "roles": ["REQUESTER"]
}
```

#### PUT /api/v1/users/{id}
*Requires: ADMIN_USER_MANAGE*

#### PATCH /api/v1/users/{id}/status
*Requires: ADMIN_USER_MANAGE — khoá/mở khoá tài khoản*
```jsonc
{ "status": "LOCKED", "reason": "Vi phạm chính sách bảo mật" }
```

#### PUT /api/v1/users/me/password
```jsonc
{ "currentPassword": "...", "newPassword": "...", "confirmPassword": "..." }
```

#### PUT /api/v1/users/me/two-factor/enable
```jsonc
// Response: TOTP secret + QR code URL
{ "secret": "BASE32SECRET", "qrCodeUrl": "data:image/png;base64,..." }
```

### 2.3 Role & Permission Endpoints

#### GET /api/v1/roles
*Requires: ADMIN_ROLE_MANAGE*

#### POST /api/v1/roles
*Requires: ADMIN_ROLE_MANAGE*
```jsonc
{ "code": "CISO", "name": "Chief Information Security Officer", "permissions": ["PR_APPROVE_L1", "..."] }
```

#### PUT /api/v1/roles/{code}/permissions
*Requires: ADMIN_ROLE_MANAGE*

#### GET /api/v1/permissions
*Danh sách tất cả permission codes*

#### POST /api/v1/users/{id}/roles
*Gán role cho user. Requires: ADMIN_ROLE_MANAGE*
```jsonc
{ "roles": ["MANAGER", "REQUESTER"] }
```

### 2.4 Delegation Endpoints

#### GET /api/v1/delegations/my
*Uỷ quyền tôi đang có (delegator = me)*

#### POST /api/v1/delegations
*Requires: Authenticated*
```jsonc
{
  "delegateId": "uuid",
  "startAt": "2025-02-01T00:00:00Z",
  "endAt": "2025-02-10T23:59:59Z",
  "maxValue": "50000000",
  "currency": "VND",
  "allowedCategories": ["OFFICE_SUPPLIES", "IT_HARDWARE"],
  "scope": "ALL"
}
```

#### PATCH /api/v1/delegations/{id}/revoke
*Requires: owner của delegation*

### 2.5 Organization Chart Endpoints

#### GET /api/v1/org/departments
*Cây phòng ban*

#### GET /api/v1/org/departments/{id}/members
*Thành viên của phòng ban*

#### GET /api/v1/org/approvers?role=MANAGER&departmentId=uuid
*Lấy approver theo role và phòng ban*

---

## 3. PURCHASE REQUEST SERVICE APIs

#### GET /api/v1/purchase-requests
*Requires: PR_VIEW_OWN (mặc định) | PR_VIEW_ALL*
```
Query: ?status=PENDING_APPROVAL&priority=URGENT&page=1&size=20&sort=createdAt,desc
       &departmentId=uuid&requesterId=uuid&fromDate=2025-01-01&toDate=2025-01-31
       &minAmount=5000000&maxAmount=200000000
```

#### GET /api/v1/purchase-requests/{prNumber}
```jsonc
// Response 200
{
  "data": {
    "id": "uuid",
    "prNumber": "PR-2025-01-00001",
    "title": "Mua laptop Dell XPS 15",
    "justification": "Phục vụ công việc development, máy cũ đã hỏng",
    "priority": "NORMAL",
    "status": "PENDING_APPROVAL",
    "requester": { "id": "uuid", "fullName": "Nguyễn Văn A", "department": "IT" },
    "lineItems": [
      {
        "lineNumber": 1,
        "itemName": "Laptop Dell XPS 15",
        "categoryCode": "IT_HARDWARE",
        "quantity": { "amount": "2", "unit": "cái" },
        "unitPrice": { "amount": "35000000.0000", "currency": "VND" },
        "totalPrice": { "amount": "70000000.0000", "currency": "VND" },
        "isFromCatalog": false,
        "specifications": "Core i7, 16GB RAM, 512GB SSD"
      }
    ],
    "totalAmount": { "amount": "70000000.0000", "currency": "VND" },
    "budgetCheck": {
      "allocated": "500000000",
      "committed": "120000000",
      "spent": "80000000",
      "available": "300000000",
      "status": "PASS"
    },
    "attachments": [
      { "id": "uuid", "fileName": "bao-gia-dell.pdf", "fileSize": 204800, "uploadedAt": "..." }
    ],
    "approvalProcess": {
      "status": "RUNNING",
      "currentStep": 1,
      "steps": [
        {
          "stepIndex": 1,
          "approverRole": "MANAGER",
          "approver": { "id": "uuid", "fullName": "Lê Văn C" },
          "status": "PENDING",
          "slaDeadline": "2025-01-17T17:30:00Z",
          "slaRemainingHours": 6.5
        }
      ]
    },
    "needByDate": "2025-02-15",
    "createdAt": "2025-01-15T08:30:00Z",
    "updatedAt": "2025-01-15T08:30:00Z"
  }
}
```

#### POST /api/v1/purchase-requests
*Requires: PR_CREATE*
```jsonc
{
  "title": "Mua laptop Dell XPS 15",
  "justification": "Phục vụ công việc development, máy cũ hỏng. Cần gấp trong tháng này.",
  "priority": "NORMAL",
  "needByDate": "2025-02-15",
  "lineItems": [
    {
      "itemCode": null,
      "itemName": "Laptop Dell XPS 15",
      "categoryCode": "IT_HARDWARE",
      "quantity": { "amount": "2", "unit": "cái" },
      "unitPrice": { "amount": "35000000", "currency": "VND" },
      "preferredVendorId": null,
      "specifications": "Core i7-13th, 16GB RAM, 512GB SSD, 15.6 inch OLED",
      "glAccountCode": "6002",
      "isFromCatalog": false
    }
  ],
  "attachmentIds": ["uuid1", "uuid2"]
}

// Response 201
{
  "success": true,
  "code": "PR_CREATED",
  "data": { "id": "uuid", "prNumber": "PR-2025-01-00001", "status": "DRAFT" }
}
```

#### POST /api/v1/purchase-requests/{id}/submit
*Kích hoạt budget check, inventory check, rồi submit. Requires: PR_CREATE + owner*
```jsonc
// Response 200 — success
{ "success": true, "code": "PR_SUBMITTED", "data": { "status": "PENDING_APPROVAL" } }

// Response 422 — budget warning (cần confirm)
{
  "success": false,
  "code": "PR_BUDGET_WARNING",
  "message": "Ngân sách còn lại 42 triệu, PR này trị giá 70 triệu (vượt 28 triệu)",
  "data": { "requiresOverrideApproval": true }
}
```

#### POST /api/v1/purchase-requests/{id}/submit-with-override
*Xác nhận submit dù budget warning. Requires: PR_CREATE + owner*

#### PUT /api/v1/purchase-requests/{id}
*Sửa PR ở trạng thái DRAFT hoặc CHANGES_REQUESTED. Requires: PR_EDIT_OWN_DRAFT + owner*

#### PATCH /api/v1/purchase-requests/{id}/cancel
*Requires: PR_CANCEL_OWN + owner + (status in DRAFT, SUBMITTED, CHANGES_REQUESTED)*
```jsonc
{ "reason": "Không còn nhu cầu mua" }
```

#### POST /api/v1/purchase-requests/attachments/upload
*Upload file trước khi tạo PR — trả về attachmentId để dùng khi submit*
```
Content-Type: multipart/form-data
field: file (max 10MB, types: pdf, xlsx, docx, jpg, png)
```

---

## 4. APPROVAL ENGINE APIs

#### GET /api/v1/approvals/inbox
*PR đang chờ tôi duyệt. Requires: PR_APPROVE_L1 | L2 | L3 | FINANCE*
```
Query: ?priority=URGENT&minAmount=5000000&page=1&size=20
```

#### POST /api/v1/approvals/tasks/{taskId}/approve
*Requires: tương ứng role được assign task*
```jsonc
{ "comment": "Đồng ý mua, phù hợp ngân sách phòng" }
```

#### POST /api/v1/approvals/tasks/{taskId}/reject
*Requires: PR_APPROVE_L1 | L2 | L3 | FINANCE*
```jsonc
{ "comment": "Giá quá cao so với thị trường, đề nghị xin thêm báo giá" }
// comment bắt buộc, > 20 ký tự
```

#### POST /api/v1/approvals/tasks/{taskId}/request-changes
*Requires: PR_REQUEST_CHANGES*
```jsonc
{
  "comment": "Cần bổ sung thông số kỹ thuật chi tiết và lý do chọn nhà cung cấp này",
  "requestedFields": ["specifications", "preferredVendorId"]
}
```

#### POST /api/v1/approvals/tasks/{taskId}/forward
*Requires: PR_FORWARD*
```jsonc
{ "forwardToUserId": "uuid", "reason": "Tôi có xung đột lợi ích với nhà cung cấp này" }
```

#### GET /api/v1/approvals/rules
*Requires: ADMIN_APPROVAL_RULE*

#### POST /api/v1/approvals/rules
*Requires: ADMIN_APPROVAL_RULE*

#### PUT /api/v1/approvals/rules/{id}
*Requires: ADMIN_APPROVAL_RULE*

---

## 5. FINANCE SERVICE APIs

#### GET /api/v1/budgets?departmentId=uuid&fiscalYear=2025
*Requires: BUDGET_VIEW_OWN_DEPT | BUDGET_VIEW_ALL*

#### GET /api/v1/budgets/{id}/dashboard
*Real-time budget stats. Requires: BUDGET_VIEW_OWN_DEPT*
```jsonc
{
  "data": {
    "allocated": "500000000",
    "committed": "120000000",
    "spent": "80000000",
    "available": "300000000",
    "availablePercent": 60.0,
    "burnRatePerMonth": "26666666",
    "forecastExhaustedAt": "2025-09-15",
    "topCategories": [
      { "categoryCode": "IT_HARDWARE", "spent": "50000000", "percent": 62.5 }
    ]
  }
}
```

#### POST /api/v1/budgets/{id}/override-approval
*Requires: BUDGET_OVERRIDE*
```jsonc
{ "prId": "uuid", "overrideReason": "Thiết bị cần gấp, ảnh hưởng production", "overrideAmount": "28000000" }
```

#### POST /api/v1/invoices
*Requires: INVOICE_CREATE*

#### POST /api/v1/invoices/{id}/match
*Kích hoạt 3-way match. Requires: INVOICE_MATCH*

#### POST /api/v1/invoices/{id}/approve
*Requires: INVOICE_APPROVE*

#### POST /api/v1/invoices/{id}/confirm-payment
*Requires: PAYMENT_CONFIRM*

---

## 6. INVENTORY SERVICE APIs

#### GET /api/v1/items?category=IT_HARDWARE&q=laptop
*Tìm kiếm catalog. Requires: Authenticated*

#### GET /api/v1/items/{itemCode}/stock
*Kiểm tra tồn kho. Requires: Authenticated*
```jsonc
{ "data": { "itemCode": "IT-001", "quantityOnHand": 5, "unit": "cái", "reorderPoint": 2 } }
```

#### POST /api/v1/goods-receipts
*Requires: GR_CREATE*

#### PUT /api/v1/goods-receipts/{id}
*Requires: GR_CREATE + owner*

#### POST /api/v1/goods-receipts/{id}/complete
*Hoàn tất nhận hàng. Requires: GR_CREATE*

#### POST /api/v1/stock/issue-out
*Cấp phát hàng từ kho cho nhân viên. Requires: GR_ISSUE_OUT*
```jsonc
{
  "prId": "uuid",
  "items": [
    { "itemCode": "IT-001", "quantity": 2, "unit": "cái", "recipientId": "uuid" }
  ]
}
```

---

## 7. VENDOR SERVICE APIs

#### GET /api/v1/vendors?status=APPROVED&category=IT_HARDWARE
*Requires: VENDOR_VIEW*

#### POST /api/v1/vendors
*Requires: VENDOR_CREATE*

#### PUT /api/v1/vendors/{id}
*Requires: VENDOR_EDIT*

#### PATCH /api/v1/vendors/{id}/approve
*Thêm vào AVL. Requires: VENDOR_APPROVE*

#### GET /api/v1/rfq?status=OPEN
*Requires: RFQ_VIEW*

#### POST /api/v1/rfq
*Requires: RFQ_CREATE*

#### POST /api/v1/rfq/{id}/award
*Chốt nhà cung cấp thắng thầu. Requires: RFQ_AWARD*
```jsonc
{ "awardedQuoteId": "uuid", "awardReason": "Giá tốt nhất, năng lực phù hợp" }
```

---

## 8. PURCHASE ORDER APIs

#### GET /api/v1/purchase-orders
*Requires: PO_VIEW_OWN | PO_VIEW_ALL*

#### POST /api/v1/purchase-orders
*Tạo manual PO từ PR đã approved. Requires: PO_CREATE*
```jsonc
{ "prId": "uuid", "vendorId": "uuid", "deliveryAddress": "Kho Hà Nội", "deliveryDeadline": "2025-02-20", "paymentTerms": "NET30" }
```
Finance lấy trusted PR line snapshot từ `/internal/purchase-requests/{id}/po-source` và vendor snapshot từ `/internal/vendors/{id}/po-source`; client không gửi vendor/line snapshot.

#### PATCH /api/v1/purchase-orders/{id}
*Cập nhật PO nháp trước khi gửi. Requires: PO_EDIT*
```jsonc
{ "deliveryAddress": "Kho Hà Nội", "deliveryDeadline": "2025-02-20", "paymentTerms": "NET30" }
```

#### POST /api/v1/purchase-orders/{id}/send
*Gửi PO cho vendor (email). Requires: PO_SEND_TO_VENDOR*

---

## 9. ANALYTICS & REPORT APIs

#### GET /api/v1/dashboard/executive
*Requires: C_LEVEL role | DIRECTOR role*

#### GET /api/v1/dashboard/manager
*Requires: MANAGER role*

#### GET /api/v1/dashboard/purchasing
*Requires: PURCHASING role*

#### GET /api/v1/reports/{reportType}
*Requires: REPORT_VIEW*
```
reportType: spending-by-department | budget-vs-plan | vendor-scorecard | 
            cycle-time | sla-compliance | three-way-match | 
            inventory-pending | rfq-savings | maverick-spending | audit-trail

Query: ?format=pdf|excel&from=2025-01-01&to=2025-03-31&departmentId=uuid
```

---

## 10. NOTIFICATION APIs

#### GET /api/v1/notifications?isRead=false&page=1&size=20
*In-app notifications. Requires: Authenticated*

#### PATCH /api/v1/notifications/{id}/read

#### PATCH /api/v1/notifications/read-all

#### WebSocket: ws://{host}/ws/notifications
*Topic: /user/{userId}/notifications*
*Gửi realtime notification khi có approval action, SLA warning, etc.*

---

## 11. ADMIN APIs

### 11.1 System Config Portal
*Requires: SYSTEM_CONFIG — tài khoản đặc biệt, tách biệt*

#### GET /api/v1/admin/config/services
*Danh sách tất cả service và biến môi trường (giá trị sensitive bị mask)*

#### PUT /api/v1/admin/config/services/{serviceName}
```jsonc
{
  "variables": [
    { "key": "REDIS_HOST", "value": "redis-cluster" },
    { "key": "KAFKA_BOOTSTRAP", "value": "kafka:9092" }
  ]
}
```

#### GET /api/v1/admin/audit-log
*Requires: SYSTEM_AUDIT_VIEW*
```
Query: ?actorId=uuid&entityType=PURCHASE_REQUEST&action=PR_APPROVED&from=2025-01-01
```

#### GET /api/v1/admin/catalog/categories
*Danh mục hàng hoá. Requires: ADMIN_CATALOG_MANAGE*

#### POST /api/v1/admin/catalog/categories

#### POST /api/v1/admin/catalog/items
