# project-context.md
## eProcure Enterprise — Master Context cho AI Agent

> **Đọc file này TRƯỚC KHI làm bất cứ việc gì trong project.**  
> File này là "bộ nhớ nền" của toàn bộ hệ thống.

---

## 1. DỰ ÁN LÀ GÌ?

**eProcure** là hệ thống số hoá quy trình mua sắm nội bộ (E-Procurement) cho doanh nghiệp từ 200+ nhân viên. Thay thế email/Excel/phê duyệt miệng bằng workflow tự động.

**Luồng nghiệp vụ cốt lõi:**
```
Nhân viên tạo PR → Phê duyệt đa cấp (Camunda BPMN) → Thu mua tạo PO →
Vendor giao hàng → Kho nhận hàng (GR) → Kế toán đối soát 3 chiều → Thanh toán
```

---

## 2. KIẾN TRÚC — GHI NHỚ NGAY

```
Microservices + Clean Architecture + Event-Driven (Kafka)

Services:
  iam-service        :8081  → Auth, RBAC, Org, Session
  pr-service         :8082  → Purchase Request lifecycle
  approval-service   :8083  → Camunda BPMN workflow
  finance-service    :8084  → Budget, PO, Invoice, 3-way match
  inventory-service  :8085  → Catalog, Stock, Goods Receipt
  vendor-service     :8086  → Vendor, RFQ, AVL
  analytics-service  :8087  → Dashboard, KPI, JasperReport
  notification-svc   :8088  → Email (Brevo), In-app, WebSocket
  admin-service      :8089  → System config, Audit log

Infrastructure:
  PostgreSQL 15  → Multi-DB, multi-schema (không dùng 'public')
  Redis 7        → Session, permission cache, idempotency
  Kafka          → Event bus async
  Keycloak       → Chỉ verify credential — KHÔNG lưu user data
  Camunda BPMN 7 → Embedded trong approval-service
```

**Clean Architecture layers (mọi service):**
```
presentation → application → domain ← infrastructure
Controller     UseCase        POJO      RepositoryImpl/Kafka/Redis
```

---

## 3. CÁC QUYẾT ĐỊNH THIẾT KẾ QUAN TRỌNG NHẤT

| Quyết định | Lý do | Hệ quả |
|---|---|---|
| **Opaque token** (không JWT) | Revoke ngay lập tức, không decode được | Mỗi request phải Redis lookup |
| **1 session duy nhất** | Bảo mật, login mới → invalidate cũ | Cần cleanup Redis khi login |
| **Không có HTTP DELETE** | Audit trail, soft delete toàn hệ thống | Dùng PATCH .../cancel hoặc .../deactivate |
| **MyBatis** (không JPA) | Kiểm soát SQL, tránh N+1, Clean Arch | Domain POJO không có ORM annotation |
| **RSA+AES hybrid** | Defense-in-depth dù đã có HTTPS | Interceptor tự động, toggle bằng ENV |
| **RBAC permission code** | Không hardcode role trong code | `@PreAuthorize("hasAuthority('PR_CREATE')")` |
| **Idempotency-Key** | Tránh duplicate khi retry/double-click | Mọi POST/PUT/PATCH phải xử lý |
| **BigDecimal cho tiền** | Tránh floating point error | DB: NUMERIC(19,4), API: string |
| **TIMESTAMPTZ** | Tránh timezone confusion | Lưu UTC, FE convert sang local |

---

## 4. DATABASE LAYOUT — PHÂN CHIA SCHEMA

```
db_iam          → schema: iam       (users, roles, permissions, sessions, delegations)
db_procurement  → schema: pr        (purchase_requests, pr_line_items, catalog)
                → schema: approval  (approval_processes, approval_steps, rules)
db_finance      → schema: finance   (budgets, purchase_orders, invoices, payments)
db_inventory    → schema: inventory (items, stock_entries, goods_receipts, movements)
db_vendor       → schema: vendor    (vendors, rfqs, quotes)
db_notification → schema: notification (notifications, templates)
db_audit        → schema: audit     (audit_logs — immutable, partitioned by year)
db_camunda      → schema: camunda   (managed by Camunda auto-schema)
```

