# UI Planning — eProcure Frontend

> Thư mục này chứa các kế hoạch chi tiết cho từng module UI còn thiếu hoặc cần nâng cấp của eProcure Angular frontend.
> Mỗi file mô tả đầy đủ: hiện trạng code thực tế, vấn đề, thiết kế giải pháp, models, services, i18n keys và nguyên tắc triển khai.

---

## Trạng thái tổng quan

| # | File | Module | Trạng thái | Phụ thuộc |
|---|---|---|---|---|
| 1 | `UI_MODULE_INVENTORY.md` | Inventory Catalog UI (Slice 5–8) | ✅ Hoàn thành | Slice 8 runtime hardening được chuyển sang E2E hardening chung theo quyết định 2026-06-12 |
| 2 | `UI_MODULE_SHELL_NAV_UX.md` | Shell Nav + UX Gaps | ✅ Hoàn thành | Build pass 2026-06-13; Budget nav/indicator deferred sang plan #3 |
| 3 | `UI_MODULE_FINANCE_BUDGET.md` | Finance Budget Management | ✅ Hoàn thành | Build pass 2026-06-13; browser test chưa mở theo yêu cầu |
| 4 | `UI_MODULE_APPROVAL_FLOW_DIAGRAM.md` | Approval Process Visualization | ✅ Hoàn thành | Build pass 2026-06-13; không mở browser test |
| 5 | `UI_MODULE_PR_LIFECYCLE_TRACEABILITY.md` | PR Lifecycle & Traceability | ✅ Hoàn thành | Build pass 2026-06-13; PO `pr_id` filter + PR Detail traceability RFQ/PO |
| 6 | `UI_MODULE_INVOICE_3WAY_MATCH.md` | Invoice 3-Way Match Visualization | ✅ Hoàn thành | Build pass 2026-06-13; dùng GR status `COMPLETE` theo contract thật |
| 7 | `UI_MODULE_RFQ_QUOTE_COMPARISON.md` | RFQ Quote Comparison Table | ✅ Hoàn thành | Build pass 2026-06-13; align RFQ status frontend với backend + comparison table |
| 8 | `UI_MODULE_DASHBOARD_CHARTS.md` | Dashboard Operations & Charts Upgrade | 🔄 Đang làm | Slice 8a-8d hoàn thành 2026-06-13; chart foundation + shell UX + executive visuals + KPI deep-dive pass build, tiếp theo 8e role dashboards |

---

## Thứ tự thực hiện

### ✅ Đã hoàn thành

#### 1. `UI_MODULE_INVENTORY.md` — Inventory Catalog UI
**Trạng thái:** hoàn thành plan này theo quyết định 2026-06-12; Slice 8 không block UI roadmap và được chuyển sang E2E/runtime hardening chung.

| Slice | Nội dung | Backend | Status |
|---|---|---|---|
| Slice 0 | Contract cleanup, StockEntry model, StockService, routes, i18n | — | ✅ |
| Slice 1 | Stock Dashboard + Movements UI | — | ✅ |
| Slice 2 | Issue Out UI | — | ✅ |
| Slice 3 | Polish GR: rejected qty, lot number, complete summary | — | ✅ |
| Slice 4 | Catalog backend (Item domain/use cases/controller) | ✅ | ✅ |
| **Slice 5** | **Catalog UI: `/inventory/catalog` + `/inventory/catalog/:itemCode`** | ✅ | ✅ |
| Slice 6 | GR draft edit backend + UI (`PUT /goods-receipts/{id}`) | ✅ | ✅ |
| Slice 7 | Stock Adjustment backend + UI (`POST /stock/adjustment`) | ✅ | ✅ |
| Slice 8 | E2E / runtime hardening | ↪ | Bỏ qua trong plan này |

**Mục tiêu hoàn thành:** Warehouse user và Admin catalog manager có thể thao tác end-to-end không cần dùng DB trực tiếp.

---

### 🔄 Sẵn sàng thực hiện — theo thứ tự ưu tiên

---

#### 2. `UI_MODULE_SHELL_NAV_UX.md` — Shell Navigation & UX Gaps
**Trạng thái:** ✅ Hoàn thành 2026-06-13. `/inventory/catalog` route đã tồn tại; Budget nav được defer sang plan #3 để không tạo dead-link.

