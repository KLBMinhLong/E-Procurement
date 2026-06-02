# E06 RFQ & Vendor
## User Stories và Use Cases

---

## 1. Epic Goal

Triển khai `vendor-service` để quản lý nhà cung cấp, Approved Vendor List (AVL), đánh giá vendor và RFQ. Epic này là bước nối giữa PR đã được phê duyệt và các epic sau:

```
PR approved -> RFQ/Vendor selection -> PO issue -> Goods receipt -> Invoice matching
```

Mục tiêu coding gần nhất là dựng Vendor master foundation đủ chắc cho các luồng:

```
Create vendor -> approve vendor into AVL -> list approved vendors -> create RFQ using approved vendors
```

RFQ award và quote evaluation sẽ làm sau khi Vendor master và AVL chạy ổn định.

---

## 2. Actors

| Actor | Vai trò |
|---|---|
| Procurement Officer | Tạo/cập nhật vendor, tạo RFQ, mời vendor báo giá |
| Procurement Manager | Phê duyệt vendor vào AVL, blacklist vendor, award RFQ |
| Finance/Compliance | Tra cứu vendor, tax code, trạng thái AVL/blacklist |
| Vendor Service | Lưu vendor master, scorecard, RFQ và quote |
| Purchase Request Service | Cung cấp PR đã approved để tạo RFQ |
| Notification Service | Nhận event vendor/RFQ nếu cần thông báo |

---

## 3. User Stories

| ID | Story | Priority | API/Event Reference |
|---|---|---|---|
| E06-US-001 | Là Procurement Officer, tôi muốn xem/tìm kiếm danh sách vendor theo status/category/AVL để chọn nhà cung cấp phù hợp. | P1 | `GET /vendors` |
| E06-US-002 | Là Procurement Officer, tôi muốn tạo vendor mới với tax code, contact và category để chuẩn bị đánh giá. | P1 | `POST /vendors` |
| E06-US-003 | Là Procurement/Finance, tôi muốn xem chi tiết vendor gồm contact, scorecard và trạng thái AVL. | P1 | `GET /vendors/{id}` |
| E06-US-004 | Là Procurement Officer, tôi muốn cập nhật thông tin vendor khi có thay đổi hồ sơ. | P1 | `PUT /vendors/{id}` |
| E06-US-005 | Là Procurement Manager, tôi muốn phê duyệt vendor vào AVL để vendor được phép tham gia RFQ. | P1 | `PATCH /vendors/{id}/approve` |
| E06-US-006 | Là Procurement Manager, tôi muốn blacklist vendor với lý do rõ ràng để chặn vendor khỏi RFQ/PO mới. | P1 | `PATCH /vendors/{id}/blacklist` |
| E06-US-007 | Là Procurement Officer, tôi muốn đánh giá vendor sau hợp tác để cập nhật scorecard. | P2 | `POST /vendors/{id}/evaluate` |
| E06-US-008 | Là Procurement Officer, tôi muốn tạo RFQ từ PR đã approved và mời tối thiểu 2 vendor approved. | P1 | `POST /rfq` |
| E06-US-009 | Là Procurement Officer/Manager, tôi muốn xem danh sách và chi tiết RFQ để theo dõi trạng thái báo giá. | P1 | `GET /rfq`, `GET /rfq/{id}` |
| E06-US-010 | Là Procurement Officer, tôi muốn đóng RFQ khi hết hạn nhận báo giá. | P2 | `PATCH /rfq/{id}/close` |
| E06-US-011 | Là Procurement Manager, tôi muốn chấm điểm quote để so sánh vendor minh bạch. | P2 | `POST /rfq/{id}/quotes/{quoteId}/evaluate` |
| E06-US-012 | Là Procurement Manager, tôi muốn award RFQ cho quote thắng để chuẩn bị tạo PO. | P1 | `POST /rfq/{id}/award` |

---

## 4. Use Cases

### E06-UC-001: ListVendorsUseCase

**Permission:** `VENDOR_VIEW`.

**Main flow:**

```
1. Controller nhận status, category, on_avl_only, q, page, size, sort.
2. UseCase normalize pagination và sort qua whitelist.
3. Repository query vendor.vendors với WHERE is_deleted = false.
4. Filter category bằng approved category mapping, không nối raw SQL.
5. Return Page<VendorSummary>.
```