**Quy tắc DB bất biến:**
- Mọi entity table: `is_deleted BOOLEAN DEFAULT false` + `deleted_at` + `deleted_by`
- Mọi SELECT chính: `WHERE is_deleted = false`
- Tiền: `NUMERIC(19,4)` — không bao giờ FLOAT/DOUBLE
- Timestamp: `TIMESTAMPTZ` — không bao giờ TIMESTAMP

---

## 5. AUTH FLOW — HIỂU ĐỂ KHÔNG LÀM SAI

```
FE                    Gateway              IAM Service          Keycloak
│                        │                      │                   │
│── POST /auth/login ───▶│                      │                   │
│  (RSA+AES encrypted)   │──── forward ────────▶│                   │
│                        │                      │── verify cred ───▶│
│                        │                      │◀── OK ────────────│
│                        │                      │ 1. gen opaque token│
│                        │                      │ 2. save Redis+DB  │
│◀── Set-Cookie: ep_session (HttpOnly) ─────────│                   │

Every Request:
FE ──── Cookie ────▶ Gateway ──── verify token (Redis) ──▶ inject X-Api-Key + X-User-ID
                                                      ──▶ Service (X-Api-Key validated)
```

**Token = opaque string 64 chars (UUID32 + random32). KHÔNG phải JWT.**

---

## 6. APPROVAL MATRIX — BUSINESS LOGIC CỐT LÕI

```
< 5M VND:    Manager
5–20M:       Manager → Finance
20–50M:      Manager → Director → Finance
50–200M:     Manager → Director → CFO  (+RFQ bắt buộc)
200–500M:    Manager → Director → CEO → CFO
> 500M:      Manager → Director → BOD (parallel) → CFO
EMERGENCY:   Manager (2h) + Director (4h) parallel → Post-Audit

Special rules:
- Người TẠO PR ≠ người DUYỆT PR (Separation of Duties — enforce trong UseCase)
- IT Software/SaaS: thêm CISO + IT_Manager bất kể giá trị
- CAPEX: thêm FinanceDirector + CEO bất kể giá trị
- Emergency > 3 lần/phòng/tháng → cảnh báo lạm dụng
```

---

## 7. PERMISSION CODE SYSTEM

```
Format: {RESOURCE}_{ACTION}[_{SCOPE}]

Dùng trong @PreAuthorize:
  @PreAuthorize("hasAuthority('PR_CREATE')")      ← ✅
  @PreAuthorize("hasRole('MANAGER')")             ← ❌ TUYỆT ĐỐI KHÔNG

Key permissions:
  PR_CREATE, PR_VIEW_OWN, PR_VIEW_ALL
  PR_APPROVE_L1 (Manager), PR_APPROVE_L2 (Director), PR_APPROVE_L3 (C-Level)
  PR_APPROVE_FINANCE, PR_APPROVE_EMERGENCY
  PO_CREATE, PO_SEND_TO_VENDOR
  GR_CREATE, GR_ISSUE_OUT
  BUDGET_VIEW_OWN_DEPT, BUDGET_OVERRIDE
  ADMIN_USER_MANAGE, ADMIN_ROLE_MANAGE, ADMIN_APPROVAL_RULE
  SYSTEM_CONFIG (Super Admin only)

Cache: Redis key = "role-perm:{roleCode}" và "user-perm:{userId}"
```

---

## 8. KAFKA TOPICS — EVENT BUS

```
procurement.pr.submitted        PR bị submit
procurement.pr.approved         PR được duyệt hoàn toàn
procurement.pr.rejected         PR bị từ chối
approval.step.assigned          Task được gán cho approver
approval.sla.breached           Quá hạn SLA
finance.budget.warning          Ngân sách < 20%
procurement.po.issued           PO phát hành cho vendor
inventory.gr.created            GR hoàn tất → trigger 3-way match
finance.invoice.matched         Invoice match thành công
procurement.emergency.abuse     Lạm dụng Emergency PR

Message envelope: { eventId, eventType, version, source, timestamp, traceId, payload }
```

---

## 9. API CONVENTIONS — ĐỌC NGAY NẾU VIẾT API

