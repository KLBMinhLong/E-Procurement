# AGENTS.md
## eProcure Enterprise — Master Agent Orchestration

---

> **Mục đích:** File này là điểm điều phối trung tâm cho mọi AI agent làm việc trong dự án eProcure.  
> **Đọc file này TRƯỚC TIÊN** trước khi đọc bất kỳ file nào khác.  
> **Áp dụng cho:** Claude, Cursor, Copilot, hoặc bất kỳ AI coding assistant nào.

---

## 1. NGUYÊN TẮC NỀN TẢNG (ĐỌC VÀ GHI NHỚ)

Trước khi làm bất cứ việc gì, agent PHẢI nội tâm hoá các nguyên tắc sau:

```
① Không có HTTP DELETE. Toàn hệ thống dùng soft delete.
② Domain layer là POJO thuần — ZERO Spring/Jakarta/MyBatis annotation.
③ @PreAuthorize dùng permission code — KHÔNG BAO GIỜ hardcode role.
④ @Transactional đặt ở UseCase layer — không phải Repository, không phải Controller.
⑤ Tiền tệ luôn là BigDecimal trong Java và NUMERIC(19,4) trong DB.
⑥ KHÔNG return null từ public method — dùng Optional<T>.
⑦ Log không được chứa password, token, secret, key.
⑧ Token là opaque string 64 chars — KHÔNG phải JWT.
⑨ Idempotency-Key bắt buộc cho mọi POST/PUT/PATCH endpoint.
⑩ ObjectMapper để convert domain ↔ entity — không mapping thủ công từng field.
```

---

## 2. HỆ THỐNG FILE THAM CHIẾU

Agent phải biết cấu trúc file để tra cứu đúng nguồn khi cần:

```
AGENTS.md                          ← File này — đọc đầu tiên

agent/memory/                      ← Bộ nhớ ngữ cảnh (đọc khi bắt đầu task)
  project-context.md               → Tổng quan hệ thống, quyết định thiết kế
  tech-stack.md                    → Versions, config patterns, JVM flags
  architecture-map.md              → Service dependencies, data flow
  domain-glossary.md               → Thuật ngữ nghiệp vụ VI/EN
  coding-patterns.md               → Code templates tái sử dụng
  progress-tracker.md              → Epics đã/đang/chưa làm
  decision-log.md                  → Decisions được ghi lại khi làm
  error-history.md                 → Bugs và cách fix đã gặp

agent/knowledge/                   ← Kiến thức chuyên sâu (tra cứu khi cần)
  api-authentication.md            → Token flow, cookie, Keycloak
  api-conventions.md               → REST conventions, response format
  api-error-codes.md               → Toàn bộ error codes theo service
  api-idempotency.md               → Idempotency implementation
  api-pagination.md                → Offset + cursor pagination
  camunda-workflow.md              → Camunda BPMN APIs, patterns
  clean-architecture-patterns.md   → Layer rules, dependency flow
  database-schema-overview.md      → Schema layout, index strategy
  domain-model-overview.md         → Aggregates, entities, events
  environment-variables.md         → Toàn bộ ENV vars
  frontend-design-system.md        → Design tokens, ep-* components
  kafka-topics.md                  → Topics, message envelope
  keycloak-custom-provider.md      → Custom provider implementation
  log4j2-config-reference.md       → Log4j2 XML, patterns, layout
  mybatis-strategy.md              → Annotation vs XML, patterns
  observability-stack.md           → OTel, Prometheus, Grafana, Loki
  opentelemetry-java-guide.md      → OTel agent setup, propagation
  profiles-strategy.md             → local/dev/prod profile rules
  rbac-permission-codes.md         → Toàn bộ permission codes
  redis-cache-patterns.md          → Redis key patterns, TTL
  security-encryption.md           → RSA+AES implementation
  service-registry.md              → Service ports, dependencies
  soft-delete-strategy.md          → Soft delete patterns, queries
  spring-boot-best-practices.md    → Spring-specific patterns
  timezone-financial-precision.md  → BigDecimal, TIMESTAMPTZ

agent/skills/                      ← Kỹ năng thực thi (dùng khi code)
  SK-01 đến SK-30                  → Xem README.md trong skills/

.cursor/rules/                     ← Rules luôn áp dụng (Cursor tự đọc)
  CODING_RULES.mdc
  SECURITY_RULES.mdc
  LOG_RULES.mdc
  ERROR_CODE_RULES.mdc
  GIT_WORKFLOW_RULES.mdc
  DOCKER_RESOURCE_RULES.mdc
  DATABASE_RULES.mdc
  TESTING_RULES.mdc
  ANGULAR_RULES.mdc

docs/                              ← Tài liệu nghiệp vụ và kỹ thuật
  PROJECT_BRIEF.md
  DOMAIN_MODEL.md
  DATABASE_SCHEMA.md
  CODING_GUIDE.md
  ENV_CONFIG.md
  adr/                             → 13 Architecture Decision Records
  api/                             → OpenAPI specs + common conventions
```

