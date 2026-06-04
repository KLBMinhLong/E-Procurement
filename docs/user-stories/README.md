# eProcure User Stories
## Story Map và Use Case cho lộ trình triển khai

---

> Mục tiêu của thư mục này là nối `PROJECT_BRIEF.md`, `DOMAIN_MODEL.md`, OpenAPI specs và bộ agent playbook thành backlog có thể code theo vertical slice.

---

## 1. Cách đọc

Đọc theo thứ tự:

```
01-story-map-E01-E15.md
E01-infrastructure-setup.md
E02-iam-service.md
E03-ui-shell-design-system.md
E04-purchase-request-service.md
E05-approval-engine.md
E06-rfq-vendor.md
E09-invoice-payment.md
E10-budget-management.md
E11-notification-realtime.md
E12-analytics-reports.md
```

Các epic E07 và E13-E15 vẫn chỉ được story map ở mức roadmap. Khi bắt đầu code từng epic còn lại, tạo file chi tiết riêng theo format E01-E06/E09-E12.

---

## 2. Quy ước ID

```
E{epic}-US-{number}  User story
E{epic}-UC-{number}  Use case
E{epic}-AC-{number}  Acceptance criteria group
```

Ví dụ:

```
E04-US-003  Requester submit PR
E04-UC-003  SubmitPurchaseRequestUseCase
```

---

## 3. MVP Boundary

MVP đầu tiên phải chứng minh được luồng nghiệp vụ:

```
User login -> create PR draft -> submit PR -> approval task assigned -> approver approve/reject -> requester sees result
```

MVP cần đủ:

```
E01 Infrastructure Setup
E02 IAM Service
E03 UI Shell & Design System
E04 Purchase Request Service
E05 Approval Engine
```

Notification, Finance và Inventory ở MVP có thể dùng adapter/stub có contract rõ. Sau MVP E01-E05, ưu tiên E10 để thay budget fallback bằng finance-service thật, rồi E11 để nhận event và gửi notification realtime/email.

---

## 4. Invariants khi chuyển story sang code

```
- Không có HTTP DELETE; dùng soft delete hoặc PATCH action endpoint.
- Domain layer là POJO thuần, không Spring/Jakarta/MyBatis annotation.
- @PreAuthorize dùng permission code, không dùng role name.
- @Transactional đặt tại UseCase.
- Money là BigDecimal trong Java, NUMERIC(19,4) trong DB, string trong JSON.
- Public method không return null; dùng Optional<T>.
- POST/PUT/PATCH có Idempotency-Key trừ endpoint auth được whitelist rõ.
- Token là opaque 64 chars, không JWT.
- Mapping domain <-> entity dùng ObjectMapper.convertValue().
- Log không chứa password, token, secret, key, encryptedPayload.
```

---

## 5. Definition of Ready cho story

Một story sẵn sàng để code khi có:

```
[ ] Actor và business value rõ.
[ ] Preconditions rõ.
[ ] Main flow và alternate/error flow rõ.
[ ] Permission code xác định.
[ ] API hoặc UI contract xác định nếu có.
[ ] Database/event/cache impact xác định.
[ ] Acceptance criteria testable.
```

---

## 6. Definition of Done cho story

Một story hoàn thành khi có:

```
[ ] Code đúng layer và invariant.
[ ] Unit/integration test phù hợp risk.
[ ] OpenAPI cập nhật nếu đổi endpoint.
[ ] Migration cập nhật nếu đổi schema.
[ ] Audit/log/security requirements được xử lý.
[ ] progress-tracker.md cập nhật.
```
