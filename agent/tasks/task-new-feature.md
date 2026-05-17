# TASK: NEW FEATURE
## eProcure Enterprise — Playbook Thêm Feature Hoàn Chỉnh

---

> Dùng file này khi người dùng yêu cầu thêm một nghiệp vụ, màn hình, luồng xử lý, integration, hoặc capability mới vào service đã có.  
> Nếu feature tạo service mới hoặc thay đổi kiến trúc liên service, đọc thêm quy trình Architect trong `AGENTS.md`.

---

## 1. TRIGGER

Kích hoạt task này khi request có dạng:

```
thêm chức năng ...
implement feature ...
tạo luồng ...
thêm màn hình ...
thêm API + xử lý nghiệp vụ ...
add support for ...
```

Ví dụ:
- Thêm chức năng submit Purchase Request.
- Thêm delegation approval flow.
- Thêm màn hình danh sách vendor.
- Thêm Kafka event khi PO được phát hành.

---

## 2. CONTEXT BẮT BUỘC PHẢI ĐỌC

Đọc theo thứ tự, chỉ mở phần liên quan nếu file quá dài:

```
AGENTS.md
agent/memory/project-context.md
agent/memory/domain-glossary.md
agent/memory/coding-patterns.md
agent/memory/progress-tracker.md
agent/knowledge/clean-architecture-patterns.md
docs/DOMAIN_MODEL.md
docs/DATABASE_SCHEMA.md
docs/api/{service}.openapi.yaml
```

Nếu feature liên quan domain cụ thể, đọc thêm:

```
agent/knowledge/api-idempotency.md       # POST/PUT/PATCH
agent/knowledge/api-error-codes.md       # BusinessException/error code
agent/knowledge/api-pagination.md        # list/search endpoint
agent/knowledge/kafka-topics.md          # event publish/consume
agent/knowledge/redis-cache-patterns.md  # cache/session/idempotency
agent/knowledge/rbac-permission-codes.md # permission code
agent/knowledge/frontend-design-system.md # Angular UI
```

---

## 3. AGENT & SKILL ROUTING

Chọn agent/skill theo phạm vi feature:

| Phạm vi | Agent | Skill bắt buộc |
|---|---|---|
| Domain model / Value Object / Event | AGENT-02 | SK-01 |
| UseCase / Command / Port | AGENT-03 | SK-02, SK-08, SK-26 |
| Repository / MyBatis / Kafka / Redis | AGENT-04 | SK-03, SK-06, SK-07, SK-25 |
| Controller / DTO / API response | AGENT-05 | SK-04, SK-27 |
| Auth / RBAC / token / encryption | AGENT-06 | SK-09, SK-10, SK-11 |
| Migration / schema / index | AGENT-07 | SK-05, SK-16 |
| Logging / tracing / audit | AGENT-08 | SK-22, SK-23, SK-24 |
| Angular component/service | AGENT-09 | SK-18, SK-19, SK-20, SK-21 |
| Tests | AGENT-10 | SK-15 |
| Docker / compose / infra config | AGENT-11 | SK-17, SK-30 |

---

## 4. EXECUTION FLOW

### Bước 1: Xác định phạm vi feature

Trả lời trước khi code:

```
- Service nào bị ảnh hưởng?
- Có thêm/sửa endpoint không?
- Có thêm/sửa table/column/index không?
- Có publish/consume Kafka event không?
- Có cache/idempotency không?
- Có UI Angular không?
- Permission code nào dùng cho endpoint?
- Error code nào cần thêm?
```

Nếu business rule chưa rõ, đọc `docs/DOMAIN_MODEL.md`, `docs/adr/`, `domain-glossary.md`; nếu vẫn chưa rõ thì hỏi người dùng bằng câu hỏi cụ thể.

### Bước 2: Viết hoặc cập nhật test trước

Ưu tiên test ở nơi risk cao nhất:

```
domain/       -> business rule, state transition, Money
application/  -> UseCase flow, idempotency, error code
presentation/ -> controller validation, permission, response wrapper
frontend/     -> component/service behavior nếu repo có test setup
```

Naming:

```java
void should_{expected_behavior}_when_{condition}()
```

### Bước 3: Implement theo layer order

