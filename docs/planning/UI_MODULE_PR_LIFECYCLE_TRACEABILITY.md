# Kế hoạch UI — PR Lifecycle & Traceability (Post-Approval Links)

> **Mục tiêu:** Bổ sung khả năng truy vết vòng đời đầy đủ của PR sau khi được duyệt — hiển thị RFQ đã tạo từ PR, PO đã được tạo, trạng thái chuyển đổi — giúp Requester và Purchasing biết chính xác PR đang ở giai đoạn nào của procurement cycle.

---

## 1. Hiện trạng

### 1.1 PR Detail có gì

**File:** `features/procurement/pages/pr-detail/pr-detail.component.html` + `.ts`

**Phần header actions:**
```typescript
// component.ts
readonly canCreatePo = computed(() => this.pr()?.status === 'APPROVED');
readonly canCreateRfq = computed(() => this.pr()?.status === 'APPROVED');
```
```html
<!-- Nút tạo PO khi APPROVED -->
@if (canCreatePo()) {
  <ep-button *epHasPermission="'PO_CREATE'" (click)="navigateCreatePo()">
    {{ 'pr.detail.action.createPo' | translate }}
  </ep-button>
}
@if (canCreateRfq()) {
  <ep-button *epHasPermission="'RFQ_CREATE'" (click)="navigateCreateRfq()">
    {{ 'pr.detail.action.createRfq' | translate }}
  </ep-button>
}
```

**Sidebar approval section:** Có timeline steps duyệt (xem planning `UI_MODULE_APPROVAL_FLOW_DIAGRAM.md`)

**Model `PurchaseRequestDetail`:**
```typescript
// Hiện có:
status: PrStatus;  // Bao gồm 'CONVERTED_TO_PO'
approvalProcess: { ... } | null;
budgetCheck: BudgetCheckResult | null;
inventoryCheck: { ... } | null;
// KHÔNG CÓ:
// - rfqId / rfqNumber
// - poId / poNumber
// - prConversionStatus (chỉ finance-service biết, không trả về từ PR API)
```

### 1.2 Các trường hợp thiếu

**Kịch bản 1 — PR APPROVED, đã tạo RFQ:**
- Requester vào PR detail → thấy status `APPROVED` và nút "Tạo RFQ" vẫn hiện
- Không biết purchasing đã tạo RFQ từ PR này chưa, RFQ số gì, trạng thái nào
- Nếu nhấn lại "Tạo RFQ" → tạo RFQ trùng (business error)

**Kịch bản 2 — PR APPROVED, đã tạo PO (manual direct PO):**
- Status PR vẫn là `APPROVED` cho đến khi callback `CONVERTED_TO_PO` hoàn tất
- Requester không biết PO đã được tạo, PO number là gì, trạng thái PO

**Kịch bản 3 — PR `CONVERTED_TO_PO`:**
- Status badge hiển thị `CONVERTED_TO_PO`
- Nhưng không có link nào sang PO tương ứng
- Purchasing không biết PO nào là từ PR này

**Kịch bản 4 — PR List không có status indicator đầy đủ:**
- PR list chỉ hiển thị `status` cơ bản
- Không có visual "PR này đang ở giai đoạn: đang RFQ / đã có PO / đang GR"

---

## 2. Vấn đề gốc rễ

### 2.1 Backend không trả về traceability links trong PR API

`GET /api/v1/purchase-requests/{id}` response hiện không bao gồm:
- `rfqIds` / `rfqNumbers` (từ vendor-service)
- `poId` / `poNumber` (từ finance-service)

Đây là cross-service data: PR service không biết về RFQ và PO của service khác.

### 2.2 Giải pháp không cần thay đổi backend

**Cách 1 — Query cross-service từ frontend:**
- Gọi `GET /api/v1/rfq?prId={prId}` (vendor-service) để tìm RFQ của PR này
- Gọi `GET /api/v1/purchase-orders?pr_id={prId}` (finance-service, nếu filter đã có) để tìm PO

