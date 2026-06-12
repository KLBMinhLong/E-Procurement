# Kế hoạch UI — RFQ Quote Comparison Table

> **Mục tiêu:** Nâng cấp phần hiển thị quotes trong RFQ Detail từ dạng card grid thông tin đơn lẻ thành bảng so sánh cạnh nhau giữa các vendor, giúp Purchasing đưa ra quyết định award nhanh và có cơ sở rõ ràng.

---

## 1. Hiện trạng

### 1.1 RFQ Detail hiện có gì

**File:** `features/vendor/pages/rfq-detail/rfq-detail.component.html`

**Quote section hiện tại:**
```html
<div class="quotes-grid">
  @for (quote of quotes(); track quote.id) {
    <div class="quote-card" [class.quote-card--awarded]="rfqData.awardedQuoteId === quote.id">
      <!-- Award ribbon nếu được chọn -->
      <div class="quote-card__header">
        <h3>{{ quote.vendorName }}</h3>
        <div class="quote-card__price">{{ formatQuoteTotal(quote) }}</div>
      </div>
      <div class="quote-card__body">
        <div>Submitted: {{ formatDate(quote.submittedAt) }}</div>
        <div>Valid until: {{ quote.validUntil }}</div>
        <div>Payment terms: {{ quote.paymentTerms }}</div>
        <!-- Score nếu đã evaluate -->
        @if (quote.evaluationScore !== null) {
          <div class="score-display">
            {{ quote.evaluationScore }} / 100
          </div>
          <p>"{{ quote.evaluationNote }}"</p>
        }
      </div>
      <div class="quote-card__actions">
        <!-- Evaluate form inline hoặc Award button -->
      </div>
    </div>
  }
</div>
```

**Vấn đề cốt lõi:** Mỗi quote là 1 card độc lập. Không có cách nhìn ngang để so sánh giá từng line item giữa vendor A, B, C cùng lúc.

### 1.2 Model quote hiện có

**File:** `features/vendor/models/vendor.model.ts`

```typescript
export interface VendorQuote {
  id: string;
  vendorId: string;
  vendorName: string;
  currency: string;
  totalAmount: string;
  validUntil: string | null;
  paymentTerms: string | null;
  notes: string | null;
  submittedAt: string;
  evaluationScore: number | null;
  evaluationNote: string | null;
  lineItems: VendorQuoteLineItem[];
}

export interface VendorQuoteLineItem {
  id: string;
  rfqLineItemId: string;
  unitPrice: string;
  deliveryDays: number | null;
}
```

**Vấn đề:** `VendorQuoteLineItem` có `rfqLineItemId` nhưng không có `itemName`. Phải join với `rfqData.lineItems` để lấy tên hàng.

---

## 2. Vấn đề hiện tại

| # | Vấn đề | Ảnh hưởng |
|---|---|---|
| 1 | Card grid mỗi vendor 1 card — không so sánh ngang được | Purchasing phải nhìn qua lại từng card, dễ nhầm |
| 2 | Không biết vendor nào có giá thấp nhất cho từng line item | Không có highlight để quét nhanh |
| 3 | `totalAmount` chỉ là tổng cộng — không thể biết tổng tính từ line items nào | Nếu vendor chỉ quote 1 phần các items thì total bị sai |
| 4 | Không có cột "Delivery Days" trong view chính | Thông tin giao hàng quan trọng nhưng ẩn trong card |
| 5 | Evaluate form xuất hiện inline trong card — layout bị vỡ khi form mở rộng | UX kém khi có nhiều quotes |
| 6 | Không có tóm tắt điểm đánh giá tổng hợp theo tiêu chí | Chỉ có 1 số điểm tổng, không phân loại technical/price/delivery |
| 7 | Award button chỉ hiện khi đã evaluate, mỗi quote riêng — không có action tổng hợp | Khó biết status tổng thể: mấy vendor chưa evaluate? |
| 8 | Không có "Quote coverage" — vendor có quote đủ tất cả line items không? | Vendor chỉ quote 3/5 items sẽ bị bỏ qua nếu không check kỹ |

