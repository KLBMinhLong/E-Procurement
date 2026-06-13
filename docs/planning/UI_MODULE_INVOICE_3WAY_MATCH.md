# Kế hoạch UI — Invoice 3-Way Match Visualization

> **Mục tiêu:** Nâng cấp màn hình Invoice Detail thành công cụ đối soát thực sự cho Accountant — thay thế hiển thị text thô của match result bằng bảng so sánh PO/GR/Invoice 3 chiều với highlight sai lệch trực quan, giúp phán quyết approve/dispute nhanh và chính xác.

**Trạng thái:** ✅ Hoàn thành 2026-06-13. Đã triển khai status strip, load PO/GR phụ trợ, aggregate GR theo `poLineItemId`, bảng 3-way comparison và fallback invoice lines khi PO/GR phụ trợ không tải được. Contract GR dùng đúng enum hiện tại `COMPLETE`.

---

## 1. Hiện trạng

### 1.1 Invoice Detail hiện có gì

**File:** `features/finance/pages/invoice-detail/invoice-detail.component.html`

**Match result section hiện tại:**
```html
<section class="detail-panel" aria-labelledby="invoice-match-heading">
  <h2>{{ 'finance.invoice.detail.section.match' | translate }}</h2>
  @if (data.matchResult; as match) {
    <dl class="info-list">
      <div>
        <dt>PO Match</dt>
        <dd><ep-badge [tone]="poMatchTone()" [labelKey]="'finance.invoice.match.status.' + match.poMatchStatus" /></dd>
      </div>
      <div>
        <dt>GR Match</dt>
        <dd><ep-badge [tone]="grMatchTone()" [labelKey]="'finance.invoice.match.status.' + match.grMatchStatus" /></dd>
      </div>
      <div>
        <dt>Qty Variance</dt>
        <dd class="mono">{{ match.qtyVariance }}</dd>  <!-- Raw string: "2.0000" -->
      </div>
      <div>
        <dt>Price Variance</dt>
        <dd class="mono">{{ match.priceVariance }}</dd>  <!-- Raw string: "-500000.0000" -->
      </div>
      <div>
        <dt>Matched At</dt>
        <dd class="mono">{{ formatDateTime(match.matchedAt) }}</dd>
      </div>
    </dl>
  } @else {
    <p>{{ 'finance.invoice.detail.match.empty' | translate }}</p>
  }
}
```

**Line items section hiện tại:**
```html
<table class="line-table">
  <thead>
    <tr>
      <th>STT</th><th>Mô tả</th><th>Số lượng</th><th>Đơn giá</th><th>Thành tiền</th>
    </tr>
  </thead>
  <tbody>
    @for (line of data.lineItems; track line.poLineItemId) {
      <tr>
        <td>{{ line.lineNumber }}</td>
        <td>{{ line.description }} / {{ shortId(line.poLineItemId) }}</td>
        <td class="mono">{{ line.quantity }}</td>   <!-- Không biết là đơn vị gì -->
        <td><ep-amount [value]="lineUnitAmount(data, line)" /></td>
        <td><ep-amount [value]="lineTotalAmount(data, line)" /></td>
      </tr>
    }
  </tbody>
</table>
```

### 1.2 Những gì THIẾU

**Từ backend API `matchResult`:**
```typescript
matchResult: {
  poMatchStatus: 'MATCHED' | 'MISMATCHED' | 'PARTIAL';
  grMatchStatus: 'MATCHED' | 'MISMATCHED' | 'PARTIAL';
  qtyVariance: string | null;      // Ví dụ "-2.0000" — thiếu bao nhiêu
  priceVariance: string | null;    // Ví dụ "500000.0000" — chênh bao nhiêu tiền
  matchedAt: string | null;
}
```

Nhưng **không có per-line breakdown** trong matchResult. Backend trả về aggregate variance, không phải từng dòng PO/GR/Invoice so sánh.

**Dữ liệu cần để hiển thị 3-way:**
- Invoice lines: có trong `data.lineItems` (lineNumber, poLineItemId, description, quantity, unitPrice, totalPrice)
- PO lines: cần gọi `GET /purchase-orders/{poId}` để lấy `lineItems` (PO đã có trong `data.po.id`)
- GR data: cần `GET /goods-receipts?po_id={poId}` để lấy completed GR của PO này — **chưa có trong FE**

---

## 2. Vấn đề hiện tại