**Cách 2 — Dùng URL params để truy ngược:**
- Khi người dùng đến từ RFQ create flow, set state
- Khi PO detail có `prNumber`, link ngược lại

**Phương án chọn:** Cách 1 (query cross-service) — đơn giản nhất, không cần backend thay đổi, dữ liệu realtime.

---

## 3. Thiết kế giải pháp

### 3.1 PR Detail — "Traceability Section"

**Vị trí:** Sidebar của PR detail, dưới approval card

**Trigger load:** Khi `pr()?.status` thuộc `['APPROVED', 'CONVERTED_TO_PO', 'CLOSED']` — load lazy.

**Cấu trúc section:**
```html
<ep-card tone="raised">
  <div class="card-section">
    <h2 class="card-section__title">{{ 'pr.detail.section.traceability' | translate }}</h2>
    
    <!-- RFQ liên quan -->
    @if (relatedRfqLoading()) { <ep-skeleton [rows]="2" /> }
    @else if (relatedRfqs().length) {
      <div class="trace-group">
        <span class="trace-label">{{ 'pr.detail.trace.rfq' | translate }}</span>
        @for (rfq of relatedRfqs(); track rfq.id) {
          <a [routerLink]="['/vendors/rfq', rfq.id]" class="trace-link">
            <ep-icon name="file-search" [size]="14" />
            {{ rfq.rfqNumber }}
            <ep-badge [tone]="rfqStatusTone(rfq.status)" [labelKey]="'rfq.status.' + rfq.status" />
          </a>
        }
      </div>
    }
    
    <!-- PO liên quan -->
    @if (relatedPoLoading()) { <ep-skeleton [rows]="2" /> }
    @else if (relatedPos().length) {
      <div class="trace-group">
        <span class="trace-label">{{ 'pr.detail.trace.po' | translate }}</span>
        @for (po of relatedPos(); track po.id) {
          <a [routerLink]="['/finance/purchase-orders', po.id]" class="trace-link">
            <ep-icon name="receipt-text" [size]="14" />
            {{ po.poNumber }}
            <ep-badge [tone]="poStatusTone(po.status)" [labelKey]="'po.status.' + po.status" />
          </a>
        }
      </div>
    }
    
    <!-- Không có liên kết nào -->
    @if (!relatedRfqs().length && !relatedPos().length && !relatedRfqLoading() && !relatedPoLoading()) {
      <p class="side-note">{{ 'pr.detail.trace.none' | translate }}</p>
    }
  </div>
</ep-card>
```

---

### 3.2 Action buttons — logic cập nhật

**Hiện tại:** Cả 2 nút "Tạo RFQ" và "Tạo PO" hiện khi `status === 'APPROVED'`

**Cần thêm logic ẩn nút khi đã có liên kết:**
```typescript
// Ẩn "Tạo RFQ" nếu đã có ít nhất 1 RFQ OPEN/EVALUATING/AWARDED từ PR này
readonly canCreateRfq = computed(() => {
  if (this.pr()?.status !== 'APPROVED') return false;
  const rfqs = this.relatedRfqs();
  const hasActiveRfq = rfqs.some(r => ['OPEN', 'EVALUATING', 'AWARDED'].includes(r.status));
  return !hasActiveRfq;
});

// Ẩn "Tạo PO" nếu đã có PO (bất kỳ status nào trừ CANCELLED)
readonly canCreatePo = computed(() => {
  if (this.pr()?.status !== 'APPROVED') return false;
  const pos = this.relatedPos();
  const hasActivePo = pos.some(p => p.status !== 'CANCELLED');
  return !hasActivePo;
});
```

---

### 3.3 PR Detail — Lifecycle Status Bar

**Vị trí:** Ngay dưới page header, trên metric strip, full-width

**Stages cho PR lifecycle:**
```
DRAFT → SUBMITTED → PENDING_APPROVAL → APPROVED → [PO / RFQ] → CLOSED
                                           ↓
                                     REJECTED | CANCELLED
                                     CHANGES_REQUESTED (loop back)
```

