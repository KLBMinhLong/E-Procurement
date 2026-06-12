# progress-tracker.md
## eProcure Enterprise — Tiến Độ Epics & Tasks

> Cập nhật file này mỗi khi hoàn thành một epic hoặc phát hiện blockers.  
> Status: ⬜ Chưa làm | 🔄 Đang làm | ✅ Xong | 🔒 Blocked

---

## DOCS LAYER
| Task | Status | Ghi chú |
|---|---|---|
| PROJECT_BRIEF.md | ✅ | |
| DOMAIN_MODEL.md | ✅ | |
| DATABASE_SCHEMA.md | ✅ | |
| CODING_GUIDE.md | ✅ | |
| ENV_CONFIG.md | ✅ | |
| README.md | ✅ | |
| adr/ (13 ADRs + README.md) | ✅ | |
| api/common/ (6 files) | ✅ | |
| iam-service.openapi.yaml | ✅ | |
| purchase-request-service.openapi.yaml | ✅ | |
| approval-service.openapi.yaml | ✅ | |
| finance-service.openapi.yaml | ✅ | |
| inventory-service.openapi.yaml | ✅ | |
| vendor-service.openapi.yaml | ✅ | |
| analytics-service.openapi.yaml | ✅ | |
| notification-service.openapi.yaml | ✅ | |
| admin-service.openapi.yaml | ✅ | |
| api/README.md | ✅ | |
| user-stories/README.md | ✅ | Quy ước story/use case và MVP boundary |
| user-stories/01-story-map-E01-E15.md | ✅ | Story map toàn bộ epic E01-E15 |
| user-stories/E01-infrastructure-setup.md | ✅ | User story/use case chi tiết MVP |
| user-stories/E02-iam-service.md | ✅ | User story/use case chi tiết MVP |
| user-stories/E03-ui-shell-design-system.md | ✅ | User story/use case chi tiết MVP |
| user-stories/E04-purchase-request-service.md | ✅ | User story/use case chi tiết MVP |
| user-stories/E05-approval-engine.md | ✅ | User story/use case chi tiết MVP |
| user-stories/E06-rfq-vendor.md | ✅ | User story/use case chi tiết chuẩn bị vendor-service foundation |
| user-stories/E07-purchase-order.md | ✅ | User story/use case chi tiết cho manual/direct PO, PR PO source, vendor PO source |
| user-stories/E10-budget-management.md | ✅ | User story/use case chi tiết chuẩn bị finance-service budget foundation |
| user-stories/E11-notification-realtime.md | ✅ | User story/use case chi tiết chuẩn bị notification-service |

## RULES LAYER (.cursor/rules/)
| File | Status | Ghi chú |
|---|---|---|
| angular-frontend.mdc | ✅ | Component, template, SCSS, i18n |
| coding.mdc | ✅ | Clean arch, naming, MyBatis, soft delete |
| database.mdc | ✅ | Schema, soft delete, index, Flyway |
| docker-resource.mdc | ✅ | Dockerfile, compose, resource limits |
| error-codes.mdc | ✅ | Error enum, exception hierarchy, handler |
| git-workflow.mdc | ✅ | Branch, commit, PR template, Jenkins |
| logging.mdc | ✅ | Log4j2, masking, layer format |
| testing.mdc | ✅ | JUnit, Postman, JMeter |

## TASK LAYER (agent/tasks/)
| File | Status | Ghi chú |
|---|---|---|
| task-new-feature.md | ✅ | Playbook thêm feature theo layer order |
| task-bug-fix.md | ✅ | Reproduce, root cause, fix, error-history |
| task-add-endpoint.md | ✅ | REST contract, DTO, Controller, OpenAPI |
| task-add-migration.md | ✅ | Flyway versioning, schema rules, rollback note |
| task-refactor.md | ✅ | Refactor boundary, invariant cleanup, verification |

