# Kế hoạch UI Module Finance — Budget Management (E10)

> **Mục tiêu:** Xây dựng giao diện quản lý ngân sách đầy đủ cho Accountant/Manager/Director, đồng bộ với backend `finance-service` đã implement xong, đảm bảo Accountant và Manager có nơi theo dõi, phê duyệt vượt chi và điều chuyển ngân sách.

---

## 1. Hiện trạng

### 1.1 Những gì đã có

| Thành phần | Vị trí | Mô tả |
|---|---|---|
| Budget widget trong PR detail | `features/procurement/pages/pr-detail/pr-detail.component.html` | Sidebar card hiển thị allocated/committed/spent/available + utilization bar + warning message khi submit PR |
| Budget info trong Dashboard manager tab | `features/dashboard/dashboard.component.html` | Panel `dashboard.manager.budget` với 4 chỉ số và 1 thanh meter đơn giản |
| Budget info trong Dashboard requester tab | `features/dashboard/dashboard.component.html` | Panel `dashboard.requester.departmentBudget` với available + availablePct |
| `BudgetCheckResult` model | `features/procurement/models/purchase-request.model.ts` | Type dùng để render widget trong PR detail |

### 1.2 Những gì CHƯA có

- **Không có route `/finance/budgets`** — không có route nào trong `finance.routes.ts` liên quan đến budget
- **Không có `BudgetService`** — không có HTTP service nào gọi `GET /budgets` hay `GET /budgets/{id}/dashboard`
- **Không có Budget model riêng** — chỉ có `BudgetCheckResult` dùng cho PR submit check, không phải `BudgetDashboard`
- **Không có mục nav "Ngân sách" trong sidebar** — `navItems` trong `shell.component.ts` không có entry budget
- **Không có màn hình list budgets** — Accountant/Manager không có nơi xem toàn bộ danh sách ngân sách phòng ban
- **Không có màn hình budget detail/dashboard** — không có nơi xem burnRate, forecastExhaustedAt, lịch sử giao dịch
- **Không có Override UI** — `PATCH /budgets/{id}/override-approval` chưa có UI
- **Không có Transfer UI** — `PATCH /budgets/{id}/transfer` chưa có UI

---

## 2. Vấn đề hiện tại

1. **Accountant và Manager bị mù về ngân sách** — Hai vai trò quan trọng nhất trong kiểm soát ngân sách không có màn hình nào để theo dõi. Họ chỉ thấy số liệu ngân sách khi vào PR detail của từng yêu cầu cụ thể.

2. **Không có cảnh báo sớm** — Backend đã publish `finance.budget.warning` khi ngân sách < 20%, nhưng ngoài notification, không có UI chủ động nào để Accountant review danh sách các budget đang warning/exceeded.

3. **Override và Transfer không có UI** — Khi PR cần vượt ngân sách, Accountant phải approve qua API trực tiếp. Không có UI workflow nào.

4. **Số liệu dashboard bị thiếu context** — Panel budget trong Manager dashboard chỉ là một widget nhỏ, không drill-down được vào chi tiết theo phòng ban hay GL account code.

---

## 3. Backend API đã sẵn sàng

| Method | Endpoint | Permission | Mô tả |
|---|---|---|---|
| `GET` | `/api/v1/budgets` | `BUDGET_VIEW_OWN_DEPT` hoặc `BUDGET_VIEW_ALL` | Danh sách ngân sách, filter theo `department_id`, `fiscal_year`, `quarter`, `status`, `gl_account_code` |
| `GET` | `/api/v1/budgets/{id}/dashboard` | `BUDGET_VIEW_OWN_DEPT` hoặc `BUDGET_VIEW_ALL` | Chi tiết 1 budget: allocated, committed, spent, available, availablePercent, burnRatePerMonth, forecastExhaustedAt, status |
| `PATCH` | `/api/v1/budgets/{id}/override-approval` | `BUDGET_OVERRIDE` | Phê duyệt chi vượt budget — body: `{ prId, overrideAmount, currency, overrideReason }` + Idempotency-Key |
| `PATCH` | `/api/v1/budgets/{id}/transfer` | `BUDGET_TRANSFER_APPROVE` | Điều chuyển ngân sách — body: `{ targetBudgetId, amount, currency, reason }` + Idempotency-Key |

