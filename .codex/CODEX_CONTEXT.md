# Codex Context — eProcure Enterprise

File này là ngữ cảnh cá nhân hoá ngắn cho Codex trong workspace này. Nó không thay thế `AGENTS.md`; khi làm việc, luôn đọc `AGENTS.md` trước, rồi dùng file này để định hướng nhanh.

## Source Of Truth

Đọc theo thứ tự ưu tiên:

1. `AGENTS.md` — orchestration, invariants, agent/task routing.
2. `.codex/CODEX_CONTEXT.md` — bootstrap ngắn cho Codex.
3. `agent/memory/project-context.md`, `tech-stack.md`, `architecture-map.md`, `coding-patterns.md`, `domain-glossary.md` — context nền.
4. `.cursor/rules/*.mdc` — rule chi tiết theo layer.
5. `docs/PROJECT_BRIEF.md`, `docs/DOMAIN_MODEL.md`, `docs/DATABASE_SCHEMA.md`, `docs/api/*.openapi.yaml`, `docs/adr/*.md` — nghiệp vụ, schema, API, ADR.
6. Source code hiện tại trong `services/`, `frontend/eprocure-web/`, `infra/`.

Nếu tài liệu và code mâu thuẫn, ưu tiên `AGENTS.md` và `.cursor/rules/`, sau đó đọc code hiện tại để giữ tương thích. Với business rule chưa rõ, hỏi người dùng thay vì tự đoán.

## Project Snapshot

eProcure Enterprise là hệ thống mua sắm nội bộ cho doanh nghiệp 200+ nhân viên, đi theo Microservices + Clean Architecture + Event-Driven Kafka.

Hiện trạng triển khai:

- Maven reactor: `services/iam-service`, `services/purchase-request-service`, `services/approval-service`, `services/finance-service`, `services/inventory-service`, `services/vendor-service`, `services/notification-service`, `infra/keycloak/eprocure-keycloak-provider`.
- Frontend: Angular 21 standalone app tại `frontend/eprocure-web`.
- Infra: Docker Compose có PostgreSQL 15, Redis 7, Kafka KRaft, Keycloak, NGINX gateway, Prometheus, Grafana, Loki, Tempo.
- Epics đã đóng theo tracker: E01 infrastructure, E03 UI shell/design system, E04 PR service, E05 Approval Engine, E13-A Admin Portal UI.
- E02 IAM đã hoàn thiện business/API surface và hardening tracker hiện tại: RSA+AES request decryption interceptor, password reset delivery qua `notification.email.send`, opaque session/RBAC/org/delegation/2FA/OAuth/reset-password, và unit tests.
- `finance-service` đã có budget foundation, dashboard/list, override/transfer, PR budget event ledger, budget alert publisher, PO foundation từ RFQ award, draft edit/send/cancel, `procurement.po.issued`, vendor email handoff, E09 invoice foundation (`GET/POST /api/v1/invoices`, `GET /api/v1/invoices/{id}`), E09 3-way match (`POST /api/v1/invoices/{id}/match`, finance GR snapshots từ `inventory.gr.created`, `finance.invoice.matched`), và E09 approve/dispute/payment actions (`POST /approve`, `/dispute`, `/confirm-payment`, `finance.payments`). `notification-service` đã có in-app notifications, business-event consumption, STOMP realtime delivery, Angular shell notification bell/feed, email dispatch outbox/retry/DLQ, template admin API/UI, và PO issued notification handling. `vendor-service` đã có E06 Vendor master + AVL foundation (`GET/POST /vendors`, `GET /vendors/{id}`, `PATCH /vendors/{id}/approve`, Flyway `vendors/vendor_contacts/vendor_scores`, Docker Compose port 8086), RFQ foundation (`rfqs/rfq_line_items/rfq_invitations`, `GET/POST /rfq`, `GET /rfq/{id}`, `PATCH /rfq/{id}/close`) và quote/award foundation (`vendor_quotes/vendor_quote_line_items`, `POST /rfq/{id}/quotes`, `POST /rfq/{id}/quotes/{quoteId}/evaluate`, `POST /rfq/{id}/award`). `inventory-service` đã hoàn tất E08 backend slice: Maven/Docker/Compose service port 8085, Flyway inventory base schema, consumer `procurement.po.issued`, PO snapshot persistence/idempotency log, GR draft APIs, complete GR receipt-in, stock query APIs, và issue-out API (`POST /api/v1/stock/issue-out`) ghi idempotent `ISSUE_OUT` movements. `analytics-service` đã có foundation (`db_analytics`, schema `analytics`, `GET /api/v1/dashboard/executive` guarded by `REPORT_VIEW`), projection ingestion từ `procurement.po.issued`, `finance.invoice.matched`, `approval.sla.breached`, role dashboard API foundation cho `/manager`, `/purchasing`, `/requester`, purchasing dashboard data từ issued PO/matched invoice projections, async report export job API foundation, KPI endpoint foundation (`/kpi/cycle-time`, `/kpi/sla-compliance`), local PDF/XLSX report worker foundation, và projection-backed report datasets có filter + type-specific native layouts cho `PO_SUMMARY`, `PR_SUMMARY`, `SLA_COMPLIANCE`, `THREE_WAY_MATCH`. `admin-service` chưa có backend riêng ngoài docs/OpenAPI.
- Việc nên ưu tiên tiếp theo: tiếp tục trên nhánh rộng E12 analytics reporting với Jasper template engine hoặc bổ sung PR lifecycle / approval completion projections để KPI có dữ liệu thật; E12 manager/requester projections, RFQ savings/department spend, E12 RFQ/GR purchasing metrics, E09 budget spent ledger và E07 manual/direct PO vẫn deferred đến khi source contract rõ. User stories chi tiết đã có tại `docs/user-stories/E06-rfq-vendor.md`, `docs/user-stories/E09-invoice-payment.md`, `docs/user-stories/E10-budget-management.md`, `docs/user-stories/E11-notification-realtime.md`, và `docs/user-stories/E12-analytics-reports.md`.