| # | Vấn đề | Tác động |
|---|---|---|
| 1 | `qtyVariance` và `priceVariance` là raw string số không có context | Accountant không biết là thiếu/thừa, bao nhiêu đơn vị, dòng nào bị lệch |
| 2 | Không có bảng so sánh PO-GR-Invoice song song theo từng line | Phải mở 3 tab riêng để so sánh thủ công |
| 3 | Match status chỉ là badge cấp độ header, không có detail per-line | PARTIAL match không cho biết dòng nào bị PARTIAL |
| 4 | Line items bảng chỉ có invoice data, không có PO/GR baseline | Không biết PO order bao nhiêu, GR nhận bao nhiêu |
| 5 | Không có visual highlight hàng bị sai lệch | Phải đọc từng số để phát hiện bất thường |
| 6 | `match.qtyVariance` là string "-2.0000" — không rõ đơn vị và dấu âm/dương | Dễ hiểu sai chiều (thiếu hay thừa?) |
| 7 | Alert banner "requiresReview" chỉ là text, không hướng dẫn action | Accountant biết cần review nhưng không biết review cái gì |

---

## 3. Dữ liệu cần load thêm

### 3.1 PO Detail (đã có service)
```typescript
// PurchaseOrderService.getById(data.po.id) → PurchaseOrderDetail
// Lấy: lineItems[] với { id, lineNumber, itemName, categoryCode, quantity{amount,unit}, unitPrice, totalPrice }
```

### 3.2 GR liên quan đến PO (chưa có trong FE)
```typescript
// GoodsReceiptService.list({ po_id: data.po.id, status: 'COMPLETE' })
// Lấy: goodsReceipts[] → mỗi GR có lineItems[] với { receivedQuantity, rejectedQuantity, itemName }
// Aggregate GR quantities by poLineItemId
```

---

## 4. Thiết kế màn hình

### 4.1 Invoice Detail Layout — restructure

**Thay đổi layout chính:** Di chuyển match result section và line items section thành **3-way match panel** full-width, thay thế cả 2 section cũ.

**Giữ nguyên:** Summary grid (4 stat cards), source info panel (vendor/PO links), action buttons, modals.

---

### 4.2 Match Status Summary — cải thiện

**Thay `<dl>` hiện tại bằng match status strip:**

```html
<div class="match-status-strip">
  <!-- PO Match -->
  <div class="match-status-card" [class]="'match-status-card--' + poMatchClass()">
    <ep-icon [name]="poMatchIcon()" />
    <div>
      <strong>{{ 'finance.invoice.match.po' | translate }}</strong>
      <ep-badge [tone]="poMatchTone()" [labelKey]="'finance.invoice.match.status.' + match.poMatchStatus" />
    </div>
  </div>
  
  <!-- GR Match -->
  <div class="match-status-card" [class]="'match-status-card--' + grMatchClass()">
    <ep-icon [name]="grMatchIcon()" />
    <div>
      <strong>{{ 'finance.invoice.match.gr' | translate }}</strong>
      <ep-badge [tone]="grMatchTone()" [labelKey]="'finance.invoice.match.status.' + match.grMatchStatus" />
    </div>
  </div>
  
  <!-- Variance Summary -->
  <div class="match-variance-summary">
    <div class="variance-item" [class.variance-item--negative]="qtyVarianceNum() < 0">
      <ep-icon name="package" />
      <span>{{ 'finance.invoice.match.qtyVariance' | translate }}</span>
      <strong>{{ formatQtyVariance() }}</strong>  <!-- "+2 đơn vị" hoặc "-2 đơn vị" -->
    </div>
    <div class="variance-item" [class.variance-item--negative]="priceVarianceNum() > 0">
      <ep-icon name="circle-dollar-sign" />
      <span>{{ 'finance.invoice.match.priceVariance' | translate }}</span>
      <ep-amount [value]="absPriceVariance()" />  <!-- |variance| -->
      <ep-badge [tone]="priceVarianceTone()" [labelKey]="priceVarianceLabel()" />  <!-- "Hóa đơn cao hơn" / "Hóa đơn thấp hơn" -->
    </div>
  </div>
</div>
```

