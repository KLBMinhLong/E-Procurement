# Kế hoạch UI — Shell Navigation & UX Gaps

> **Mục tiêu:** Chốt lại navigation sau Inventory, rồi cải thiện UX của Approval Inbox (grouping, SLA visual), PR Create form (catalog autocomplete inline), và GR Create form (quantity validation trên prefill hiện có). Budget nav/indicator chuyển sang plan Budget để đi cùng route/service thật.

---

## 0. Rà soát lại theo code hiện tại — 2026-06-12

Plan này là plan kế tiếp sau Inventory. Một số giả định ban đầu đã thay đổi sau các slice Inventory:

| Nhóm | Trạng thái thực tế | Điều chỉnh scope |
|---|---|---|
| 2a Shell nav | `/inventory/catalog` đã có trong sidebar bằng `nav.inventoryCatalog`; `/finance/budgets` route chưa tồn tại | Không thêm nav Budget một mình để tránh dead link. Budget nav sẽ đi cùng plan `UI_MODULE_FINANCE_BUDGET.md` khi tạo route. |
| 2b Approval Inbox | Inbox vẫn là table phẳng; đã có stat total/overdue/emergency và `EpSlaBarComponent` | Giữ scope grouping + SLA countdown, dùng icon đã đăng ký (`shield-alert`, `alarm-clock`, `inbox`) thay vì thêm icon mới nếu không cần. |
| 2c PR Create | Đã có Catalog Picker Modal và `CatalogService.searchItems()`; chưa có inline autocomplete; budget indicator chưa có service public trong FE | Làm inline autocomplete trước. Budget indicator chỉ làm sau khi có `BudgetService` public từ plan Budget; không gọi `/internal/budgets/check` từ Angular. |
| 2d GR Create | Đã prefill line items khi chọn PO từ dữ liệu `PurchaseOrderService.list()`; received default hiện là `0` | Không rebuild full flow. Chỉ polish: validate `received + rejected <= ordered`, hiển thị cảnh báo rõ, dùng `getById()` nếu list response thiếu lineItems. |

**Thứ tự đề xuất cho plan này:** 2b → 2c → 2d. Nhóm 2a chỉ còn là checklist/no-op cho catalog nav; Budget nav chuyển sang plan Budget.

---

## 1. Sidebar Navigation — thiếu mục

### 1.1 Hiện trạng

**File:** `layout/shell/shell.component.ts`

```typescript
readonly navItems = computed<NavigationItem[]>(() => [
  { icon: 'layout-dashboard',  labelKey: 'nav.dashboard',            route: '/dashboard' },
  { icon: 'shopping-cart',     labelKey: 'nav.purchaseRequest',      route: '/procurement',
    permissions: ['PR_VIEW_OWN', 'PR_VIEW_DEPARTMENT', 'PR_VIEW_ALL'] },
  { icon: 'inbox',             labelKey: 'nav.approvals',            route: '/approvals',
    permissions: ['PR_APPROVE_L1', ...] },
  { icon: 'building-2',        labelKey: 'nav.vendors',              route: '/vendors',
    permissions: ['VENDOR_VIEW'] },
  { icon: 'file-search',       labelKey: 'nav.rfq',                  route: '/vendors/rfq',
    permissions: ['RFQ_VIEW', 'RFQ_CREATE'] },
  { icon: 'package',           labelKey: 'nav.inventory',            route: '/inventory',
    permissions: ['GR_VIEW', 'GR_CREATE', 'GR_ISSUE_OUT', 'ADMIN_CATALOG_MANAGE'] },
  { icon: 'boxes',             labelKey: 'nav.inventoryCatalog',     route: '/inventory/catalog',
    permissions: ['GR_VIEW', 'ADMIN_CATALOG_MANAGE'] },
  { icon: 'receipt-text',      labelKey: 'nav.purchaseOrders',       route: '/finance/purchase-orders',
    permissions: ['PO_VIEW_OWN', 'PO_VIEW_ALL', 'PO_CREATE'] },
  { icon: 'file-check-2',      labelKey: 'nav.invoices',             route: '/finance/invoices',
    permissions: ['INVOICE_VIEW', 'INVOICE_CREATE', 'PAYMENT_CONFIRM'] },
  { icon: 'users',             labelKey: 'nav.adminUsers',           route: '/admin/users',
    permissions: ['ADMIN_USER_VIEW', 'ADMIN_USER_MANAGE'] },
  { icon: 'user-cog',          labelKey: 'nav.adminRoles',           route: '/admin/roles',
    permissions: ['ADMIN_ROLE_MANAGE'] },
  { icon: 'shield',            labelKey: 'nav.adminRbac',            route: '/admin/rbac',
    permissions: ['ADMIN_ROLE_MANAGE'] },
  { icon: 'network',           labelKey: 'nav.adminOrgChart',        route: '/admin/org-chart',
    permissions: ['ORG_VIEW', 'ADMIN_DEPARTMENT_MANAGE'] },
  { icon: 'file-code-2',       labelKey: 'nav.notificationTemplates', route: '/admin/notification-templates',
    permissions: ['SYSTEM_CONFIG'] },
  { icon: 'workflow',          labelKey: 'nav.approvalRules',        route: '/approvals/rules',
    permissions: ['ADMIN_APPROVAL_RULE'] }
].filter(item => this.permissionService.hasAnyPermission(item.permissions)));
```

