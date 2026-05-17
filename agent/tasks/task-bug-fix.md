# TASK: BUG FIX
## eProcure Enterprise — Playbook Sửa Lỗi Có Reproduction

---

> Dùng file này khi người dùng báo lỗi runtime, test fail, CI fail, hành vi sai business rule, lỗi UI/API/DB, hoặc regression.  
> Mục tiêu: tái hiện lỗi, sửa đúng root cause, thêm test phòng ngừa, cập nhật `error-history.md` nếu lỗi có giá trị tái sử dụng.

---

## 1. TRIGGER

Kích hoạt task này khi request có dạng:

```
fix bug ...
sửa lỗi ...
test đang fail ...
API trả sai ...
UI bị lỗi ...
CI fail ...
không đúng business rule ...
```

---

## 2. CONTEXT BẮT BUỘC PHẢI ĐỌC

Đọc theo thứ tự:

```
AGENTS.md
agent/memory/project-context.md
agent/memory/error-history.md
agent/memory/coding-patterns.md
.cursor/rules/testing.mdc
.cursor/rules/logging.mdc
```

Đọc thêm theo loại lỗi:

```
API/Controller  -> agent/knowledge/api-conventions.md, docs/api/{service}.openapi.yaml
Business rule   -> docs/DOMAIN_MODEL.md, agent/memory/domain-glossary.md
DB/MyBatis      -> docs/DATABASE_SCHEMA.md, agent/knowledge/mybatis-strategy.md, agent/knowledge/soft-delete-strategy.md
Security/Auth   -> agent/knowledge/api-authentication.md, agent/knowledge/security-encryption.md, agent/knowledge/rbac-permission-codes.md
Frontend        -> agent/knowledge/frontend-design-system.md, .cursor/rules/angular-frontend.mdc
CI/Docker       -> agent/knowledge/environment-variables.md, .cursor/rules/docker-resource.mdc
```

---

## 3. EXECUTION FLOW

### Bước 1: Khoanh vùng lỗi

Thu thập tối thiểu:

```
- Lệnh/test/API nào tái hiện lỗi?
- Expected behavior là gì?
- Actual behavior là gì?
- Service/module/file liên quan?
- Có regression từ thay đổi gần đây không?
- Có lỗi tương tự trong error-history.md không?
```

Không sửa theo cảm tính nếu chưa hiểu đường đi của dữ liệu.

### Bước 2: Reproduce bằng test hoặc command

Ưu tiên tạo test fail trước:

```
Domain bug      -> unit test domain
UseCase bug     -> unit test application
Controller bug  -> web/controller integration test
Mapper bug      -> mapper/integration test nếu test infra đã có
Frontend bug    -> component/service test nếu project có setup
```

Nếu không thể viết test hợp lý, ghi rõ lệnh/manual scenario đã dùng để reproduce.

### Bước 3: Tìm root cause

Kiểm tra các nhóm lỗi thường gặp:

```
Clean Architecture:
[ ] Domain có import framework không?
[ ] Logic nghiệp vụ có bị đặt ở Controller/Repository không?

Soft delete:
[ ] SELECT thiếu is_deleted = false?
[ ] Code đang dùng DELETE vật lý?

Idempotency:
[ ] POST/PUT/PATCH thiếu Idempotency-Key?
[ ] UseCase chưa check idempotency đầu tiên?

Security:
[ ] @PreAuthorize dùng role thay vì permission code?
[ ] Log lộ password/token/secret/key?

DB:
[ ] Money dùng double/float?
[ ] Timestamp thiếu timezone?
[ ] ORDER BY dùng raw input?

Mapping:
[ ] Có manual mapping domain <-> entity thay vì ObjectMapper.convertValue()?
[ ] Field DB snake_case có @Result/resultMap đúng không?
```

### Bước 4: Sửa tối thiểu đúng root cause

Nguyên tắc:

```
- Không refactor rộng nếu không cần để fix bug.
- Không revert thay đổi người dùng nếu không được yêu cầu.
- Không sửa migration đã commit; tạo migration mới nếu cần ALTER.
- Không bỏ qua invariant chỉ để test pass.
- Không che lỗi bằng catch(Exception) rồi return success.
```

### Bước 5: Verify

Chạy theo mức phù hợp:

```
mvn test
mvn -pl {service} test
npm test
npm run lint
newman run ...
docker compose config
```

Nếu không chạy được, ghi rõ lý do và phần đã verify thủ công.

---

## 4. OUTPUT CONTRACT

Một bug fix hoàn chỉnh phải có:

```
[ ] Mô tả root cause ngắn gọn.
[ ] Code fix đúng phạm vi.
[ ] Test fail-before/pass-after hoặc command reproduce.
[ ] Không tạo regression với invariants trong AGENTS.md.
[ ] Cập nhật error-history.md nếu lỗi có khả năng lặp lại.
[ ] Cập nhật progress-tracker.md nếu task thuộc epic đang theo dõi.
```

---

## 5. ERROR HISTORY FORMAT

Khi lỗi lặp lại hoặc có bài học quan trọng, thêm vào `agent/memory/error-history.md`:

```md
## [YYYY-MM-DD] {Short bug title}

- Bug: {mô tả lỗi nhìn từ user/system}
- Impact: {service/endpoint/test bị ảnh hưởng}
- Root cause: {nguyên nhân thật}
- Fix: {thay đổi đã làm}
- Prevention: {test/checklist/rule để tránh lặp lại}
```

---

## 6. FINAL CHECKLIST

```
[ ] Lỗi đã được reproduce trước khi sửa hoặc có lý do rõ nếu không reproduce được.
[ ] Root cause nằm trong phần trả lời cuối, không chỉ nói "đã fix".
[ ] Có test hoặc lệnh verify.
[ ] Không thêm HTTP DELETE.
[ ] Không thêm role hardcode.
[ ] Không thêm framework import vào domain.
[ ] Không thêm log sensitive.
[ ] Không return null từ public method.
[ ] Không dùng double/float cho tiền.
[ ] Không dùng raw SQL ORDER BY từ input.
```