**Acceptance criteria:**

```
[ ] Chỉ trả vendor chưa soft delete.
[ ] Sort qua whitelist, không dùng raw ORDER BY từ request.
[ ] Pagination meta đúng API convention.
[ ] on_avl_only=true chỉ trả vendor status APPROVED và is_on_approved_vendor_list=true.
```

### E06-UC-002: CreateVendorUseCase

**Permission:** `VENDOR_CREATE`.

**Endpoint:** `POST /vendors`.

**Main flow:**

```
1. Verify Idempotency-Key.
2. Validate name, taxCode, email, phone, categories.
3. Check taxCode chưa tồn tại ở vendor active.
4. Tạo Vendor aggregate status PENDING, isOnApprovedVendorList=false.
5. Save vendor và contacts trong cùng transaction.
6. Cache idempotency response.
```

**Alternate/error flows:**

```
- Duplicate taxCode -> VND_002.
- categories rỗng -> validation error.
- Idempotency hit -> trả response cũ, không tạo vendor thứ hai.
```

**Acceptance criteria:**

```
[ ] Vendor mới luôn ở PENDING.
[ ] taxCode unique với vendor active.
[ ] Không log taxCode/email đầy đủ nếu log không cần thiết.
[ ] POST bắt buộc Idempotency-Key.
```

### E06-UC-003: GetVendorDetailUseCase

**Permission:** `VENDOR_VIEW`.

**Main flow:**

```
1. Load vendor by id với is_deleted=false.
2. Load contacts và scorecard.
3. Return VendorDetail.
```

**Acceptance criteria:**

```
[ ] Không return null; vendor không tồn tại throw VND_001.
[ ] contacts trả list rỗng nếu chưa có.
[ ] scorecard trả null hoặc empty object theo OpenAPI contract đã chốt.
```

### E06-UC-004: UpdateVendorUseCase

**Permission:** `VENDOR_EDIT`.

**Endpoint:** `PUT /vendors/{id}`.

**Main flow:**

```
1. Verify Idempotency-Key.
2. Load vendor active.
3. Update các field được phép: name, email, phone, address, categories, notes.
4. Không cho đổi taxCode bằng endpoint update thông thường.
5. Save và cache response.
```

**Acceptance criteria:**

```
[ ] BLACKLISTED vendor không được chuyển ngược trạng thái bằng update thông tin.
[ ] updated_by/updated_at được ghi nhận.
[ ] PUT bắt buộc Idempotency-Key.
```

### E06-UC-005: ApproveVendorUseCase

**Permission:** `VENDOR_APPROVE`.

**Endpoint:** `PATCH /vendors/{id}/approve`.

**Main flow:**

```
1. Verify Idempotency-Key.
2. Load vendor active.
3. Chỉ cho approve vendor status PENDING hoặc INACTIVE.
4. Set status=APPROVED, isOnApprovedVendorList=true, approvedBy, approvedAt.
5. Cache response.
```

**Acceptance criteria:**

```
[ ] BLACKLISTED vendor không được approve.
[ ] Approve lại vendor APPROVED trả kết quả idempotent, không đổi audit không cần thiết.
[ ] PATCH bắt buộc Idempotency-Key.
```

### E06-UC-006: BlacklistVendorUseCase

**Permission:** `VENDOR_APPROVE`.

**Endpoint:** `PATCH /vendors/{id}/blacklist`.

**Main flow:**

```
1. Verify Idempotency-Key.
2. Validate reason tối thiểu 20 ký tự.
3. Set status=BLACKLISTED, isOnApprovedVendorList=false.
4. Ghi blacklistReason, blacklistedBy, blacklistedAt.
5. Future RFQ/PO queries phải loại vendor BLACKLISTED.
```

**Acceptance criteria:**

```
[ ] Reason bắt buộc và đủ dài.
[ ] Vendor BLACKLISTED không xuất hiện trong on_avl_only=true.
[ ] Không có HTTP DELETE vendor.
```

### E06-UC-007: EvaluateVendorUseCase

**Permission:** `VENDOR_EDIT`.

**Endpoint:** `POST /vendors/{id}/evaluate`.

**Main flow:**