### 1.2 Mục thiếu sau rà soát

| Route | Icon | labelKey | Permissions |
|---|---|---|---|
| `/finance/budgets` | `wallet-cards` | `nav.budgets` | `BUDGET_VIEW_OWN_DEPT`, `BUDGET_VIEW_ALL` |

`/inventory/catalog` không còn thiếu. Không đổi sang `nav.catalog` vì i18n hiện đang dùng `nav.inventoryCatalog` nhất quán với route breadcrumb `route.inventory.catalog`.

**Lưu ý quan trọng:** `/finance/budgets` chưa có route/component trong `finance.routes.ts`. Không thêm nav entry trỏ tới route chưa tồn tại trong plan này.

### 1.3 Thay đổi cần làm

**Không thực hiện ngay trong plan #2 nếu chưa tạo route Budget.**

Khi làm plan `UI_MODULE_FINANCE_BUDGET.md`, thêm entry sau `/finance/invoices` cùng lúc với routes `finance/budgets` và `finance/budgets/:id`:

```typescript
{ icon: 'wallet-cards', labelKey: 'nav.budgets', route: '/finance/budgets',
  permissions: ['BUDGET_VIEW_OWN_DEPT', 'BUDGET_VIEW_ALL'] },
```

**Thêm vào i18n:**
```json
"nav.budgets": "Ngân sách"
```

---

## 2. Approval Inbox — Grouping và SLA visual

### 2.1 Hiện trạng

**File:** `features/approvals/pages/approval-inbox/approval-inbox.component.html`

- Có 3 stat cards: total / overdue / emergency
- Có filter chips: priority, entity type, overdue-only toggle
- Table list với columns: number, title, priority badge, amount, requester, SLA bar, assignedAt

**Vấn đề:**
- Inbox phẳng — tất cả tasks cùng 1 list, kể cả emergency và bình thường
- Phải click "overdue only" để lọc — không thấy ngay
- Không có visual grouping theo urgency
- Không có count badge phân loại ngay trên header

### 2.2 Cải thiện 1 — Section grouping

**Thay list phẳng bằng 3 sections có thể collapse:**