**Helper methods:**
```typescript
readonly qtyVarianceNum = computed(() => parseFloat(this.invoice()?.matchResult?.qtyVariance ?? '0'));
readonly priceVarianceNum = computed(() => parseFloat(this.invoice()?.matchResult?.priceVariance ?? '0'));
readonly absPriceVariance = computed(() => Math.abs(this.priceVarianceNum()).toFixed(4));

formatQtyVariance(): string {
  const v = this.qtyVarianceNum();
  const sign = v > 0 ? '+' : '';
  return `${sign}${v.toFixed(2)} đơn vị`;
}

priceVarianceLabel(): string {
  const v = this.priceVarianceNum();
  if (v > 0) return 'finance.invoice.match.overCharged';   // Hóa đơn cao hơn PO
  if (v < 0) return 'finance.invoice.match.underCharged';  // Hóa đơn thấp hơn PO
  return 'finance.invoice.match.exact';
}
```

---

### 4.3 3-Way Match Table — bảng so sánh trung tâm

**Thay `<section class="line-panel">` hiện tại bằng bảng 3 chiều:**

```html
<section class="three-way-panel" aria-labelledby="three-way-heading">
  <div class="panel-heading">
    <ep-icon name="scale" />
    <h2 id="three-way-heading">{{ 'finance.invoice.match.threeWayTitle' | translate }}</h2>
  </div>

  @if (isLoadingMatchData()) {
    <ep-skeleton [rows]="5" />
  } @else {
    <div class="three-way-table-wrapper">
      <table class="three-way-table">
        <thead>
          <tr>
            <th>{{ 'finance.invoice.match.col.lineItem' | translate }}</th>
            <!-- PO Column Group -->
            <th colspan="2" class="col-group col-group--po">
              <ep-icon name="file-text" /> {{ 'finance.invoice.match.col.poGroup' | translate }}
              <span class="mono">{{ invoice()?.po?.poNumber }}</span>
            </th>
            <!-- GR Column Group -->
            <th colspan="2" class="col-group col-group--gr">
              <ep-icon name="package-check" /> {{ 'finance.invoice.match.col.grGroup' | translate }}
            </th>
            <!-- Invoice Column Group -->
            <th colspan="2" class="col-group col-group--invoice">
              <ep-icon name="receipt-text" /> {{ 'finance.invoice.match.col.invoiceGroup' | translate }}
              <span class="mono">{{ invoice()?.invoiceNumber }}</span>
            </th>
            <th>{{ 'finance.invoice.match.col.matchResult' | translate }}</th>
          </tr>
          <tr class="sub-header">
            <th></th>
            <th>{{ 'finance.invoice.match.col.qty' | translate }}</th>
            <th>{{ 'finance.invoice.match.col.unitPrice' | translate }}</th>
            <th>{{ 'finance.invoice.match.col.receivedQty' | translate }}</th>
            <th>{{ 'finance.invoice.match.col.rejectedQty' | translate }}</th>
            <th>{{ 'finance.invoice.match.col.qty' | translate }}</th>
            <th>{{ 'finance.invoice.match.col.unitPrice' | translate }}</th>
            <th></th>
          </tr>
        </thead>
        <tbody>
          @for (row of matchRows(); track row.poLineItemId) {
            <tr [class.three-way-table__row--mismatch]="row.hasVariance">
              <td class="item-cell">
                <strong>{{ row.itemName }}</strong>
                <span class="mono text-muted">{{ row.categoryCode }}</span>
              </td>
              <!-- PO Data -->
              <td class="align-right mono">{{ row.poQty }} {{ row.unit }}</td>
              <td class="align-right"><ep-amount [value]="row.poUnitPrice" /></td>
              <!-- GR Data -->
              <td class="align-right mono" [class.text-success]="row.grReceivedQty === row.poQty">
                {{ row.grReceivedQty !== null ? row.grReceivedQty + ' ' + row.unit : '--' }}
              </td>
              <td class="align-right mono" [class.text-danger]="row.grRejectedQty > 0">
                {{ row.grRejectedQty > 0 ? row.grRejectedQty : '--' }}
              </td>
              <!-- Invoice Data -->
              <td class="align-right mono" [class.text-warning]="row.invoiceQty !== row.poQty">
                {{ row.invoiceQty }} {{ row.unit }}
                @if (row.invoiceQty !== row.poQty) {
                  <span class="variance-tag">{{ formatVariance(row.invoiceQty, row.poQty) }}</span>
                }
              </td>
              <td class="align-right" [class.text-warning]="row.priceVariancePct > 0.01">
                <ep-amount [value]="row.invoiceUnitPrice" />
                @if (row.priceVariancePct > 0.01) {
                  <span class="variance-tag">{{ formatPct(row.priceVariancePct) }}</span>
                }
              </td>
              <!-- Match Result per line -->
              <td>
                @if (!row.hasVariance) {
                  <ep-badge tone="success" labelKey="finance.invoice.match.status.MATCHED" />
                } @else {
                  <ep-badge tone="warning" labelKey="finance.invoice.match.status.PARTIAL" />
                }
              </td>
            </tr>
          }
        </tbody>
      </table>
    </div>
  }
</section>
```

