# Kế hoạch UI — PR Lifecycle & Traceability

> **Mục tiêu:** Bổ sung truy vết RFQ/PO sinh ra từ PR để Requester/Purchasing biết PR đang ở đâu trong procurement cycle, mở nhanh tài liệu liên quan, và tránh tạo RFQ/PO trùng sau khi PR đã approved.

> **Trạng thái triển khai:** ✅ Hoàn thành 2026-06-13 — finance-service đã expose `GET /purchase-orders?pr_id=...`, PR Detail đã có traceability card RFQ/PO + duplicate-action guard, `mvn -pl services/finance-service test` và `npm run build` pass.

---

## 0. Rà soát lại theo code hiện tại — 2026-06-13

Plan này là plan kế tiếp sau `UI_MODULE_APPROVAL_FLOW_DIAGRAM.md`. Sau plan #4, phần lifecycle visualization nền đã hoàn thành, nên scope plan #5 được chỉnh lại để tập trung vào **traceability links RFQ/PO** và **duplicate-action guard**.

| Nhóm | Trạng thái thực tế | Điều chỉnh scope |
|---|---|---|
| `ep-pr-lifecycle` | Đã tạo trong plan #4 và đã wire vào `pr-detail.component` ngay dưới page header | Không tạo lại lifecycle bar, không thêm stage UI mới trong plan này. |
| Approval timeline | PR Detail đã dùng `ep-approval-steps` vertical compact | Không động lại approval timeline trừ khi traceability cần spacing nhỏ. |
| RFQ filter theo PR | Backend/vendor OpenAPI có `GET /rfq?pr_id=...`; frontend `RfqService.list()` đã nhận `filter.prId` và map sang `pr_id` | Chỉ thêm helper `listByPrId()` nếu giúp PR Detail gọn hơn; không cần backend cho RFQ. |
| PO filter theo PR | Finance DB/repository có `pr_id`, nhưng public `GET /purchase-orders` controller/OpenAPI/frontend filter chưa expose `pr_id` | Cần slice backend/API nhỏ trước khi PR Detail có thể load PO liên quan ổn định. |
| PO response | `PurchaseOrderResponse`/FE model có `prId`, `prNumber`, `prConversionStatus`; chưa expose `rfqId/rfqNumber` ra response | Traceability ở PR Detail chỉ cần PO link/status/number/conversion; không yêu cầu `rfqId` trong plan này. |
| PR List trace indicator | Không có `hasRfq/hasPo` trong PR list response | Không làm PR List để tránh N+1 và backend projection mới. |

**Thứ tự thực hiện đề xuất:** 5a PO `pr_id` filter backend/API → 5b FE service helpers → 5c PR Detail traceability card → 5d duplicate-action guard + reload → 5e i18n + build verify.

---

## 1. Hiện trạng

### 1.1 PR Detail hiện có

**Files chính:**
- `frontend/eprocure-web/src/app/features/procurement/pages/pr-detail/pr-detail.component.ts`
- `frontend/eprocure-web/src/app/features/procurement/pages/pr-detail/pr-detail.component.html`
- `frontend/eprocure-web/src/app/features/procurement/pages/pr-detail/pr-detail.component.scss`

**Đã có sau plan #4:**
- Header actions `Tạo RFQ`, `Tạo PO`.
- `ep-pr-lifecycle` full-width dưới page header.
- Sidebar approval card dùng `ep-approval-steps`.

**Action logic hiện tại:**
```typescript
readonly canCreatePo = computed(() => this.pr()?.status === 'APPROVED');
readonly canCreateRfq = computed(() => this.pr()?.status === 'APPROVED');
```

**Vấn đề:** khi đã có RFQ/PO liên quan, user vẫn thấy nút tạo mới.

### 1.2 Traceability data hiện có

**RFQ:**
- `docs/api/vendor-service.openapi.yaml` có `GET /rfq?pr_id={prId}`.
- `RfqService.list({ prId })` đã map `prId` → `pr_id`.
- `RfqDetail` có `id`, `rfqNumber`, `prId`, `prNumber`, `title`, `status`, `awardedQuoteId`, `createdAt`.

**PO:**
- `finance.purchase_orders` có `pr_id`.
- `PurchaseOrderResponse` có `id`, `poNumber`, `prId`, `prNumber`, `status`, `prConversionStatus`, `createdAt`.
- `GET /purchase-orders` hiện chỉ nhận `status`, `vendor_id`, `from_date`, `to_date`, `page`, `size`, `sort`.

