# TASK: REFACTOR
## eProcure Enterprise — Playbook Refactor An Toàn

---

> Dùng file này khi người dùng yêu cầu cải thiện cấu trúc code, đổi tên, tách layer, gom duplication, chuẩn hoá theo AGENTS.md, hoặc sửa technical debt mà không đổi hành vi nghiệp vụ.  
> Nếu refactor làm đổi behavior, chuyển sang `task-new-feature.md` hoặc `task-bug-fix.md`.

---

## 1. TRIGGER

Kích hoạt task này khi request có dạng:

```
refactor ...
clean up ...
chuẩn hóa ...
tách layer ...
đổi tên ...
loại bỏ duplication ...
áp dụng coding rules ...
```

---

## 2. CONTEXT BẮT BUỘC PHẢI ĐỌC

```
AGENTS.md
agent/memory/project-context.md
agent/memory/coding-patterns.md
agent/knowledge/clean-architecture-patterns.md
docs/CODING_GUIDE.md
.cursor/rules/coding.mdc
.cursor/rules/testing.mdc
```

Đọc thêm theo phạm vi:

```
DB/MyBatis      -> docs/DATABASE_SCHEMA.md, agent/knowledge/mybatis-strategy.md
API             -> agent/knowledge/api-conventions.md, docs/api/{service}.openapi.yaml
Security        -> agent/knowledge/rbac-permission-codes.md, agent/knowledge/security-encryption.md
Frontend        -> agent/knowledge/frontend-design-system.md, .cursor/rules/angular-frontend.mdc
Docker/Infra    -> .cursor/rules/docker-resource.mdc, agent/knowledge/environment-variables.md
```

---

## 3. REFACTOR BOUNDARY

Trước khi sửa, ghi rõ boundary:

```
- Module/package nào được phép thay đổi?
- Behavior nào phải giữ nguyên?
- Public API có được đổi không?
- DB schema có được đổi không?
- Có cần migration không?
- Có test hiện có bảo vệ behavior không?
```

Nếu boundary không rõ và refactor có thể đổi API/DB/business behavior, hỏi người dùng trước.

---

## 4. REFACTOR TARGETS ƯU TIÊN

Ưu tiên sửa các vi phạm invariant:

```
P0:
[ ] HTTP DELETE endpoint -> PATCH .../cancel/deactivate/revoke.
[ ] Domain import Spring/Jakarta/MyBatis/Jackson.
[ ] @PreAuthorize dùng role.
[ ] @Transactional nằm ở Controller/Repository.
[ ] Log sensitive data.
[ ] SQL injection qua ${} hoặc raw ORDER BY.

P1:
[ ] Public method return null.
[ ] Money dùng double/float.
[ ] SELECT thiếu is_deleted = false.
[ ] Manual domain <-> entity field mapping.
[ ] Controller chứa business logic.
[ ] UseCase gọi UseCase khác.

P2:
[ ] Duplicate mapping/validation lặp lại nhiều nơi.
[ ] Naming không theo convention.
[ ] Missing JavaDoc ở public API quan trọng.
[ ] Frontend thiếu OnPush/takeUntilDestroyed/translate pipe.
```

---

## 5. EXECUTION FLOW

### Bước 1: Baseline

Chạy hoặc ghi nhận test/lint hiện tại nếu có:

```
mvn test
mvn -pl {service} test
npm test
npm run lint
```

Nếu baseline fail sẵn, ghi rõ fail nào có trước khi refactor.

### Bước 2: Refactor nhỏ theo batch

Làm theo batch dễ review:

```
1. Move/rename package/class nếu cần.
2. Sửa dependency direction/layer violations.
3. Tách business logic từ Controller sang UseCase/domain.
4. Chuẩn hoá repository + ObjectMapper conversion.
5. Chuẩn hoá MyBatis query và soft delete filter.
6. Chuẩn hoá log/masking/error code.
7. Chạy test sau mỗi batch lớn.
```

Không trộn refactor với feature mới trừ khi người dùng yêu cầu rõ.

### Bước 3: Giữ contract

Không đổi những thứ sau nếu không được yêu cầu:

```
- URL path/method
- Request/response schema
- Error code public
- DB column/table name
- Kafka topic/event schema
- Permission code
- i18n key
```

Nếu bắt buộc đổi contract, cập nhật docs/spec tương ứng và nêu rõ trong final.

### Bước 4: Tests

Refactor hoàn chỉnh phải có ít nhất một trong các lớp bảo vệ:

```
[ ] Existing tests pass.
[ ] Thêm characterization test trước khi sửa behavior phức tạp.
[ ] Snapshot/API contract test nếu có contract dễ vỡ.
[ ] Manual verification command nếu project chưa có test setup.
```

---

## 6. OUTPUT CONTRACT

Refactor deliverable phải gồm:

```
[ ] Code đã chuẩn hóa trong boundary đã xác định.
[ ] Không đổi behavior ngoài phạm vi.
[ ] Tests/lint/verification đã chạy hoặc lý do không chạy được.
[ ] Docs/OpenAPI/schema cập nhật nếu contract bắt buộc đổi.
[ ] progress-tracker.md cập nhật nếu refactor thuộc epic tracked.
[ ] decision-log.md cập nhật nếu có quyết định kỹ thuật mới.
```

---

## 7. FINAL CHECKLIST

```
Architecture:
[ ] Domain vẫn là POJO thuần.
[ ] Dependency flow không bị đảo.
[ ] @Transactional chỉ ở UseCase.
[ ] Controller không chứa business logic.

Behavior:
[ ] Không đổi API/DB/event contract ngoài phạm vi.
[ ] Không bỏ validation/error code hiện có.
[ ] Không làm mất audit/soft delete/idempotency.

Security:
[ ] Không thêm role hardcode.
[ ] Không log sensitive.
[ ] Không raw SQL injection.

Code quality:
[ ] Không return null từ public method.
[ ] Không manual mapping domain <-> entity nếu ObjectMapper dùng được.
[ ] Không thêm TODO/FIXME chưa xử lý.
[ ] Tests hoặc verification pass.
```
