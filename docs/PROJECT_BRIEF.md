# PROJECT BRIEF — eProcure Enterprise
## Hệ thống Quản lý Mua sắm Doanh nghiệp

---

> **Version:** 1.0.0  
> **Status:** Approved  
> **Last Updated:** 2025-01  
> **Owner:** Engineering Lead  

---

## 1. BỐI CẢNH & MỤC TIÊU

### 1.1 Bối cảnh

eProcure là hệ thống số hoá toàn bộ quy trình mua sắm nội bộ dành cho doanh nghiệp từ **200 nhân viên trở lên**. Hiện tại quy trình mua sắm đang được thực hiện thủ công qua email, file Excel và phê duyệt miệng, dẫn đến:

- Thời gian xử lý trung bình 5–7 ngày làm việc cho một yêu cầu mua sắm
- Ngân sách vượt chi mà không có cảnh báo sớm
- Thiếu audit trail khi cần kiểm toán nội/ngoại
- Không kiểm soát được hàng tồn kho, dẫn đến mua thừa
- Khó phát hiện gian lận và xung đột lợi ích

### 1.2 Mục tiêu kinh doanh

| # | Mục tiêu | KPI | Baseline | Target |
|---|---|---|---|---|
| 1 | Rút ngắn cycle time | Thời gian PR → PO | 5–7 ngày | < 2 ngày |
| 2 | Kiểm soát ngân sách | % cảnh báo vượt ngân sách phát hiện real-time | 0% | 100% |
| 3 | Tuân thủ kiểm toán | % action có audit trail đầy đủ | ~30% | 100% |
| 4 | Giảm mua thừa | % lần mua trùng với tồn kho | N/A | < 5% |
| 5 | Hiệu suất approver | % duyệt đúng SLA | N/A | > 85% |

### 1.3 Phạm vi hệ thống (In-Scope)

- **Purchase Request (PR):** Tạo, quản lý, theo dõi vòng đời yêu cầu mua sắm
- **Approval Workflow:** Engine phê duyệt đa cấp linh hoạt với Camunda BPMN
- **Request for Quotation (RFQ):** Thu thập và so sánh báo giá nhà cung cấp
- **Purchase Order (PO):** Phát hành và theo dõi đơn đặt hàng
- **Goods Receipt (GR):** Nhận hàng, kiểm tra và nhập kho
- **Invoice & 3-Way Match:** Đối soát hoá đơn với PO và GR
- **Budget Management:** Kiểm soát ngân sách phòng ban real-time
- **Inventory:** Quản lý tồn kho đơn giản (không thay thế WMS đầy đủ)
- **Vendor Management:** Danh sách nhà cung cấp, AVL, scorecard
- **Analytics & Reporting:** Dashboard, báo cáo xuất Excel/PDF (JasperReport)
- **Notification:** Email (Brevo SMTP), in-app, realtime WebSocket
- **IAM:** Tài khoản, phân quyền RBAC, sơ đồ tổ chức, SSO (Google, Keycloak)
- **Admin Portal:** Cấu hình hệ thống, quản lý người dùng, approval rules
- **Internal Config Portal:** Quản lý biến môi trường và cấu hình từng service

### 1.4 Ngoài phạm vi (Out-of-Scope)

- ERP đầy đủ (SAP, Oracle integration) — chỉ export file
- Kế toán tài chính đầy đủ (General Ledger)
- WMS đầy đủ (chỉ tracking cơ bản)
- Module mua sắm cho khách hàng bên ngoài (B2C/B2B portal)
- Mobile native app (responsive web trên mobile là đủ)

---

## 2. NGƯỜI DÙNG & VAI TRÒ

### 2.1 Ma trận người dùng

| Role Code | Tên vai trò | Tần suất dùng | Thao tác chính |
|---|---|---|---|
| `REQUESTER` | Nhân viên | Hàng ngày | Tạo PR, theo dõi trạng thái |
| `MANAGER` | Trưởng phòng | Hàng ngày | Duyệt cấp 1, xem ngân sách phòng |
| `DIRECTOR` | Giám đốc khối | Vài lần/tuần | Duyệt cấp 2, analytics |
| `C_LEVEL` | Ban giám đốc | Ít khi | Duyệt cấp 3, executive dashboard |
| `PURCHASING` | Cán bộ thu mua | Hàng ngày | Tạo PO, chạy RFQ, theo dõi giao hàng |
| `ACCOUNTANT` | Kế toán | Hàng ngày | Duyệt ngân sách, đối soát hoá đơn |
| `WAREHOUSE` | Thủ kho | Hàng ngày | Nhận hàng, nhập kho, cấp phát |
| `ADMIN` | Admin hệ thống | Khi cần | Cấu hình rules, quản lý danh mục |
| `SUPER_ADMIN` | Super Admin | Hiếm | Cấu hình môi trường, toàn quyền |

### 2.2 Quy tắc phân quyền đặc biệt

- Người tạo PR **≠** Người duyệt PR **≠** Người thanh toán (Separation of Duties)
- Người tạo PR không thể tự duyệt PR của mình, kể cả khi có role Manager
- CEO/BOD không thể đơn phương duyệt PR do chính mình tạo
- SUPER_ADMIN có màn hình quản trị riêng biệt với hệ thống nghiệp vụ chính