## MEMORY LAYER (agent/memory/)
| File | Status | Ghi chú |
|---|---|---|
| architecture-map.md | ✅ | Service relationships, data flow |
| coding-patterns.md | ✅ | Code templates tái sử dụng |
| decision-log.md | ✅ | Quyết định kỹ thuật đã chốt |
| domain-glossary.md | ✅ | Thuật ngữ nghiệp vụ VI/EN, permission/status/event vocabulary |
| error-history.md | ✅ | Lịch sử lỗi và cách xử lý |
| progress-tracker.md | ✅ | File này |
| project-context.md | ✅ | Master context |
| tech-stack.md | ✅ | Versions, config patterns |

## CODEX PERSONALIZATION (.codex/)
| File | Status | Ghi chú |
|---|---|---|
| CODEX_CONTEXT.md | ✅ | Bootstrap ngắn cho Codex: source-of-truth, snapshot, task bootstrap, commands |
| config.toml | ✅ | Runtime local config |
| skills/ui-ux-pro-max | ✅ | Skill UI/UX cục bộ cho frontend/design task |

## EPICS — CODE

### E01: Infrastructure Setup
| Task | Status |
|---|---|
| Docker Compose dev environment | ✅ |
| PostgreSQL init scripts (multiple DBs) | ✅ |
| Kafka topic creation scripts | ✅ |
| Keycloak realm + custom provider setup | ✅ |
| Redis config | ✅ |
| Prometheus + Grafana + Loki + Tempo | ✅ |
| NGINX config (dev + prod) | ✅ |

### E02: IAM Service
| Task | Status |
|---|---|
| Spring Boot project setup | ✅ |
| Flyway migrations (users, roles, permissions, sessions) | ✅ |
| MyBatis mappers | ✅ |
| Login + Keycloak integration | ✅ |
| Opaque token generation + Redis session | ✅ |
| RSA+AES encryption interceptor | ✅ |
| HttpOnly Cookie management | ✅ |
| Single session enforcement | ✅ |
| Forgot/reset password token + IAM credential reset | ✅ |
| Password reset email delivery adapter | ✅ |
| RBAC permission loading | ✅ |
| Admin user CRUD API | ✅ |
| Admin role/permission API | ✅ |
| 2FA (TOTP) | ✅ |
| Google OAuth | ✅ |
| Forgot password flow | ✅ |
| Delegation API | ✅ |
| Org chart API | ✅ |
| Unit tests | ✅ |

### E03: UI Shell & Design System (Angular)
| Task | Status |
|---|---|
| Angular project setup | ✅ |
| Design tokens (SCSS variables) | ✅ |
| ep-button, ep-badge, ep-card | ✅ |
| ep-table (với pagination) | ✅ |
| ep-modal, ep-form-field | ✅ |
| ep-amount, ep-sla-bar, ep-stat-card | ✅ |
| ep-avatar, ep-icon (custom SVG) | ✅ |
| ep-approval-action, ep-filter-bar | ✅ |
| ep-breadcrumb, ep-lang-switcher | ✅ | Sửa lỗi ghi đè key `route` trong i18n & hỗ trợ UUIDs động |
| ep-empty-state, ep-skeleton | ✅ |
| /ui-showcase page | ✅ |
| i18n setup (vi/en) | ✅ |
| Auth interceptors | ✅ |
| Error interceptor | ✅ |
| WebSocket service | ✅ |
| Permission guard + directive | ✅ |
| Rewrite User Profile UI Screen (Personal details, avatar selector, password change checklist, 2FA toggle setup) | ✅ |
| Unit tests / compilation validation | ✅ |

### E04: Purchase Request Service
| Task | Status |
|---|---|
| Spring Boot project setup | ✅ |
| Flyway migrations | ✅ |
| Domain model (PurchaseRequest, PrLineItem, Money) | ✅ |
| CreatePR UseCase | ✅ |
| SubmitPR UseCase (budget check + inventory check) | ✅ |
| UpdatePR UseCase | ✅ |
| CancelPR UseCase | ✅ |
| GetPR Detail + List (paginated, filtered) | ✅ |
| File attachment upload | ✅ |
| Catalog API | ✅ |
| Kafka event publishing | ✅ |
| Unit tests (domain + CreatePR + SubmitPR + UpdatePR + CancelPR + pending approval callback) | ✅ | 35 tests pass |
| Frontend: PR list page | ✅ |
| Frontend: PR create form | ✅ |
| Frontend: PR detail page | ✅ |

