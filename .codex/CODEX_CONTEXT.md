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

- Maven reactor: `services/iam-service`, `services/purchase-request-service`, `infra/keycloak/eprocure-keycloak-provider`.
- Frontend: Angular 21 standalone app tại `frontend/eprocure-web`.
- Infra: Docker Compose có PostgreSQL 15, Redis 7, Kafka KRaft, Keycloak, NGINX gateway, Prometheus, Grafana, Loki, Tempo.
- Epics xong chính: E01 infra, E02 IAM phần lớn, E03 UI shell/design system, E04 PR service, E13-A Admin Portal UI.
- Epic chưa làm chính: E05 Approval Engine, E06-E12, phần còn lại E13, E14, E15.

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
mvn -pl infra/keycloak/eprocure-keycloak-provider test
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