---

## 2. Scope Sau Rà Soát

### In scope

1. Backend finance-service expose `GET /purchase-orders?pr_id={uuid}`.
2. Cập nhật OpenAPI `docs/api/finance-service.openapi.yaml`.
3. Frontend `PurchaseOrderListFilter` + `PurchaseOrderService.listByPrId()`.
4. Frontend `RfqService.listByPrId()` helper dựa trên `list({ prId })` hiện có.
5. PR Detail sidebar traceability card:
   - RFQ liên quan.
   - PO liên quan.
   - skeleton riêng, empty state rõ, reload button.
   - link router sang RFQ/PO detail.
6. `canCreateRfq` / `canCreatePo` ẩn action khi đã có document active.

### Out of scope

- Không tạo lại `ep-pr-lifecycle`.
- Không thêm PR List trace icons vì thiếu `hasRfq/hasPo` trong list API.
- Không tạo projection/cross-service aggregation mới ở PR service.
- Không yêu cầu PO response expose `rfqId/rfqNumber`.
- Không mở browser test nếu user vẫn muốn dừng ở mức build.

---

## 3. Slice 5a — Finance PO Filter `pr_id`

**Mục tiêu:** Cho frontend query PO liên quan đến PR bằng API chính thức, không dùng workaround.

**Files backend cần sửa:**
- `services/finance-service/src/main/java/com/eprocure/finance/application/port/in/ListPurchaseOrdersQuery.java`
- `services/finance-service/src/main/java/com/eprocure/finance/domain/repository/PurchaseOrderFilter.java`
- `services/finance-service/src/main/java/com/eprocure/finance/application/usecase/ListPurchaseOrdersUseCase.java`
- `services/finance-service/src/main/java/com/eprocure/finance/presentation/controller/PurchaseOrderController.java`
- `services/finance-service/src/main/java/com/eprocure/finance/presentation/mapper/PurchaseOrderPresentationMapper.java`
- `services/finance-service/src/main/resources/mapper/PurchaseOrderMapper.xml`
- `docs/api/finance-service.openapi.yaml`

**Backend behavior:**
- Add optional query param `pr_id`.
- Filter `finance.purchase_orders.po.pr_id = #{filter.prId}` khi có.
- Vẫn giữ ownership rule hiện tại: nếu user chỉ có `PO_VIEW_OWN`, filter thêm `purchasing_officer_id = actorId`.
- Không thêm endpoint mới.
- Không sửa migration.

**Test tối thiểu:**
- Nếu đã có test cho `ListPurchaseOrdersUseCase`, thêm case filter `prId`.
- Nếu controller test chưa có, ít nhất chạy `mvn -pl services/finance-service test`.

---

## 4. Slice 5b — Frontend Service Helpers

**Files sửa:**
- `frontend/eprocure-web/src/app/features/vendor/services/rfq.service.ts`
- `frontend/eprocure-web/src/app/features/finance/models/purchase-order.model.ts`
- `frontend/eprocure-web/src/app/features/finance/services/purchase-order.service.ts`

**RFQ helper:**
```typescript
listByPrId(prId: string): Observable<ApiResponse<RfqDetail[]> & { meta: PageMeta }> {
  return this.list({ page: 1, size: 10, sort: 'createdAt,desc', prId });
}
```

**PO filter/helper:**
```typescript
export interface PurchaseOrderListFilter {
  page: number;
  size: number;
  sort: string;
  status?: PurchaseOrderStatus;
  vendor_id?: string;
  pr_id?: string;
  from_date?: string;
  to_date?: string;
}

listByPrId(prId: string): Observable<ApiResponse<PurchaseOrder[]> & { meta: PageMeta }> {
  return this.list({ page: 1, size: 10, sort: 'createdAt,desc', pr_id: prId });
}
```

---

## 5. Slice 5c — PR Detail Traceability Card

**Files sửa:**
- `frontend/eprocure-web/src/app/features/procurement/pages/pr-detail/pr-detail.component.ts`
- `frontend/eprocure-web/src/app/features/procurement/pages/pr-detail/pr-detail.component.html`
- `frontend/eprocure-web/src/app/features/procurement/pages/pr-detail/pr-detail.component.scss`