```html
<!-- Section: EMERGENCY (luôn expanded, collapse được) -->
@if (emergencyTasks().length) {
  <div class="inbox-section inbox-section--emergency">
    <button class="inbox-section__header" (click)="toggleSection('emergency')">
      <ep-icon name="shield-alert" />
      <h2>{{ 'approvals.inbox.section.emergency' | translate }}</h2>
      <span class="count-badge count-badge--danger">{{ emergencyTasks().length }}</span>
      <ep-icon [name]="sectionOpen('emergency') ? 'chevron-up' : 'chevron-down'" />
    </button>
    @if (sectionOpen('emergency')) {
      <!-- Inline table với emergency tasks -->
      <table class="inbox-table inbox-table--compact">
        <!-- Rows -->
      </table>
    }
  </div>
}

<!-- Section: OVERDUE (expanded nếu có tasks) -->
@if (overdueTasks().length) {
  <div class="inbox-section inbox-section--overdue">
    <button class="inbox-section__header" (click)="toggleSection('overdue')">
      <ep-icon name="alarm-clock" />
      <h2>{{ 'approvals.inbox.section.overdue' | translate }}</h2>
      <span class="count-badge count-badge--warning">{{ overdueTasks().length }}</span>
      <ep-icon [name]="sectionOpen('overdue') ? 'chevron-up' : 'chevron-down'" />
    </button>
    @if (sectionOpen('overdue')) {
      <table class="inbox-table inbox-table--compact">...</table>
    }
  </div>
}

<!-- Section: NORMAL (expanded) -->
<div class="inbox-section">
  <button class="inbox-section__header" (click)="toggleSection('normal')">
    <ep-icon name="inbox" />
    <h2>{{ 'approvals.inbox.section.normal' | translate }}</h2>
    <span class="count-badge">{{ normalTasks().length }}</span>
    <ep-icon [name]="sectionOpen('normal') ? 'chevron-up' : 'chevron-down'" />
  </button>
  @if (sectionOpen('normal')) {
    <table class="inbox-table inbox-table--compact">...</table>
  }
</div>
```

**Computed signals mới:**
```typescript
readonly emergencyTasks = computed(() =>
  this.items().filter(t => t.priority === 'EMERGENCY')
);
readonly overdueTasks = computed(() =>
  this.items().filter(t => t.priority !== 'EMERGENCY' && t.sla.isOverdue)
);
readonly normalTasks = computed(() =>
  this.items().filter(t => t.priority !== 'EMERGENCY' && !t.sla.isOverdue)
);

readonly openSections = signal<Set<string>>(new Set(['emergency', 'overdue', 'normal']));

toggleSection(name: string): void {
  this.openSections.update(s => {
    const next = new Set(s);
    next.has(name) ? next.delete(name) : next.add(name);
    return next;
  });
}

sectionOpen(name: string): boolean {
  return this.openSections().has(name);
}
```

### 2.3 Cải thiện 2 — SLA countdown text

Thêm text countdown bên cạnh `ep-sla-bar`:
```html
<td class="sla-col">
  <ep-sla-bar [assignedAt]="task.assignedAt" [deadline]="task.sla.deadlineAt" />
  <span class="sla-countdown" [class.sla-countdown--overdue]="task.sla.isOverdue">
    @if (task.sla.isOverdue) {
      {{ 'approvals.inbox.sla.overdue' | translate: { hours: absHours(slaRemainingHours(task)) } }}
    } @else {
      {{ 'approvals.inbox.sla.remaining' | translate: { hours: slaRemainingHours(task) } }}
    }
  </span>
</td>
```

```typescript
slaRemainingHours(task: ApprovalTaskSummary): number {
  if (task.sla.remainingHours !== null && task.sla.remainingHours !== undefined) {
    return task.sla.remainingHours;
  }
  const deadline = new Date(task.sla.deadlineAt).getTime();
  return Math.round((deadline - Date.now()) / (1000 * 60 * 60));
}

absHours(value: number): number {
  return Math.abs(value);
}
```

### 2.4 i18n keys
```json
"approvals.inbox.section.emergency": "Khẩn cấp",
"approvals.inbox.section.overdue": "Quá hạn",
"approvals.inbox.section.normal": "Bình thường",
"approvals.inbox.sla.remaining": "Còn {{hours}}h",
"approvals.inbox.sla.overdue": "Trễ {{hours}}h"
```

---

## 3. PR Create Form — Catalog Autocomplete

### 3.1 Hiện trạng

**File:** `features/procurement/pages/pr-create/pr-create.component.html`

Hiện tại có **Catalog Picker Modal** khi click "Tìm trong Catalog":
```html
<ep-button type="button" icon="search" (click)="openCatalogPicker($index)">
  {{ 'pr.create.action.searchCatalog' | translate }}
</ep-button>

<!-- Modal tách riêng -->
<ep-modal [open]="showCatalogPicker()" [title]="'pr.create.catalog.title' | translate">
  <input type="text" [value]="catalogSearchQuery()" (input)="onCatalogSearch(...)">
  <ul class="catalog-results">
    @for (item of catalogItems(); track item.id) {
      <li (click)="selectCatalogItem(item)">{{ item.name }} | {{ item.itemCode }}</li>
    }
  </ul>
</ep-modal>
```

