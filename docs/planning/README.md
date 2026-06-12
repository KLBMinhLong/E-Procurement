# UI Planning — eProcure Frontend

> Thư mục này chứa các kế hoạch chi tiết cho từng module UI còn thiếu hoặc cần nâng cấp của eProcure Angular frontend.
> Mỗi file mô tả đầy đủ: hiện trạng code thực tế, vấn đề, thiết kế giải pháp, models, services, i18n keys và nguyên tắc triển khai.

---

## Trạng thái tổng quan

| # | File | Module | Trạng thái | Phụ thuộc |
|---|---|---|---|---|
| 1 | `UI_MODULE_INVENTORY.md` | Inventory Catalog UI (Slice 5–8) | 🔄 **Đang làm** | — |
| 2 | `UI_MODULE_SHELL_NAV_UX.md` | Shell Nav + UX Gaps | ⬜ Tiếp theo | Inventory catalog route cần có trước khi thêm nav entry |
| 3 | `UI_MODULE_FINANCE_BUDGET.md` | Finance Budget Management | ⬜ Chờ | Cần nav entry từ #2 |
| 4 | `UI_MODULE_APPROVAL_FLOW_DIAGRAM.md` | Approval Process Visualization | ⬜ Chờ | Không phụ thuộc — standalone |
| 5 | `UI_MODULE_PR_LIFECYCLE_TRACEABILITY.md` | PR Lifecycle & Traceability | ⬜ Chờ | Nên làm sau #4 (dùng chung ep-approval-steps) |
| 6 | `UI_MODULE_INVOICE_3WAY_MATCH.md` | Invoice 3-Way Match Visualization | ⬜ Chờ | Cần GoodsReceiptService đã ổn định từ #1 |
| 7 | `UI_MODULE_RFQ_QUOTE_COMPARISON.md` | RFQ Quote Comparison Table | ⬜ Chờ | Không phụ thuộc — standalone |
| 8 | `UI_MODULE_DASHBOARD_CHARTS.md` | Dashboard Charts & Visual Upgrade | ⬜ Chờ | Nên làm sau #3 (Budget nav cần có trước quick links) |

---

## Thứ tự thực hiện

### 🔄 Đang làm

#### 1. `UI_MODULE_INVENTORY.md` — Inventory Catalog UI
**Slice đang làm:** Slice 5 trở đi (Slice 0–4 đã hoàn thành theo progress-tracker).

| Slice | Nội dung | Backend | Status |
|---|---|---|---|
| Slice 0 | Contract cleanup, StockEntry model, StockService, routes, i18n | — | ✅ |
| Slice 1 | Stock Dashboard + Movements UI | — | ✅ |
| Slice 2 | Issue Out UI | — | ✅ |
| Slice 3 | Polish GR: rejected qty, lot number, complete summary | — | ✅ |
| Slice 4 | Catalog backend (Item domain/use cases/controller) | ✅ | ✅ |
| **Slice 5** | **Catalog UI: `/inventory/catalog` + `/inventory/catalog/:itemCode`** | ✅ | ✅ |
| Slice 6 | GR draft edit backend + UI (`PUT /goods-receipts/{id}`) | ✅ | ✅ |
| Slice 7 | Stock Adjustment backend + UI (`POST /stock/adjustment`) | ⬜ | ⬜ |
| Slice 8 | E2E / runtime hardening | ⬜ | ⬜ |

**Mục tiêu hoàn thành:** Warehouse user và Admin catalog manager có thể thao tác end-to-end không cần dùng DB trực tiếp.

---

### ⬜ Tiếp theo — theo thứ tự ưu tiên

---

#### 2. `UI_MODULE_SHELL_NAV_UX.md` — Shell Navigation & UX Gaps
**Làm ngay sau khi `/inventory/catalog` route tồn tại.**

Gồm 4 nhóm thay đổi nhỏ, độc lập, có thể làm theo thứ tự bất kỳ:

| Nhóm | Thay đổi | File chính |
|---|---|---|
| 2a | Thêm `nav.budgets` và `nav.catalog` vào sidebar | `shell.component.ts` |
| 2b | Approval Inbox: section grouping Emergency/Overdue/Normal + SLA countdown | `approval-inbox.component.*` |
| 2c | PR Create: autocomplete catalog inline + budget remaining indicator | `pr-create.component.*` |
| 2d | GR Create: PO line item prefill khi chọn PO | `gr-create.*` |