---

## 3. Thiết kế giải pháp

### 3.1 Tab/Toggle view — 2 chế độ xem

Giữ nguyên card grid như hiện tại (cho trường hợp ít quotes), **thêm chế độ "Comparison Table"** toggle được:

```html
<div class="quotes-view-toggle">
  <button
    [class.active]="quoteViewMode() === 'cards'"
    (click)="quoteViewMode.set('cards')">
    <ep-icon name="layout-grid" /> {{ 'rfq.detail.quotes.viewCards' | translate }}
  </button>
  <button
    [class.active]="quoteViewMode() === 'compare'"
    (click)="quoteViewMode.set('compare')">
    <ep-icon name="columns-3" /> {{ 'rfq.detail.quotes.viewCompare' | translate }}
  </button>
</div>

@if (quoteViewMode() === 'compare') {
  <!-- Comparison table -->
} @else {
  <!-- Card grid hiện tại -->
}
```

**Default:** `compare` khi có ≥ 2 quotes, `cards` khi chỉ có 1 quote.

---

### 3.2 Comparison Table Layout

**Cấu trúc bảng:** Header row = vendors, body rows = line items

```
                    | Vendor A    | Vendor B    | Vendor C    |
--------------------|-------------|-------------|-------------|
Tổng giá báo        | 45,000,000  | 42,500,000  | 47,200,000  |
Điểm đánh giá       | 87/100      | 92/100      | --          |
Valid until         | 30/06       | 25/06       | 28/06       |
Payment terms       | Net 30      | Net 15      | COD         |
Delivery days (avg) | 7 ngày      | 5 ngày      | 10 ngày     |
─────────── Line Items ───────────────────────────────────────
Item 1: Laptop X    | 22,000,000★ | 23,000,000  | 21,800,000  |
Item 2: Màn hình Y  | 8,500,000   | 7,800,000★  | 9,000,000   |
Item 3: Chuột Z     | 500,000★    | 600,000     | 450,000★    |
```

★ = giá thấp nhất cho item đó (highlight `var(--color-success)`)

---

### 3.3 Template comparison table