### E13-A: Admin Portal (User, RBAC, Org Chart UI) [PRIORITIZED 🚀]
| Task | Status |
|---|---|
| Create Admin lazy-loaded module & routing config (`/admin`) | ✅ |
| Create Admin HTTP Services (User, RBAC, Org Chart tree) | ✅ |
| Frontend UI: User Management Screen (CRUD & Lock/Unlock) | ✅ |
| Frontend UI: Role-Permission Matrix Screen (RBAC mapping) | ✅ |
| Frontend UI: Org Chart Visual Tree Screen (Department hierarchy) | ✅ |
| i18n localization (VI/EN) & Integration tests | ✅ |

### E05: Approval Engine
| Task | Status |
|---|---|
| Spring Boot + Camunda setup | ✅ |
| BPMN files (pr-approval-process.bpmn, emergency.bpmn) | ✅ |
| ApprovalRule engine (select rule by conditions) | ✅ |
| Approval chain resolution (từ org chart) | ✅ |
| SLA calculation (business hours) | ✅ |
| Start approval process (idempotent event handling + Camunda start + process/step persistence) | ✅ |
| Kafka PR submitted consumer + approval.step.assigned publisher | ✅ |
| PR status callback to PENDING_APPROVAL | ✅ |
| Approve/Reject/RequestChanges/Forward actions | ✅ |
| Backend: Approval inbox, counts & task detail API | ✅ |
| SLA timer + escalation | ✅ |
| Delegation awareness | ✅ |
| Conflict of interest check | ✅ |
| Admin: CRUD approval rules | ✅ |
| Frontend: Approval inbox | ✅ |
| Frontend: Task detail + actions | ✅ |
| Frontend: Approval rule admin page | ✅ |
| Unit tests (rule selection foundation + BPMN diagram metadata + chain/SLA resolution + process start + Kafka consumer + approval actions + inbox/detail retrieval + SLA escalation + delegation awareness + admin rule CRUD) | ✅ | 36 tests pass |

### E06–E15: (Xem PROJECT_BRIEF.md)
| Epic | Status |
|---|---|
| E13-A: Admin Portal (User/RBAC/Org Tree UI) | ✅ |
| E06: RFQ & Vendor | ✅ |
| E07: Purchase Order | ✅ |
| E08: Goods Receipt & Inventory | ✅ |
| E09: Invoice & Payment | ✅ |
| E10: Budget Management | ✅ |
| E11: Notification & Realtime | ✅ |
| E12: Analytics & Reports | ✅ |
| E13: Admin & Config Portal (Rest UI/BPMN) | ⬜ |
| E14: Security Hardening | ⬜ |
| E15: Testing & CI/CD | 🔄 | E15-A Runtime/API Smoke Pack baseline passed on Docker; Newman/perf/CI hardening remain |

### E10: Budget Management
| Task | Status | Ghi chú |
|---|---|---|
| User story/use case chi tiết | ✅ | `docs/user-stories/E10-budget-management.md` |
| Spring Boot finance-service module setup | ✅ | Scaffold service + Docker/compose; `mvn test` xanh |
| Flyway migrations (budgets, budget_transactions) | ✅ | Budget foundation + local seed |
| Internal budget check API | ✅ | `GET /internal/budgets/check` + use case tests |
| PR service FinanceBudgetCheckAdapter | ✅ | Feature flag thay fallback; full reactor test xanh |
| Kafka commit/release budget events | ✅ | Finance consumes submitted/approved/rejected/cancelled/changes-requested and writes idempotent ledger |
| Budget dashboard/list API | ✅ | `GET /api/v1/budgets` + `GET /api/v1/budgets/{id}/dashboard`; scope quyền + Redis cache |
| Override/transfer API | ✅ | `PATCH /api/v1/budgets/{id}/override-approval` + `PATCH /api/v1/budgets/{id}/transfer`; idempotent audit/ledger + tests |
| Budget warning/exceeded events | ✅ | `BudgetAlertService` publishes `finance.budget.warning` / `finance.budget.exceeded` for low/negative projected available budget; notification consumption remains E11 |