---

### 4.4 matchRows computed — logic xây dựng bảng

```typescript
// Invoice Detail Component TS

interface MatchRow {
  poLineItemId: string;
  itemName: string;
  categoryCode: string;
  unit: string;
  // PO baseline
  poQty: number;
  poUnitPrice: string;
  // GR actuals (aggregate từ tất cả completed GR)
  grReceivedQty: number | null;
  grRejectedQty: number;
  // Invoice claims
  invoiceQty: number;
  invoiceUnitPrice: string;
  // Computed
  priceVariancePct: number;  // |invoiceUnitPrice - poUnitPrice| / poUnitPrice
  hasVariance: boolean;
}

readonly matchRows = computed<MatchRow[]>(() => {
  const invoice = this.invoice();
  const poLines = this.poDetail()?.lineItems ?? [];
  const grSummary = this.grSummary();  // Map<poLineItemId, {received, rejected}>

  if (!invoice || !poLines.length) return [];

  return invoice.lineItems.map(invLine => {
    const poLine = poLines.find(p => p.id === invLine.poLineItemId);
    const gr = grSummary.get(invLine.poLineItemId);
    const poQty = parseFloat(poLine?.quantity?.amount ?? '0');
    const invQty = parseFloat(invLine.quantity ?? '0');
    const poPrice = parseFloat(poLine?.unitPrice ?? '0');
    const invPrice = parseFloat(invLine.unitPrice ?? '0');
    const priceVariancePct = poPrice > 0 ? Math.abs(invPrice - poPrice) / poPrice : 0;

    return {
      poLineItemId: invLine.poLineItemId,
      itemName: poLine?.itemName ?? invLine.description,
      categoryCode: poLine?.categoryCode ?? '',
      unit: poLine?.quantity?.unit ?? '',
      poQty,
      poUnitPrice: poLine?.unitPrice ?? '0',
      grReceivedQty: gr?.received ?? null,
      grRejectedQty: gr?.rejected ?? 0,
      invoiceQty: invQty,
      invoiceUnitPrice: invLine.unitPrice,
      priceVariancePct,
      hasVariance: Math.abs(invQty - poQty) > 0.001 || priceVariancePct > 0.01
    };
  });
});
```

---

### 4.5 GR Aggregate — tổng hợp từ nhiều GR

```typescript
// Compute GR summary Map<poLineItemId, {received, rejected}>
readonly grSummary = computed<Map<string, {received: number; rejected: number}>>(() => {
  const map = new Map<string, {received: number; rejected: number}>();
  const grs = this.completedGrs();
  for (const gr of grs) {
    for (const line of gr.lineItems ?? []) {
      const key = line.poLineItemId ?? line.catalogItemCode;
      if (!key) continue;
      const existing = map.get(key) ?? { received: 0, rejected: 0 };
      existing.received += parseFloat(line.acceptedQuantity ?? '0');
      existing.rejected += parseFloat(line.rejectedQuantity ?? '0');
      map.set(key, existing);
    }
  }
  return map;
});
```

---

## 5. New State và Services

**Thêm vào Invoice Detail Component:**
```typescript
// Mới
readonly poDetail = signal<PurchaseOrderDetail | null>(null);
readonly completedGrs = signal<GoodsReceipt[]>([]);
readonly isLoadingMatchData = signal(false);

private loadMatchData(): void {
  const invoice = this.invoice();
  if (!invoice?.po?.id || invoice.status === 'PENDING_MATCH') return;
  
  this.isLoadingMatchData.set(true);
  
  forkJoin({
    po: this.poService.getById(invoice.po.id),
    grs: this.grService.list({ po_id: invoice.po.id, status: 'COMPLETE', page: 1, size: 50 })
  })
    .pipe(
      takeUntilDestroyed(this.destroyRef),
      finalize(() => this.isLoadingMatchData.set(false))
    )
    .subscribe({
      next: ({ po, grs }) => {
        this.poDetail.set(po.data);
        this.completedGrs.set(grs.data ?? []);
      }
    });
}
```