```html
@if (quoteViewMode() === 'compare' && quotes().length >= 2) {
  <div class="comparison-table-wrapper">
    <table class="comparison-table">
      <thead>
        <tr>
          <th class="comparison-table__row-header"></th>
          @for (quote of quotes(); track quote.id) {
            <th class="comparison-table__vendor-col"
                [class.comparison-table__vendor-col--awarded]="rfqData.awardedQuoteId === quote.id"
                [class.comparison-table__vendor-col--best-score]="quote.id === bestScoreQuoteId()">
              <div class="vendor-header">
                <strong>{{ quote.vendorName }}</strong>
                @if (rfqData.awardedQuoteId === quote.id) {
                  <ep-badge tone="success" labelKey="rfq.detail.quotes.awarded" />
                }
                @if (quote.id === bestScoreQuoteId()) {
                  <ep-badge tone="info" labelKey="rfq.detail.quotes.bestScore" />
                }
              </div>
            </th>
          }
        </tr>
      </thead>
      <tbody>
        <!-- Summary rows -->
        <tr class="comparison-table__summary-row">
          <td class="row-label">{{ 'rfq.detail.compare.totalAmount' | translate }}</td>
          @for (quote of quotes(); track quote.id) {
            <td class="align-right" [class.comparison-table__cell--lowest]="quote.id === lowestTotalQuoteId()">
              <ep-amount [value]="quote.totalAmount" />
              @if (quote.id === lowestTotalQuoteId()) {
                <ep-icon name="trending-down" [size]="12" class="best-icon" title="Giá thấp nhất" />
              }
            </td>
          }
        </tr>
        
        <tr>
          <td class="row-label">{{ 'rfq.detail.compare.score' | translate }}</td>
          @for (quote of quotes(); track quote.id) {
            <td class="align-right">
              @if (quote.evaluationScore !== null) {
                <strong class="score-value" [class.score-value--best]="quote.id === bestScoreQuoteId()">
                  {{ quote.evaluationScore }}<span>/100</span>
                </strong>
              } @else {
                <span class="text-muted">--</span>
              }
            </td>
          }
        </tr>
        
        <tr>
          <td class="row-label">{{ 'rfq.detail.compare.validUntil' | translate }}</td>
          @for (quote of quotes(); track quote.id) {
            <td class="mono align-right">{{ formatDate(quote.validUntil) }}</td>
          }
        </tr>
        
        <tr>
          <td class="row-label">{{ 'rfq.detail.compare.paymentTerms' | translate }}</td>
          @for (quote of quotes(); track quote.id) {
            <td>{{ quote.paymentTerms || '--' }}</td>
          }
        </tr>
        
        <tr>
          <td class="row-label">{{ 'rfq.detail.compare.avgDeliveryDays' | translate }}</td>
          @for (quote of quotes(); track quote.id) {
            <td class="align-right mono" [class.comparison-table__cell--best]="quote.id === fastestDeliveryQuoteId()">
              {{ avgDeliveryDays(quote) }} {{ 'rfq.detail.compare.days' | translate }}
            </td>
          }
        </tr>
        
        <!-- Coverage warning -->
        <tr>
          <td class="row-label">{{ 'rfq.detail.compare.coverage' | translate }}</td>
          @for (quote of quotes(); track quote.id) {
            <td class="align-right">
              {{ quoteCoverage(quote) }}
              @if (quoteCoverage(quote) < 100) {
                <ep-badge tone="warning" [label]="quoteCoverage(quote) + '%'" />
              }
            </td>
          }
        </tr>

        <!-- Divider row -->
        <tr class="comparison-table__section-header">
          <td colspan="{{ quotes().length + 1 }}">
            {{ 'rfq.detail.compare.lineItemPrices' | translate }}
          </td>
        </tr>
        
        <!-- Per-line-item rows -->
        @for (rfqItem of rfqData.lineItems; track rfqItem.id) {
          <tr [class.comparison-table__row--item]="true">
            <td class="row-label item-name">
              <strong>{{ rfqItem.itemName }}</strong>
              <span class="text-muted">{{ rfqItem.categoryCode }}</span>
            </td>
            @for (quote of quotes(); track quote.id) {
              @let linePrice = getLinePrice(quote, rfqItem.id);
              <td class="align-right"
                  [class.comparison-table__cell--lowest]="isLowestForLine(quote.id, rfqItem.id)"
                  [class.comparison-table__cell--missing]="!linePrice">
                @if (linePrice) {
                  <ep-amount [value]="linePrice.unitPrice" />
                  @if (isLowestForLine(quote.id, rfqItem.id)) {
                    <ep-icon name="star" [size]="10" class="best-icon" />
                  }
                  @if (linePrice.deliveryDays) {
                    <small class="delivery-note">{{ linePrice.deliveryDays }}d</small>
                  }
                } @else {
                  <span class="text-muted" title="{{ 'rfq.detail.compare.notQuoted' | translate }}">—</span>
                }
              </td>
            }
          </tr>
        }
      </tbody>
    </table>
  </div>
}
```

---

### 3.4 Computed helpers