### E11: Notification & Realtime
| Task | Status | Ghi chú |
|---|---|---|
| User story/use case chi tiết | ✅ | `docs/user-stories/E11-notification-realtime.md` |
| Spring Boot notification-service module setup | ✅ | Maven module + Docker/compose service on port 8088 |
| Flyway migrations (notification_templates, notifications, event_processing_log) | ✅ | In-app notification schema + template seed + Kafka dedup log |
| In-app notification feed/count/read API | ✅ | `GET /api/v1/notifications`, `/count`, `PATCH /read`, `PATCH /read-all`; guarded by `NOTIFICATION_VIEW_OWN` |
| Business event consumer foundation | ✅ | Consumes approval/PR/finance notification topics; budget alerts route to configured finance recipients |
| WebSocket realtime delivery | ✅ | STOMP endpoint `/ws/notifications`, user queue `/user/queue/notifications`, after-commit `SimpMessagingTemplate` delivery |
| Email dispatch + retry/DLQ | ✅ | `notification.email.send` creates EMAIL outbox rows; scheduled dispatch uses logging/Brevo provider, retry attempts, and dead-letter persistence |
| Template admin API/UI | ✅ | `SYSTEM_CONFIG` guarded list/update/preview APIs plus Angular `/admin/notification-templates` UI |
| Frontend notification bell/feed | ✅ | Shell bell dropdown loads count/feed, marks read/all-read, and subscribes to realtime STOMP messages |
| Unit tests | ✅ | Budget/email consume, email retry/DLQ, template admin update/preview, and WebSocket adapter tests pass |

### E06: RFQ & Vendor
| Task | Status | Ghi chú |
|---|---|---|
| User story/use case chi tiết | ✅ | `docs/user-stories/E06-rfq-vendor.md` |
| Spring Boot vendor-service module setup | ✅ | Maven module + Docker/compose service on port 8086 |
| Flyway migrations (vendors, vendor_contacts, vendor_scores) | ✅ | Vendor master + AVL status + score seed |
| Vendor list/create/detail/approve API | ✅ | `GET/POST /api/v1/vendors`, `GET /api/v1/vendors/{id}`, `PATCH /approve`; permission + idempotency |
| RFQ schema + PR source contract | ✅ | `rfqs`, `rfq_line_items`, `rfq_invitations`; PR internal `GET /internal/purchase-requests/{id}/rfq-source` |
| RFQ create/list/detail/close API | ✅ | `GET/POST /api/v1/rfq`, `GET /api/v1/rfq/{id}`, `PATCH /api/v1/rfq/{id}/close`; PR approved + AVL validation |
| RFQ quote submit/evaluate/award | ✅ | `vendor_quotes`, `vendor_quote_line_items`; `POST /rfq/{id}/quotes`, `POST /rfq/{id}/quotes/{quoteId}/evaluate`, `POST /rfq/{id}/award` |
| PO handoff after award event | ✅ | `procurement.rfq.awarded` published after award commit; finance-service consumes it and creates/list/detail DRAFT PO |
| Unit tests | ✅ | Vendor master + RFQ quote/use-case tests pass |
| Frontend: Vendor Management | ✅ | 2026-06-08: `/vendors/list`, `/vendors/create`, `/vendors/:id` — i18n VI/EN, models khớp backend (`VendorSummary`/`VendorDetail`, address object, scorecard, AVL filter), approve modal + reload; bỏ deactivate (API chưa có) |
| Frontend: RFQ Management | ✅ | 2026-06-08: `/vendors/rfq`, `/vendors/rfq/create`, `/vendors/rfq/:id` — list/detail/create, quotes từ `GET /rfq/{id}`, submit/evaluate/award/close, award reason ≥20 chars; i18n đầy đủ; `npm run build` pass |
| Frontend: PR → RFQ handoff | ✅ | 2026-06-12: PR detail có nút tạo RFQ cho PR APPROVED; `/vendors/rfq/create?prId=...` tự chọn PR nguồn, gợi ý tiêu đề, hiển thị source note; `npm run build` pass |
| Frontend: Inventory/GR UI | ✅ | 2026-06-08: `/inventory/goods-receipts` list/create/detail — warehouse dropdown (`GET /warehouses` mới), PO page=1, complete modal DRAFT-only, i18n đầy đủ; Stock/catalog UI ⬜ |