**Response model `BudgetDashboard`:**
```typescript
interface BudgetDashboard {
  id: string;                        // UUID
  departmentId: string;              // UUID
  fiscalYear: number;
  quarter: number | null;
  glAccountCode: string;
  allocated: string;                 // NUMERIC string, VD "500000000.0000"
  committed: string;
  spent: string;
  available: string;
  availablePercent: number;          // 0-100 float
  burnRatePerMonth: string | null;
  forecastExhaustedAt: string | null; // date ISO
  status: 'PLANNING' | 'SUBMITTED' | 'APPROVED' | 'ACTIVE' | 'CLOSED';
}
```

---

## 4. Information Architecture đề xuất

| Route | Permission | Mô tả |
|---|---|---|
| `/finance/budgets` | `BUDGET_VIEW_OWN_DEPT` hoặc `BUDGET_VIEW_ALL` | Danh sách budgets + filter + summary KPI |
| `/finance/budgets/:id` | `BUDGET_VIEW_OWN_DEPT` hoặc `BUDGET_VIEW_ALL` | Budget detail dashboard — drill-down |

Sidebar nav cần thêm (trong `shell.component.ts`):
```typescript
{
  icon: 'wallet-cards',
  labelKey: 'nav.budgets',
  route: '/finance/budgets',
  permissions: ['BUDGET_VIEW_OWN_DEPT', 'BUDGET_VIEW_ALL']
}
```

---

## 5. Thiết kế màn hình chi tiết

### 5.1 Budget List Page — `/finance/budgets`

**Layout:** Page header + KPI strip + filter bar + table list

**KPI strip (4 stat cards):**
- Tổng budget active trong fiscal year hiện tại
- Tổng available (tổng hợp tất cả phòng ban)
- Số budget đang WARNING (available < 20%)
- Số budget đang EXCEEDED (available < 0)

**Filter bar:**
```
[fiscal_year: number] [quarter: select 1-4|All] [department_id: text] [status: select] [gl_account_code: text] [Lọc]
```

**Table columns:**
| Cột | Kiểu | Ghi chú |
|---|---|---|
| Phòng ban | text | departmentId — hiển thị name nếu có từ IAM, fallback UUID short |
| GL Account | mono text | glAccountCode |
| Năm / Quý | text | `2025 / Q2` hoặc `2025 / Cả năm` |
| Allocated | ep-amount | |
| Committed | ep-amount | Tiền đang lock (PR pending approval) |
| Spent | ep-amount | Tiền đã thực chi (invoice paid) |
| Available | ep-amount | Màu đỏ nếu < 0, amber nếu < 20% |
| Utilization | progress bar inline | `(committed + spent) / allocated * 100` |
| Status | ep-badge | ACTIVE=success, CLOSED=neutral, PLANNING=info, WARNING=warning |
| Forecast | date | forecastExhaustedAt — "Dự kiến hết ngày X" nếu có |

Khi click row → navigate `/finance/budgets/:id`

**Phân trang:** offset pagination, 20 items/page

**Empty state:** icon `wallet-cards`, message khi chưa có budget nào hoặc filter không có kết quả

---

### 5.2 Budget Detail Page — `/finance/budgets/:id`

**Layout:** Page header + metric strip + main grid (2 cols) + actions section

**Page header:**
- Title: `{glAccountCode} — {departmentId short}`, subtitle: `Năm {fiscalYear} Q{quarter}`
- Badge status
- Action buttons (nếu có permission):
  - `[Override Approval]` — hiện khi có `BUDGET_OVERRIDE`, chỉ enabled khi status = ACTIVE
  - `[Transfer Budget]` — hiện khi có `BUDGET_TRANSFER_APPROVE`, chỉ enabled khi status = ACTIVE

**Metric strip (4 cards):**
```
[Allocated | {amount}]  [Committed | {amount}]  [Spent | {amount}]  [Available | {amount} | màu theo tình trạng]
```

**Main grid — 2 columns:**

