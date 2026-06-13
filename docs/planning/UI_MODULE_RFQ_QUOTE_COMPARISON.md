# Kế hoạch UI — RFQ Quote Comparison Table

> **Mục tiêu:** Nâng cấp phần hiển thị quotes trong RFQ Detail từ dạng card grid thông tin đơn lẻ thành bảng so sánh cạnh nhau giữa các vendor, giúp Purchasing đưa ra quyết định award nhanh và có cơ sở rõ ràng.

**Rà soát 2026-06-13:** Plan này cần thực hiện thêm bước align contract trước UI. Backend/OpenAPI hiện dùng RFQ status `DRAFT`, `PUBLISHED`, `CLOSED`, `AWARDED`, `CANCELLED`, trong khi frontend đang dùng `OPEN`, `EVALUATING`. Khi triển khai phải sửa model/list/detail conditions theo backend trước, nếu không action nộp báo giá/đóng/đánh giá/chọn thầu sẽ không hiện đúng.

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
  rfqLineItemId: string;
  itemName: string;
  unitPrice: string;
  currency: string;
  quantity: string;
  totalPrice: string;
  deliveryDays: number | null;
  warranty: string | null;
}
```

**Ghi chú sau rà soát:** Model frontend hiện đã có `itemName`, `currency`, `quantity`, `totalPrice`, `warranty`. Không cần gọi API thêm để lấy tên hàng. Vẫn nên join với `rfqData.lineItems` bằng `rfqLineItemId` để giữ thứ tự/baseline RFQ và hiển thị `categoryCode`, `specifications`, `quantity/unit` gốc.

### 1.3 Contract drift cần sửa trước UI

**Backend truth:** `services/vendor-service/src/main/java/.../RfqStatus.java` và `docs/api/vendor-service.openapi.yaml`

```typescript
type RfqStatus = 'DRAFT' | 'PUBLISHED' | 'CLOSED' | 'AWARDED' | 'CANCELLED';
```

**Frontend hiện tại:** `vendor.model.ts`, `rfq-list.component.ts`, `rfq-detail.component.ts`

```typescript
type RfqStatus = 'OPEN' | 'EVALUATING' | 'AWARDED' | 'CLOSED' | 'CANCELLED';
```

**Cần chỉnh trong slice đầu tiên:**
- `RfqStatus` frontend đổi sang enum backend thật.
- `STATUS_TONE`, filter list, counters và i18n list stat đổi `OPEN -> PUBLISHED`, bỏ `EVALUATING`.
- RFQ Detail: submit quote/close chỉ dựa trên `PUBLISHED`; evaluate/award UI dựa trên `CLOSED` và quote đã có `evaluationScore`.
- Không sửa backend/OpenAPI cho status trong plan này vì backend đang nhất quán với migration/test.

---

## 2. Vấn đề hiện tại

| # | Vấn đề | Ảnh hưởng |
|---|---|---|
| 0 | Frontend RFQ status đang lệch backend (`OPEN/EVALUATING` vs `PUBLISHED/CLOSED`) | Nút submit/close/evaluate/award có thể không hiện hoặc filter status lỗi |
| 1 | Card grid mỗi vendor 1 card — không so sánh ngang được | Purchasing phải nhìn qua lại từng card, dễ nhầm |
| 2 | Không biết vendor nào có giá thấp nhất cho từng line item | Không có highlight để quét nhanh |
| 3 | `totalAmount` chỉ là tổng cộng — chưa đối chiếu từng `totalPrice` theo RFQ line | Nếu vendor chỉ quote một phần items thì cần thấy rõ coverage |
| 4 | Không có cột "Delivery Days" trong view chính | Thông tin giao hàng quan trọng nhưng ẩn trong card |
| 5 | Evaluate form xuất hiện inline trong card — layout bị vỡ khi form mở rộng | UX kém khi có nhiều quotes |
| 6 | Không có tóm tắt tiến độ đánh giá | Không biết còn bao nhiêu quote chưa chấm điểm trước khi award |
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

**Default:** `compare` khi có ≥ 2 quotes, `cards` khi chỉ có 1 quote. Nếu người dùng đã đổi toggle trong phiên hiện tại thì không tự reset khi reload RFQ.

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
              <ep-amount [value]="quoteAmount(quote)" />
              @if (quote.id === lowestTotalQuoteId()) {
                <ep-icon name="trending-down" [size]="12" class="best-icon" [attr.title]="'rfq.detail.compare.lowestPrice' | translate" />
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
              @if (quoteCoverage(quote) < 100) {
                <span class="coverage-pill coverage-pill--warning">{{ quoteCoverageLabel(quote) }}</span>
              } @else {
                <span class="mono">{{ quoteCoverageLabel(quote) }}</span>
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
                  <ep-amount [value]="quoteLineUnitAmount(linePrice)" />
                  @if (isLowestForLine(quote.id, rfqItem.id)) {
                    <ep-icon name="star" [size]="10" class="best-icon" />
                  }
                  @if (linePrice.deliveryDays) {
                    <small class="delivery-note">{{ linePrice.deliveryDays }} {{ 'rfq.detail.compare.days' | translate }}</small>
                  }
                  @if (linePrice.warranty) {
                    <small class="delivery-note">{{ linePrice.warranty }}</small>
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

quoteAmount(quote: VendorQuote): Money {
  return { amount: quote.totalAmount, currency: quote.currency };
}

quoteLineUnitAmount(line: VendorQuoteLineItem): Money {
  return { amount: line.unitPrice, currency: line.currency };
}

quoteLineTotalAmount(line: VendorQuoteLineItem): Money {
  return { amount: line.totalPrice, currency: line.currency };
}

quoteCoverageLabel(quote: VendorQuote): string {
  return `${this.quoteCoverage(quote)}%`;
}

isLowestForLine(quoteId: string, rfqLineItemId: string): boolean {
  const prices = this.quotes()
    .map(q => ({
      quoteId: q.id,
      price: parseFloat(q.lineItems.find(l => l.rfqLineItemId === rfqLineItemId)?.unitPrice ?? 'Infinity')
    }))
    .filter(p => Number.isFinite(p.price));
  
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
**Thêm:** `evaluatingQuote = signal<VendorQuote | null>(null)` và `showEvaluateModal = computed(() => this.evaluatingQuote() !== null)`.

---

### 3.6 Quote status summary bar

Thêm phía trên quotes section khi RFQ đã có quote và đang ở trạng thái `PUBLISHED` hoặc `CLOSED`; action evaluate/award chính dùng `CLOSED` để khớp backend lifecycle:

```html
@if ((rfqData.status === 'PUBLISHED' || rfqData.status === 'CLOSED') && quotes().length) {
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
  background: var(--color-neutral-subtle);
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

.coverage-pill {
  display: inline-flex;
  align-items: center;
  padding: 0 var(--spacing-2);
  border: var(--border-subtle);
  border-radius: var(--radius-1);
  font-family: var(--font-mono);
  font-size: var(--font-size-xs);
  font-weight: 700;
}

.coverage-pill--warning {
  border-color: var(--color-warning-border);
  background: var(--color-warning-subtle);
  color: var(--color-warning);
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
"rfq.detail.compare.lowestPrice": "Giá thấp nhất",
"rfq.detail.compare.days": "ngày",
"rfq.detail.eval.progress": "Đã đánh giá {{evaluated}}/{{total}} báo giá",
"rfq.detail.eval.allDone": "Đã đánh giá đủ",
"rfq.detail.modal.evaluateTitle": "Đánh giá báo giá — {{vendor}}",
"rfq.detail.action.saveEvaluation": "Lưu đánh giá"
```

---

## 6. Nguyên tắc triển khai

- Bước đầu tiên bắt buộc: align RFQ status frontend với backend (`PUBLISHED/CLOSED`, không dùng `OPEN/EVALUATING` trong logic mới)
- Không cần backend/API mới cho comparison table; dữ liệu quote line đã có đủ trong `RfqDetail.quotes[].lineItems[]`
- Chỉ thêm comparison view — không xóa card grid hiện tại; 2 view cùng tồn tại, toggle được
- `quoteViewMode` là `signal<'cards' | 'compare'>` trong component — mặc định `compare` nếu `quotes().length >= 2`
- `isLowestForLine` nên dựa trên computed map (`lineLowestQuoteIds`) thay vì gọi reduce trực tiếp trong template nhiều lần
- Evaluate modal: dùng `ep-modal` component đã có — không tạo modal mới
- Coverage < 100%: hiển thị custom `.coverage-pill--warning` với `%`; không dùng `ep-badge [label]` vì component chỉ nhận `labelKey/status`
- Sticky first column (row header): `position: sticky; left: 0` trong CSS để khi scroll ngang vẫn thấy tên item
- Score range slider: native `<input type="range">` — không cần component mới
- `VendorQuoteLineItem` đã có `itemName`, `currency`, `quantity`, `totalPrice`, `warranty`; join với `rfqData.lineItems` chỉ để lấy baseline/thứ tự/category/specs
- `ep-amount` dùng object `{ amount, currency }` cho quote total và line item price
- `Idempotency-Key` đã được `idempotencyInterceptor` tự gắn cho POST/PATCH; service không cần tự sinh key trừ khi muốn key ổn định riêng

---

## 7. Kế hoạch triển khai đã chỉnh

| Bước | Nội dung | Ghi chú |
|---|---|---|
| 7a | Align RFQ frontend status với backend contract | `RfqStatus`, `STATUS_TONE`, list filters/counters, detail action conditions; dùng `PUBLISHED` cho open-submit/close, `CLOSED` cho evaluate/award |
| 7b | Thêm `quoteViewMode` + view toggle UI | Default `compare` khi có từ 2 quotes, giữ card grid hiện có |
| 7c | Build comparison table vendor columns x RFQ line rows | Sticky first column, horizontal scroll, amount object cho `ep-amount` |
| 7d | Computed helpers | `lowestTotalQuoteId`, `bestScoreQuoteId`, `fastestDeliveryQuoteId`, `quoteCoverage`, `lineLowestQuoteIds`, amount helpers |
| 7e | Tách evaluate inline form thành `ep-modal` | Thay `selectedQuoteId` bằng `evaluatingQuote`; giữ permission `RFQ_EVALUATE` |
| 7f | Quote evaluation progress + award affordance | Progress theo số quote đã chấm; award chỉ khi status `CLOSED` và quote có score |
| 7g | i18n + SCSS polish | Không hardcode text/màu; dùng token hiện có |
| 7h | Verify | `npm run build`; `git diff --check`; không mở browser nếu user không yêu cầu |