### E07: Purchase Order
| Task | Status | Ghi chú |
|---|---|---|
| User story/use case chi tiết | ✅ | `docs/user-stories/E07-purchase-order.md`; chốt manual PO contract: PR `po-source`, PR `converted-to-po`, Vendor `po-source`, Finance callback outbox |
| RFQ award consumer + PO persistence | ✅ | finance-service consumes `procurement.rfq.awarded`, dedups via `finance.event_processing_log`, creates `finance.purchase_orders` + `finance.po_line_items` |
| PO list/detail API | ✅ | `GET /api/v1/purchase-orders`, `GET /api/v1/purchase-orders/{id}` guarded by `PO_VIEW_OWN`/`PO_VIEW_ALL` |
| PO draft edit before send | ✅ | `PATCH /api/v1/purchase-orders/{id}` updates delivery details/payment terms for DRAFT PO with `PO_EDIT` + Idempotency-Key |
| PO send/cancel actions | ✅ | `POST /api/v1/purchase-orders/{id}/send` and `PATCH /api/v1/purchase-orders/{id}/cancel`; idempotent Redis replay + status fallback |
| PO issue event | ✅ | Finance publishes `procurement.po.issued`; Notification subscribes and creates `PO_ISSUED` in-app notification |
| Vendor email handoff on PO send | ✅ | Finance publishes `notification.email.send` with rendered subject/body for vendor email dispatch |
| PR PO source + converted callback | ✅ | `GET /internal/purchase-requests/{id}/po-source` + `PATCH /internal/purchase-requests/{id}/converted-to-po`; purchase-request-service tests pass |
| Vendor PO source contract | ✅ | `GET /internal/vendors/{id}/po-source`; vendor-service tests pass |
| Manual PO create API | ✅ | `POST /api/v1/purchase-orders` creates DRAFT PO from approved PR + AVL vendor, stores callback outbox, exposes `prConversionStatus`, and blocks send until callback delivered |
| Frontend: Manual PO create UI | ✅ | Angular `/finance/purchase-orders` list/detail/create screens, PR detail handoff, PO navigation/i18n; `npm run build` passes |
| Frontend: PO detail actions | ✅ | 2026-06-12: PO detail hỗ trợ edit DRAFT delivery/payment terms, send to vendor khi PR callback DELIVERED + vendor email sẵn sàng, cancel trước fulfillment; permission-gated modals, i18n VI/EN; `npm run build` pass |