**State mới:**
```typescript
private readonly rfqService = inject(RfqService);
private readonly poService = inject(PurchaseOrderService);

readonly relatedRfqs = signal<RfqDetail[]>([]);
readonly relatedPos = signal<PurchaseOrder[]>([]);
readonly relatedRfqLoading = signal(false);
readonly relatedPoLoading = signal(false);

readonly shouldLoadTraceability = computed(() =>
  ['APPROVED', 'CONVERTED_TO_PO', 'CLOSED'].includes(this.pr()?.status ?? '')
);
readonly traceabilityLoading = computed(() => this.relatedRfqLoading() || this.relatedPoLoading());
readonly hasTraceability = computed(() => this.relatedRfqs().length > 0 || this.relatedPos().length > 0);
```

**Load rule:**
- Gọi sau `loadPr(id)` success.
- Chỉ load khi `status ∈ APPROVED/CONVERTED_TO_PO/CLOSED`.
- RFQ và PO load độc lập; lỗi một request không block request còn lại.
- Khi không có permission/403, không toast lỗi trong PR detail, chỉ để empty/hidden state.

**Template card:**
```html
@if (shouldLoadTraceability()) {
  <ep-card tone="raised">
    <div class="card-section traceability-card">
      <div class="section-title-row">
        <h2 class="card-section__title">{{ 'pr.detail.section.traceability' | translate }}</h2>
        <ep-button variant="ghost" size="sm" icon="refresh-cw" (click)="reloadTraceability()">
          {{ 'action.refresh' | translate }}
        </ep-button>
      </div>

      @if (traceabilityLoading()) {
        <ep-skeleton [rows]="2" />
      } @else if (hasTraceability()) {
        <!-- RFQ group + PO group -->
      } @else {
        <p class="side-note">{{ 'pr.detail.trace.none' | translate }}</p>
      }
    </div>
  </ep-card>
}
```

**Trace link behavior:**
- RFQ: `this.router.navigate(['/vendors', 'rfq', rfq.id])`.
- PO: `this.router.navigate(['/finance', 'purchase-orders', po.id])`.
- Dùng `<button type="button">` hoặc clickable card với keyboard Enter/Space, tránh `<a href>`.

---

## 6. Slice 5d — Duplicate Action Guard

**Cập nhật computed hiện có:**
```typescript
readonly canCreateRfq = computed(() => {
  if (this.pr()?.status !== 'APPROVED') return false;
  return !this.relatedRfqs().some((rfq) => ['DRAFT', 'PUBLISHED', 'CLOSED', 'AWARDED'].includes(rfq.status));
});

readonly canCreatePo = computed(() => {
  if (this.pr()?.status !== 'APPROVED') return false;
  return !this.relatedPos().some((po) => po.status !== 'CANCELLED');
});
```

**Lưu ý status RFQ thật:** backend enum hiện là `DRAFT`, `PUBLISHED`, `CLOSED`, `AWARDED`, `CANCELLED`; không dùng `OPEN/EVALUATING`.

**Refresh after handoff:**
- Sau khi user quay lại PR detail từ RFQ/PO create, traceability load lại theo `loadPr()`.
- Có nút reload traceability để xử lý eventual consistency/race condition.

---

## 7. Slice 5e — i18n + Verify

**i18n keys thêm vào `vi.json` / `en.json`:**
```json
{
  "pr": {
    "detail": {
      "section": {
        "traceability": "Liên kết tài liệu"
      },
      "trace": {
        "rfq": "RFQ liên quan",
        "po": "Đơn đặt hàng liên quan",
        "none": "Chưa có RFQ hoặc PO được tạo từ PR này",
        "rfqCount": "{{count}} RFQ",
        "poCount": "{{count}} PO",
        "conversion": "Đồng bộ PR",
        "openRfq": "Mở RFQ {{number}}",
        "openPo": "Mở PO {{number}}"
      }
    }
  }
}
```

**Verify:**
```powershell
mvn -pl services/finance-service test
cd frontend/eprocure-web
npm run build
git diff --check
```

---

## 8. Nguyên tắc triển khai

- Không thêm HTTP DELETE endpoint.
- Backend filter phải đi qua query param typed UUID, không nối raw SQL.
- MyBatis filter tiếp tục giữ `po.is_deleted = FALSE`.
- OpenAPI phải cập nhật nếu thêm query param backend.
- Frontend request dùng service hiện có, `withCredentials: true`.
- Mọi visible text dùng translate key.
- CSS dùng design tokens `var(--...)`, không hardcode màu.
- Không query traceability ở PR List để tránh N+1.
- Không block render chính khi traceability load lỗi/chậm.
- Build/test pass mới coi plan hoàn thành.