```
1. Verify Idempotency-Key.
2. Validate mỗi score trong 0..100.
3. Tính overallScore từ quality/delivery/price/responsiveness.
4. Upsert scorecard và lưu evaluation history nếu cần.
5. Return VendorScorecard.
```

**Acceptance criteria:**

```
[ ] Score nằm trong 0..100.
[ ] overallScore deterministic.
[ ] POST bắt buộc Idempotency-Key.
```

### E06-UC-008: CreateRfqUseCase

**Permission:** `RFQ_CREATE`.

**Endpoint:** `POST /rfq`.

**Main flow:**

```
1. Verify Idempotency-Key.
2. Validate PR đã APPROVED qua PR service/internal port.
3. Validate invitedVendorIds có tối thiểu 2 vendor APPROVED và đang trong AVL.
4. Tạo RFQ number RFQ-YYYY-MM-XXXXX.
5. Persist RFQ, line items snapshot và invitations.
6. Publish RFQ created/invited event nếu notification cần dùng.
```

**Acceptance criteria:**

```
[ ] Không thể mời vendor PENDING/BLACKLISTED/INACTIVE.
[ ] RFQ snapshot không bị thay đổi khi PR hoặc vendor đổi sau đó.
[ ] RFQ number unique và deterministic theo sequence.
```

### E06-UC-009: ListAndGetRfqUseCase

**Permission:** `RFQ_VIEW`.

**Main flow:**

```
1. List RFQ theo status/pr_id/page/size.
2. Detail trả RFQ, invitations, quotes, award info.
3. Query chính filter is_deleted=false.
```

**Acceptance criteria:**

```
[ ] Pagination meta đúng.
[ ] Detail không return deleted RFQ.
[ ] Quote amount dùng string JSON, BigDecimal trong Java.
```

### E06-UC-010: CloseRfqUseCase

**Permission:** `RFQ_CREATE`.

**Endpoint:** `PATCH /rfq/{id}/close`.

**Main flow:**

```
1. Verify Idempotency-Key.
2. Load RFQ PUBLISHED.
3. Set status=CLOSED, closedAt.
4. Reject new quote submissions after close.
```

**Acceptance criteria:**

```
[ ] DRAFT/AWARDED/CANCELLED không close lại.
[ ] PATCH bắt buộc Idempotency-Key.
```

### E06-UC-011: EvaluateQuoteUseCase

**Permission:** `RFQ_EVALUATE`.

**Endpoint:** `POST /rfq/{id}/quotes/{quoteId}/evaluate`.

**Main flow:**

```
1. Verify Idempotency-Key.
2. Load RFQ và quote thuộc RFQ.
3. Validate score 0..100.
4. Save evaluationScore/evaluationNote.
```

**Acceptance criteria:**

```
[ ] Không đánh giá quote thuộc RFQ khác.
[ ] Score deterministic và audit được evaluator.
```

### E06-UC-012: AwardRfqUseCase

**Permission:** `RFQ_AWARD`.

**Endpoint:** `POST /rfq/{id}/award`.

**Main flow:**

```
1. Verify Idempotency-Key.
2. Load RFQ CLOSED hoặc PUBLISHED đã hết hạn.
3. Validate quoteId thuộc RFQ và vendor không bị blacklist tại thời điểm award.
4. Set awardedVendorId, awardedQuoteId, awardReason, status=AWARDED.
5. Publish `procurement.rfq.awarded` event chuẩn bị cho PO service.
```

**Acceptance criteria:**

```
[ ] Không award RFQ chưa có quote hợp lệ.
[ ] Không award vendor BLACKLISTED.
[ ] Award idempotent theo Idempotency-Key.
[ ] Publish event sau transaction commit, không publish lại khi idempotency replay.
```

---

## 5. First Coding Slice

Lát cắt đầu tiên nên làm:

```
1. Spring Boot vendor-service module setup.
2. Flyway migration Vendor master: vendors, vendor_contacts, vendor_scores.
3. Vendor domain model và repository.
4. API: GET /vendors, POST /vendors, GET /vendors/{id}, PATCH /vendors/{id}/approve.
5. Unit tests cho domain và create/approve use cases.
6. Docker Compose service trên port 8086.
```

RFQ tables/use cases sẽ làm ở lát cắt tiếp theo sau khi Vendor master + AVL đã build/test xanh.