Thứ tự mặc định:

```
1. Domain
2. Application
3. Infrastructure
4. Presentation/API
5. Frontend
6. Observability/Audit
7. Tests/update fixtures
8. Docs/OpenAPI/progress tracker
```

Không đảo chiều dependency. Controller delegate ngay sang UseCase. UseCase không gọi UseCase khác.

### Bước 4: Cập nhật tài liệu contract

Nếu thêm hoặc sửa API:

```
docs/api/{service}.openapi.yaml
agent/knowledge/api-error-codes.md nếu thêm mã lỗi mới
agent/knowledge/rbac-permission-codes.md nếu thêm permission code mới
```

Nếu thêm event:

```
agent/knowledge/kafka-topics.md
agent/memory/architecture-map.md nếu thay đổi data flow
```

Nếu thêm DB object:

```
docs/DATABASE_SCHEMA.md
src/main/resources/db/migration/V{N}__{description}.sql
```

---

## 5. OUTPUT CONTRACT

Feature hoàn chỉnh phải có đủ những phần liên quan:

```
Domain:
  src/main/java/com/eprocure/{service}/domain/model/...
  src/main/java/com/eprocure/{service}/domain/repository/...
  src/main/java/com/eprocure/{service}/domain/event/...

Application:
  src/main/java/com/eprocure/{service}/application/usecase/...
  src/main/java/com/eprocure/{service}/application/port/in/...
  src/main/java/com/eprocure/{service}/application/port/out/...

Infrastructure:
  src/main/java/com/eprocure/{service}/infrastructure/persistence/...
  src/main/java/com/eprocure/{service}/infrastructure/kafka/...
  src/main/java/com/eprocure/{service}/infrastructure/redis/...

Presentation:
  src/main/java/com/eprocure/{service}/presentation/controller/...
  src/main/java/com/eprocure/{service}/presentation/request/...
  src/main/java/com/eprocure/{service}/presentation/response/...
  src/main/java/com/eprocure/{service}/presentation/mapper/...

Database:
  src/main/resources/db/migration/V{N}__{description}.sql

Tests:
  src/test/java/com/eprocure/{service}/domain/...
  src/test/java/com/eprocure/{service}/application/...
  src/test/java/com/eprocure/{service}/presentation/...

Docs:
  docs/api/{service}.openapi.yaml
  agent/memory/progress-tracker.md
  agent/memory/decision-log.md nếu có quyết định mới
```

---

## 6. FINAL CHECKLIST

```
Architecture:
[ ] Domain không import Spring/Jakarta/MyBatis/Jackson.
[ ] Dependency flow đúng: presentation -> application -> domain <- infrastructure.
[ ] @Transactional chỉ đặt tại UseCase.
[ ] Không gọi UseCase từ UseCase khác.

API:
[ ] Không có HTTP DELETE.
[ ] POST/PUT/PATCH có Idempotency-Key.
[ ] @PreAuthorize dùng hasAuthority('PERMISSION_CODE'), không dùng role.
[ ] Response bọc ApiResponse<T>.
[ ] OpenAPI spec cập nhật.

Data:
[ ] Tiền dùng BigDecimal và NUMERIC(19,4).
[ ] Timestamp dùng TIMESTAMPTZ.
[ ] SELECT chính có is_deleted = false.
[ ] Soft delete đủ is_deleted, deleted_at, deleted_by.
[ ] ORDER BY qua whitelist.

Security/Logging:
[ ] Không log password, token, secret, key, encryptedPayload.
[ ] userId/email/phone được mask trước khi log.
[ ] Error code đúng prefix service.
[ ] Audit log cho state-changing operation quan trọng.

Frontend nếu có:
[ ] Component dùng ChangeDetectionStrategy.OnPush.
[ ] Subscription dùng takeUntilDestroyed(this.destroyRef).
[ ] Text hiển thị dùng translate pipe.
[ ] HTTP request dùng withCredentials: true.
[ ] POST/PUT/PATCH có Idempotency-Key.

Verification:
[ ] Unit/integration tests liên quan pass.
[ ] Không có TODO chưa xử lý trong code deliver.
[ ] Cập nhật progress-tracker.md.
```
