# Story Map E01-E15
## eProcure Enterprise Roadmap

---

## 1. Product Backbone

Luồng end-to-end mục tiêu:

```
Authenticate -> Create PR -> Approve PR -> Run RFQ -> Issue PO -> Receive Goods -> Match Invoice -> Pay -> Report/Audit
```

Backbone theo người dùng:

| Stage | Actor chính | Outcome |
|---|---|---|
| Access | User, Admin | Đăng nhập an toàn, quyền đúng, phiên được kiểm soát |
| Request | Requester | Tạo PR có line items, ngân sách, tồn kho, attachment |
| Approve | Manager, Director, Finance, C-Level | Duyệt đúng ma trận, SLA, delegation, SoD |
| Source | Purchasing, Vendor Admin | RFQ, so sánh báo giá, chọn vendor |
| Order | Purchasing | Tạo và gửi PO |
| Receive | Warehouse | Nhận hàng, ghi nhận GR, cập nhật tồn kho |
| Match | Accountant | Đối soát PO-GR-Invoice và xử lý lệch |
| Pay | Accountant | Xác nhận thanh toán |
| Govern | Admin, Super Admin | Cấu hình, audit, security, reporting |

---

## 2. Release Strategy

| Release | Epic | Mục tiêu |
|---|---|---|
| R0 Foundation | E01, E02, E03 | Có môi trường chạy local/dev, login, UI shell |
| R1 PR Approval MVP | E04, E05 | PR draft -> submit -> approval -> final status |
| R2 Procurement Execution | E06, E07 | RFQ/vendor và PO |
| R3 Receiving & Finance | E08, E09, E10 | GR, invoice, payment, budget |
| R4 Engagement & Insight | E11, E12 | Notification, realtime, analytics, reports |
| R5 Governance & Hardening | E13, E14, E15 | Admin portal, security hardening, CI/CD, test/perf |

---

## 3. Epic Story Map

| Epic | Capability | MVP/Priority | Depends On | Primary Artifacts |
|---|---|---|---|---|
| E01 Infrastructure Setup | Docker Compose, PostgreSQL multi DB, Redis, Kafka, Keycloak, monitoring, NGINX | MVP | None | compose, init SQL, env, healthchecks |
| E02 IAM Service | Auth, opaque token, RBAC, session, org chart, delegation | MVP | E01 | iam-service, Keycloak realm, Redis session |
| E03 UI Shell & Design System | Angular shell, design tokens, shared components, i18n, interceptors | MVP | E01, E02 | web app, ep-* components, guards |
| E04 Purchase Request Service | PR lifecycle, catalog, budget/inventory check, attachments, events | MVP | E01, E02, E03 | pr-service, PR pages |
| E13-A Admin Portal UI | Giao diện User CRUD, phân quyền RBAC và Org Tree | MVP | E02, E03 | Admin UI, User/RBAC/Org pages |
| E05 Approval Engine | Camunda workflow, rules, inbox, actions, SLA, delegation, SoD | MVP | E01, E02, E04, E13-A | approval-service, approval UI |
| E06 RFQ & Vendor | Vendor master, AVL, RFQ, quote comparison | P1 | E04, E05 | vendor-service, RFQ pages |
| E07 Purchase Order | PO creation from approved PR, vendor send, PO tracking | P1 | E04, E06 | finance/procurement PO module |
| E08 Goods Receipt & Inventory | Catalog, stock, GR, issue out, stock movement | P1 | E07 | inventory-service, warehouse UI |
| E09 Invoice & Payment | Invoice entry, 3-way match, payment tracking | P1 | E07, E08, E10 | finance-service |
| E10 Budget Management | Budget setup, commitment, warning, override, transfer | P1 | E02, E04 | finance-service budget module |
| E11 Notification & Realtime | Email, in-app notification, WebSocket | P1 | E02, E04, E05 | notification-service |
| E12 Analytics & Reports | KPI dashboard, JasperReport PDF/Excel | P2 | E04-E11 | analytics-service |
| E13 Admin & Config Portal | Approval rules, catalog, users, system config, audit viewer | P2 | E02, E03, E05 | admin-service/admin UI |
| E14 Security Hardening | RSA+AES, audit coverage, rate limit, penetration readiness | P2 | E01-E13 | security config, test reports |
| E15 Testing & CI/CD | JUnit, Postman, JMeter, Jenkins pipelines | Cross-cutting | All | test suites, Jenkinsfile |

---

## 4. MVP Vertical Slice

Slice đầu tiên cần code theo thứ tự:

```
1. E01: docker compose nền + PostgreSQL/Redis/Kafka/Keycloak chạy health.
2. E02: IAM login tạo opaque cookie + /users/me trả user và permissions.
3. E03: Angular shell login + interceptor withCredentials + Idempotency-Key.
4. E04: PR draft/create/submit/list/detail + PrSubmittedEvent.
5. E13-A: Admin UI quản lý User, phân quyền RBAC và sơ đồ phòng ban (Org Tree).
6. E05: Approval process start + inbox + approve/reject/request changes.
7. E04: PR nhận PrApproved/PrRejected/PrChangesRequested event và cập nhật status.
```

Luồng demo:

```
Requester login
Requester creates draft PR
Requester submits PR
Approval Engine creates task for Manager
Manager login and opens approval inbox
Manager approves
Requester sees PR status APPROVED
```

---

## 5. Cross-Epic Risks

| Risk | Epic | Mitigation |
|---|---|---|
| Auth flow phụ thuộc Keycloak nhưng token là opaque custom | E02 | Keycloak chỉ verify credential; IAM sở hữu session/token |
| Approval cần org chart và permission | E02, E05 | Cung cấp OrgApproverPort từ IAM trước khi hoàn thiện UI admin |
| PR submit cần Finance/Inventory nhưng services chưa có | E04, E10, E08 | MVP dùng BudgetCheckPort/InventoryCheckPort stub có contract |
| Notification chưa có trong MVP | E05, E11 | Publish event trước, consumer notification làm sau |
| Docker 8GB RAM giới hạn | E01 | Resource limits và monitoring profile optional |
| Scope tài liệu quá lớn | All | Chi tiết hóa từng epic ngay trước khi code epic đó |

---

## 6. Current Continuation Plan

Hiện trạng cập nhật 2026-06-06:

- E01-E06, E08-E12 và E13-A đã có implementation foundation theo tracker.
- `finance-service` đã có budget, RFQ-award PO, PO list/detail/edit/send/cancel, invoice, 3-way match, payment, và report source projections.
- `vendor-service`, `inventory-service`, `notification-service`, và `analytics-service` đã có backend foundation.
- E07 còn thiếu phần direct/manual PO từ approved PR. Contract chi tiết nằm tại `E07-purchase-order.md`.
- E13 backend/admin config phần còn lại, E14 security hardening, và E15 testing/CI-CD vẫn là epic chưa làm.

Thứ tự nên làm tiếp:

```text
1. E07 manual/direct PO source contracts and implementation:
   PR po-source + converted-to-po, Vendor po-source, Finance POST /purchase-orders.
2. E07 frontend manual PO creation UI after backend contracts are green.
3. E15 runtime/API verification and CI/CD hardening for the full service set.
4. E13 remaining admin/config backend and UI surfaces.
5. E14 security hardening and penetration-readiness tasks.
6. E12 source-enrichment follow-ups where needed for richer reports.
```