**Cột trái — Utilization panel:**
- Thanh utilization dày (2rem height), màu gradient: xanh → amber → đỏ theo %
- Label: `{availablePercent}% còn lại`
- Breakdown bar segments: committed (amber) + spent (red) + available (green) xếp ngang
- Burn rate: `Tiêu {burnRatePerMonth}/tháng`
- Forecast: `Dự kiến hết ngân sách: {forecastExhaustedAt}` — badge danger nếu trong vòng 30 ngày
- Waterfall mini: 3 cột dọc (Allocated → minus Committed → minus Spent → = Available) dạng CSS

**Cột phải — Info panel:**
- Department ID (UUID)
- GL Account Code
- Fiscal Year / Quarter
- Status badge
- Tất cả timestamps: createdAt (nếu API trả về)

**Override Approval Modal** (khi click `[Override Approval]`):
```
Fields:
  - prId (text, required): UUID của PR cần vượt ngân sách
  - overrideAmount (number, required): Số tiền phê duyệt vượt
  - currency (text, default VND)
  - overrideReason (textarea, required, minLength 20)
  
Validation:
  - overrideAmount > 0
  - overrideReason ≥ 20 chars

Submit: PATCH /budgets/{id}/override-approval + Idempotency-Key header
Sau submit: reload budget detail + toast success
```

**Transfer Budget Modal** (khi click `[Transfer Budget]`):
```
Fields:
  - targetBudgetId (text/select, required): UUID budget đích — ideally là dropdown từ GET /budgets filter cùng fiscal year
  - amount (number, required): Số tiền điều chuyển ≤ available
  - currency (text, default VND)
  - reason (textarea, required, minLength 10)
  
Validation:
  - amount > 0 và ≤ available hiện tại
  - targetBudgetId ≠ id hiện tại

Submit: PATCH /budgets/{id}/transfer + Idempotency-Key header
Sau submit: reload budget detail + toast success với sourceDashboard/targetDashboard mới
```

---

## 6. Models cần tạo

**File: `features/finance/models/budget.model.ts`**
```typescript
export type BudgetStatus = 'PLANNING' | 'SUBMITTED' | 'APPROVED' | 'ACTIVE' | 'CLOSED';

export interface BudgetDashboard {
  id: string;
  departmentId: string;
  fiscalYear: number;
  quarter: number | null;
  glAccountCode: string;
  allocated: string;
  committed: string;
  spent: string;
  available: string;
  availablePercent: number;
  burnRatePerMonth: string | null;
  forecastExhaustedAt: string | null;
  status: BudgetStatus;
}

export interface BudgetListFilter {
  page: number;
  size: number;
  department_id?: string;
  fiscal_year?: number;
  quarter?: number;
  status?: BudgetStatus;
  gl_account_code?: string;
}

export interface BudgetOverrideRequest {
  prId: string;
  overrideAmount: string;
  currency: string;
  overrideReason: string;
}

export interface BudgetTransferRequest {
  targetBudgetId: string;
  amount: string;
  currency: string;
  reason: string;
}

export interface BudgetOverrideResult {
  id: string;
  budgetId: string;
  prId: string;
  overrideAmount: string;
  currency: string;
  overrideReason: string;
  approvedBy: string;
  approvedAt: string;
  status: 'APPROVED';
}

export interface BudgetTransferResult {
  id: string;
  sourceBudgetId: string;
  targetBudgetId: string;
  amount: string;
  currency: string;
  reason: string;
  approvedBy: string;
  approvedAt: string;
  sourceDashboard: BudgetDashboard;
  targetDashboard: BudgetDashboard;
}
```

---

## 7. Service cần tạo

**File: `features/finance/services/budget.service.ts`**
```typescript
@Injectable({ providedIn: 'root' })
export class BudgetService {
  list(filter: BudgetListFilter): Observable<ApiResponse<BudgetDashboard[]> & { meta: PageMeta }>
  getById(id: string): Observable<ApiResponse<BudgetDashboard>>
  override(id: string, request: BudgetOverrideRequest): Observable<ApiResponse<BudgetOverrideResult>>
  transfer(id: string, request: BudgetTransferRequest): Observable<ApiResponse<BudgetTransferResult>>
}
```

---

## 8. Routes cần thêm