---

## 3. CÁC LOẠI AGENT & NHIỆM VỤ

### AGENT-01: ARCHITECT AGENT
**Kích hoạt khi:** Thiết kế service mới, thay đổi kiến trúc, quyết định công nghệ

**Nguồn tham chiếu bắt buộc:**
```
agent/memory/project-context.md
agent/memory/architecture-map.md
docs/adr/                         ← Đọc tất cả ADRs trước khi ra quyết định
docs/DOMAIN_MODEL.md
agent/knowledge/clean-architecture-patterns.md
```

**Quy trình làm việc:**
```
1. Đọc project-context.md để hiểu hệ thống hiện tại
2. Đọc các ADRs liên quan để không mâu thuẫn với quyết định cũ
3. Đề xuất giải pháp mới nếu cần
4. Tạo ADR mới trong docs/adr/ theo format chuẩn
5. Cập nhật architecture-map.md nếu thay đổi service dependencies
6. Cập nhật decision-log.md với quyết định mới
```

**Output chuẩn:**
- ADR file mới: `docs/adr/ADR-{N}-{description}.md`
- Cập nhật `agent/memory/architecture-map.md`
- Cập nhật `agent/memory/decision-log.md`

---

### AGENT-02: BACKEND DOMAIN AGENT
**Kích hoạt khi:** Tạo domain model, entity, value object, domain event

**Nguồn tham chiếu bắt buộc:**
```
agent/memory/project-context.md     ← Đọc trước
agent/memory/domain-glossary.md     ← Tên đặt theo đây
docs/DOMAIN_MODEL.md               ← Business rules
agent/knowledge/clean-architecture-patterns.md
agent/memory/coding-patterns.md     ← Mục 6: Domain Entity Pattern
agent/skills/SK-01-domain-model-generation.md
```

**Quy tắc bất biến:**
```java
// Domain package: com.eprocure.{service}.domain.model
// KHÔNG import Spring, Jakarta, MyBatis, Jackson
// Constructor private → dùng static factory method create()
// Không có setter → chỉ có business methods
// Enum values: UPPER_SNAKE_CASE trong com.eprocure.{service}.domain.model.{Entity}Status
```

**Output chuẩn:**
```
src/main/java/com/eprocure/{service}/domain/
  model/{Entity}.java
  model/vo/{ValueObject}.java        (nếu cần)
  model/{Entity}Status.java          (enum)
  repository/{Entity}Repository.java (interface, POJO thuần)
  event/{Entity}CreatedEvent.java    (record, nếu cần)
```

---

### AGENT-03: BACKEND USE CASE AGENT
**Kích hoạt khi:** Viết application layer, use cases, commands

**Nguồn tham chiếu bắt buộc:**
```
agent/memory/coding-patterns.md     ← Mục 1: UseCase Pattern
agent/knowledge/api-idempotency.md  ← Luôn xử lý idempotency
agent/knowledge/api-error-codes.md  ← Throw đúng error code
agent/skills/SK-02-use-case-scaffolding.md
agent/skills/SK-08-exception-error-code.md
agent/skills/SK-26-idempotency-handler.md
```

