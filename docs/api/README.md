# API DOCUMENTATION INDEX
## eProcure Enterprise — REST API Reference

---

> **OpenAPI Version:** 3.0.3  
> **Base URL:** `https://{host}/api/v1`  
> **Auth:** HttpOnly Cookie `ep_session` (opaque token)  
> Tất cả spec có thể import vào **Swagger UI**, **Postman**, hoặc **Insomnia**.

---

## 📁 CẤU TRÚC THƯ MỤC

```
docs/api/
├── common/                          ← Quy ước chung (đọc trước)
│   ├── API_CONVENTIONS.md           ← HTTP methods, URL rules, request/response format
│   ├── ERROR_RESPONSE.md            ← Mã lỗi toàn hệ thống theo service
│   ├── PAGINATION.md                ← Offset và cursor pagination
│   ├── AUTHENTICATION.md            ← Login flow, token, 2FA, OAuth Google
│   └── IDEMPOTENCY.md               ← Idempotency-Key, implementation
│
├── iam-service.openapi.yaml         ← Auth, Users, Roles, Delegation, Org Chart
├── purchase-request-service.openapi.yaml  ← PR lifecycle, Catalog, Attachments
├── approval-service.openapi.yaml    ← Inbox, Tasks, Processes, Rules
├── finance-service.openapi.yaml     ← Budget, Purchase Orders, Invoices, Payments
├── inventory-service.openapi.yaml   ← Items, Warehouses, Stock, Goods Receipt
├── vendor-service.openapi.yaml      ← Vendors, AVL, RFQ, Quotes
├── analytics-service.openapi.yaml   ← Dashboards, KPIs, Report Export
├── notification-service.openapi.yaml← In-app, Email, WebSocket/STOMP
├── admin-service.openapi.yaml       ← System Config, Audit Log, Health
└── README.md                        ← File này
```

---

## 🗺️ SERVICE MAP

| Service | File | Port (dev) | Chức năng chính |
|---|---|---|---|
| IAM Service | `iam-service.openapi.yaml` | 8081 | Auth, RBAC, Org, Delegation |
| PR Service | `purchase-request-service.openapi.yaml` | 8082 | Purchase Request lifecycle |
| Approval Engine | `approval-service.openapi.yaml` | 8083 | Camunda BPMN, approval tasks |
| Finance Service | `finance-service.openapi.yaml` | 8084 | Budget, PO, Invoice, Payment |
| Inventory Service | `inventory-service.openapi.yaml` | 8085 | Catalog, Stock, GR |
| Vendor Service | `vendor-service.openapi.yaml` | 8086 | Vendors, RFQ, AVL |
| Analytics Service | `analytics-service.openapi.yaml` | 8087 | Dashboard, KPI, Reports |
| Notification Service | `notification-service.openapi.yaml` | 8088 | Email, In-app, WebSocket |
| Admin Service | `admin-service.openapi.yaml` | 8089 | Config, Audit, Health |

---

## 🔐 PERMISSION QUICK REFERENCE

> Xem danh sách đầy đủ trong `docs/DOMAIN_MODEL.md` và `docs/CODING_GUIDE.md`

| Màn hình / Feature | Permission Code bắt buộc |
|---|---|
| Tạo PR | `PR_CREATE` |
| Xem PR của mình | `PR_VIEW_OWN` |
| Xem tất cả PR | `PR_VIEW_ALL` |
| Duyệt PR cấp Manager | `PR_APPROVE_L1` |
| Duyệt PR cấp Director | `PR_APPROVE_L2` |
| Duyệt PR cấp C-Level | `PR_APPROVE_L3` |
| Duyệt PR bước Finance | `PR_APPROVE_FINANCE` |
| Tạo PO | `PO_CREATE` |
| Gửi PO cho vendor | `PO_SEND_TO_VENDOR` |
| Tạo RFQ | `RFQ_CREATE` |
| Chốt RFQ | `RFQ_AWARD` |
| Nhận hàng | `GR_CREATE` |
| Cấp phát kho | `GR_ISSUE_OUT` |
| Xem dashboard ngân sách | `BUDGET_VIEW_OWN_DEPT` |
| Duyệt vượt ngân sách | `BUDGET_OVERRIDE` |
| Quản lý người dùng | `ADMIN_USER_MANAGE` |
| Quản lý roles | `ADMIN_ROLE_MANAGE` |
| Cấu hình hệ thống | `SYSTEM_CONFIG` |
| Xem audit log | `SYSTEM_AUDIT_VIEW` |

---

## 🚀 QUICKSTART CHO DEVELOPER

### 1. Import vào Swagger UI (local)