**Điểm tốt:** Có catalog picker modal, có search, có selectCatalogItem().

**Vấn đề:**
- Phải click button → mở modal → tìm kiếm → select — 3 bước
- Khi đang nhập itemName trực tiếp trong input field, không có gợi ý tự động
- Budget remaining không hiển thị real-time khi nhập giá — người dùng không biết mình có vượt ngân sách không

### 3.2 Cải thiện 1 — Inline autocomplete trên itemName field

**Thêm datalist hoặc custom dropdown:**
```html
<ep-form-field [label]="'pr.create.lineItem.itemName' | translate">
  <div class="autocomplete-wrapper">
    <input
      [id]="'line-item-name-' + $index"
      type="text"
      formControlName="itemName"
      class="ep-input"
      autocomplete="off"
      [placeholder]="'pr.create.lineItem.itemNamePlaceholder' | translate"
      (input)="onItemNameInput($index, $any($event.target).value)"
      (focus)="onItemNameFocus($index)"
      (blur)="onItemNameBlur($index)">
    
    @if (autocompleteOpen($index) && autocompleteItems($index).length) {
      <ul class="autocomplete-dropdown" role="listbox">
        @for (item of autocompleteItems($index); track item.id) {
          <li class="autocomplete-option"
              role="option"
              (mousedown)="selectAutocompleteItem($index, item)">
            <span class="autocomplete-option__name">{{ item.name }}</span>
            <span class="autocomplete-option__meta">{{ item.itemCode }} · {{ item.categoryCode }}</span>
            <ep-amount [value]="item.unitPrice.amount" />
          </li>
        }
      </ul>
    }
  </div>
</ep-form-field>
```

**State mới trong component:**
```typescript
// Map: lineIndex → autocomplete state
readonly autocompleteOpenMap = signal<Map<number, boolean>>(new Map());
readonly autocompleteItemsMap = signal<Map<number, CatalogItem[]>>(new Map());

autocompleteOpen(index: number): boolean {
  return this.autocompleteOpenMap().get(index) ?? false;
}

autocompleteItems(index: number): CatalogItem[] {
  return this.autocompleteItemsMap().get(index) ?? [];
}

onItemNameInput(index: number, value: string): void {
  if (value.length < 2) {
    this.autocompleteOpenMap.update(m => { m.set(index, false); return new Map(m); });
    return;
  }
  // Debounce 300ms
  this.catalogService.searchItems({ q: value, size: 8 })
    .pipe(takeUntilDestroyed(this.destroyRef))
    .subscribe(res => {
      this.autocompleteItemsMap.update(m => { m.set(index, res.data?.slice(0, 8) ?? []); return new Map(m); });
      this.autocompleteOpenMap.update(m => { m.set(index, true); return new Map(m); });
    });
}

selectAutocompleteItem(index: number, item: CatalogItem): void {
  const group = this.lineItems.at(index);
  group.patchValue({
    itemName: item.name,
    categoryCode: item.categoryCode,
    quantityUnit: item.unit,
    unitPriceAmount: item.unitPrice.amount,
    isFromCatalog: true
  });
  this.autocompleteOpenMap.update(m => { m.set(index, false); return new Map(m); });
}
```

**Debounce:** 300ms — tránh gọi API mỗi keystroke.

`glAccountCode` vẫn để người dùng nhập/chọn theo form hiện tại vì `CatalogItem` của PR service chưa trả GL account.

### 3.3 Deferred — Budget Remaining Indicator

