# Kế hoạch UI Module Finance — Budget Management (E10)

> **Mục tiêu:** Xây dựng giao diện quản lý ngân sách đầy đủ cho Accountant/Manager/Director, đồng bộ với backend `finance-service` đã implement xong, đảm bảo Accountant và Manager có nơi theo dõi, phê duyệt vượt chi và điều chuyển ngân sách.

---

## 0. Rà soát lại theo code hiện tại — 2026-06-13

Plan này là plan kế tiếp sau `UI_MODULE_SHELL_NAV_UX.md`. Backend Finance Budget đã có public API, frontend chưa có route/service/page budget.

| Nhóm | Trạng thái thực tế | Điều chỉnh scope |
|---|---|---|
| API list/detail | OpenAPI và `BudgetController` khớp: `GET /budgets`, `GET /budgets/{id}/dashboard` | Service frontend nên đặt method `getDashboard(id)`, không gọi `GET /budgets/{id}` vì endpoint đó không tồn tại. |
| Budget status | Backend chỉ trả `PLANNING`, `SUBMITTED`, `APPROVED`, `ACTIVE`, `CLOSED` | UI tự tính `healthTone`: `EXCEEDED` nếu `available < 0`, `WARNING` nếu `availablePercent <= 20`, còn lại `HEALTHY`; không đưa `WARNING/EXCEEDED` vào `BudgetStatus`. |
| Override modal | Backend validate `overrideAmount > 0`, `currency` optional length 3, `overrideReason` min 50/max 1000 | Plan cũ ghi min 20 là sai. Sửa thành min 50. |
| Transfer modal | Backend validate `amount > 0`, `currency` optional length 3, `reason` min 20/max 1000; use case validate active/same year/currency/source available | Plan cũ ghi reason min 10 là sai. UI có thể prefilter target budget cùng fiscal year và `ACTIVE`, nhưng backend vẫn là source of truth. |
| Idempotency | `idempotencyInterceptor` đã tự thêm `Idempotency-Key` cho POST/PUT/PATCH nếu service chưa set | `BudgetService` có thể dùng `HttpClient` pattern hiện tại của `finance` và dựa vào interceptor; không cần truyền key thủ công trừ khi muốn retry ổn định trong cùng modal submit. |
| KPI list | API list không có aggregate toàn bộ ngoài `meta.totalElements`; data chỉ là page hiện tại | KPI strip ở slice đầu nên ghi rõ là tổng hợp trên page/filter hiện tại, trừ `totalElements`. Không giả lập global total allocated/available. |
| Department name | API chỉ trả `departmentId`, chưa có IAM department lookup trong frontend service hiện tại | Hiển thị `shortId(departmentId)` trước; department name enrichment deferred đến slice IAM lookup sau nếu cần. |
| Transaction timeline | API detail hiện chưa trả transaction history | Không build timeline thật trong plan này. Chỉ build dashboard/read model + Override/Transfer modals. |
| Icon/nav | `wallet-cards` đã được đăng ký trong `app.config.ts` | Có thể thêm nav `/finance/budgets` cùng route trong slice 3a, đặt giữa Purchase Orders và Invoices. |

**Thứ tự thực hiện đề xuất:** 3a model/service/routes/nav/i18n shell → 3b Budget List → 3c Budget Detail read-only → 3d Override/Transfer modals → 3e build verify.

**Trạng thái triển khai 2026-06-13:** ✅ Slice 3a hoàn thành ở mức build: đã có `budget.model.ts`, `budget.service.ts`, lazy routes `/finance/budgets`, `/finance/budgets/:id`, sidebar `nav.budgets`, i18n shell và placeholder component cho list/detail.

**Cập nhật 2026-06-13:** ✅ Slice 3b hoàn thành ở mức build: `BudgetListComponent` đã gọi `GET /budgets`, có KPI theo page/filter hiện tại, filter fiscal year/quarter/department/status/GL, sort whitelist phía UI, table amount/status/health/utilization/forecast và pagination. Chưa mở browser test theo yêu cầu dừng ở mức build.

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
| `PATCH` | `/api/v1/budgets/{id}/override-approval` | `BUDGET_OVERRIDE` | Phê duyệt chi vượt budget — body: `{ prId, overrideAmount, currency, overrideReason }`; `overrideReason` min 50; `Idempotency-Key` do interceptor tự thêm |
| `PATCH` | `/api/v1/budgets/{id}/transfer` | `BUDGET_TRANSFER_APPROVE` | Điều chuyển ngân sách — body: `{ targetBudgetId, amount, currency, reason }`; `reason` min 20; `Idempotency-Key` do interceptor tự thêm |

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