**Visual:**
```
●──────────●──────────●──────────●──────────◌──────────◌
 Nháp      Đã nộp     Đang duyệt  Đã duyệt   PO/RFQ     Đóng
 ✓          ✓          ✓●          (current)
```

**Implementation:**
```typescript
// Tính stage hiện tại
readonly lifecycleStage = computed(() => {
  const status = this.pr()?.status;
  const stageMap: Record<string, number> = {
    'DRAFT': 0,
    'SUBMITTED': 1,
    'PENDING_APPROVAL': 2,
    'CHANGES_REQUESTED': 2,    // vẫn ở stage duyệt
    'APPROVED': 3,
    'CONVERTED_TO_PO': 4,
    'CLOSED': 5,
    'REJECTED': -1,            // terminal state đặc biệt
    'CANCELLED': -1
  };
  return stageMap[status ?? ''] ?? 0;
});
```

---

### 3.4 PR List — Additional Status Context

**File:** `features/procurement/pages/pr-list/`

**Hiện tại:** Chỉ có `ep-badge` cho `status` cơ bản.

**Thêm:** Mini trace indicator — icon nhỏ khi PR đã có RFQ hoặc PO liên kết:
```html
@if (pr.hasRfq) {
  <ep-icon name="file-search" [size]="12" class="trace-indicator" title="Có RFQ" />
}
@if (pr.hasPo) {
  <ep-icon name="receipt-text" [size]="12" class="trace-indicator" title="Có PO" />
}
```

**Lưu ý:** Thông tin `hasRfq` / `hasPo` cần backend thêm vào PR list response, hoặc chỉ hiển thị khi user click vào detail (không load cho cả list vì performance).

**Phương án thực tế:** Chỉ làm ở PR detail (sidebar section) — không thêm ở PR list để tránh N+1 queries.

---

## 4. Services cần thay đổi

### 4.1 RFQ Service — thêm filter `prId`

**File:** `features/vendor/services/rfq.service.ts`

**Thêm method:**
```typescript
listByPrId(prId: string): Observable<ApiResponse<RfqSummary[]>> {
  const params = new HttpParams().set('pr_id', prId).set('page', 1).set('size', 10);
  return this.http.get<...>(`${this.baseUrl}/rfq`, { params, withCredentials: true });
}
```

**Interface `RfqSummary` (đã có trong models nhưng cần verify):**
```typescript
interface RfqSummary {
  id: string;
  rfqNumber: string;
  status: 'OPEN' | 'EVALUATING' | 'AWARDED' | 'CLOSED' | 'CANCELLED';
  prId: string | null;
  prNumber: string | null;
  title: string;
  submissionDeadline: string;
  awardedQuoteId: string | null;
}
```

### 4.2 Purchase Order Service — thêm filter `pr_id`

**File:** `features/finance/services/purchase-order.service.ts`

**Kiểm tra `PurchaseOrderListFilter`** — thêm `pr_id?: string` nếu chưa có:
```typescript
export interface PurchaseOrderListFilter {
  page: number;
  size: number;
  sort: string;
  status?: string;
  vendor_id?: string;
  pr_id?: string;       // ← cần thêm nếu chưa có
  from_date?: string;
  to_date?: string;
}
```

**Thêm method:**
```typescript
listByPrId(prId: string): Observable<ApiResponse<PurchaseOrder[]>> {
  const params = new HttpParams().set('pr_id', prId).set('page', 1).set('size', 5);
  return this.http.get<...>(`${this.baseUrl}/purchase-orders`, { params, withCredentials: true });
}
```

---

## 5. PR Detail Component — thêm state và logic