**Checklist trước khi viết UseCase:**
```
□ UseCase nhận Command object (không nhận Request DTO trực tiếp)
□ Bước 1 luôn là idempotency check
□ @Transactional đặt tại method execute()
□ Log [ACTION] Start/Step/Complete với masked userId
□ Domain validation throw BusinessException có error code
□ Không gọi UseCase từ UseCase khác
□ Không có @Autowired (dùng @RequiredArgsConstructor)
```

**Output chuẩn:**
```
src/main/java/com/eprocure/{service}/application/
  usecase/{Verb}{Noun}UseCase.java
  port/in/{Verb}{Noun}Command.java
  port/out/{ExternalService}Port.java  (nếu gọi service khác)
```

---

### AGENT-04: BACKEND INFRASTRUCTURE AGENT
**Kích hoạt khi:** Viết Repository impl, MyBatis mapper, Kafka, Redis adapter

**Nguồn tham chiếu bắt buộc:**
```
agent/memory/coding-patterns.md     ← Mục 2,3,4: Repo + Mapper Pattern
agent/knowledge/mybatis-strategy.md
agent/knowledge/redis-cache-patterns.md
agent/knowledge/kafka-topics.md
agent/knowledge/soft-delete-strategy.md
docs/DATABASE_SCHEMA.md             ← Tên table, column, index
agent/skills/SK-03-repository-mybatis-mapper.md
agent/skills/SK-06-kafka-producer-consumer.md
agent/skills/SK-07-redis-cache-adapter.md
agent/skills/SK-25-objectmapper-conversion.md
```

**Quy tắc MyBatis:**
```
Simple CRUD (1 table, không JOIN) → annotation trong @Mapper interface
Complex query (JOIN, dynamic WHERE, aggregation) → XML mapper
ORDER BY PHẢI qua whitelist mapping — không inject raw string
Luôn có WHERE is_deleted = false trong SELECT chính
Dùng COUNT(*) OVER() cho pagination (1 query)
```

**Output chuẩn:**
```
src/main/java/com/eprocure/{service}/infrastructure/
  persistence/
    entity/{Entity}DbEntity.java
    mapper/{Entity}Mapper.java         (@Mapper interface)
    repository/{Entity}RepositoryImpl.java
  redis/{Entity}CacheAdapter.java
  kafka/producer/{Entity}EventProducer.java
  kafka/consumer/{Entity}EventConsumer.java

src/main/resources/
  mapper/{Entity}Mapper.xml           (complex queries)
  db/migration/V{N}__{description}.sql
```

---

### AGENT-05: BACKEND API AGENT
**Kích hoạt khi:** Viết Controller, Request/Response DTO, Presentation mapper

**Nguồn tham chiếu bắt buộc:**
```
agent/memory/coding-patterns.md     ← Mục 5: Controller Pattern
agent/knowledge/api-conventions.md  ← URL naming, response format
agent/knowledge/api-error-codes.md  ← Error codes đúng service
agent/knowledge/api-pagination.md   ← PageRequest, PageMeta
docs/api/{service}.openapi.yaml     ← API spec đã định nghĩa
agent/skills/SK-04-rest-controller-generation.md
agent/skills/SK-27-pagination-response.md
```

**Checklist Controller:**
```
□ @PreAuthorize("hasAuthority('...')") trên mọi endpoint — permission code
□ @Valid trên @RequestBody
□ @RequestHeader("Idempotency-Key") cho POST/PUT/PATCH
□ log.info("[CONTROLLER] METHOD /path | userId={}") khi vào
□ Delegate ngay sang UseCase — không có if/else nghiệp vụ
□ Response bọc trong ApiResponse<T>
□ Không có HTTP DELETE method
□ PATCH .../cancel hoặc .../deactivate thay cho DELETE
```

**API Response format phải tuân thủ:**
```json
{ "success": true/false, "code": "...", "message": null/"...",
  "data": {...}, "meta": null/{pagination}, "timestamp": "...", "requestId": "..." }
```

---

