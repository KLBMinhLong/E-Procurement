# Kế hoạch UI — Dashboard Charts & Visual Upgrade

> **Mục tiêu:** Nâng cấp toàn bộ phần visualizations trong Dashboard từ CSS div giả lập thành charts thực sự với trục, legend, tooltip và interaction, đồng thời cải thiện layout, quick links và navigation context.

---

## 1. Hiện trạng

### 1.1 Cách render chart hiện tại

**File:** `features/dashboard/dashboard.component.html` + `.scss`

**Tất cả "biểu đồ" hiện tại đều là CSS div:**

**Monthly Trend (Bar chart giả lập):**
```html
<div class="trend-grid">
  @for (month of dashboard.monthlyTrend; track month.month) {
    <div class="trend-bar">
      <div class="trend-bar__columns">
        <span class="trend-bar__spent" [style.height]="barWidth(month.spent, maxMonthlySpend())"></span>
        <span class="trend-bar__budget" [style.height]="barWidth(month.budget, maxMonthlySpend())"></span>
      </div>
      <strong>{{ month.month }}</strong>
      <small>{{ month.prCount }} PRs</small>
    </div>
  }
</div>
```
SCSS: `.trend-bar__spent { background: var(--color-accent); }` — chỉ là div với chiều cao dynamic

**Department Spend (Bar chart giả lập):**
```html
<div class="metric-list">
  @for (dept of dashboard.spendByDepartment; track dept.departmentCode) {
    <div class="metric-row">
      <div class="metric-row__label">...</div>
      <div class="metric-row__bar">
        <span [style.width]="barWidth(dept.spent, maxDepartmentSpend())"></span>
      </div>
      <ep-amount />
      <ep-badge />
    </div>
  }
</div>
```

**Category Spend (Bar chart giả lập):**
```html
<div class="compact-bars">
  @for (point of dashboard.spendByCategory; track point.label) {
    <div class="compact-bar">
      <span>{{ point.label }}</span>
      <div><i [style.width]="barWidth(point.value, maxCategorySpend())"></i></div>
      <strong>{{ point.value }}</strong>
    </div>
  }
</div>
```

**PO Pipeline (Stacked bar giả lập):**
```html
<div class="pipeline-stack">
  <span class="pipeline-stack__draft" [style.width]="barWidth(...)"></span>
  <span class="pipeline-stack__pending" [style.width]="barWidth(...)"></span>
  ...
</div>
```

**SLA Summary:** Chỉ là số + dl list — không có vòng tròn hay gauge.

### 1.2 Những gì THIẾU

1. **Không có trục số** — không biết giá trị thực của từng cột/bar
2. **Không có tooltip khi hover** — không thể đọc số liệu chính xác
3. **Không có legend** — monthly trend có 2 màu (spent vs budget) nhưng không có legend
4. **KPI cards dùng icon cố định** `chart-no-axes-combined` cho mọi loại metric
5. **Không có quick action links** — từ dashboard không có lối tắt sang workflow chính
6. **Reports tab ẩn** — không có trong sidebar nav, chỉ hiện khi vào `/dashboard` trực tiếp
7. **Dashboard filter departmentId** là text input UUID — không user-friendly, không ai nhớ UUID
8. **Manager budget panel** chỉ có 1 thanh, không drill-down vào trend theo tháng
9. **Không có realtime auto-refresh** — data stale sau 30 phút không refresh

---

## 2. Thư viện chart đề xuất

### 2.1 Lựa chọn

**Chart.js + `ng2-charts`** — phù hợp nhất cho dự án này vì:
- Bundle nhỏ, tree-shakeable (~60KB gzipped cho cả bộ)
- Tích hợp Angular tốt với `ng2-charts` (Angular wrapper chính thức)
- Hỗ trợ: bar, line, doughnut, pie, radar
- Responsive out of the box
- Không cần thêm D3 (quá nặng cho use case này)