**Derived UI state không có trong API:**
```typescript
type BudgetHealthTone = 'HEALTHY' | 'WARNING' | 'EXCEEDED';

function budgetHealthTone(budget: BudgetDashboard): BudgetHealthTone {
  if (Number(budget.available) < 0) return 'EXCEEDED';
  if (Number(budget.availablePercent) <= 20) return 'WARNING';
  return 'HEALTHY';
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

**KPI strip (4 stat cards, tính từ page/filter hiện tại):**
- Tổng budget theo filter: dùng `meta.totalElements`
- Tổng allocated đang hiển thị: sum `allocated` trong page hiện tại
- Tổng available đang hiển thị: sum `available` trong page hiện tại
- Số budget WARNING/EXCEEDED đang hiển thị: tính từ `availablePercent` và `available`

Không hiển thị tổng allocated/available toàn hệ thống nếu chưa có aggregate API riêng.

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
| Status | ep-badge | `PLANNING/SUBMITTED/APPROVED/ACTIVE/CLOSED`; health tone warning/exceeded là badge phụ do UI tính |
| Forecast | date | forecastExhaustedAt — "Dự kiến hết ngày X" nếu có |

Khi click row → navigate `/finance/budgets/:id`.

**Phân trang:** offset pagination, 20 items/page; sort default `fiscalYear,desc`.

**Empty state:** icon `wallet-cards`, message khi chưa có budget nào hoặc filter không có kết quả

---

### 5.2 Budget Detail Page — `/finance/budgets/:id`

**Layout:** Page header + metric strip + main grid (2 cols) + actions section

**Page header:**
- Title: `{glAccountCode} — {departmentId short}`, subtitle: `Năm {fiscalYear} Q{quarter}` hoặc `Cả năm`
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
- Không hiển thị timestamps trong slice này vì `BudgetDashboardResponse` chưa trả `createdAt/updatedAt`.

**Override Approval Modal** (khi click `[Override Approval]`):
```
Fields:
  - prId (text, required): UUID của PR cần vượt ngân sách
  - overrideAmount (number, required): Số tiền phê duyệt vượt
  - currency (text, default VND)
  - overrideReason (textarea, required, minLength 50, maxLength 1000)
  
Validation:
  - overrideAmount > 0
  - overrideReason ≥ 50 chars

Submit: PATCH `/budgets/{id}/override-approval`; `Idempotency-Key` được interceptor tự thêm nếu service không set explicit
Sau submit: reload budget detail + toast success
```

**Transfer Budget Modal** (khi click `[Transfer Budget]`):
```
Fields:
  - targetBudgetId (text/select, required): UUID budget đích — dropdown từ `GET /budgets` filter cùng fiscal year và `status=ACTIVE`, exclude current budget; fallback nhập UUID nếu list không đủ
  - amount (number, required): Số tiền điều chuyển ≤ available
  - currency (text, default VND)
  - reason (textarea, required, minLength 20, maxLength 1000)
  
Validation:
  - amount > 0 và ≤ available hiện tại
  - targetBudgetId ≠ id hiện tại

Submit: PATCH `/budgets/{id}/transfer`; `Idempotency-Key` được interceptor tự thêm nếu service không set explicit
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
  sort?: string;
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

export type BudgetHealthTone = 'HEALTHY' | 'WARNING' | 'EXCEEDED';
```

---

## 7. Service cần tạo

**File: `features/finance/services/budget.service.ts`**
```typescript
@Injectable({ providedIn: 'root' })
export class BudgetService {
  list(filter: BudgetListFilter): Observable<ApiResponse<BudgetDashboard[]> & { meta: PageMeta }>
  getDashboard(id: string): Observable<ApiResponse<BudgetDashboard>>
  override(id: string, request: BudgetOverrideRequest): Observable<ApiResponse<BudgetOverrideResult>>
  transfer(id: string, request: BudgetTransferRequest): Observable<ApiResponse<BudgetTransferResult>>
}
```

**Implementation note:** dùng `HttpClient` + `API_BASE_URL` giống `PurchaseOrderService`/`InvoiceService`. `credentialsInterceptor` thêm `withCredentials` và `idempotencyInterceptor` thêm `Idempotency-Key`; vẫn có thể set header explicit nếu cần một key ổn định cho retry trong cùng submit.

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
"finance.budget.detail.burnRate": "Tốc độ tiêu: {{rate}}/tháng",
"finance.budget.detail.forecastExhausted": "Dự kiến hết ngân sách: {{date}}",
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
- Mọi POST/PATCH phải có `Idempotency-Key`; hiện frontend đã có `idempotencyInterceptor` tự thêm nếu service không set explicit
- `withCredentials: true` đã được `credentialsInterceptor` thêm; service vẫn có thể khai báo explicit để giữ pattern finance hiện tại
- Budget amount: hiển thị qua `ep-amount` component — không format thủ công
- Utilization bar: CSS custom property `var(--color-success)` → `var(--color-warning)` → `var(--color-danger)` theo threshold 80% / 95%
- Không hardcode màu — dùng CSS variable
- `available < 0` → màu `var(--color-danger)`, `availablePercent` 0–20% → `var(--color-warning)`, > 20% → `var(--color-success)`
- Permission `BUDGET_OVERRIDE` và `BUDGET_TRANSFER_APPROVE` check bằng `*epHasPermission` directive trên action buttons
- Không dùng màu/palette từ tool design bên ngoài; ưu tiên design system hiện tại của dự án: Enterprise Dark Command Center, CSS tokens, Lucide icons, layout dense/scannable.
