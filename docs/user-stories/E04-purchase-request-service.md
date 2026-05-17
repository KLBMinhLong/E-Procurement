# E04 Purchase Request Service
## User Stories và Use Cases

---

## 1. Epic Goal

Cho phép requester tạo, sửa, submit, theo dõi và hủy Purchase Request; hệ thống kiểm tra ngân sách/tồn kho qua ports, phát event sang Approval Engine, và cập nhật trạng thái khi approval trả kết quả.

---

## 2. Actors

| Actor | Vai trò |
|---|---|
| Requester | Tạo, sửa, submit, hủy PR của mình |
| Manager/Approver | Xem PR cần duyệt qua Approval Engine |
| Finance/Inventory Service | Cung cấp budget/inventory check |
| Approval Engine | Nhận PrSubmittedEvent và gửi kết quả |
| Purchasing | Nhận PR đã approved để tạo PO ở E07 |

---

## 3. User Stories

| ID | Story | Priority | API Reference |
|---|---|---|---|
| E04-US-001 | Là Requester, tôi muốn xem danh sách PR của mình với filter/pagination. | MVP | `GET /purchase-requests` |
| E04-US-002 | Là Requester, tôi muốn tạo PR draft với line items và attachment. | MVP | `POST /purchase-requests` |
| E04-US-003 | Là Requester, tôi muốn xem chi tiết PR, line items, approval summary và checks. | MVP | `GET /purchase-requests/{id}` |
| E04-US-004 | Là Requester, tôi muốn sửa PR khi còn DRAFT hoặc CHANGES_REQUESTED. | MVP | `PUT /purchase-requests/{id}` |
| E04-US-005 | Là Requester, tôi muốn submit PR để hệ thống check budget/inventory và khởi tạo approval. | MVP | `PATCH /purchase-requests/{id}/submit` |
| E04-US-006 | Là Requester, tôi muốn submit với budget override khi budget warning/fail nhưng có quyền/luồng duyệt. | P1 | `PATCH /purchase-requests/{id}/submit-with-override` |
| E04-US-007 | Là Requester, tôi muốn hủy PR khi chưa converted to PO. | MVP | `PATCH /purchase-requests/{id}/cancel` |
| E04-US-008 | Là Requester, tôi muốn upload attachment cho PR. | MVP | `POST /purchase-requests/attachments/upload` |
| E04-US-009 | Là Requester, tôi muốn search catalog items/categories để nhập line item nhanh. | MVP | `/catalog/*` |
| E04-US-010 | Là Approval Engine, tôi muốn PR service publish `procurement.pr.submitted` khi PR được submit. | MVP | Kafka |
| E04-US-011 | Là PR Service, tôi muốn consume approval result event để cập nhật PR status. | MVP | Kafka |
| E04-US-012 | Là Requester, tôi muốn UI create/list/detail PR đầy đủ loading/error/empty states. | MVP | Angular |

---

## 4. Use Cases

### E04-UC-001: ListPurchaseRequestsUseCase

**Permission:** `PR_VIEW_OWN`, `PR_VIEW_DEPARTMENT`, hoặc `PR_VIEW_ALL`.

**Main flow:**

```
1. Controller nhận filter page/size/sort.
2. UseCase xác định scope theo permission.
3. Repository query PR với WHERE is_deleted = false.
4. Sort qua whitelist.
5. Return Page<PurchaseRequestSummary>.
```

**Acceptance criteria:**

```
[ ] Requester mặc định chỉ thấy PR của mình.
[ ] Manager có thể xem PR phòng ban nếu có permission.
[ ] Pagination meta đúng API convention.
[ ] Không raw ORDER BY từ client.
```

### E04-UC-002: CreatePurchaseRequestUseCase

**Permission:** `PR_CREATE`.

**Preconditions:**

```
- Idempotency-Key hợp lệ.
- Có ít nhất 1 line item.
- Justification > 50 ký tự.
- Money fields là BigDecimal/string, currency VND.
```

**Main flow:**

```
1. Check idempotency.
2. Validate command.
3. Generate prNumber PR-YYYY-MM-XXXXX.
4. Build PurchaseRequest status DRAFT.
5. Calculate totalAmount từ lineItems.
6. Save aggregate.
7. Cache idempotency response.
8. Return PR detail.
```

**Alternate/error flows:**

```
- Không có line item -> PR_020.
- Money invalid/negative -> VAL_* hoặc PR_* theo convention.
- Idempotency hit -> return cached response với Idempotency-Replayed.
```

**Acceptance criteria:**

```
[ ] @Transactional ở UseCase.
[ ] Controller không có business logic.
[ ] Domain tự tính totalAmount.
[ ] Repository dùng ObjectMapper.convertValue().
```

### E04-UC-003: UpdatePurchaseRequestUseCase

**Allowed statuses:** `DRAFT`, `CHANGES_REQUESTED`.

**Main flow:**

```
1. Check idempotency.
2. Load PR by id and requester scope.
3. Validate canBeEditedBy(actorId).
4. Replace editable fields and line items.
5. Recalculate totalAmount.
6. Save.
7. Return updated detail.
```