**Thêm vào sidebar summary card (đã có):**
```html
<div class="summary-row">
  <span class="summary-row__label">{{ 'pr.create.summary.totalAmount' | translate }}</span>
  <ep-amount [value]="totalAmount()" currency="VND" />
</div>

<!-- Thêm: Budget indicator (load lazy khi component init nếu user có departmentId) -->
@if (departmentBudget()) {
  <div class="summary-row" [class.summary-row--warning]="isOverBudget()">
    <span class="summary-row__label">{{ 'pr.create.summary.budgetAvailable' | translate }}</span>
    <ep-amount [value]="departmentBudget()!.available" />
  </div>
  <div class="budget-indicator">
    <div class="budget-indicator__bar">
      <span [style.width]="budgetUsageAfterThisPr()"></span>
    </div>
    @if (isOverBudget()) {
      <span class="budget-indicator__warning">
        <ep-icon name="circle-alert" [size]="12" />
        {{ 'pr.create.summary.overBudgetWarning' | translate }}
      </span>
    }
  </div>
}
```

**State mới:**
```typescript
readonly departmentBudget = signal<BudgetCheckResult | null>(null);

readonly isOverBudget = computed(() => {
  const budget = this.departmentBudget();
  if (!budget) return false;
  return parseFloat(this.totalAmount()) > parseFloat(budget.available.amount ?? '0');
});

readonly budgetUsageAfterThisPr = computed(() => {
  const budget = this.departmentBudget();
  if (!budget) return '0%';
  const allocated = parseFloat(budget.allocated.amount ?? '0');
  if (allocated <= 0) return '0%';
  const usedAfter = parseFloat(budget.committed.amount ?? '0')
    + parseFloat(budget.spent.amount ?? '0')
    + parseFloat(this.totalAmount());
  return `${Math.min(100, (usedAfter / allocated * 100)).toFixed(0)}%`;
});
```

**Load:** Không gọi `/internal/budgets/check` từ Angular. Indicator này chỉ thực hiện sau khi plan Budget tạo `BudgetService` dùng public API `GET /budgets` hoặc `GET /budgets/{id}/dashboard` với permission `BUDGET_VIEW_OWN_DEPT | BUDGET_VIEW_ALL`.

**Lưu ý:** Budget check trong PR create chỉ là **indicator**, không chặn submit. Backend mới là source of truth khi submit.

### 3.4 i18n keys
```json
"pr.create.summary.budgetAvailable": "Ngân sách còn lại",
"pr.create.summary.overBudgetWarning": "Tổng tiền có thể vượt ngân sách",
"pr.create.autocomplete.hint": "Gõ ≥ 2 ký tự để tìm trong catalog"
```

---

## 4. GR Create Form — PO Line Item Prefill

### 4.1 Hiện trạng

**File:** `features/inventory/pages/gr-create/`

Hiện tại GR Create đã có PO dropdown và đã prefill line items từ `PurchaseOrderService.list({ page: 1, size: 100 })`. Khi chọn PO, form tạo rows gồm `poLineItemId`, item name, ordered quantity, unit, unit price, currency, received/rejected quantity, rejection reason, lot number.

**Vấn đề cần giải quyết:**

- Thiếu validation tổng `receivedQuantity + rejectedQuantity <= orderedQuantity`.
- `receivedQuantity` đang default `0`; giữ nguyên để tránh vô tình submit full receipt, trừ khi người dùng chủ động nhập.
- Nếu sau này `GET /purchase-orders` không trả `lineItems` đầy đủ, dùng fallback `PurchaseOrderService.getById(poId)`.

### 4.2 Thiết kế prefill flow

**Form GR Create — section line items:**
```html
<!-- Khi chọn PO: -->
@if (selectedPoId()) {
  @if (isLoadingPoLines()) {
    <ep-skeleton [rows]="3" />
  } @else if (poLineItems().length) {
    <div class="gr-line-items">
      <h3>{{ 'inventory.gr.create.lineItems' | translate }}</h3>
      <div class="gr-table-header">
        <span>{{ 'inventory.gr.create.col.item' | translate }}</span>
        <span>{{ 'inventory.gr.create.col.ordered' | translate }}</span>
        <span>{{ 'inventory.gr.create.col.received' | translate }}</span>
        <span>{{ 'inventory.gr.create.col.rejected' | translate }}</span>
        <span>{{ 'inventory.gr.create.col.rejectReason' | translate }}</span>
      </div>
      @for (line of grLineItems.controls; track $index) {
        <div [formGroupName]="$index" class="gr-line-row">
          <div class="gr-line-row__name">
            <strong>{{ poLineItems()[$index]?.itemName }}</strong>
            <span class="text-muted">{{ poLineItems()[$index]?.categoryCode }}</span>
          </div>
          <span class="ordered-qty mono">
            {{ poLineItems()[$index]?.quantity?.amount }} {{ poLineItems()[$index]?.quantity?.unit }}
          </span>
          <input type="number" formControlName="receivedQuantity" class="ep-input mono"
            min="0" [max]="poLineItems()[$index]?.quantity?.amount">
          <input type="number" formControlName="rejectedQuantity" class="ep-input mono" min="0">
          <input type="text" formControlName="rejectionReason" class="ep-input"
            [placeholder]="'inventory.gr.create.col.rejectReasonPlaceholder' | translate">
        </div>
      }
    </div>
  }
}
```