### AGENT-06: SECURITY AGENT
**Kích hoạt khi:** Viết auth flow, encryption, token management, security config

**Nguồn tham chiếu bắt buộc:**
```
agent/knowledge/api-authentication.md
agent/knowledge/security-encryption.md
agent/knowledge/rbac-permission-codes.md
agent/knowledge/keycloak-custom-provider.md
agent/knowledge/redis-cache-patterns.md  ← Session keys
agent/skills/SK-09-security-rbac-guard.md
agent/skills/SK-10-encryption-interceptor.md
agent/skills/SK-11-jwt-token-handling.md
agent/skills/SK-29-keycloak-custom-provider.md
```

**Security invariants (không được vi phạm):**
```
Token: opaque 64 chars, lưu Redis (primary) + DB (backup)
Cookie: HttpOnly + Secure + SameSite=Strict — KHÔNG response body
Single session: login mới → invalidate token cũ ngay lập tức
Password: BCrypt(password + userId_salt, cost=12)
Separation of Duties: requester ≠ approver — check trong UseCase
@PreAuthorize: permission code — KHÔNG role name
SQL injection: #{param} MyBatis — KHÔNG ${param} cho user input
ORDER BY: whitelist mapping — KHÔNG raw string injection
```

---

### AGENT-07: DATABASE AGENT
**Kích hoạt khi:** Viết Flyway migration, SQL schema, index strategy

**Nguồn tham chiếu bắt buộc:**
```
docs/DATABASE_SCHEMA.md             ← Schema hiện tại — đọc trước khi tạo
agent/knowledge/database-schema-overview.md
agent/knowledge/soft-delete-strategy.md
agent/knowledge/timezone-financial-precision.md
agent/skills/SK-05-flyway-migration-script.md
agent/skills/SK-16-flyway-rollback-plan.md
```

**Migration checklist (bắt buộc mỗi file):**
```
□ Naming: V{N}__{description}.sql (N tiếp theo sau file cuối)
□ Schema: CREATE SCHEMA IF NOT EXISTS {schema}; (không dùng 'public')
□ Primary key: UUID DEFAULT gen_random_uuid()
□ Tiền tệ: NUMERIC(19,4) — KHÔNG FLOAT/DOUBLE
□ Timestamp: TIMESTAMPTZ — KHÔNG TIMESTAMP
□ Soft delete: is_deleted BOOLEAN NOT NULL DEFAULT FALSE + deleted_at + deleted_by
□ Audit: created_at TIMESTAMPTZ NOT NULL DEFAULT NOW() + updated_at + created_by
□ Constraints: CHECK constraint cho enum column
□ Indexes: FK columns + partial index (WHERE is_deleted=FALSE) + composite cho common queries
□ Trigger: auto-update updated_at
□ Comment: COMMENT ON TABLE + COMMENT ON COLUMN quan trọng
□ Không sửa file migration đã commit — tạo V_new__ để ALTER
```

---

### AGENT-08: OBSERVABILITY AGENT
**Kích hoạt khi:** Cấu hình logging, tracing, metrics, alerting

**Nguồn tham chiếu bắt buộc:**
```
agent/knowledge/log4j2-config-reference.md
agent/knowledge/observability-stack.md
agent/knowledge/opentelemetry-java-guide.md
agent/skills/SK-22-log4j2-custom-layout.md
agent/skills/SK-23-opentelemetry-tracing.md
agent/skills/SK-24-audit-log-writer.md
```

**Log format bắt buộc theo layer:**
```
[REQUEST]    → Filter: "[REQUEST] METHOD url | ip=x | [RESPONSE] status ms"
[CONTROLLER] → "[CONTROLLER] METHOD /path | userId=masked"
[ACTION]     → "[ACTION] Start|Step|Complete ActionName | key=value"
[REPO]       → "[REPO] operation tableName | id=x" (DEBUG)
[CACHE]      → "[CACHE] hit|miss|put|evict | key=prefix:x" (DEBUG)
[TOKEN]      → "[TOKEN] issue description | ip=x" (WARN)
[APIKEY]     → "[APIKEY] issue | ip=x" (WARN)
[SECURITY]   → "[SECURITY] Unauthorized | userId=x | permission=x" (WARN)
[AUDIT]      → "[AUDIT] action | actor=x | entity=type/id | result=x" (INFO)
[EXCEPTION]  → "[EXCEPTION][CODE] message | context" (WARN/ERROR)
```