```typescript
readonly lowestTotalQuoteId = computed(() => {
  const qs = this.quotes();
  if (!qs.length) return null;
  return qs.reduce((min, q) =>
    parseFloat(q.totalAmount) < parseFloat(min.totalAmount) ? q : min
  ).id;
});

readonly bestScoreQuoteId = computed(() => {
  const qs = this.quotes().filter(q => q.evaluationScore !== null);
  if (!qs.length) return null;
  return qs.reduce((max, q) =>
    (q.evaluationScore ?? 0) > (max.evaluationScore ?? 0) ? q : max
  ).id;
});

readonly fastestDeliveryQuoteId = computed(() => {
  const qs = this.quotes().filter(q => this.avgDeliveryDays(q) > 0);
  if (!qs.length) return null;
  return qs.reduce((min, q) =>
    this.avgDeliveryDays(q) < this.avgDeliveryDays(min) ? q : min
  ).id;
});

// Tỷ lệ line items được quote (%)
quoteCoverage(quote: VendorQuote): number {
  const rfqLines = this.rfq()?.lineItems?.length ?? 0;
  if (!rfqLines) return 100;
  const quoted = quote.lineItems.length;
  return Math.round((quoted / rfqLines) * 100);
}

avgDeliveryDays(quote: VendorQuote): number {
  const days = quote.lineItems
    .map(l => l.deliveryDays)
    .filter(d => d !== null) as number[];
  if (!days.length) return 0;
  return Math.round(days.reduce((a, b) => a + b, 0) / days.length);
}

getLinePrice(quote: VendorQuote, rfqLineItemId: string): VendorQuoteLineItem | null {
  return quote.lineItems.find(l => l.rfqLineItemId === rfqLineItemId) ?? null;
}

isLowestForLine(quoteId: string, rfqLineItemId: string): boolean {
  const prices = this.quotes()
    .map(q => ({
      quoteId: q.id,
      price: parseFloat(q.lineItems.find(l => l.rfqLineItemId === rfqLineItemId)?.unitPrice ?? 'Infinity')
    }))
    .filter(p => isFinite(p.price));
  
  if (!prices.length) return false;
  const minPrice = Math.min(...prices.map(p => p.price));
  return prices.find(p => p.quoteId === quoteId)?.price === minPrice;
}
```

---

### 3.5 Evaluate Modal — tách ra khỏi card

**Thay vì** evaluate form xuất hiện inline trong card/bảng → dùng `ep-modal`:

```html
<ep-modal
  [open]="showEvaluateModal()"
  [title]="'rfq.detail.modal.evaluateTitle' | translate: { vendor: evaluatingQuote()?.vendorName }"
  (close)="closeEvaluateModal()">
  <div class="evaluate-form">
    <!-- Score với visual rating -->
    <ep-form-field [label]="'rfq.detail.eval.score' | translate">
      <div class="score-input-row">
        <input type="range" min="0" max="100" [(ngModel)]="evalScore" class="score-range">
        <input type="number" min="0" max="100" [(ngModel)]="evalScore" class="score-number">
        <span>/ 100</span>
      </div>
      <!-- Visual breakdown optional -->
    </ep-form-field>
    
    <ep-form-field [label]="'rfq.detail.eval.note' | translate">
      <textarea [(ngModel)]="evalNote" rows="3" class="ep-textarea"></textarea>
    </ep-form-field>
    
    <div class="modal-actions">
      <ep-button variant="ghost" (click)="closeEvaluateModal()">{{ 'action.cancel' | translate }}</ep-button>
      <ep-button variant="primary" [loading]="isActioning()" (click)="saveEvaluationFromModal()">
        {{ 'rfq.detail.action.saveEvaluation' | translate }}
      </ep-button>
    </div>
  </div>
</ep-modal>
```

**Xóa:** `selectedQuoteId` signal và inline evaluate form trong card/bảng.

---

### 3.6 Quote status summary bar

Thêm phía trên quotes section, sau khi RFQ status = EVALUATING:

```html
@if (rfqData.status === 'EVALUATING') {
  <div class="eval-progress-bar">
    <span>{{ 'rfq.detail.eval.progress' | translate: { evaluated: evaluatedCount(), total: quotes().length } }}</span>
    <div class="progress-track">
      <div class="progress-fill" [style.width]="evalProgressPct()"></div>
    </div>
    @if (evaluatedCount() === quotes().length) {
      <ep-badge tone="success" labelKey="rfq.detail.eval.allDone" />
    }
  </div>
}
```

---

## 4. SCSS cần thêm