### E08: Goods Receipt & Inventory
| Task | Status | Ghi chú |
|---|---|---|
| Spring Boot inventory-service module setup | ✅ | Maven module + Docker/compose service on port 8085 |
| Flyway migration inventory foundation | ✅ | `warehouses`, `items`, `stock_entries`, `goods_receipts`, `goods_receipt_line_items`, `stock_movements`, PO snapshots, Kafka event log |
| PO issued consumer + snapshot persistence | ✅ | inventory-service consumes `procurement.po.issued`, dedups via `inventory.event_processing_log`, stores issued PO header/line snapshots |
| Unit tests | ✅ | PO issued snapshot + GR create/list/get/complete + stock query + issue-out use-case tests pass |
| Goods Receipt create/list/detail API | ✅ | `GET/POST /api/v1/goods-receipts`, `GET /api/v1/goods-receipts/{id}`; creates DRAFT GR from issued PO snapshot with DB idempotency |
| Complete GR + stock receipt movement | ✅ | `POST /api/v1/goods-receipts/{id}/complete`; resolves catalog `itemCode`, updates `stock_entries`, creates `RECEIPT_IN` movements, publishes `inventory.gr.created` |
| Stock list/movement API | ✅ | `GET /api/v1/items/{itemCode}/stock`, `GET /api/v1/warehouses/{id}/stock`, `GET /api/v1/stock/movements`; read-only stock projections with `GR_VIEW` |
| Issue-out API | ✅ | `POST /api/v1/stock/issue-out`; validates active item/warehouse, decrements stock atomically, stores idempotent request header, creates `ISSUE_OUT` movements |
| Catalog item backend API | ✅ | 2026-06-12: Slice 4 done — `GET/POST /api/v1/items`, `GET/PUT /api/v1/items/{itemCode}` implemented with Item domain/repository/use cases/controller, `ADMIN_CATALOG_MANAGE` mutation permission, `GR_VIEW` read permission, idempotency log table `inventory.catalog_item_mutation_requests`, unit tests for duplicate/missing/search/update/replay; `mvn -pl services/inventory-service test` passes |
| Frontend: GR List & Create | ✅ | 2026-06-12: Slice 3 done — GR list has `po_id` quick filter, create form supports rejected quantity, rejection reason, and lot number, and sends backend-compatible line payload; `npm run build` passes |
| Frontend: GR Detail & Complete | ✅ | 2026-06-12: Slice 3 done — detail displays lot/rejection fields, links stock/movements by warehouse/item, stores complete response summary with movements created and updated stock balances; `npm run build` passes |
| Frontend: Stock Dashboard & Movements | ✅ | 2026-06-12: Slice 0-1 done — `StockEntry` model aligned to backend, `StockService` added, Inventory permissions normalized away from non-seeded `STOCK_VIEW`, routes `/inventory/stock` and `/inventory/stock/movements` added, warehouse stock dashboard and immutable movement ledger implemented; `npm run build` passes |
| Frontend: Issue Out Stock | ✅ | 2026-06-12: Slice 2 done — route `/inventory/issue-out` added with `GR_ISSUE_OUT`, form uses warehouse stock as item selector, validates available quantity before submit, posts `StockService.issueOut()` with generated `Idempotency-Key`, displays created movements and reloads stock; `npm run build` passes |
| Frontend: Catalog UI | ✅ | 2026-06-12: Slice 5 done — added `InventoryCatalogService`, catalog models, `/inventory/catalog` list with filters/create/edit modal, `/inventory/catalog/:itemCode` detail with stock summary/edit modal, sidebar/breadcrumb/i18n wiring; read routes use `GR_VIEW`, mutation controls require `ADMIN_CATALOG_MANAGE`; `npm run build` passes |

### E09: Invoice & Payment
| Task | Status | Ghi chú |
|---|---|---|
| User story/use case chi tiết | ✅ | `docs/user-stories/E09-invoice-payment.md` |
| Invoice schema foundation | ✅ | `finance.invoices`, `finance.invoice_line_items`, idempotency key, vendor+invoice unique index |
| Invoice create/list/detail API | ✅ | `GET/POST /api/v1/invoices`, `GET /api/v1/invoices/{id}`; creates `PENDING_MATCH` invoices from existing PO snapshot |
| Unit tests | ✅ | Invoice create/list/detail + idempotency/missing PO/vendor mismatch tests pass |
| 3-way match API | ✅ | `POST /api/v1/invoices/{id}/match`; consumes `inventory.gr.created` into finance GR snapshots, compares PO + GR + Invoice, publishes `finance.invoice.matched` when matched |
| Approve/dispute/payment actions | ✅ | `POST /approve`, `/dispute`, `/confirm-payment`; stores `finance.payments` and marks invoice `PAID` |
| Frontend: Invoice & Payment UI | ✅ | 2026-06-12: `/finance/invoices` list/filter/summary, `/finance/invoices/create` from invoiceable PO with PO line baseline, `/finance/invoices/:id` match/approve/dispute/confirm-payment actions; PO detail links to create invoice |
| Budget spent ledger link | ✅ | Released commitment hold and recorded invoice spend on payment confirmation |