**Masking bắt buộc trước khi log:**
```java
email   → LogMaskingUtil.maskEmail(email)    // n***@c***.com
phone   → LogMaskingUtil.maskPhone(phone)    // 09*****678
userId  → LogMaskingUtil.maskId(userId)      // 550e8400...
// KHÔNG LOG: password, token, secret, key, encryptedPayload
```

---

### AGENT-09: FRONTEND AGENT
**Kích hoạt khi:** Viết Angular component, service, interceptor, routing

**Nguồn tham chiếu bắt buộc:**
```
agent/knowledge/frontend-design-system.md
agent/memory/project-context.md         ← API endpoints, auth flow
docs/api/{service}.openapi.yaml         ← API contracts
agent/skills/SK-18-angular-component-smart.md
agent/skills/SK-19-angular-service-http.md
agent/skills/SK-20-angular-guard-interceptor.md
agent/skills/SK-21-angular-i18n-ngx-translate.md
```

**Frontend invariants (không được vi phạm):**
```
ChangeDetectionStrategy.OnPush trên MỌI component
takeUntilDestroyed(this.destroyRef) cho MỌI subscription
translate pipe cho MỌI text hiển thị — không hardcode string
CSS Custom Properties (var(--...)) cho MỌI màu sắc — không hardcode
withCredentials: true trong MỌI HTTP request (cookie auth)
Idempotency-Key header trong MỌI POST/PUT/PATCH
ep-* prefix cho shared components
ep-amount cho tiền tệ, ep-table cho danh sách có pagination
```

---

### AGENT-10: TEST AGENT
**Kích hoạt khi:** Viết unit test, integration test (Postman), setup JMeter

**Nguồn tham chiếu bắt buộc:**
```
agent/knowledge/api-conventions.md   ← Response format để assert
agent/knowledge/api-error-codes.md   ← Error codes để test negative cases
agent/memory/coding-patterns.md      ← Business rules cần test
agent/skills/SK-15-junit-unit-test.md
```

**Test naming convention:**
```java
void should_{expected_behavior}_when_{condition}()
// VD:
void should_create_pr_when_valid_command()
void should_throw_pr_020_when_no_line_items()
void should_return_cached_when_idempotency_hit()
```

**Coverage targets:**
```
domain/     → 80%+ (business logic quan trọng nhất)
application/ → 70%+ (use cases)
Không cần:  infrastructure/ (integration test cover)
```

---

### AGENT-11: DEVOPS AGENT
**Kích hoạt khi:** Viết Dockerfile, docker-compose, Jenkins pipeline

**Nguồn tham chiếu bắt buộc:**
```
agent/knowledge/environment-variables.md
docs/ENV_CONFIG.md
agent/knowledge/observability-stack.md
agent/skills/SK-17-docker-compose-service.md
agent/skills/SK-30-docker-resource-budget.md
```

**Resource limits (máy 8GB RAM — không được vi phạm):**
```yaml
# approval-service (Camunda nặng nhất)
limits: { cpus: '0.50', memory: 768M }

# iam, pr, finance (Standard Java service)
limits: { cpus: '0.25', memory: 512M }

# inventory, vendor, analytics, notification, admin (Light service)
limits: { cpus: '0.25', memory: 256M }

# PostgreSQL, Kafka
limits: { cpus: '0.50', memory: 512M }

# Redis, Keycloak, NGINX
limits: { cpus: '0.25', memory: 512M } hoặc nhỏ hơn
```

**Dockerfile checklist:**
```
□ Multi-stage build (builder + runtime)
□ eclipse-temurin:17-jre-alpine cho runtime
□ Non-root user (addgroup + adduser)
□ HEALTHCHECK với start_period đủ (60-90s cho Java)
□ JAVA_OPTS: -Xmx không vượt 75% memory limit
□ -javaagent:/app/agents/opentelemetry-javaagent.jar
□ TZ environment variable
□ .dockerignore đầy đủ
```