```
Base:      /api/v1/{resource}
Response:  { success, code, message, data, meta, timestamp, requestId }
No DELETE: Dùng PATCH .../cancel | .../deactivate | .../revoke
Money:     String trong JSON ("70000000.0000"), BigDecimal trong Java
Header:    Idempotency-Key bắt buộc cho POST/PUT/PATCH
Auth:      HttpOnly Cookie "ep_session"
Error:     { success:false, code:"PR_002", message:"...", details:[...] }

Error code prefix: IAM_, PR_, APR_, FIN_, INV_, VND_, GW_, SYS_, VAL_
```

---

## 10. TECH STACK — QUICK REFERENCE

```
Backend:   Java 17, Spring Boot 3.x, MyBatis, HikariCP, Flyway
Auth:      Keycloak (custom provider), Opaque token, BCrypt
Cache:     Redis 7 (Lettuce)
Messaging: Kafka + Brevo SMTP
Workflow:  Camunda BPMN 7 (embedded)
Report:    JasperReport (PDF + Excel)
Realtime:  WebSocket/STOMP
Logging:   Log4j2 (NOT Logback), OpenTelemetry Java Agent
Metrics:   Micrometer → Prometheus → Grafana
Tracing:   OTel → Tempo
Log Agg:   Log4j2 → Loki
DB:        PostgreSQL 15+ (NUMERIC, TIMESTAMPTZ, UUID, JSONB, Array)
Frontend:  Angular 17, SCSS, IBM Plex fonts, Bootstrap+Tailwind+AntD+Material
i18n:      ngx-translate (VI/EN)
Container: Docker + Docker Compose
CI/CD:     Jenkins
Test:      JUnit 5, Mockito, Postman/Newman, JMeter
```

---

## 11. SPRING PROFILES

```
local  → Dev chạy ngoài Docker, localhost endpoints, encryption=OFF
dev    → Dev trong Docker, log DEBUG, encryption=OFF, email=OFF
prod   → Full production, log INFO, encryption=ON, email=ON

application.yml        → Base config (không sensitive)
application-{profile}.yml → Override per environment
ENV vars               → Ghi đè tất cả (KHÔNG commit sensitive value)
```

---

## 12. DOCKER RESOURCE LIMITS (8GB RAM MACHINE)

```
approval-service  → 0.50 CPU, 768MB RAM  (Camunda nặng)
iam-service       → 0.25 CPU, 512MB RAM
pr-service        → 0.25 CPU, 512MB RAM
finance-service   → 0.25 CPU, 512MB RAM
postgres          → 0.50 CPU, 512MB RAM
kafka             → 0.50 CPU, 512MB RAM
keycloak          → 0.25 CPU, 512MB RAM
inventory/vendor  → 0.25 CPU, 256MB RAM mỗi service
redis             → 0.10 CPU, 128MB RAM
notification/admin→ 0.10 CPU, 256MB RAM mỗi service
monitoring stack  → dùng docker --profile monitoring (tắt khi thiếu RAM)
```

---

## 13. LOG FORMAT — ĐỌC TRƯỚC KHI VIẾT LOG

```java
// Layer prefix bắt buộc:
[REQUEST]     → Filter: HTTP in/out
[CONTROLLER]  → Controller method entry
[ACTION]      → UseCase Start/Step/Complete
[REPO]        → Repository operation (DEBUG)
[CACHE]       → Redis hit/miss/put/evict (DEBUG)
[TOKEN]       → Token validation failure (WARN)
[APIKEY]      → API key failure (WARN)
[SECURITY]    → Unauthorized (WARN)
[AUDIT]       → Audit trail (INFO, file riêng)
[EXCEPTION]   → Exception với error code (WARN/ERROR)

// Masking bắt buộc:
email   → n***@c***.com
phone   → 09*****678
userId  → 550e8400...  (8 ký tự đầu)
KHÔNG BAO GIỜ LOG: password, token, secret, key, encryptedPayload
```

---

## 14. LỖI THƯỜNG GẶP — TRÁNH NGAY