**Acceptance criteria:**

```
[ ] Không cho sửa PR PENDING_APPROVAL/APPROVED/CANCELLED.
[ ] Requester không sửa PR của người khác nếu thiếu permission.
[ ] Không return null khi PR không tồn tại; throw BusinessException code PR_NOT_FOUND.
```

### E04-UC-004: SubmitPurchaseRequestUseCase

**Allowed statuses:** `DRAFT`, `CHANGES_REQUESTED`.

**Main flow:**

```
1. Check idempotency.
2. Load PR.
3. Validate PR complete: title, justification, line items, needByDate, urgencyReason nếu cần.
4. Call BudgetCheckPort.
5. Call InventoryCheckPort.
6. Store budget/inventory snapshot.
7. Transition DRAFT/CHANGES_REQUESTED -> SUBMITTED.
8. Publish PrSubmittedEvent to `procurement.pr.submitted`.
9. Cache idempotency response.
10. Return PR detail.
```

**Alternate/error flows:**

```
- Budget FAIL không có override -> return PR_BUDGET_INSUFFICIENT.
- Inventory có stock -> return suggestion nhưng không block nếu policy cho phép.
- Emergency PR thiếu urgencyReason -> validation error.
```

**Acceptance criteria:**

```
[ ] BudgetCheckPort/InventoryCheckPort là application port, không gọi HTTP trực tiếp từ domain.
[ ] PrSubmittedEvent có event envelope: eventId, eventType, version, source, timestamp, traceId, payload.
[ ] Submit là PATCH, không POST action tùy tiện nếu spec đã chốt PATCH.
```

### E04-UC-005: CancelPurchaseRequestUseCase

**Permission:** `PR_CANCEL_OWN`.

**Main flow:**

```
1. Check idempotency.
2. Load PR.
3. Validate actor có quyền hủy và status cho phép.
4. Transition DRAFT/SUBMITTED/CHANGES_REQUESTED -> CANCELLED.
5. Nếu đã có approval process, publish cancellation event hoặc call ApprovalCancelPort.
6. Save and audit.
```

**Acceptance criteria:**

```
[ ] Không dùng HTTP DELETE.
[ ] Không hard delete row.
[ ] PR APPROVED/CONVERTED_TO_PO không bị requester cancel trực tiếp.
```

### E04-UC-006: UploadAttachmentUseCase

**Main flow:**

```
1. Validate file type/size.
2. Store file metadata and binary location.
3. Return AttachmentInfo.
4. Attach to PR on create/update.
```

**Acceptance criteria:**

```
[ ] Không log file content.
[ ] Reject executable/script mime types.
[ ] Attachment metadata có audit fields.
```

### E04-UC-007: HandleApprovalResultUseCases

**Events consumed:**

```
procurement.pr.approved
procurement.pr.rejected
procurement.pr.changes-requested
```

**Main flow:**

```
1. Consumer receives event.
2. Validate event idempotency by eventId.
3. Load PR.
4. Transition status according to event.
5. Persist approval summary snapshot.
6. Publish notification event if needed.
```

**Acceptance criteria:**

```
[ ] Duplicate event does not duplicate status/action.
[ ] Invalid transition logs warning and moves to dead-letter strategy if configured.
[ ] PR approved becomes APPROVED; rejected becomes REJECTED; request changes becomes CHANGES_REQUESTED.
```

---

## 5. Frontend Use Cases

```
PrListPage: filter/sort/page, empty/error/loading states.
PrCreatePage: form, line item editor, catalog picker, attachment upload.
PrDetailPage: status, approval timeline, budget/inventory snapshot, actions by status.
```

Acceptance:

```
[ ] Mọi text dùng translate key.
[ ] ep-table cho list.
[ ] ep-amount cho money.
[ ] Idempotency-Key giữ nguyên khi retry submit/create/update/cancel.
```

---

## 6. Technical Deliverables

```
pr-service Spring Boot project
Flyway migrations: purchase_requests, pr_line_items, attachments, catalog tables
Domain: PurchaseRequest, PrLineItem, Money, Quantity, Attachment, BudgetCheckResult, InventoryCheckResult
UseCases: List, GetDetail, Create, Update, Submit, SubmitWithOverride, Cancel, UploadAttachment
Ports: BudgetCheckPort, InventoryCheckPort, ApprovalCancelPort/EventPublisher
Kafka: PrSubmittedEvent producer, approval result consumers
Controllers matching purchase-request-service.openapi.yaml
Angular PR list/create/detail pages
Unit tests domain/application
```

---

## 7. MVP Acceptance Checklist

```
[ ] Requester can create draft PR.
[ ] Requester can submit PR and event is published.
[ ] Approval result event updates PR status.
[ ] PR list/detail reflect current status.
[ ] No HTTP DELETE.
[ ] All POST/PUT/PATCH require Idempotency-Key.
[ ] Money uses BigDecimal/NUMERIC(19,4)/JSON string.
[ ] SELECT queries filter is_deleted = false.
```