---

## 4. WORKFLOW THEO TASK TYPE

### 4.1 Task: Tạo Service Mới

```
Bước 1: Đọc project-context.md → Hiểu architecture hiện tại
Bước 2: Đọc docs/DOMAIN_MODEL.md → Identify domain objects
Bước 3: Đọc docs/DATABASE_SCHEMA.md → Kiểm tra schema đã có
Bước 4: Tạo theo thứ tự:
  a. Domain Layer     → AGENT-02 (Entity, ValueObject, Repository interface)
  b. Application Layer → AGENT-03 (UseCases, Commands, Ports)
  c. Infrastructure    → AGENT-04 (RepositoryImpl, Mapper, Kafka, Redis)
  d. Presentation      → AGENT-05 (Controller, Request/Response DTO)
  e. Database          → AGENT-07 (Flyway migration)
  f. Observability     → AGENT-08 (log4j2.xml, actuator config)
  g. Container         → AGENT-11 (Dockerfile, docker-compose entry)
  h. Tests             → AGENT-10 (Unit tests, Postman collection)
Bước 5: Cập nhật progress-tracker.md
```

### 4.2 Task: Thêm Feature Vào Service Đã Có

```
Bước 1: Đọc service code hiện tại để hiểu pattern đang dùng
Bước 2: Kiểm tra docs/api/{service}.openapi.yaml để biết API spec
Bước 3: Kiểm tra docs/DATABASE_SCHEMA.md để biết schema hiện tại
Bước 4: Xác định impact:
  - Chỉ thêm UseCase mới? → AGENT-03 + AGENT-05
  - Thêm column vào DB?  → AGENT-07 (Flyway V_new) + AGENT-04
  - Thêm event mới?      → AGENT-04 (Kafka producer/consumer)
Bước 5: Viết unit test trước (TDD approach)
Bước 6: Implement feature
Bước 7: Cập nhật OpenAPI spec nếu thêm endpoint
```

### 4.3 Task: Fix Bug

```
Bước 1: Đọc error-history.md → Xem bug tương tự đã gặp chưa
Bước 2: Reproduce lỗi với unit test (viết test fail trước)
Bước 3: Fix code
Bước 4: Verify test pass
Bước 5: Ghi vào error-history.md: bug mô tả + root cause + fix
```

### 4.4 Task: Review Code

```
Checklist tối thiểu:
□ Không có HTTP DELETE endpoint
□ Domain package không import Spring/Jakarta/MyBatis
□ @PreAuthorize dùng permission code (không role name)
□ @Transactional đặt ở UseCase
□ BigDecimal cho mọi tính toán tiền
□ Optional<T> thay vì return null
□ Log không chứa sensitive data
□ WHERE is_deleted = false trong SELECT query
□ Idempotency-Key được xử lý
□ Soft delete: is_deleted + deleted_at + deleted_by
□ Tiền trong DB: NUMERIC(19,4)
□ ObjectMapper.convertValue() thay vì manual mapping
```

### 4.5 Task: Viết Migration SQL

```
Bước 1: Đọc docs/DATABASE_SCHEMA.md → Lấy version cuối cùng
Bước 2: Xác định V{N+1} tiếp theo
Bước 3: Dùng template từ coding-patterns.md mục 9
Bước 4: Đặt file: src/main/resources/db/migration/V{N}}__{description}.sql
Bước 5: Kiểm tra:
  - Có SCHEMA IF NOT EXISTS không?
  - Tiền dùng NUMERIC(19,4)?
  - Timestamp dùng TIMESTAMPTZ?
  - Soft delete columns có đủ không?
  - Index cho FK columns và common queries?
  - Trigger auto-update updated_at?
  - COMMENT ON TABLE?
  - CHECK constraint cho enum columns?
```

---

## 5. CONTEXT LOADING STRATEGY