---

## 3. KIẾN TRÚC TỔNG QUAN

### 3.1 Sơ đồ microservices

```
                        ┌─────────────────────┐
                        │   Angular Frontend   │
                        │   (NGINX, HTTPS)     │
                        └──────────┬──────────┘
                                   │ HTTPS/WSS
                        ┌──────────▼──────────┐
                        │    API GATEWAY       │
                        │  (Spring Cloud GW /  │
                        │   NGINX + rate limit)│
                        └──┬──────┬──────┬────┘
               REST/JWT    │      │      │   x-api-key (internal)
          ┌────────────────┘      │      └────────────────┐
          │                       │                       │
 ┌────────▼────────┐   ┌──────────▼──────────┐  ┌───────▼──────────┐
 │  IAM Service    │   │  PR Service          │  │ Approval Engine  │
 │  (auth,rbac,    │   │  (purchase request,  │  │ (Camunda BPMN,   │
 │   org chart)    │   │   budget check)      │  │  workflow)       │
 └────────┬────────┘   └──────────┬──────────┘  └───────┬──────────┘
          │                       │                       │
          │            ┌──────────▼──────────┐            │
          │            │   Kafka Event Bus    │◄───────────┘
          │            └──────────┬──────────┘
          │       ┌───────────────┼───────────────┐
          │       │               │               │
 ┌────────▼───────▼──┐  ┌────────▼───────┐  ┌────▼────────────┐
 │ Accounting/Finance│  │  Inventory Svc  │  │ Notification Svc│
 │ (budget, invoice, │  │  (kho, GR,      │  │ (email, push,   │
 │  payment)         │  │   cấp phát)     │  │  Brevo SMTP)    │
 └───────────────────┘  └────────────────┘  └─────────────────┘
          │
 ┌────────▼──────────────────────────────────────────────────┐
 │                  Infrastructure Layer                      │
 │  PostgreSQL | Redis | Keycloak | Kafka | JasperReport     │
 │  Prometheus | Grafana | Loki | Tempo | OpenTelemetry       │
 └────────────────────────────────────────────────────────────┘
```

### 3.2 Phân chia Database & Schema

| Service | Database | Schema |
|---|---|---|
| IAM Service | `db_iam` | `iam` |
| PR Service | `db_procurement` | `pr` |
| Approval Engine | `db_procurement` | `approval` |
| Accounting Service | `db_finance` | `finance` |
| Inventory Service | `db_inventory` | `inventory` |
| Notification Service | `db_notification` | `notification` |
| Camunda (BPMN) | `db_camunda` | `camunda` |
| Audit Log (Immutable) | `db_audit` | `audit` |

---

## 4. YÊU CẦU KỸ THUẬT

### 4.1 Stack công nghệ

| Layer | Technology | Version / Note |
|---|---|---|
| Frontend | Angular | 17+ |
| Frontend UI | Bootstrap + TailwindCSS + SCSS | Design tokens từ UI Design System |
| Frontend i18n | ngx-translate | VI/EN tự động |
| Frontend Web Server | NGINX | Reverse proxy, HTTPS, HSTS |
| Backend Language | Java | 17 LTS |
| Backend Framework | Spring Boot | 3.x |
| Backend API | RESTful (không có DELETE endpoint) | Soft delete toàn hệ thống |
| ORM / SQL | MyBatis | Không dùng JPA/Hibernate |
| DB Migration | Flyway | Version-based migration |
| Connection Pool | HikariCP | |
| Auth Provider | Keycloak | Custom Provider, không lưu user data |
| Cache | Redis | Session, permission mapping, dashboard |
| Message Broker | Kafka | Event-driven async |
| Email | Brevo SMTP | Template-based |
| Workflow | Camunda BPMN 7 | Maker-Checker patterns |
| Report | JasperReport | Export PDF/Excel |
| Realtime | WebSocket (STOMP) | Notification realtime |
| Logging | Log4j2 | Custom layout, màu terminal |
| Tracing | OpenTelemetry Java Agent | Tempo backend |
| Metrics | Prometheus | Grafana dashboard |
| Log Aggregation | Loki | Grafana log exploration |
| Database | PostgreSQL 15+ | Multi-schema, index hợp lý |
| Container | Docker + Docker Compose | Full stack |
| CI/CD | Jenkins | Pipeline as code |
| Source Control | Git / GitHub | Branch strategy: main/develop/feature |
| Testing | JUnit (unit), Postman (integration), JMeter (perf/sec) | |

### 4.2 Yêu cầu phi chức năng

| Yêu cầu | Target |
|---|---|
| Availability | 99.5% (môi trường production) |
| Response time API | P95 < 500ms, P99 < 1s |
| Concurrent users | 500 users đồng thời |
| Data retention | Audit log 5 năm |
| Security | HTTPS/TLS 1.3, HSTS, mã hoá payload RSA+AES |
| RBAC | Không hardcode role trong code, dùng `@PreAuthorize` |

### 4.3 Ràng buộc môi trường dev