### E12: Analytics & Reports
| Task | Status | Ghi chú |
|---|---|---|
| User story/use case chi tiết | ✅ | `docs/user-stories/E12-analytics-reports.md` |
| Spring Boot analytics-service module setup | ✅ | Maven module + Docker/compose service on port 8087 |
| Flyway migration analytics read model | ✅ | `db_analytics`, schema `analytics`, executive dashboard snapshot tables |
| Executive dashboard API foundation | ✅ | `GET /api/v1/dashboard/executive` guarded by `REPORT_VIEW`, returns fresh snapshot or empty dashboard |
| Unit tests | ✅ | Executive dashboard + analytics projection use-case tests pass |
| Projection/event ingestion | ✅ | Consumes `procurement.pr.submitted`, `procurement.po.issued`, `finance.invoice.matched`, `approval.sla.breached`, `approval.step.assigned`, `procurement.rfq.awarded`, `inventory.gr.created`; stores idempotent projections and refreshes executive dashboard snapshots where applicable |
| Manager/purchasing/requester dashboards | ✅ | `/purchasing` reads PO + invoice projections; `/manager` reads PR submitted + SLA breach projections scoped by dept; `/requester` reads PR submitted projections scoped by requester; RFQ/GR-specific purchasing metrics remain follow-up |
| KPI/report export APIs | ✅ | Async report export API, cycle-time KPI from PR submitted + PO issued projections, SLA KPI with assigned-step denominator, local PDF worker with JasperReports templates, POI-based XLSX workbook export, and type-specific report datasets for all report types. Source-enrichment contracts remain future work for IAM department labels, `departmentId`/`status` filters, RFQ baseline prices, finance budget plan snapshots, maverick-abuse events, and immutable system audit projection. |
| Frontend: Analytics dashboard/report export UI | ✅ | 2026-06-12: `/dashboard` thay mock bằng analytics dashboard thật, permission-aware tabs executive/manager/purchasing/requester, cycle-time/SLA KPI panels, async report export form + job refresh/download; i18n VI/EN |

### E15: Testing & CI/CD
| Task | Status | Ghi chú |
|---|---|---|
| E15-A Runtime/API Smoke Pack | ✅ | `tests/smoke/e15-runtime-smoke.mjs` passed on 2026-06-07: Docker compose health, HTTP health, and gateway API flow login -> PR -> approval -> manual PO -> PO send -> GR -> invoice -> match/approve/pay -> notification -> analytics dashboard/KPI |
| Smoke seed actors | ✅ | IAM Flyway `V8__seed_e15_smoke_actors.sql` adds `purchasing`, `warehouse`, `accountant`, `superadmin`, `PURCHASING`/`WAREHOUSE`/`ACCOUNTANT`/`SUPER_ADMIN`, and `NOTIFICATION_VIEW_OWN` mapping for runtime smoke |
| Smoke budget seed | ✅ | Finance Flyway `V10__seed_procurement_smoke_budget.sql` adds active 2026 PROCUREMENT budget for requester department so PR submit uses real finance budget check |
| Smoke catalog/inventory seed | ✅ | PR Flyway `V5__seed_e15_smoke_catalog_item.sql` and Inventory Flyway `V6__seed_e15_smoke_inventory_item.sql` add deterministic `E15-OFFICE-KIT` item data used by create PR and GR completion |
| Runtime smoke local docs | ✅ | `tests/smoke/README.md` documents alternate host ports, service DNS overrides, disabled local trace export, seed actors, and script modes |
| Newman/Postman CI pack | ⬜ | Follow-up after runtime script baseline is green on Docker |
| JMeter/performance baseline | ⬜ | Follow-up after runtime script baseline is green on Docker |
| Jenkins pipeline hardening | ⬜ | Follow-up after smoke/Newman/perf baseline |