Agent phải quyết định đọc file nào tùy theo task để tránh context overflow:

### Task nhỏ (< 1 file thay đổi)
```
Đọc bắt buộc:
  agent/memory/project-context.md   (sections 3,4,14 — quick rules)
  agent/memory/coding-patterns.md   (pattern tương ứng)
  .cursor/rules/ file tương ứng

Đọc khi cần (không bắt buộc):
  docs/DATABASE_SCHEMA.md (nếu liên quan DB)
  docs/api/*.openapi.yaml (nếu liên quan API)
```

### Task trung bình (1 feature hoàn chỉnh)
```
Đọc bắt buộc:
  agent/memory/project-context.md   (toàn bộ)
  agent/memory/domain-glossary.md   (tên đặt đúng)
  agent/memory/coding-patterns.md   (patterns dùng)
  agent/knowledge/ (files liên quan đến feature)
  docs/DOMAIN_MODEL.md              (business rules)
  docs/api/{service}.openapi.yaml   (API contract)
```

### Task lớn (service mới hoặc redesign)
```
Đọc toàn bộ:
  agent/memory/ (tất cả files)
  agent/knowledge/ (files liên quan)
  docs/ (PROJECT_BRIEF, DOMAIN_MODEL, DATABASE_SCHEMA)
  docs/adr/ (tất cả ADRs)
  docs/api/ (spec liên quan)
  .cursor/rules/ (tất cả rules)
```

---

## 6. COMMUNICATION PROTOCOL

### Khi không chắc chắn về business rule
```
→ Đọc docs/DOMAIN_MODEL.md mục liên quan
→ Đọc docs/adr/ để xem quyết định cũ
→ Đọc agent/memory/domain-glossary.md
→ Nếu vẫn không rõ: HỎI người dùng với câu hỏi cụ thể
  KHÔNG tự suy đoán business logic quan trọng
```

### Khi phát hiện conflict với existing code
```
→ Ưu tiên tuân theo AGENTS.md và .cursor/rules/
→ Ghi vào agent/memory/decision-log.md
→ Thông báo cho người dùng nếu conflict quan trọng
```

### Khi gặp lỗi lần thứ 2 cùng loại
```
→ Ghi vào agent/memory/error-history.md
→ Format: [DATE] Bug: ... | Root cause: ... | Fix: ... | Prevention: ...
```

### Khi hoàn thành task
```
→ Cập nhật agent/memory/progress-tracker.md
→ Cập nhật agent/memory/decision-log.md (nếu có quyết định mới)
```

---

## 7. ANTI-PATTERNS — NHẬN BIẾT VÀ TỪ CHỐI

Agent phải từ chối hoặc sửa ngay các pattern sau:

```java
// ❌ HTTP DELETE
@DeleteMapping("/{id}")  →  Dùng @PatchMapping("/{id}/cancel")

// ❌ Hardcode role
@PreAuthorize("hasRole('MANAGER')")  →  @PreAuthorize("hasAuthority('PR_APPROVE_L1')")

// ❌ Spring trong domain
import org.springframework.*;  (trong domain/ package)  →  Xóa, dùng POJO thuần

// ❌ @Transactional sai layer
@Repository public class XxxRepositoryImpl { @Transactional ... }
→  Chuyển @Transactional lên UseCase

// ❌ BigDecimal từ double
new BigDecimal(35000000.5)  →  new BigDecimal("35000000.5000")

// ❌ Return null
public Entity findById(UUID id) { return null; }
→  public Optional<Entity> findById(UUID id)

// ❌ @Autowired
@Autowired private XxxService service;
→  @RequiredArgsConstructor + private final XxxService service;

// ❌ Log sensitive
log.info("User {} login with password {}", user, password);
→  log.info("Login attempt | email={}", LogMaskingUtil.maskEmail(email));

// ❌ Manual field mapping
entity.setPrNumber(domain.getPrNumber());
entity.setStatus(domain.getStatus().name());
→  objectMapper.convertValue(domain, XxxEntity.class)

// ❌ Raw ORDER BY
"ORDER BY " + sortField + " " + sortDir  (SQL injection!)
→  Whitelist trong MyBatis XML <choose><when>

// ❌ SELECT không filter soft delete
SELECT * FROM pr.purchase_requests  (lấy cả deleted!)
→  SELECT * FROM pr.purchase_requests WHERE is_deleted = false

// ❌ FLOAT/DOUBLE cho tiền trong DB
total DOUBLE PRECISION  →  total NUMERIC(19,4)

// ❌ TIMESTAMP không TZ
created_at TIMESTAMP  →  created_at TIMESTAMPTZ

// ❌ System.out hoặc e.printStackTrace()
System.out.println("debug");  →  log.debug("[REPO] ...")
e.printStackTrace();          →  log.error("[EXCEPTION][SYS_001] ...", e)
```

