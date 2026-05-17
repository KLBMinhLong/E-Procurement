# SKILLS LAYER — eProcure Enterprise
## Agent Capability Registry · Antigravity Coding Agent

---

> **Version:** 1.0.0
> **Scope:** Full-stack microservices — Java Spring Boot · Angular · PostgreSQL · Kafka · Redis · Camunda · Docker
> **Audience:** AI coding agent (Antigravity) thực thi code generation, scaffolding, review, và migration tasks
> **Dependency:** Đọc sau `README.md`, `CODING_GUIDE.md`, `DATABASE_SCHEMA.md`, `DOMAIN_MODEL.md`

---

## OVERVIEW — Cách đọc tài liệu này

Mỗi **Skill** là một khả năng độc lập mà agent có thể kích hoạt. Mỗi skill bao gồm:

- **Trigger** — Khi nào kích hoạt skill này
- **Inputs** — Agent cần biết gì trước khi chạy
- **Rules** — Ràng buộc bất biến (vi phạm = sai hoàn toàn)
- **Output Contract** — Kết quả đầu ra phải đáp ứng gì
- **Template / Pattern** — Code mẫu chuẩn
- **Checklist** — Tự kiểm tra trước khi deliver

---

## SKILL INDEX

| ID | Skill Name | Layer | Mức độ phức tạp |
|---|---|---|---|
| SK-01 | [Domain Model Generation](SK-01-domain-model-generation.md) | Domain | ★★★ |
| SK-02 | [Use Case Scaffolding](SK-02-use-case-scaffolding.md) | Application | ★★★ |
| SK-03 | [Repository & MyBatis Mapper](SK-03-repository-mybatis-mapper.md) | Infrastructure | ★★★ |
| SK-04 | [REST Controller Generation](SK-04-rest-controller-generation.md) | Presentation | ★★ |
| SK-05 | [Flyway Migration Script](SK-05-flyway-migration-script.md) | Database | ★★★ |
| SK-06 | [Kafka Producer & Consumer](SK-06-kafka-producer-consumer.md) | Infrastructure | ★★★ |
| SK-07 | [Redis Cache Adapter](SK-07-redis-cache-adapter.md) | Infrastructure | ★★ |
| SK-08 | [Exception & Error Code](SK-08-exception-error-code.md) | Common | ★★ |
| SK-09 | [Security & RBAC Guard](SK-09-security-rbac-guard.md) | Security | ★★★★ |
| SK-10 | [Encryption Interceptor](SK-10-encryption-interceptor.md) | Security | ★★★★ |
| SK-11 | [JWT / Token Handling](SK-11-jwt-token-handling.md) | Security | ★★★★ |
| SK-12 | [Approval Engine (Camunda)](SK-12-approval-engine-camunda.md) | Workflow | ★★★★★ |
| SK-13 | [Budget Check Logic](SK-13-budget-check-logic.md) | Domain | ★★★★ |
| SK-14 | [Notification Dispatch](SK-14-notification-dispatch.md) | Application | ★★★ |
| SK-15 | [JUnit Unit Test](SK-15-junit-unit-test.md) | Testing | ★★★ |
| SK-16 | [Flyway Rollback Plan](SK-16-flyway-rollback-plan.md) | Database | ★★★ |
| SK-17 | [Docker Compose Service](SK-17-docker-compose-service.md) | Infrastructure | ★★ |
| SK-18 | [Angular Component (Smart)](SK-18-angular-component-smart.md) | Frontend | ★★★ |
| SK-19 | [Angular Service & HTTP](SK-19-angular-service-http.md) | Frontend | ★★★ |
| SK-20 | [Angular Guard & Interceptor](SK-20-angular-guard-interceptor.md) | Frontend | ★★★★ |
| SK-21 | [Angular i18n (ngx-translate)](SK-21-angular-i18n-ngx-translate.md) | Frontend | ★★ |
| SK-22 | [Log4j2 Custom Layout](SK-22-log4j2-custom-layout.md) | Observability | ★★ |
| SK-23 | [OpenTelemetry Tracing](SK-23-opentelemetry-tracing.md) | Observability | ★★★ |
| SK-24 | [Audit Log Writer](SK-24-audit-log-writer.md) | Security | ★★★ |
| SK-25 | [ObjectMapper Conversion](SK-25-objectmapper-conversion.md) | Common | ★★ |
| SK-26 | [Idempotency Handler](SK-26-idempotency-handler.md) | Common | ★★★ |
| SK-27 | [Pagination Response](SK-27-pagination-response.md) | Presentation | ★★ |
| SK-28 | [JasperReport Template](SK-28-jasperreport-template.md) | Reporting | ★★★ |
| SK-29 | [Keycloak Custom Provider](SK-29-keycloak-custom-provider.md) | Security | ★★★★ |
| SK-30 | [Docker Resource Budget](SK-30-docker-resource-budget.md) | Infrastructure | ★★ |