**Lý do ưu tiên:** Sidebar nav là điểm vào cho Budget và Catalog — cần có trước khi làm module Budget và để Catalog UI vừa xây có thể truy cập được. Các UX gap còn lại (2b, 2c, 2d) nhỏ, ít rủi ro, tăng trải nghiệm ngay lập tức.

---

#### 3. `UI_MODULE_FINANCE_BUDGET.md` — Finance Budget Management
**Làm sau khi nav entry `nav.budgets` đã có (từ bước 2a).**

Tạo hoàn toàn mới — không có code cũ để sửa:

| Bước | Nội dung |
|---|---|
| 3a | Tạo `budget.model.ts` và `budget.service.ts` |
| 3b | Thêm routes `finance/budgets` và `finance/budgets/:id` vào `finance.routes.ts` |
| 3c | Build `BudgetListComponent` — list với KPI strip + filter + table |
| 3d | Build `BudgetDetailComponent` — utilization panel + info + Override/Transfer modals |
| 3e | i18n VI/EN, build verify |

**Lý do ưu tiên:** Accountant và Manager hoàn toàn không có UI nào cho ngân sách — đây là gap nghiệp vụ lớn nhất sau Inventory.

---

#### 4. `UI_MODULE_APPROVAL_FLOW_DIAGRAM.md` — Approval Process Visualization
**Có thể làm song song với #3, không phụ thuộc nhau.**

| Bước | Nội dung |
|---|---|
| 4a | Tạo shared component `ep-approval-steps` (horizontal + vertical compact) |
| 4b | Thay timeline thủ công trong `approval-detail.component.html` |
| 4c | Thay timeline thủ công trong `pr-detail.component.html` |
| 4d | Tạo `ep-pr-lifecycle` component (lifecycle status bar) |
| 4e | Thêm link "Xem PR đầy đủ" trong approval detail |
| 4f | Build verify |

**Lý do ưu tiên:** Component `ep-approval-steps` sẽ được dùng lại trong bước 5 (PR Traceability). Tạo trước để tránh code trùng.

---

#### 5. `UI_MODULE_PR_LIFECYCLE_TRACEABILITY.md` — PR Lifecycle & Traceability
**Làm sau #4 vì dùng `ep-approval-steps` và `ep-pr-lifecycle` đã có.**

| Bước | Nội dung |
|---|---|
| 5a | Thêm `listByPrId()` vào `RfqService` và `PurchaseOrderService` |
| 5b | Thêm Traceability section vào `pr-detail.component` (sidebar card RFQ + PO links) |
| 5c | Cập nhật logic `canCreateRfq` / `canCreatePo` để ẩn nút khi đã có liên kết |
| 5d | Build verify |

**Lý do ưu tiên:** Giải quyết vấn đề Requester không biết PR của mình đang ở đâu trong cycle, và ngăn Purchasing tạo RFQ/PO trùng.

---