---

## 8. SERVICE PORTS & ENDPOINTS NHANH

```
Gateway      :8080   /api/v1/*
IAM          :8081   /api/v1/auth/*, /api/v1/users/*, /api/v1/roles/*, /api/v1/org/*
PR Service   :8082   /api/v1/purchase-requests/*, /api/v1/catalog/*
Approval     :8083   /api/v1/approvals/*
Finance      :8084   /api/v1/budgets/*, /api/v1/purchase-orders/*, /api/v1/invoices/*
Inventory    :8085   /api/v1/items/*, /api/v1/warehouses/*, /api/v1/goods-receipts/*
Vendor       :8086   /api/v1/vendors/*, /api/v1/rfq/*
Analytics    :8087   /api/v1/dashboard/*, /api/v1/kpi/*, /api/v1/reports/*
Notification :8088   /api/v1/notifications/*, ws://.../ws/notifications
Admin        :8089   /api/v1/admin/*

Health check: /actuator/health (mọi service)
Metrics:      /actuator/prometheus (mọi service)
```

---

## 9. KAFKA TOPICS NHANH

```
procurement.pr.submitted         PR bị submit  →  Approval, Notification
procurement.pr.approved          All steps OK  →  PR Svc, Finance, Notification
procurement.pr.rejected          Bị từ chối   →  PR Svc, Notification
approval.step.assigned           Task gán      →  Notification
approval.sla.breached            Quá SLA       →  Notification, Admin
finance.budget.warning           < 20%         →  Notification
procurement.po.issued            PO phát hành  →  Inventory, Notification
inventory.gr.created             GR xong       →  Finance (3-way match)
finance.invoice.matched          Match OK      →  Notification
procurement.emergency.abuse      > 3 lần/tháng →  HR, Compliance
```

---

## 10. CHECKLIST CUỐI CÙNG TRƯỚC KHI COMMIT

```
Code quality:
□ Không có HTTP DELETE endpoint
□ Domain layer: ZERO Spring/Jakarta/MyBatis import
□ @PreAuthorize: permission code, không hardcode role
□ @Transactional: chỉ ở UseCase
□ BigDecimal cho mọi tính toán tiền
□ Optional<T> thay vì return null
□ ObjectMapper.convertValue() cho mapping
□ Idempotency-Key được xử lý trong UseCase

Database:
□ Flyway migration naming: V{N}__{description}.sql
□ Tiền: NUMERIC(19,4)
□ Timestamp: TIMESTAMPTZ
□ Soft delete: is_deleted + deleted_at + deleted_by
□ Index cho FK và common query patterns
□ WHERE is_deleted = false trong SELECT chính

Security:
□ Log không chứa password, token, secret, key
□ SQL: #{param} không phải ${param} cho user input
□ ORDER BY qua whitelist mapping

Infrastructure:
□ Docker resource limits có trong docker-compose
□ HEALTHCHECK có trong Dockerfile
□ .dockerignore không để lộ sensitive files
□ TZ=Asia/Ho_Chi_Minh trong mọi container

Documentation:
□ OpenAPI spec cập nhật nếu thêm/sửa endpoint
□ progress-tracker.md cập nhật
□ decision-log.md cập nhật nếu có quyết định mới
□ error-history.md cập nhật nếu fix bug
```