---

## SKILLS (Separated Files)

Mỗi skill được tách thành file riêng trong thư mục này. Dùng bảng SKILL INDEX phía trên để mở nhanh.

---

## AGENT EXECUTION PROTOCOL

Khi nhận yêu cầu code, agent thực hiện theo thứ tự:

```
1. IDENTIFY   → Xác định Skill ID cần dùng (có thể nhiều skill cùng lúc)
2. READ RULES → Đọc lại Rules của từng skill — không bỏ qua
3. CHECK CONTEXT:
     - Service nào? (iam, pr, approval, finance, inventory, vendor, notification)
     - Schema nào? (iam, pr, approval, finance, inventory, vendor, notification, audit)
     - Permission code nào cần thêm?
4. GENERATE   → Dùng template, thay thế {placeholder} bằng giá trị thực
5. SELF-REVIEW:
     [ ] Không có DELETE endpoint
     [ ] Không có JPA/Hibernate import trong domain
     [ ] Không có hardcode role trong @PreAuthorize
     [ ] Không có hardcode string tiếng Việt trong Angular template
     [ ] Không có hardcode màu trong CSS
     [ ] BigDecimal cho tiền tệ
     [ ] UUID cho ID
     [ ] is_deleted filter trong mọi query
     [ ] TZ=Asia/Ho_Chi_Minh trong Docker env
     [ ] Resource limits đặt đúng
     [ ] JavaDoc trên public API
     [ ] Error code theo convention của service
6. DELIVER    → Code hoàn chỉnh, không thiếu import, không có TODO chưa giải quyết
```

---

## CROSS-CUTTING CHECKLIST (Áp dụng cho MỌI output)

```
SECURITY
  [ ] @PreAuthorize với permission code (không phải role)
  [ ] Không log sensitive data (password, token, key)
  [ ] x-api-key validate trong mọi service (downstream)
  [ ] HttpOnly Cookie cho token

DATABASE
  [ ] Không dùng schema public
  [ ] Mọi table có audit fields (is_deleted, deleted_at, deleted_by, created_by, created_at)
  [ ] NUMERIC(19,4) cho tiền tệ
  [ ] TIMESTAMPTZ (UTC) cho timestamps
  [ ] Index có WHERE is_deleted = false

BACKEND
  [ ] Soft delete (không có DELETE endpoint, không có DB DELETE)
  [ ] @Transactional ở UseCase layer
  [ ] ObjectMapper cho conversion (không manual mapping)
  [ ] Idempotency key handled
  [ ] Pagination trả về đúng ApiResponse + meta format

FRONTEND
  [ ] | translate pipe cho mọi text hiển thị
  [ ] CSS variable cho màu sắc
  [ ] Loading + Error state
  [ ] withCredentials: true cho HTTP
  [ ] Idempotency-Key header trong interceptor

INFRASTRUCTURE
  [ ] Docker resource limits đặt
  [ ] TZ=Asia/Ho_Chi_Minh
  [ ] healthcheck configured
  [ ] SPRING_PROFILES_ACTIVE từ env var

OBSERVABILITY
  [ ] Log đúng layer (FILTER/CONTROLLER/SERVICE/REPO/CACHE/SECURITY/AUDIT)
  [ ] [ACTION] Start / Complete log tại Service layer
  [ ] traceId/spanId trong MDC (OpenTelemetry inject tự động)
  [ ] Audit log cho mọi state-changing operation
```