```bash
# Chạy Swagger UI bằng Docker
docker run -p 8090:8080 \
  -e SWAGGER_JSON_URL=http://localhost:8082/v3/api-docs \
  swaggerapi/swagger-ui

# Hoặc import file yaml trực tiếp tại swagger.io/tools/swagger-editor
```

### 2. Import vào Postman

```
1. Mở Postman → Import → File
2. Chọn file *.openapi.yaml tương ứng
3. Postman tự tạo Collection với tất cả endpoints
4. Set biến môi trường: base_url, session_cookie
```

### 3. Set up biến môi trường Postman

```json
{
  "base_url": "http://localhost:8080/api/v1",
  "session_cookie": "",
  "idempotency_key": "{{$randomUUID}}"
}
```

### 4. Flow test cơ bản

```
① GET  /auth/public-key          → Lấy RSA public key
② POST /auth/login               → Đăng nhập, lấy cookie
③ GET  /users/me                 → Verify session
④ POST /purchase-requests        → Tạo PR (với Idempotency-Key)
⑤ POST /purchase-requests/{id}/submit → Submit PR
⑥ GET  /approvals/inbox          → Xem inbox (với tài khoản manager)
⑦ POST /approvals/tasks/{id}/approve → Phê duyệt
```

---

## 📋 CHUẨN RESPONSE ENVELOPE

```jsonc
// Thành công
{
  "success": true,
  "code": "PR_CREATED",
  "message": null,
  "data": { ... },
  "meta": null,             // hoặc PageMeta khi phân trang
  "timestamp": "2025-01-15T08:30:00.000Z",
  "requestId": "uuid"
}

// Lỗi
{
  "success": false,
  "code": "PR_002",
  "message": "Ngân sách không đủ. Còn lại: 42,000,000 VND",
  "data": null,
  "details": [              // chỉ có khi validation error
    { "field": "...", "reason": "..." }
  ],
  "meta": null,
  "timestamp": "...",
  "requestId": "..."
}
```

---

## 🔄 KAFKA EVENT TOPICS

| Topic | Publisher | Consumer | Trigger |
|---|---|---|---|
| `procurement.pr.submitted` | PR Service | Approval Engine, Notification | PR được submit |
| `procurement.pr.approved` | Approval Engine | PR Svc, Finance, Notification | Tất cả bước approved |
| `procurement.pr.rejected` | Approval Engine | PR Svc, Notification | Bị từ chối |
| `approval.step.assigned` | Approval Engine | Notification | Task gán cho approver |
| `approval.sla.breached` | Approval Engine | Notification, Admin | Quá hạn SLA |
| `finance.budget.warning` | Finance | Notification | Ngân sách < 20% |
| `procurement.po.issued` | Finance | Inventory, Notification | PO phát hành |
| `inventory.gr.created` | Inventory | Finance (3-way match) | GR hoàn tất |
| `finance.invoice.matched` | Finance | Notification | 3-way match OK |

---

## 🌐 WEBSOCKET

```
URL:      wss://{host}/ws/notifications
Protocol: STOMP over WebSocket
Auth:     Cookie ep_session (browser tự gửi)
Topic:    /user/me/notifications

Xem chi tiết tại: notification-service.openapi.yaml → /ws/info
```

---

## 📊 JASPER REPORT TEMPLATES

| Report | Endpoint | Format |
|---|---|---|
| Spending by Department | `POST /reports/export` type=SPENDING_BY_DEPARTMENT | PDF, Excel |
| Budget vs Plan | `POST /reports/export` type=BUDGET_VS_PLAN | PDF, Excel |
| Vendor Scorecard | `POST /reports/export` type=VENDOR_SCORECARD | PDF, Excel |
| SLA Compliance | `POST /reports/export` type=SLA_COMPLIANCE | PDF, Excel |
| 3-Way Match | `POST /reports/export` type=THREE_WAY_MATCH | Excel |
| Audit Trail | `POST /reports/export` type=AUDIT_TRAIL | Excel |
| PR Summary | `POST /reports/export` type=PR_SUMMARY | PDF, Excel |
| Maverick Spending | `POST /reports/export` type=MAVERICK_SPENDING | PDF, Excel |

---

## 🔗 RELATED DOCUMENTS

| Tài liệu | Đường dẫn |
|---|---|
| Project Brief | `docs/PROJECT_BRIEF.md` |
| Architecture Decision Records | `docs/ADR/ADR_ALL.md` |
| Domain Model | `docs/DOMAIN_MODEL.md` |
| Database Schema | `docs/DATABASE_SCHEMA.md` |
| Coding Guide | `docs/CODING_GUIDE.md` |
| Environment Config | `docs/ENV_CONFIG.md` |