#### 6. `UI_MODULE_INVOICE_3WAY_MATCH.md` — Invoice 3-Way Match Visualization
**Làm sau khi Inventory GR đã ổn định (Slice 3 của #1 đã xong — đã hoàn thành).**

| Bước | Nội dung |
|---|---|
| 6a | Thêm `PurchaseOrderService.getById()` và `GoodsReceiptService.list()` inject vào invoice detail |
| 6b | Thêm state `poDetail`, `completedGrs`, `grSummary`, `matchRows` |
| 6c | Cải thiện match status summary strip (thay `<dl>` cũ) |
| 6d | Build 3-way comparison table (thay `<section class="line-panel">` cũ) |
| 6e | i18n + SCSS |
| 6f | Build verify |

**Lý do ưu tiên:** Accountant cần bảng so sánh để quyết định approve/dispute — hiện tại chỉ có số variance thô, không đủ để phán quyết.

---

#### 7. `UI_MODULE_RFQ_QUOTE_COMPARISON.md` — RFQ Quote Comparison Table
**Độc lập hoàn toàn — có thể làm bất kỳ lúc nào sau #1.**

| Bước | Nội dung |
|---|---|
| 7a | Thêm toggle `quoteViewMode` signal và view toggle UI |
| 7b | Build comparison table (vendor columns × item rows) |
| 7c | Computed helpers: lowestTotal, bestScore, fastestDelivery, coverage |
| 7d | Tách evaluate form ra `ep-modal` (xóa inline form) |
| 7e | Thêm quote status summary progress bar |
| 7f | i18n + SCSS |
| 7g | Build verify |

**Lý do ưu tiên:** Purchasing cần so sánh ngang để award vendor — hiện tại phải nhìn từng card riêng. Quan trọng cho business nhưng không block workflow hiện tại.

---

#### 8. `UI_MODULE_DASHBOARD_CHARTS.md` — Dashboard Charts & Visual Upgrade
**Làm cuối cùng vì cần thêm dependency (`chart.js` + `ng2-charts`) và phức tạp nhất.**

| Bước | Nội dung |
|---|---|
| 8a | `npm install chart.js ng2-charts --save-exact` |
| 8b | Resolve CSS variables thành chart theme object |
| 8c | Thay Monthly Trend div giả lập → Chart.js grouped bar |
| 8d | Thay Category Spend compact bars → doughnut chart |
| 8e | Thêm SLA gauge (half-doughnut) |
| 8f | Cải thiện Department Spend bar (2 segments: committed + spent) |
| 8g | Cập nhật KPI card icons theo loại metric |
| 8h | Thêm Quick Action Links section |
| 8i | Thêm auto-refresh timer (5 phút) + last updated indicator |
| 8j | Thay department text input → select dropdown (load từ IAM) |
| 8k | i18n + SCSS |
| 8l | Build verify |

**Lý do để cuối:** Không block bất kỳ workflow nghiệp vụ nào — đây là polish. Thêm `chart.js` là thay đổi bundle, cần test kỹ. Tất cả data đã có từ backend.

---

## Sơ đồ phụ thuộc

```
#1 Inventory (Slice 5-8)
    │
    ├──► #2a Shell Nav (thêm catalog nav entry)
    │         │
    │         └──► #3 Finance Budget
    │
    │    #2b Approval Inbox Grouping  (độc lập)
    │    #2c PR Create Autocomplete    (độc lập)
    │    #2d GR Create PO Prefill      (độc lập)
    │
    ├──► #4 Approval Flow Diagram (ep-approval-steps)
    │         │
    │         └──► #5 PR Traceability
    │
    ├──► #6 Invoice 3-Way Match (GR data ổn định)
    │
    ├──► #7 RFQ Comparison (hoàn toàn độc lập)
    │
    └──► #8 Dashboard Charts (làm sau cùng)
```

---

## Quy ước chung cho tất cả module

Mọi component mới hoặc sửa đổi phải tuân thủ:

- `standalone: true`, `ChangeDetectionStrategy.OnPush`
- State bằng `signal()` / `computed()` — không dùng `BehaviorSubject` cho local UI state
- `takeUntilDestroyed(this.destroyRef)` cho mọi subscription
- `withCredentials: true` trong mọi HTTP request
- `Idempotency-Key: crypto.randomUUID()` header trong mọi POST/PUT/PATCH
- Mọi text hiển thị dùng `translate` pipe — không hardcode string tiếng Việt
- CSS dùng `var(--...)` token — không hardcode màu hex/rgb
- Không có HTTP DELETE method — dùng PATCH state transition
- Build pass: `npm run build` (frontend) trước khi coi slice là xong
- Permission gate: `*epHasPermission` directive hoặc `permissionGuard` trên route — không check role

---

## Cách đọc file planning

Mỗi file có cấu trúc chuẩn:

1. **Hiện trạng** — trích dẫn code thực tế đang có, liệt kê đúng file path
2. **Vấn đề** — những gì thiếu hoặc sai, tác động đến người dùng
3. **Giải pháp** — thiết kế chi tiết với code snippet Angular (template + TS)
4. **Models/Interfaces** — TypeScript types cần tạo hoặc cập nhật
5. **Services** — HTTP service methods cần thêm
6. **i18n keys** — tất cả keys cần thêm vào `vi.json` và `en.json`
7. **Nguyên tắc triển khai** — constraints và lưu ý quan trọng
