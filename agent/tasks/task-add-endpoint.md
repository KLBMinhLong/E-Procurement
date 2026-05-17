# TASK: ADD ENDPOINT
## eProcure Enterprise — Playbook Thêm Hoặc Sửa API Endpoint

---

> Dùng file này khi người dùng yêu cầu thêm endpoint REST, sửa contract API, thêm request/response DTO, hoặc expose một UseCase qua HTTP.  
> Endpoint chỉ là presentation layer; business logic phải nằm trong UseCase.

---

## 1. TRIGGER

Kích hoạt task này khi request có dạng:

```
thêm endpoint ...
tạo API ...
expose API ...
sửa request/response ...
thêm route controller ...
```

---

## 2. CONTEXT BẮT BUỘC PHẢI ĐỌC

```
AGENTS.md
agent/memory/project-context.md
agent/memory/coding-patterns.md      # Mục Controller Pattern
agent/knowledge/api-conventions.md
agent/knowledge/api-error-codes.md
agent/knowledge/api-idempotency.md   # Nếu POST/PUT/PATCH
agent/knowledge/api-pagination.md    # Nếu list/search
agent/knowledge/rbac-permission-codes.md
docs/api/{service}.openapi.yaml
.cursor/rules/coding.mdc
.cursor/rules/error-codes.mdc
```

Nếu endpoint có nghiệp vụ mới, dùng thêm `task-new-feature.md`.

---

## 3. ENDPOINT DESIGN RULES

```
Base path: /api/v1/{resource}
Resource: lowercase-hyphen, plural noun
No DELETE: dùng PATCH .../cancel, .../deactivate, .../revoke
POST/PUT/PATCH: bắt buộc Idempotency-Key
Auth: cookie ep_session, principal lấy từ security context
Response: ApiResponse<T>
Money in JSON: string, ví dụ "70000000.0000"
Pagination: data list + meta pagination
```

Ví dụ hợp lệ:

```
GET   /api/v1/purchase-requests
GET   /api/v1/purchase-requests/{id}
POST  /api/v1/purchase-requests
PATCH /api/v1/purchase-requests/{id}/submit
PATCH /api/v1/purchase-requests/{id}/cancel
```

Ví dụ không hợp lệ:

```
DELETE /api/v1/purchase-requests/{id}
POST   /api/v1/purchaseRequests
GET    /api/v1/purchase-requests?sort=raw_sql
```

---

## 4. EXECUTION FLOW

### Bước 1: Xác định contract

Trước khi code, xác định:

```
- Method + path?
- Permission code?
- Request DTO?
- Response DTO?
- Error codes?
- Có Idempotency-Key không?
- Có pagination/filter/sort không?
- UseCase nào được gọi?
```

### Bước 2: Cập nhật OpenAPI trước hoặc cùng lúc với code

File:

```
docs/api/{service}.openapi.yaml
```

OpenAPI phải thể hiện:

```
[ ] requestBody schema
[ ] response schema bọc ApiResponse
[ ] error responses theo common convention
[ ] Idempotency-Key header cho POST/PUT/PATCH
[ ] security requirement
[ ] pagination params nếu có list endpoint
```

### Bước 3: Tạo DTO và mapper presentation

Vị trí:

```
src/main/java/com/eprocure/{service}/presentation/request/
src/main/java/com/eprocure/{service}/presentation/response/
src/main/java/com/eprocure/{service}/presentation/mapper/
```

Rules:

```
- Request DTO dùng validation annotation.
- Controller không tự build domain object.
- Mapper chuyển Request + principal -> Command.
- Response không expose internal fields, token, secret, encrypted payload.
```

### Bước 4: Tạo hoặc gọi UseCase

Nếu UseCase chưa có, dừng `task-add-endpoint` và chuyển sang `task-new-feature`.

UseCase nhận:

```
Command object
idempotencyKey nếu state-changing
actor/user id trong command
```

### Bước 5: Tạo Controller method

Template tối thiểu:

```java
@PatchMapping("/{id}/cancel")
@PreAuthorize("hasAuthority('PR_CANCEL')")
public ResponseEntity<ApiResponse<PurchaseRequestResponse>> cancel(
        @PathVariable UUID id,
        @Valid @RequestBody CancelPrRequest request,
        @AuthenticationPrincipal UserPrincipal principal,
        @RequestHeader("Idempotency-Key") String idempotencyKey) {

    log.info("[CONTROLLER] PATCH /purchase-requests/{id}/cancel | userId={}",
            principal.getMaskedId());
    var result = cancelUseCase.execute(mapper.toCommand(id, request, principal.getId()), idempotencyKey);
    return ResponseEntity.ok(ApiResponse.success("PR_CANCELLED", mapper.toResponse(result)));
}
```

### Bước 6: Tests

Tối thiểu:

```
[ ] should_return_success_when_valid_request()
[ ] should_return_validation_error_when_invalid_request()
[ ] should_require_idempotency_key_when_state_changing()
[ ] should_require_permission_code_when_unauthorized()
```

---

## 5. OUTPUT CONTRACT

Endpoint hoàn chỉnh phải có:

```
src/main/java/com/eprocure/{service}/presentation/controller/{Entity}Controller.java
src/main/java/com/eprocure/{service}/presentation/request/{Action}Request.java
src/main/java/com/eprocure/{service}/presentation/response/{Entity}Response.java
src/main/java/com/eprocure/{service}/presentation/mapper/{Entity}PresentationMapper.java
src/main/java/com/eprocure/{service}/application/usecase/{Action}UseCase.java nếu chưa có
src/main/java/com/eprocure/{service}/application/port/in/{Action}Command.java nếu chưa có
docs/api/{service}.openapi.yaml
src/test/java/com/eprocure/{service}/presentation/...
```

---

## 6. FINAL CHECKLIST

```
Controller:
[ ] Không có @DeleteMapping.
[ ] Không có business if/else trong controller.
[ ] @PreAuthorize dùng hasAuthority('PERMISSION_CODE').
[ ] @Valid trên @RequestBody.
[ ] @RequestHeader("Idempotency-Key") cho POST/PUT/PATCH.
[ ] Log [CONTROLLER] khi vào method, userId được mask.
[ ] Response bọc ApiResponse<T>.

Contract:
[ ] OpenAPI cập nhật.
[ ] Error code đúng prefix service.
[ ] Pagination meta đúng convention nếu có list.
[ ] Money serialize dạng string.

Security:
[ ] Không expose token/password/secret/key.
[ ] Không hardcode role.
[ ] Không log request body chứa sensitive data.

Verification:
[ ] Controller/API tests pass.
[ ] progress-tracker.md cập nhật nếu endpoint thuộc task tracked.
```