Gồm 4 nhóm thay đổi nhỏ, độc lập, có thể làm theo thứ tự bất kỳ:

| Nhóm | Thay đổi | File chính |
|---|---|---|
| 2a | Verify catalog nav đã có; defer Budget nav đến plan #3 để tránh dead link | `shell.component.ts` |
| 2b | Approval Inbox: section grouping Emergency/Overdue/Normal + SLA countdown | `approval-inbox.component.*` |
| 2c | PR Create: autocomplete catalog inline; budget indicator deferred đến plan Budget | `pr-create.component.*` |
| 2d | GR Create: polish prefill hiện có + validate received/rejected không vượt ordered | `gr-create.*` |

**Lý do ưu tiên:** Catalog UI đã có điểm vào; plan này xử lý các UX gap nhỏ, ít rủi ro. Budget nav sẽ được thêm cùng Budget route trong plan #3 để không tạo liên kết hỏng.

---

#### 3. `UI_MODULE_FINANCE_BUDGET.md` — Finance Budget Management
**Đã rà soát 2026-06-13, làm sau plan #2.** Tạo Budget route và `nav.budgets` trong cùng slice đầu tiên của plan này.

Tạo hoàn toàn mới — không có code cũ để sửa:

| Bước | Nội dung |
|---|---|
| 3a | ✅ Tạo `budget.model.ts`, `budget.service.ts`, routes `finance/budgets`, nav `nav.budgets`, i18n shell |
| 3b | ✅ Build `BudgetListComponent` — KPI theo page/filter hiện tại + filter + table |
| 3c | ✅ Build `BudgetDetailComponent` read-only — utilization panel + info + health tone |
| 3d | ✅ Thêm Override/Transfer modals theo validation backend thật |
| 3e | ✅ i18n hoàn chỉnh + `npm run build` verify |

**Lý do ưu tiên:** Accountant và Manager hoàn toàn không có UI nào cho ngân sách — đây là gap nghiệp vụ lớn nhất sau Inventory.

---

#### 4. `UI_MODULE_APPROVAL_FLOW_DIAGRAM.md` — Approval Process Visualization
**Đã rà soát 2026-06-13, làm sau plan #3.** Scope đã chỉnh theo code/API thật: không yêu cầu field backend chưa có như `forwardedTo`/`bypassReason`, dùng đúng `features/approvals/...`, tạo shared view component nhận được cả `ApprovalStepDetail` và `ApprovalStepSummary`.

| Bước | Nội dung |
|---|---|
| 4a | ✅ Tạo shared component `ep-approval-steps` (horizontal + vertical compact) |
| 4b | ✅ Wire Approval Detail: overview full-width + thay sidebar timeline + link PR đầy đủ |
| 4c | ✅ Tạo shared component `ep-pr-lifecycle` (PR lifecycle status bar) |
| 4d | ✅ Wire PR Detail: lifecycle bar + thay sidebar approval timeline |
| 4e | ✅ Navigation/UX polish: Approval Inbox link + empty/keyboard/focus states |
| 4f | ✅ i18n hoàn chỉnh + `npm run build` verify |

**Lý do ưu tiên:** Component `ep-approval-steps` sẽ được dùng lại trong bước 5 (PR Traceability). Tạo trước để tránh code trùng.

---

#### 5. `UI_MODULE_PR_LIFECYCLE_TRACEABILITY.md` — PR Lifecycle & Traceability
**Trạng thái:** ✅ Hoàn thành 2026-06-13. #4 đã hoàn thành `ep-pr-lifecycle` và `ep-approval-steps`; plan #5 đã bổ sung traceability RFQ/PO, duplicate-action guard và backend PO `pr_id` filter.

| Bước | Nội dung |
|---|---|
| 5a | ✅ Backend/API: thêm PO list filter `pr_id` trong finance-service + OpenAPI |
| 5b | ✅ Frontend service helpers: `RfqService.listByPrId()` + `PurchaseOrderService.listByPrId()` |
| 5c | ✅ PR Detail traceability card: RFQ/PO links + skeleton/empty/reload |
| 5d | ✅ Duplicate-action guard: ẩn tạo RFQ/PO khi đã có document active |
| 5e | ✅ i18n + `mvn -pl services/finance-service test` + `npm run build` verify |