**Thêm vào `features/finance/finance.routes.ts`:**
```typescript
{
  path: 'budgets',
  loadComponent: () => import('./pages/budget-list/budget-list.component').then(m => m.BudgetListComponent),
  canActivate: [permissionGuard],
  data: { requiredPermissions: ['BUDGET_VIEW_OWN_DEPT', 'BUDGET_VIEW_ALL'] },
  title: 'route.finance.budgets'
},
{
  path: 'budgets/:id',
  loadComponent: () => import('./pages/budget-detail/budget-detail.component').then(m => m.BudgetDetailComponent),
  canActivate: [permissionGuard],
  data: { requiredPermissions: ['BUDGET_VIEW_OWN_DEPT', 'BUDGET_VIEW_ALL'] },
  title: 'route.finance.budgetDetail'
}
```

---

## 9. i18n keys cần thêm

**`vi.json` và `en.json`:**
```json
"nav.budgets": "Ngân sách",
"finance.budget.list.title": "Quản lý Ngân sách",
"finance.budget.list.eyebrow": "Finance",
"finance.budget.col.department": "Phòng ban",
"finance.budget.col.glAccount": "Mã GL",
"finance.budget.col.period": "Năm / Quý",
"finance.budget.col.allocated": "Phân bổ",
"finance.budget.col.committed": "Đã cam kết",
"finance.budget.col.spent": "Đã chi",
"finance.budget.col.available": "Còn lại",
"finance.budget.col.utilization": "Sử dụng",
"finance.budget.col.status": "Trạng thái",
"finance.budget.col.forecast": "Dự báo hết",
"finance.budget.status.PLANNING": "Đang lập",
"finance.budget.status.SUBMITTED": "Đã nộp",
"finance.budget.status.APPROVED": "Đã duyệt",
"finance.budget.status.ACTIVE": "Đang hoạt động",
"finance.budget.status.CLOSED": "Đã đóng",
"finance.budget.detail.title": "Chi tiết Ngân sách",
"finance.budget.detail.burnRate": "Tốc độ tiêu: {rate}/tháng",
"finance.budget.detail.forecastExhausted": "Dự kiến hết ngân sách: {date}",
"finance.budget.detail.forecastSafe": "Ngân sách đủ đến cuối kỳ",
"finance.budget.detail.action.override": "Duyệt vượt chi",
"finance.budget.detail.action.transfer": "Điều chuyển ngân sách",
"finance.budget.override.title": "Duyệt chi vượt ngân sách",
"finance.budget.override.field.prId": "PR ID cần vượt chi",
"finance.budget.override.field.amount": "Số tiền phê duyệt vượt (VND)",
"finance.budget.override.field.reason": "Lý do duyệt vượt",
"finance.budget.transfer.title": "Điều chuyển ngân sách",
"finance.budget.transfer.field.targetBudget": "Ngân sách đích",
"finance.budget.transfer.field.amount": "Số tiền điều chuyển (VND)",
"finance.budget.transfer.field.reason": "Lý do điều chuyển",
"finance.budget.kpi.totalAllocated": "Tổng phân bổ",
"finance.budget.kpi.totalAvailable": "Tổng còn lại",
"finance.budget.kpi.warningCount": "Ngân sách cảnh báo",
"finance.budget.kpi.exceededCount": "Ngân sách vượt chi"
```

---

## 10. Nguyên tắc triển khai

- Tất cả component: `standalone: true`, `ChangeDetectionStrategy.OnPush`, state bằng `signal/computed`
- `takeUntilDestroyed(this.destroyRef)` cho mọi subscription
- Mọi POST/PATCH gửi `Idempotency-Key: crypto.randomUUID()` header
- `withCredentials: true` cho mọi request
- Budget amount: hiển thị qua `ep-amount` component — không format thủ công
- Utilization bar: CSS custom property `var(--color-success)` → `var(--color-warning)` → `var(--color-danger)` theo threshold 80% / 95%
- Không hardcode màu — dùng CSS variable
- `availablePercent` < 0 → màu `var(--color-danger)`, 0–20% → `var(--color-warning)`, > 20% → `var(--color-success)`
- Permission `BUDGET_OVERRIDE` và `BUDGET_TRANSFER_APPROVE` check bằng `*epHasPermission` directive trên action buttons