**Thêm vào `pr-detail.component.ts`:**
```typescript
// Inject thêm
private readonly rfqService = inject(RfqService);
private readonly poService = inject(PurchaseOrderService);

// State mới
readonly relatedRfqs = signal<RfqSummary[]>([]);
readonly relatedPos = signal<PurchaseOrder[]>([]);
readonly relatedRfqLoading = signal(false);
readonly relatedPoLoading = signal(false);

// Computed cập nhật (override hiện có)
readonly canCreateRfq = computed(() => {
  if (this.pr()?.status !== 'APPROVED') return false;
  return !this.relatedRfqs().some(r => ['OPEN', 'EVALUATING', 'AWARDED'].includes(r.status));
});
readonly canCreatePo = computed(() => {
  if (this.pr()?.status !== 'APPROVED') return false;
  return !this.relatedPos().some(p => p.status !== 'CANCELLED');
});

// Load trong ngOnInit sau loadPr() success
private loadTraceability(prId: string): void {
  const status = this.pr()?.status;
  if (!['APPROVED', 'CONVERTED_TO_PO', 'CLOSED'].includes(status ?? '')) return;
  
  this.relatedRfqLoading.set(true);
  this.rfqService.listByPrId(prId)
    .pipe(takeUntilDestroyed(this.destroyRef), finalize(() => this.relatedRfqLoading.set(false)))
    .subscribe({ next: res => this.relatedRfqs.set(res.data ?? []) });
  
  this.relatedPoLoading.set(true);
  this.poService.listByPrId(prId)
    .pipe(takeUntilDestroyed(this.destroyRef), finalize(() => this.relatedPoLoading.set(false)))
    .subscribe({ next: res => this.relatedPos.set(res.data ?? []) });
}
```

---

## 6. i18n keys cần thêm

```json
"pr.detail.section.traceability": "Liên kết tài liệu",
"pr.detail.trace.rfq": "RFQ liên quan",
"pr.detail.trace.po": "Đơn đặt hàng liên quan",
"pr.detail.trace.none": "Chưa có RFQ hoặc PO được tạo từ PR này",
"pr.detail.section.lifecycle": "Vòng đời yêu cầu",
"pr.lifecycle.DRAFT": "Nháp",
"pr.lifecycle.SUBMITTED": "Đã nộp",
"pr.lifecycle.PENDING_APPROVAL": "Đang duyệt",
"pr.lifecycle.APPROVED": "Đã duyệt",
"pr.lifecycle.CONVERTED_TO_PO": "Đã tạo PO",
"pr.lifecycle.REJECTED": "Bị từ chối",
"pr.lifecycle.CANCELLED": "Đã hủy",
"pr.lifecycle.CLOSED": "Đã đóng"
```

---

## 7. Phụ thuộc và rủi ro

| Điểm | Loại | Mô tả | Giải pháp |
|---|---|---|---|
| `GET /rfq?pr_id={prId}` | API filter | Cần verify backend vendor-service có filter `pr_id` không | Kiểm tra `vendor-service.openapi.yaml`, nếu thiếu cần thêm backend |
| `GET /purchase-orders?pr_id={prId}` | API filter | Tương tự | Kiểm tra `finance-service.openapi.yaml` filter params |
| Load traceability bị chậm | Performance | 2 request song song khi vào PR detail APPROVED | Dùng `forkJoin`, chỉ load khi status phù hợp, không block render chính |
| RFQ/PO không có khi mới tạo | Race condition | Vừa tạo RFQ, quay lại PR detail ngay → chưa có data | Thêm `[Reload]` button trong traceability section |

---

## 8. Nguyên tắc triển khai

- Load traceability lazy — chỉ trigger khi `status` ∈ `['APPROVED', 'CONVERTED_TO_PO', 'CLOSED']`
- Traceability section không block render chính của PR detail — skeleton riêng trong section
- `relatedRfqs()` và `relatedPos()` load song song bằng 2 request độc lập (không dùng forkJoin để tránh block một cái khi cái kia lỗi)
- Khi request lỗi (404, 403): section ẩn đi — không hiển thị error message trong traceability
- Link sang RFQ/PO dùng `routerLink` (Angular router), không dùng `href`
- Lifecycle status bar: full-width, không có overflow, mobile-friendly (thu gọn text, chỉ hiện icon)