**Cài đặt:**
```bash
npm install chart.js ng2-charts --save-exact
```

**Phiên bản khuyến nghị:** `chart.js@4.x`, `ng2-charts@6.x` (Angular 21 compatible)

**Import trong component:**
```typescript
import { BaseChartDirective } from 'ng2-charts';
import { Chart, registerables } from 'chart.js';
Chart.register(...registerables);
```

### 2.2 Alternative không cần thư viện ngoài

Nếu không muốn thêm dependency: dùng **SVG thuần** trong Angular template với computed coordinates. Phức tạp hơn nhưng zero-dependency. Chỉ phù hợp cho bar chart đơn giản.

**Khuyến nghị: Dùng Chart.js** vì hệ thống đã có độ phức tạp đủ cao để justify một chart library nhỏ.

---

## 3. Thiết kế chart cho từng section

### 3.1 Monthly Trend — Grouped Bar Chart

**Thay `<div class="trend-grid">` bằng:**
```html
<div class="chart-canvas-wrapper">
  <canvas baseChart
    [data]="monthlyTrendChartData()"
    [options]="monthlyTrendOptions"
    type="bar"
    aria-label="Biểu đồ chi tiêu theo tháng">
  </canvas>
</div>
```

**Chart data computed:**
```typescript
readonly monthlyTrendChartData = computed(() => {
  const trend = this.executiveDashboard()?.monthlyTrend ?? [];
  return {
    labels: trend.map(m => m.month),
    datasets: [
      {
        label: this.translate('dashboard.executive.spent'),
        data: trend.map(m => parseFloat(m.spent ?? '0')),
        backgroundColor: 'var(--color-accent)',
        borderRadius: 4
      },
      {
        label: this.translate('dashboard.executive.budget'),
        data: trend.map(m => parseFloat(m.budget ?? '0')),
        backgroundColor: 'rgba(var(--color-info-rgb), 0.5)',
        borderRadius: 4
      }
    ]
  };
});
```

**Chart options:**
```typescript
readonly monthlyTrendOptions: ChartOptions<'bar'> = {
  responsive: true,
  maintainAspectRatio: false,
  plugins: {
    legend: { position: 'bottom', labels: { color: 'var(--color-text-secondary)' } },
    tooltip: {
      callbacks: {
        label: (ctx) => `${ctx.dataset.label}: ${formatVND(ctx.parsed.y)}`
      }
    }
  },
  scales: {
    y: {
      ticks: {
        color: 'var(--color-text-muted)',
        callback: (value) => formatShortVND(Number(value))  // "45M", "1.2B"
      },
      grid: { color: 'rgba(var(--color-border-rgb), 0.3)' }
    },
    x: {
      ticks: { color: 'var(--color-text-muted)' },
      grid: { display: false }
    }
  }
};
```

---

### 3.2 Department Spend — Horizontal Bar Chart

**Giữ nguyên `metric-list` layout** (nó đẹp và compact) nhưng thêm value labels và improve visual:

```html
<div class="metric-list">
  @for (dept of dashboard.spendByDepartment; track dept.departmentCode) {
    <div class="metric-row">
      <div class="metric-row__label">
        <strong>{{ dept.departmentName }}</strong>
        <span>{{ dept.departmentCode }}</span>
      </div>
      <div class="metric-row__bar-container">
        <div class="metric-row__bar">
          <!-- Committed segment -->
          <span class="metric-row__bar-committed"
            [style.width]="barWidth(dept.committed ?? 0, maxDepartmentSpend())"
            [title]="'Committed: ' + formatAmount(dept.committed)">
          </span>
          <!-- Spent segment -->
          <span class="metric-row__bar-spent"
            [style.width]="barWidth(dept.spent, maxDepartmentSpend())"
            [title]="'Spent: ' + formatAmount(dept.spent)">
          </span>
        </div>
        <span class="metric-row__percent">{{ percent(dept.utilization) }}</span>
      </div>
      <ep-amount [value]="money(dept.spent)" />
      <ep-badge [tone]="statusTone(dept.status)" ... />
    </div>
  }
</div>
```