## Non-Negotiable Invariants

- Không tạo HTTP DELETE endpoint. Dùng soft delete và `PATCH .../cancel`, `.../deactivate`, `.../revoke`.
- Domain layer là POJO thuần: không Spring, Jakarta, MyBatis, Jackson annotation.
- `@PreAuthorize` dùng permission code với `hasAuthority(...)`, không hardcode role.
- `@Transactional` đặt ở UseCase layer.
- Money dùng `BigDecimal` trong Java, `NUMERIC(19,4)` trong PostgreSQL, string trong JSON.
- Public method không return `null`; dùng `Optional<T>` khi có thể không có dữ liệu.
- Log không chứa password, token, secret, key, encrypted payload, private key.
- Token là opaque string 64 chars, không phải JWT.
- `Idempotency-Key` bắt buộc cho mọi POST/PUT/PATCH.
- Domain/entity conversion dùng `ObjectMapper.convertValue()`; không mapping thủ công từng field trừ khi có lý do rất rõ.

## Task Bootstrap

Backend domain task:

- Đọc `agent/memory/domain-glossary.md`, `docs/DOMAIN_MODEL.md`, `agent/knowledge/clean-architecture-patterns.md`, `agent/skills/SK-01-domain-model-generation.md`.
- Kiểm domain package không có framework import.

Backend use case/API task:

- Đọc `agent/memory/coding-patterns.md`, `agent/knowledge/api-idempotency.md`, `agent/knowledge/api-error-codes.md`, `docs/api/{service}.openapi.yaml`.
- TDD khi sửa logic có rủi ro: viết hoặc cập nhật test trước khi sửa.

Infrastructure/DB task:

- Đọc `docs/DATABASE_SCHEMA.md`, `agent/knowledge/mybatis-strategy.md`, `agent/knowledge/soft-delete-strategy.md`, `.cursor/rules/database.mdc`.
- Migration dùng version tiếp theo trong đúng service, không sửa migration cũ nếu đã tồn tại trong lịch sử.

Frontend task:

- Đọc `agent/knowledge/frontend-design-system.md`, `.cursor/rules/angular-frontend.mdc`, OpenAPI spec liên quan.
- Giữ `ChangeDetectionStrategy.OnPush`, `takeUntilDestroyed`, `withCredentials`, `Idempotency-Key`, translate pipe, CSS custom properties, `ep-*` shared components.

DevOps task:

- Đọc `agent/knowledge/environment-variables.md`, `docs/ENV_CONFIG.md`, `.cursor/rules/docker-resource.mdc`, `agent/skills/SK-30-docker-resource-budget.md`.
- Tôn trọng resource budget cho máy 8GB RAM.

Review task:

- Lead with findings by severity.
- Kiểm anti-pattern trong `AGENTS.md` mục 7 và checklist cuối.

## Common Commands

Root/backend:

```powershell
mvn test
mvn -pl services/iam-service test
mvn -pl services/purchase-request-service test
mvn -pl services/approval-service test
mvn -pl infra/keycloak/eprocure-keycloak-provider test
mvn -pl services/finance-service test
mvn -pl services/inventory-service test
mvn -pl services/vendor-service test
mvn -pl services/notification-service test
```

Frontend:

```powershell
Set-Location frontend/eprocure-web
npm install
npm run build
npm test
```

Infra:

```powershell
Copy-Item .env.example .env
docker compose up -d postgres redis kafka kafka-init keycloak nginx-gateway
docker compose --profile monitoring up -d prometheus grafana loki tempo
```

## Documentation Updates

Khi hoàn thành task:

- Cập nhật `agent/memory/progress-tracker.md` nếu task/epic status thay đổi.
- Cập nhật `agent/memory/decision-log.md` nếu có quyết định kỹ thuật mới.
- Cập nhật `agent/memory/error-history.md` khi fix bug đáng nhớ hoặc gặp lỗi lặp lại.
- Cập nhật OpenAPI trong `docs/api/` khi thêm/sửa endpoint.
- Cập nhật ADR nếu thay đổi kiến trúc hoặc quyết định công nghệ.

## Current Pitfalls To Avoid

- `agent/memory/domain-glossary.md` là nguồn thuật ngữ ngắn; nếu thiếu thuật ngữ, bổ sung từ `docs/DOMAIN_MODEL.md`.
- `.cursor/rules` trong repo dùng tên thực tế: `coding.mdc`, `database.mdc`, `logging.mdc`, `error-codes.mdc`, `git-workflow.mdc`, `docker-resource.mdc`, `testing.mdc`, `angular-frontend.mdc`.
- `spring-boot-starter-logging` phải được exclude; dùng Log4j2.
- Keycloak custom provider nằm trong Maven reactor; khi đổi module reactor phải kiểm Dockerfile build cache của IAM/Keycloak.
- OAuth callback browser flow phải redirect 302 về frontend, không trả raw JSON.