```scss
.comparison-table-wrapper {
  overflow-x: auto;  // horizontal scroll trên mobile
  -webkit-overflow-scrolling: touch;
}

.comparison-table {
  min-width: 600px;
  border-collapse: collapse;
  width: 100%;
  
  th, td {
    padding: var(--spacing-3);
    border-bottom: var(--border-subtle);
    font-size: var(--font-size-sm);
    vertical-align: top;
  }
  
  th { 
    background: var(--color-surface);
    font-weight: 700;
  }
}

// Vendor column với awarded highlight
.comparison-table__vendor-col--awarded {
  background: color-mix(in srgb, var(--color-success) 8%, transparent);
  
  th { border-top: 3px solid var(--color-success); }
}

.comparison-table__vendor-col--best-score {
  th { border-top: 3px solid var(--color-accent); }
}

// Cell lowest price
.comparison-table__cell--lowest {
  background: color-mix(in srgb, var(--color-success) 12%, transparent);
  font-weight: 700;
  color: var(--color-success);
}

// Cell not quoted
.comparison-table__cell--missing {
  background: color-mix(in srgb, var(--color-neutral) 8%, transparent);
  color: var(--color-text-muted);
}

// Summary rows (total, score)
.comparison-table__summary-row {
  td { 
    background: var(--color-surface-raised);
    font-weight: 700;
  }
}

// Section header (divider)
.comparison-table__section-header td {
  background: var(--color-neutral-subtle);
  color: var(--color-text-muted);
  font-size: var(--font-size-xs);
  font-weight: 700;
  letter-spacing: 0.08em;
  text-transform: uppercase;
  padding: var(--spacing-2) var(--spacing-3);
}

.best-icon {
  color: var(--color-success);
  margin-left: var(--spacing-1);
}

.score-value {
  font-family: var(--font-mono);
  font-size: var(--font-size-lg);
  
  span { font-size: var(--font-size-xs); color: var(--color-text-muted); }
}

.score-value--best { color: var(--color-accent); }

.delivery-note {
  display: block;
  color: var(--color-text-muted);
  font-size: var(--font-size-xs);
}
```

---

## 5. i18n keys cần thêm

```json
"rfq.detail.quotes.viewCards": "Xem dạng thẻ",
"rfq.detail.quotes.viewCompare": "So sánh",
"rfq.detail.quotes.bestScore": "Điểm cao nhất",
"rfq.detail.quotes.awarded": "Đã chọn",
"rfq.detail.compare.totalAmount": "Tổng giá báo",
"rfq.detail.compare.score": "Điểm đánh giá",
"rfq.detail.compare.validUntil": "Hiệu lực đến",
"rfq.detail.compare.paymentTerms": "Điều khoản TT",
"rfq.detail.compare.avgDeliveryDays": "Thời gian giao (TB)",
"rfq.detail.compare.coverage": "Độ phủ báo giá",
"rfq.detail.compare.lineItemPrices": "Giá từng mặt hàng",
"rfq.detail.compare.notQuoted": "Không báo giá mặt hàng này",
"rfq.detail.compare.days": "ngày",
"rfq.detail.eval.progress": "Đã đánh giá {evaluated}/{total} báo giá",
"rfq.detail.eval.allDone": "Đã đánh giá đủ",
"rfq.detail.modal.evaluateTitle": "Đánh giá báo giá — {vendor}",
"rfq.detail.action.saveEvaluation": "Lưu đánh giá"
```

---

## 6. Nguyên tắc triển khai

- Chỉ thêm comparison view — không xóa card grid hiện tại; 2 view cùng tồn tại, toggle được
- `quoteViewMode` là `signal<'cards' | 'compare'>` trong component — mặc định `compare` nếu `quotes().length >= 2`
- `isLowestForLine` computation: tính mỗi lần render — không cần cache vì dữ liệu tĩnh sau khi load
- Evaluate modal: dùng `ep-modal` component đã có — không tạo modal mới
- Coverage < 100%: hiển thị `ep-badge tone="warning"` với % trong comparison table, thêm tooltip "Nhà cung cấp không báo giá đủ {n} mặt hàng"
- Sticky first column (row header): `position: sticky; left: 0` trong CSS để khi scroll ngang vẫn thấy tên item
- Score range slider: native `<input type="range">` — không cần component mới
- `VendorQuoteLineItem` cần có `itemName` từ join với `rfqData.lineItems` — không gọi API thêm, dữ liệu đã có trong `rfq.lineItems` signal