**Lý do ưu tiên:** Giải quyết vấn đề Requester không biết PR của mình đang ở đâu trong cycle, và ngăn Purchasing tạo RFQ/PO trùng.

---

#### 6. `UI_MODULE_INVOICE_3WAY_MATCH.md` — Invoice 3-Way Match Visualization
**Trạng thái:** ✅ Hoàn thành 2026-06-13. Invoice Detail đã có status strip, load PO/GR phụ trợ, aggregate GR theo `poLineItemId`, bảng so sánh PO/GR/Invoice và fallback invoice lines khi dữ liệu phụ trợ không khả dụng.

| Bước | Nội dung |
|---|---|
| 6a | ✅ Thêm helper load PO/GR phụ trợ vào invoice detail |
| 6b | ✅ Thêm state `poDetail`, `completedGrs`, `grSummary`, `matchRows` |
| 6c | ✅ Cải thiện match status summary strip (thay `<dl>` cũ) |
| 6d | ✅ Build 3-way comparison table (thay `<section class="line-panel">` cũ) |
| 6e | ✅ i18n + SCSS |
| 6f | ✅ Build verify |

**Lý do ưu tiên:** Accountant cần bảng so sánh để quyết định approve/dispute — hiện tại chỉ có số variance thô, không đủ để phán quyết.

---

#### 7. `UI_MODULE_RFQ_QUOTE_COMPARISON.md` — RFQ Quote Comparison Table
**Trạng thái:** ✅ Hoàn thành 2026-06-13. RFQ frontend đã align status theo backend (`PUBLISHED/CLOSED`), RFQ Detail có comparison table theo vendor, helper highlight giá thấp/điểm tốt/giao nhanh, coverage, progress đánh giá và modal evaluate.

| Bước | Nội dung |
|---|---|
| 7a | ✅ Align RFQ frontend status với backend contract (`PUBLISHED/CLOSED`, bỏ logic mới dùng `OPEN/EVALUATING`) |
| 7b | ✅ Thêm toggle `quoteViewMode` signal và view toggle UI |
| 7c | ✅ Build comparison table (vendor columns × RFQ line rows) |
| 7d | ✅ Computed helpers: lowestTotal, bestScore, fastestDelivery, coverage, lineLowestQuoteIds |
| 7e | ✅ Tách evaluate form ra `ep-modal` |
| 7f | ✅ Thêm quote evaluation progress + award affordance |
| 7g | ✅ i18n + SCSS |
| 7h | ✅ Build verify |

**Lý do ưu tiên:** Purchasing cần so sánh ngang để award vendor — hiện tại phải nhìn từng card riêng. Quan trọng cho business nhưng không block workflow hiện tại.

---

#### 8. `UI_MODULE_DASHBOARD_CHARTS.md` — Dashboard Operations & Charts Upgrade
**Đã rà soát 2026-06-13.** Frontend dashboard đã có tabs executive/manager/purchasing/requester/reports và gọi analytics API thật; backend analytics hiện đủ cho phase UI đầu. Plan #8 đã được chỉnh từ “thêm chart” thành nâng `/dashboard` thành operational command center: chart thật, filter dùng được, URL tab state, quick actions, stale/refresh indicator, KPI deep-dive và reports UX.

| Bước | Nội dung |
|---|---|
| 8a | ✅ Chart dependency + foundation (`chart.js`/`ng2-charts`, theme resolver, computed chart data/options) |
| 8b | ✅ Dashboard shell UX: URL-driven tab, auto-refresh, last updated/cache indicator, department dropdown từ IAM |
| 8c | ✅ Executive visuals: KPI icon map, monthly grouped bar, category doughnut, SLA gauge, department spend, top vendors |
| 8d | KPI deep-dive: cycle-time trend/priority, SLA role compliance/worst approvers |
| 8e | Role dashboards thành workflow hub: manager/purchasing/requester links + CTAs |
| 8f | Quick actions + reports UX: permission cards, report presets, job polling, session persistence |
| 8g | i18n + SCSS + accessibility + responsive |
| 8h | Build verify |

**Lý do ưu tiên hiện tại:** Các module nghiệp vụ chính đã có UI; dashboard là nơi gom tín hiệu vận hành và dẫn người dùng vào workflow xử lý. Đây không còn là polish thuần túy, mà là bước hoàn thiện trải nghiệm vận hành end-to-end.

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