- Máy dev: 8GB RAM, Docker thực tế 4–5GB khả dụng
- Giới hạn resource Docker: CPU 0.25, RAM 512MB mỗi service (mặc định)
- Hỗ trợ Spring Profiles: `local` | `dev` | `prod`

---

## 5. SECURITY MODEL

### 5.1 Flow xác thực

```
FE (Angular)          API Gateway          IAM Service          Keycloak
     │                     │                    │                    │
     │──── encrypt(RSA+AES) ─────────────────>  │                   │
     │     POST /auth/login                     │──── verify ──────>│
     │                     │                    │<─── OK ────────── │
     │                     │                    │ generate opaque    │
     │                     │                    │ token, save Redis  │
     │<────────────── encrypted_token ──────────│                   │
     │ (HttpOnly Cookie)                        │                   │

FE Request:
     │──── Cookie: token ──>│                   │
     │                      │── verify token ──>│ (Redis lookup)
     │                      │<── user+roles ─── │
     │                      │ inject x-api-key  │
     │                      │──────────────────>│ (downstream service)
```

### 5.2 Token Strategy

- Token là **opaque string** (chuỗi ngẫu nhiên, không phải JWT) — không mang thông tin
- Lưu trong Redis (primary) + DB (backup/audit)
- 1 phiên đăng nhập duy nhất — đăng nhập mới sẽ invalidate token cũ
- Token không hết hạn theo thời gian — chỉ bị thu hồi khi logout/login mới
- Lưu phía FE trong HttpOnly Cookie (không accessible từ JS)

### 5.3 Encryption Model

```
Mỗi Request:
  1. FE generate AES-256 key ngẫu nhiên
  2. FE mã hoá payload bằng AES key
  3. FE mã hoá AES key bằng RSA Public Key của BE
  4. Gửi: { encryptedPayload, encryptedAesKey }

Response:
  1. BE tạo AES key mới cho response
  2. Mã hoá response bằng AES key
  3. Mã hoá AES key bằng RSA Public Key của FE (key exchange khi init)
  4. Gửi: { encryptedResponse, encryptedAesKey }

Toggle: Tắt mã hoá ở môi trường local/dev qua env var ENCRYPTION_ENABLED=false
```

---

## 6. APPROVAL MATRIX (TÓM TẮT)

Xem tài liệu `DOMAIN_MODEL.md` phần Approval Engine để biết ma trận đầy đủ.

| Giá trị PR | Chuỗi phê duyệt tối thiểu |
|---|---|
| < 5 triệu | Manager |
| 5–20 triệu | Manager → Kế toán |
| 20–50 triệu | Manager → Director → Kế toán |
| 50–200 triệu | Manager → Director → CFO (+ RFQ bắt buộc) |
| 200–500 triệu | Manager → Director → CEO → CFO |
| > 500 triệu | Manager → Director → HĐQT → CFO |
| EMERGENCY | Manager + Director (parallel, 2h SLA, 24/7) |

---

## 7. MILESTONES & PHÂN CHIA EPIC

| # | Epic | Mô tả |
|---|---|---|
| E01 | Infrastructure Setup | Docker, DB, Redis, Kafka, Keycloak, monitoring stack |
| E02 | IAM & Auth | Đăng nhập, RBAC, org chart, session, 2FA, OAuth Google |
| E03 | UI Shell & Design System | Angular shell, component library, i18n |
| E04 | Purchase Request | Tạo PR, budget check, inventory check, catalog |
| E05 | Approval Engine | Camunda BPMN, approval rules, SLA, escalation |
| E06 | RFQ & Vendor | Thu thập báo giá, AVL, vendor scorecard |
| E07 | Purchase Order | Tạo PO, tracking, blanket PO |
| E08 | Goods Receipt & Inventory | GR, nhập kho, cấp phát |
| E09 | Invoice & Payment | 3-way match, đối soát, payment tracking |
| E10 | Budget Management | Dashboard ngân sách, budget override, transfer |
| E11 | Notification & Realtime | Email templates, push, WebSocket |
| E12 | Analytics & Reports | JasperReport, dashboard, xuất Excel/PDF |
| E13 | Admin & Config Portal | Approval rules, catalog, system config |
| E14 | Security Hardening | Encryption, audit log, penetration readiness |
| E15 | Testing & CI/CD | JUnit, Postman, JMeter, Jenkins pipeline |

---

## 8. ĐỊNH NGHĨA HOÀN THÀNH (DEFINITION OF DONE)

- [ ] Code review bởi ít nhất 1 người khác
- [ ] Unit test coverage ≥ 70% cho service layer
- [ ] Integration test Postman collection cập nhật
- [ ] Không có hardcode string tiếng Việt trong template Angular
- [ ] Không có hardcode màu trong component (dùng CSS variable)
- [ ] Không có hardcode role trong `@PreAuthorize` — chỉ dùng permission code
- [ ] Log không chứa thông tin nhạy cảm (password, token)
- [ ] Swagger/OpenAPI spec cập nhật
- [ ] Flyway migration script có rollback plan
- [ ] Docker resource limits đặt đúng
- [ ] README cập nhật nếu thay đổi setup