**Thêm 2 màu segment:** committed (amber) + spent (accent) thay vì 1 màu đơn — hiển thị rõ hơn phần đã cam kết vs đã tiêu thực sự.

---

### 3.3 Category Spend — Doughnut Chart (thay compact bars)

**Thay `<div class="compact-bars">` bằng doughnut chart nhỏ:**
```html
<div class="category-chart-wrapper">
  <canvas baseChart
    [data]="categorySpendChartData()"
    [options]="categoryChartOptions"
    type="doughnut"
    aria-label="Chi tiêu theo danh mục">
  </canvas>
</div>
```

**Chart data:**
```typescript
readonly categorySpendChartData = computed(() => {
  const cats = this.executiveDashboard()?.spendByCategory ?? [];
  const CATEGORY_COLORS = [
    'var(--color-accent)', 'var(--color-info)', 'var(--color-success)',
    'var(--color-warning)', 'var(--color-danger)', 'var(--color-text-muted)'
  ];
  return {
    labels: cats.map(c => c.label),
    datasets: [{
      data: cats.map(c => parseFloat(c.value ?? '0')),
      backgroundColor: cats.map((_, i) => CATEGORY_COLORS[i % CATEGORY_COLORS.length]),
      borderWidth: 2,
      borderColor: 'var(--color-surface)'
    }]
  };
});
```

---

### 3.4 SLA Compliance — Gauge/Arc (thay text chỉ số)

**Dùng doughnut chart bán vòng (half-doughnut):**
```typescript
const onTimePct = dashboard.approvalSla.onTimePercent;
{
  labels: ['Đúng hạn', 'Trễ hạn'],
  datasets: [{
    data: [onTimePct, 100 - onTimePct],
    backgroundColor: [
      onTimePct >= 85 ? 'var(--color-success)' : onTimePct >= 70 ? 'var(--color-warning)' : 'var(--color-danger)',
      'var(--color-neutral-subtle)'
    ],
    circumference: 180,
    rotation: -90
  }]
}
```

**Giữ lại dl list** bên dưới gauge cho các chỉ số avgCycleHours, overdueCount.

---

### 3.5 KPI Cards — icon khác nhau theo loại metric

**Thay `'chart-no-axes-combined'` dùng icon map:**
```typescript
// Trong analytics.model.ts, thêm iconName vào KpiCard
export interface KpiCard {
  label: string;
  value: string | number;
  unit?: string;
  status?: KpiStatus;
  trend?: { ... };
  icon?: string;  // ← thêm field này, backend populate hoặc FE map
}

// Fallback icon map theo label keyword
function resolveKpiIcon(label: string): string {
  if (label.includes('PR') || label.includes('request')) return 'shopping-cart';
  if (label.includes('budget') || label.includes('spend')) return 'wallet';
  if (label.includes('PO') || label.includes('order')) return 'receipt-text';
  if (label.includes('SLA') || label.includes('time')) return 'timer';
  if (label.includes('vendor')) return 'building-2';
  if (label.includes('invoice')) return 'file-check';
  return 'chart-no-axes-combined';  // fallback
}
```

---

## 4. Quick Action Links

### 4.1 Vị trí

Thêm sau KPI grid, trước dashboard panels — chỉ hiển thị những action user có permission:

```html
<div class="quick-actions" [attr.aria-label]="'dashboard.quickActions.label' | translate">
  @if (permissionService.hasPermission('PR_CREATE')) {
    <a routerLink="/procurement/create" class="quick-action-card">
      <ep-icon name="plus-circle" />
      <span>{{ 'dashboard.quick.createPr' | translate }}</span>
    </a>
  }
  @if (permissionService.hasAnyPermission(['PR_APPROVE_L1', 'PR_APPROVE_L2', 'PR_APPROVE_L3'])) {
    <a routerLink="/approvals" class="quick-action-card quick-action-card--urgent"
       [attr.data-badge]="counts()?.overdue > 0 ? counts().overdue : null">
      <ep-icon name="inbox" />
      <span>{{ 'dashboard.quick.approvalInbox' | translate }}</span>
    </a>
  }
  @if (permissionService.hasPermission('PO_CREATE')) {
    <a routerLink="/finance/purchase-orders/create" class="quick-action-card">
      <ep-icon name="file-plus-2" />
      <span>{{ 'dashboard.quick.createPo' | translate }}</span>
    </a>
  }
  @if (permissionService.hasAnyPermission(['BUDGET_VIEW_OWN_DEPT', 'BUDGET_VIEW_ALL'])) {
    <a routerLink="/finance/budgets" class="quick-action-card">
      <ep-icon name="wallet-cards" />
      <span>{{ 'dashboard.quick.viewBudgets' | translate }}</span>
    </a>
  }
  @if (permissionService.hasPermission('GR_CREATE')) {
    <a routerLink="/inventory/goods-receipts/create" class="quick-action-card">
      <ep-icon name="package-check" />
      <span>{{ 'dashboard.quick.createGr' | translate }}</span>
    </a>
  }
  @if (permissionService.hasPermission('INVOICE_CREATE')) {
    <a routerLink="/finance/invoices/create" class="quick-action-card">
      <ep-icon name="receipt-text" />
      <span>{{ 'dashboard.quick.createInvoice' | translate }}</span>
    </a>
  }
</div>
```

**SCSS:**
```scss
.quick-actions {
  display: flex;
  flex-wrap: wrap;
  gap: var(--spacing-3);
}

.quick-action-card {
  position: relative;
  display: flex;
  align-items: center;
  gap: var(--spacing-2);
  padding: var(--spacing-3) var(--spacing-4);
  border: var(--border-subtle);
  border-radius: var(--radius-3);
  background: var(--color-surface);
  color: var(--color-text-secondary);
  cursor: pointer;
  text-decoration: none;
  font-size: var(--font-size-sm);
  font-weight: 700;
  transition: background var(--motion-fast), color var(--motion-fast);
  
  &:hover {
    background: var(--color-accent-subtle);
    color: var(--color-accent);
    border-color: var(--color-warning-border);
  }
  
  // Overdue badge
  &[data-badge]::after {
    content: attr(data-badge);
    position: absolute;
    top: -6px;
    right: -6px;
    min-width: 18px;
    height: 18px;
    padding: 0 4px;
    border-radius: var(--radius-pill);
    background: var(--color-danger);
    color: white;
    font-size: 10px;
    font-weight: 800;
    display: flex;
    align-items: center;
    justify-content: center;
  }
}
```

---

## 5. Auto-refresh

**Thêm vào Dashboard component:**
```typescript
private readonly AUTO_REFRESH_INTERVAL_MS = 5 * 60 * 1000;  // 5 phút
private refreshTimer: ReturnType<typeof setInterval> | null = null;

ngOnInit(): void {
  // ... existing code
  this.startAutoRefresh();
}

private startAutoRefresh(): void {
  this.refreshTimer = setInterval(() => {
    this.loadDashboards();
    this.loadKpis();
  }, this.AUTO_REFRESH_INTERVAL_MS);
  this.destroyRef.onDestroy(() => {
    if (this.refreshTimer) clearInterval(this.refreshTimer);
  });
}
```

**Thêm "Last updated" indicator:**
```html
<span class="last-updated">
  {{ 'dashboard.lastUpdated' | translate: { time: formatTime(lastUpdatedAt()) } }}
</span>
```

---

## 6. Department filter — cải thiện UX

**Thay text input UUID bằng select dropdown** khi có thể (lazy load từ IAM):