**Inject thêm:**
```typescript
private readonly poService = inject(PurchaseOrderService);
private readonly grService = inject(GoodsReceiptService);
```

---

## 6. SCSS — visual cues

```scss
// Hàng có sai lệch
.three-way-table__row--mismatch {
  background: color-mix(in srgb, var(--color-warning) 8%, transparent);
  
  &:hover {
    background: color-mix(in srgb, var(--color-warning) 14%, transparent);
  }
}

// Column group headers
.col-group--po { background: color-mix(in srgb, var(--color-info) 10%, transparent); }
.col-group--gr { background: color-mix(in srgb, var(--color-success) 10%, transparent); }
.col-group--invoice { background: color-mix(in srgb, var(--color-accent) 10%, transparent); }

// Variance tag inline
.variance-tag {
  display: inline-block;
  padding: 0 0.25rem;
  border-radius: var(--radius-1);
  background: var(--color-warning-subtle);
  color: var(--color-warning);
  font-size: var(--font-size-xs);
  font-weight: 700;
}

// Match status strip
.match-status-card--matched { border-left: 3px solid var(--color-success); }
.match-status-card--mismatched { border-left: 3px solid var(--color-danger); }
.match-status-card--partial { border-left: 3px solid var(--color-warning); }
```

---

## 7. i18n keys cần thêm

```json
"finance.invoice.match.threeWayTitle": "Đối soát 3 chiều PO / GR / Hóa đơn",
"finance.invoice.match.po": "Đơn đặt hàng (PO)",
"finance.invoice.match.gr": "Phiếu nhận hàng (GR)",
"finance.invoice.match.col.lineItem": "Mặt hàng",
"finance.invoice.match.col.poGroup": "PO",
"finance.invoice.match.col.grGroup": "GR (đã nhận)",
"finance.invoice.match.col.invoiceGroup": "Hóa đơn",
"finance.invoice.match.col.qty": "Số lượng",
"finance.invoice.match.col.unitPrice": "Đơn giá",
"finance.invoice.match.col.receivedQty": "Đã nhận",
"finance.invoice.match.col.rejectedQty": "Bị từ chối",
"finance.invoice.match.col.matchResult": "Kết quả",
"finance.invoice.match.qtyVariance": "Lệch số lượng",
"finance.invoice.match.priceVariance": "Lệch giá trị",
"finance.invoice.match.overCharged": "Hóa đơn cao hơn PO",
"finance.invoice.match.underCharged": "Hóa đơn thấp hơn PO",
"finance.invoice.match.exact": "Khớp chính xác",
"finance.invoice.match.status.MATCHED": "Khớp",
"finance.invoice.match.status.MISMATCHED": "Không khớp",
"finance.invoice.match.status.PARTIAL": "Khớp một phần"
```

---

## 8. Nguyên tắc triển khai

- 3-way table chỉ load khi `invoice.status !== 'PENDING_MATCH'` — khi chưa match thì không có matchResult, chỉ hiển thị invoice lines đơn giản
- Load PO + GR data song song bằng `forkJoin`
- GR query: `status=COMPLETE` và `po_id={poId}` — lấy tất cả GR đã hoàn tất của PO
- Khi không có PO detail (lỗi 403/404): hiển thị invoice lines cũ như hiện tại
- Khi không có GR data: cột GR hiển thị `--` với tooltip "Chưa có GR hoàn tất"
- `priceVariancePct > 1%` mới highlight để tránh false positive từ làm tròn số
- Tất cả amount: dùng `ep-amount` component
- Không hardcode màu — dùng CSS variables
- Table responsive: horizontal scroll trên màn hình nhỏ

---

## 9. Kết quả triển khai

| Bước | Nội dung | Status |
|---|---|---|
| 6a | Inject `PurchaseOrderService` và `GoodsReceiptService`, thêm helper load phụ trợ không redirect | ✅ |
| 6b | Thêm state `poDetail`, `completedGrs`, `grSummary`, `matchRows` | ✅ |
| 6c | Cải thiện match status summary strip thay `<dl>` cũ | ✅ |
| 6d | Build 3-way comparison table thay line panel cũ, có fallback invoice lines | ✅ |
| 6e | i18n VI/EN + SCSS token-based | ✅ |
| 6f | Verify `npm run build` | ✅ |