```
❌ Dùng HTTP DELETE endpoint
❌ SELECT * không có WHERE is_deleted = false
❌ BigDecimal từ double literal: new BigDecimal(70000000.5) → FLOAT ERROR
   ✅ Đúng: new BigDecimal("70000000.5000")
❌ @PreAuthorize("hasRole('MANAGER')") → hardcode role
   ✅ Đúng: @PreAuthorize("hasAuthority('PR_APPROVE_L1')")
❌ @Transactional ở Repository hoặc Controller
   ✅ Đúng: @Transactional ở UseCase
❌ return null từ Repository method
   ✅ Đúng: return Optional<T>
❌ Import Spring/Jakarta trong domain/ package
❌ Log password, token, sensitive data
❌ Viết logic nghiệp vụ trong Controller
❌ Bỏ qua Idempotency-Key trong POST endpoint
❌ ORDER BY inject raw string từ user (SQL injection)
   ✅ Dùng whitelist mapping trong MyBatis XML
❌ TIMESTAMP không có TZ (dùng TIMESTAMPTZ)
❌ System.out.println() hoặc e.printStackTrace()
❌ Hardcode màu trong Angular SCSS
❌ Hardcode text trong Angular template (dùng | translate)
```

---

## 15. FILE NAVIGATION NHANH

```
docs/
  PROJECT_BRIEF.md           → Tổng quan dự án, epics
  DOMAIN_MODEL.md            → Entities, aggregates, domain events, ubiquitous language
  DATABASE_SCHEMA.md         → DDL đầy đủ, index strategy
  CODING_GUIDE.md            → Project structure, naming, checklist
  ENV_CONFIG.md              → Toàn bộ biến môi trường
  README.md                  → Index tổng quan tài liệu
  adr/ADR-001-microservice-clean-architecture.md → Quyết định kiến trúc #1
  adr/ADR-002-custom-opaque-token.md             → Quyết định kiến trúc #2
  adr/ADR-003-rsa-aes-encryption.md              → Quyết định kiến trúc #3
  adr/ADR-004-soft-delete-strategy.md            → Quyết định kiến trúc #4
  adr/ADR-005-mybatis-over-jpa.md                → Quyết định kiến trúc #5
  adr/ADR-006-camunda-workflow.md                → Quyết định kiến trúc #6
  adr/ADR-007-observability-stack.md             → Quyết định kiến trúc #7
  adr/ADR-008-postgresql-strategy.md             → Quyết định kiến trúc #8
  adr/ADR-009-angular-design-system.md           → Quyết định kiến trúc #9
  adr/ADR-010-profile-strategy.md                → Quyết định kiến trúc #10
  adr/ADR-011-rbac-permission-code.md            → Quyết định kiến trúc #11
  adr/ADR-012-idempotency-key.md                 → Quyết định kiến trúc #12
  adr/ADR-013-timezone-financial-precision.md    → Quyết định kiến trúc #13
  api/README.md              → Index API + service map
  api/common/                → Conventions, Auth flow, Pagination, Idempotency, Errors
  api/*.openapi.yaml         → OpenAPI 3.0 spec từng service

.cursor/rules/
  angular-frontend.mdc       → Angular component/template/SCSS/i18n rules
  coding.mdc                 → Clean arch, naming, injection, MyBatis, soft delete
  database.mdc               → Schema naming, soft delete, indexes, Flyway, HikariCP
  docker-resource.mdc        → Dockerfile, docker-compose, resource limits
  error-codes.mdc            → Error code enum, exception hierarchy, GlobalExceptionHandler
  git-workflow.mdc           → Branch strategy, commit convention, PR template, Jenkins
  logging.mdc                → Log4j2, layer prefix, masking, log4j2.xml
  testing.mdc                → JUnit, Postman, JMeter, coverage targets

.cursor/memory/
  architecture-map.md        → Sơ đồ service relationships
  coding-patterns.md         → Code patterns tái sử dụng
  decision-log.md            → Quyết định kỹ thuật đã chốt
  error-history.md           → Lịch sử lỗi và cách xử lý
  progress-tracker.md        → Epics và tasks đã/đang/chưa làm
  project-context.md         ← File này — đọc đầu tiên
  tech-stack.md              → Chi tiết tech stack + version
```