```html
<ep-form-field [label]="'dashboard.filter.departmentId' | translate">
  <select id="dashboard-department-id" formControlName="departmentId">
    <option value="">{{ 'dashboard.filter.allDepartments' | translate }}</option>
    @for (dept of departments(); track dept.id) {
      <option [value]="dept.id">{{ dept.name }} ({{ dept.code }})</option>
    }
  </select>
</ep-form-field>
```

**Load departments từ IAM service** (`GET /api/v1/org/departments`) khi user có `REPORT_VIEW`:
```typescript
// Load một lần khi init
private loadDepartments(): void {
  if (!this.canViewExecutive()) return;
  this.iamService.getDepartments()
    .pipe(takeUntilDestroyed(this.destroyRef))
    .subscribe({ next: res => this.departments.set(res.data ?? []) });
}
```

---

## 7. Chart canvas wrapper SCSS

```scss
.chart-canvas-wrapper {
  position: relative;
  height: 14rem;  // fixed height để chart không collapse
  width: 100%;
}

// Monthly trend cần cao hơn
.chart-canvas-wrapper--tall {
  height: 18rem;
}

// Doughnut/gauge nhỏ hơn
.chart-canvas-wrapper--gauge {
  height: 10rem;
}
```

---

## 8. i18n keys cần thêm

```json
"dashboard.quick.createPr": "Tạo yêu cầu mua",
"dashboard.quick.approvalInbox": "Hộp thư duyệt",
"dashboard.quick.createPo": "Tạo đơn mua",
"dashboard.quick.viewBudgets": "Xem ngân sách",
"dashboard.quick.createGr": "Tạo phiếu nhận hàng",
"dashboard.quick.createInvoice": "Tạo hóa đơn",
"dashboard.quickActions.label": "Thao tác nhanh",
"dashboard.lastUpdated": "Cập nhật lúc {time}",
"dashboard.filter.allDepartments": "Tất cả phòng ban",
"dashboard.executive.spent": "Đã chi",
"dashboard.executive.budget": "Ngân sách"
```

---

## 9. Phụ thuộc và rủi ro

| Điểm | Loại | Mô tả | Giải pháp |
|---|---|---|---|
| Chart.js không tích hợp CSS variables | Technical | Chart.js dùng canvas — không đọc được `var(--color-accent)` trực tiếp | Resolve CSS variables bằng `getComputedStyle(document.documentElement).getPropertyValue('--color-accent')` khi init |
| CSS variable trong chart options | Technical | Phải resolve tại runtime, không reactive khi theme thay đổi | Một lần resolve khi component init là đủ (không có dark/light toggle) |
| `ng2-charts` bundle size | Bundle | +~60KB gzipped | Acceptable — nhỏ hơn nhiều so với Apache ECharts hay Victory |
| Angular SSR compatibility | Technical | Chart.js cần browser `canvas` | Dashboard không có SSR — không vấn đề |
| IAM departments API cross-service | API | GET /org/departments cần verify đã có trong IAM service | Kiểm tra `iam-service.openapi.yaml` — nếu chưa có, giữ text input |

---

## 10. Nguyên tắc triển khai

- Mỗi chart là computed signal → `ChartData` — tự cập nhật khi dashboard data thay đổi
- Chart options là `readonly` constant trong component (không reactive) — trừ labels cần translate
- Chart labels phải qua i18n: dùng `TranslateService.instant()` trong computed (không dùng `| translate` pipe trong canvas context)
- CSS variable resolve: 1 lần trong `ngOnInit`, lưu vào `readonly chartTheme = {}` object
- `baseChart` directive import trong component imports array (standalone)
- Không dùng `ngOnChanges` — dùng `signal/computed` với `effect()` nếu cần side effect chart update
- Tất cả `<canvas>` phải có `aria-label` cho accessibility
- Responsive: `maintainAspectRatio: false` + fixed height container trong SCSS