**State cần thêm/chỉnh nhẹ:**
```typescript
readonly isLoadingPoLines = signal(false);

onPoSelect(poId: string): void {
  const po = this.pos().find((candidate) => candidate.id === poId);
  if (po?.lineItems?.length) {
    this.prefillGrLines(po);
    return;
  }
  // Fallback: load detail only when list response does not carry lineItems.
}

private prefillGrLines(poLines: PoLineItem[]): void {
  // Clear existing formArray
  while (this.grLineItems.length) this.grLineItems.removeAt(0);
  
  // Add one control per PO line
  for (const line of poLines) {
    this.grLineItems.push(this.fb.group({
      poLineItemId: [line.id, Validators.required],
      receivedQuantity: [0, [Validators.required, Validators.min(0)]],
      rejectedQuantity: [0, [Validators.required, Validators.min(0)]],
      rejectionReason: [''],
      lotNumber: ['']
    }));
  }
}
```

**Validation:** `receivedQuantity + rejectedQuantity <= orderedQuantity` — chặn submit và hiển thị lỗi nếu nhận/từ chối nhiều hơn số đặt.

### 4.3 i18n keys
```json
"inventory.gr.create.col.ordered": "SL đặt",
"inventory.gr.create.col.received": "SL nhận",
"inventory.gr.create.col.rejected": "SL từ chối",
"inventory.gr.create.col.rejectReason": "Lý do từ chối",
"inventory.gr.create.col.rejectReasonPlaceholder": "Hàng hỏng, sai chủng loại...",
"inventory.gr.create.validation.overQuantity": "Số lượng nhận vượt quá số đặt hàng"
```

---

## 5. Tóm tắt thay đổi theo file

| File | Loại thay đổi | Mức độ |
|---|---|---|
| `layout/shell/shell.component.ts` | Không đổi trong plan này nếu chưa tạo Budget route; catalog nav đã có | No-op |
| `features/approvals/pages/approval-inbox/approval-inbox.component.html` | Thêm section grouping | Trung bình |
| `features/approvals/pages/approval-inbox/approval-inbox.component.ts` | Thêm 3 computed + toggleSection | Nhỏ |
| `features/procurement/pages/pr-create/pr-create.component.html` | Thêm autocomplete dropdown; giữ catalog modal hiện có | Trung bình |
| `features/procurement/pages/pr-create/pr-create.component.ts` | Thêm autocomplete state/debounce; budget indicator deferred | Trung bình |
| `features/inventory/pages/gr-create/*` | Polish prefill hiện có, thêm over-quantity validation | Nhỏ |
| `assets/i18n/vi.json` + `en.json` | Thêm ~20 keys mới | Nhỏ |

---

## 6. Nguyên tắc triển khai

- Autocomplete dropdown: dùng `mousedown` (không phải `click`) để không trigger `blur` trước khi chọn
- Autocomplete debounce: 300ms — implement bằng `Subject + debounceTime(300) + switchMap` hoặc đơn giản hơn bằng `setTimeout`
- Budget indicator trong PR create: chỉ informational — không chặn submit
- GR prefill: `formArray` đã re-build khi chọn PO mới; bổ sung validation tổng quantity trước khi submit
- Section grouping trong Approval Inbox: giữ nguyên filter chips hiện tại — grouping là additional layer trên top của filtered results
- Tất cả thay đổi backward-compatible: không xóa tính năng cũ, chỉ thêm/cải thiện
