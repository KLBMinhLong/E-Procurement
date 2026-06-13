# Kế hoạch UI — Dashboard Operations & Charts Upgrade

> **Mục tiêu:** Nâng cấp `/dashboard` từ trang tổng quan có các CSS bar đơn giản thành operational command center đầy đủ cho Executive, Manager, Purchasing, Requester và Reports. Trọng tâm là dữ liệu dễ đọc, chart có trục/legend/tooltip, quick actions theo quyền, filter dùng được cho người dùng thật, drilldown/link sang workflow nghiệp vụ và trạng thái dữ liệu rõ ràng.

**Rà soát 2026-06-13:** Backend analytics đã có đủ API nền tảng cho phase UI này. Không cần thêm endpoint chart mới trong slice đầu. Việc backend còn thiếu là source-enrichment nâng cao, không nên block UI upgrade.

---

## 1. Hiện Trạng Code Thật

### 1.1 Frontend hiện có

**Files chính:**
- `frontend/eprocure-web/src/app/features/dashboard/dashboard.component.ts`
- `frontend/eprocure-web/src/app/features/dashboard/dashboard.component.html`
- `frontend/eprocure-web/src/app/features/dashboard/dashboard.component.scss`
- `frontend/eprocure-web/src/app/features/dashboard/analytics.service.ts`
- `frontend/eprocure-web/src/app/features/dashboard/analytics.model.ts`

Dashboard hiện đã có:
- Permission-aware tabs: `executive`, `manager`, `purchasing`, `requester`, `reports`.
- API calls tới analytics:
  - `GET /dashboard/executive`
  - `GET /dashboard/manager`
  - `GET /dashboard/purchasing`
  - `GET /dashboard/requester`
  - `GET /kpi/cycle-time`
  - `GET /kpi/sla-compliance`
  - `POST /reports/export`
  - `GET /reports/jobs/{jobId}`
  - `GET /reports/jobs/{jobId}/download`
- Report export UI đã có trong tab `reports`, job list hiện chỉ giữ trong session component.
- i18n VI/EN đã có phần dashboard nền tảng.
- Sidebar hiện chỉ có `nav.dashboard`, không có nav riêng cho reports.

### 1.2 Backend/API hiện có

**Contract:** `docs/api/analytics-service.openapi.yaml`

Backend analytics đã expose đủ dữ liệu cho UI upgrade:
- Executive: KPI cards, spend by department/category, monthly trend, approval SLA, top vendors, `cachedAt`.
- Manager: budget status, pending approvals, recent PRs, SLA warnings.
- Purchasing: PO pipeline, RFQ/GR/invoice workload, vendor performance.
- Requester: PR stats, department budget summary, recent PRs.
- KPI deep dive: cycle-time by priority/trend, SLA by approver role/worst approvers.
- Reports: async export + job status + download.

**Department filter:** IAM đã có `GET /api/v1/org/departments`, frontend đã có `AdminOrgService.getDepartments()` và model `AdminDepartment`. Vì vậy không nên bắt user nhập UUID phòng ban trong dashboard.

### 1.3 Package hiện tại

Trước slice 8a, `frontend/eprocure-web/package.json` chưa có `chart.js` hoặc `ng2-charts`. Hiện đã bổ sung:
- `chart.js@4.5.1`
- `ng2-charts@10.0.0`
- `@angular/cdk@21.2.14`

`@angular/cdk` được pin về bản Angular 21 tương thích vì `ng2-charts@10` có peer dependency CDK. Chart provider được scope tại `DashboardComponent`, không đặt global trong `app.config.ts`, để Chart.js chỉ đi vào lazy chunk dashboard và không làm vượt initial bundle budget.

---

## 2. Gap Người Dùng Hiện Tại

| # | Gap | Tác động |
|---|---|---|
| 1 | Các biểu đồ hiện là CSS div, không có trục/tooltip/legend | Người dùng không đọc được số liệu chính xác, khó so sánh |
| 2 | KPI card dùng một icon cố định | Không phân biệt được nhóm PR, spend, SLA, vendor, invoice |
| 3 | Department filter là text UUID | Không dùng được trong thực tế vận hành |
| 4 | Không có last updated/stale/cached indicator dù backend có `cachedAt` và cache TTL 5 phút | Người dùng không biết dữ liệu mới hay cũ |
| 5 | Không có auto-refresh | Dashboard có thể stale khi mở lâu |
| 6 | Không có quick actions theo quyền | Dashboard không dẫn người dùng vào workflow xử lý tiếp theo |
| 7 | Active tab không phản ánh trên URL | Không share/bookmark được dashboard theo vai trò hoặc reports |
| 8 | KPI deep-dive chưa dùng hết dữ liệu `trend`, `byPriority`, `byApproverRole`, `worstApprovers` | Thiếu khả năng phân tích nguyên nhân chậm/trễ |
| 9 | Role dashboards còn read-only thô, thiếu link sang PR/PO/RFQ/GR/Invoice liên quan | Người dùng phải tự đi menu để xử lý |
| 10 | Reports UX chỉ giữ job trong component memory | Refresh page là mất danh sách job vừa tạo |

---

## 3. Nguyên Tắc Thiết Kế

Theo `ui-ux-pro-max`, dashboard phù hợp nhất là **Data-Dense Dashboard**: nhiều widget/chart/table, layout gọn, tối đa khả năng scan dữ liệu. Áp dụng vào repo này như sau:

- Không đổi theme tổng thể. Giữ Enterprise Dark Command Center hiện có.
- Không thêm font/palette mới; dùng token repo: `var(--color-*)`, `var(--font-*)`, `var(--spacing-*)`.
- Dùng Lucide qua `ep-icon`, không dùng emoji.
- Cards/panels gọn, không tạo hero/landing.
- Chart phải có:
  - title/description bằng i18n,
  - legend/tooltip,
  - empty/loading state,
  - `aria-label`,
  - responsive fixed-height wrapper,
  - fallback text/table khi chart không có data.

---

## 4. Quyết Định Kỹ Thuật

### 4.1 Chart library

Dùng `chart.js` + `ng2-charts`.

```powershell
cd frontend/eprocure-web
npm install chart.js@4.5.1 ng2-charts@10.0.0 @angular/cdk@21.2.14 --save-exact
```

Lý do:
- Đủ cho bar/line/doughnut/gauge.
- Wrapper Angular rõ ràng.
- Bundle nhỏ hơn các BI/chart libraries lớn.
- Không cần custom SVG phức tạp cho tooltip/axis.

### 4.2 CSS variable trong canvas

Không truyền `var(--color-accent)` trực tiếp vào Chart.js. Phải resolve runtime:

```typescript
private readCssVar(name: string): string {
  return getComputedStyle(document.documentElement).getPropertyValue(name).trim();
}
```

Tạo chart theme object trong component hoặc helper local:
- `accent`
- `info`
- `success`
- `warning`
- `danger`
- `border`
- `surface`
- `textMuted`
- `textPrimary`

### 4.3 Không cần backend mới ở phase 1

Phase UI đầu chỉ dùng contract hiện có. Backend follow-up không block plan:
- Report job history endpoint nếu muốn xem lại job sau refresh trên nhiều thiết bị.
- Approval completion/on-time denominator để SLA compliance là tỷ lệ thật hơn, hiện backend mô tả đang breach-based.
- IAM department labels/source enrichment sâu hơn cho analytics datasets.
- RFQ baseline price, finance budget plan snapshot, maverick-abuse immutable projections.

---

## 5. Kế Hoạch Triển Khai Đã Chỉnh

### Slice 8a — Chart dependency + chart foundation

**Trạng thái 2026-06-13:** ✅ Hoàn thành. `npm run build` pass; initial bundle vẫn dưới budget nhờ chart provider local trong dashboard lazy chunk.

| Việc | File |
|---|---|
| Cài `chart.js` + `ng2-charts` + `@angular/cdk` save-exact | `frontend/eprocure-web/package.json`, `package-lock.json` |
| Cấu hình chart provider local tại `DashboardComponent` | `dashboard.component.ts` |
| Tạo chart theme resolver + format helpers | `dashboard.component.ts` |
| Tạo computed chart data/options nền tảng | `dashboard.component.ts` |
| Build verify sau dependency | `npm run build` |

**Lưu ý:** Nếu `ng2-charts` chưa tương thích Angular 21 tại thời điểm install, fallback là chart SVG thuần cho monthly/department/priority và giữ doughnut/gauge bằng CSS/SVG. Không ép dependency nếu build fail do peer incompatibility.

### Slice 8b — Dashboard shell UX: filters, URL state, refresh

**Trạng thái 2026-06-13:** ✅ Hoàn thành. Đã tách `ep-dashboard-control-bar` để giữ `DashboardComponent` tập trung orchestration, sync tab vào `?tab=...`, thêm data freshness strip, auto-refresh 5 phút khi document visible, guard chống request chồng, và department dropdown từ IAM với fallback nhập UUID.

| Việc | Chi tiết |
|---|---|
| URL-driven tab | Sync `activeTab` với query param `?tab=executive|manager|purchasing|requester|reports` để share/bookmark |
| Last updated indicator | Hiển thị `cachedAt` của executive dashboard và timestamp lần reload cuối |
| Auto-refresh | 5 phút/lần, pause khi `document.hidden`, cleanup bằng `DestroyRef` |
| Refresh affordance | Nút refresh hiện loading + disabled đúng, không bắn request chồng |
| Department dropdown | Dùng `AdminOrgService.getDepartments()`, flatten tree; fallback input text chỉ khi load departments fail |
| Filter layout | Compact, responsive, không làm dashboard bị đẩy quá dài trên mobile |

### Slice 8c — Executive dashboard visual upgrade

**Trạng thái 2026-06-13:** ✅ Hoàn thành. Executive tab đã dùng `ng2-charts` cho monthly grouped bar, category doughnut và approval SLA half-gauge; KPI cards có semantic icon mapping; các chart có `aria-label`, empty fallback và summary/list phụ để vẫn đọc được số liệu khi cần.

| Widget | Nâng cấp |
|---|---|
| KPI cards | Icon map theo label key/semantic: spend, PR, RFQ, SLA, invoice, vendor |
| Monthly trend | Grouped bar chart: spent vs budget, axis, legend, tooltip, PR count trong tooltip |
| Department spend | Horizontal utilization chart/list: budget baseline + spent, percent, health badge |
| Category spend | Doughnut chart + legend/table fallback |
| Approval SLA | Half-doughnut gauge + avg cycle/overdue summary |
| Top vendors | Dense table/list có rank, spend, order count, score tone |

### Slice 8d — KPI deep-dive panel

**Trạng thái 2026-06-13:** ✅ Hoàn thành frontend. KPI panel dùng chart thật cho cycle time trend, priority cycle time và SLA role compliance; bổ sung bảng worst approvers, empty state theo từng chart và i18n VI/EN.

| Khu vực | Nâng cấp |
|---|---|
| Cycle time | Line chart theo `trend[]`, bar chart theo `byPriority[]`, target marker |
| SLA compliance | Role compliance bar/table từ `byApproverRole[]`, worst approvers table từ `worstApprovers[]` |
| Filter sync | Dùng cùng `fromDate`, `toDate`, `departmentId` với filter panel |
| Empty states | Nếu KPI endpoint trả empty, hiển thị giải thích và CTA refresh/export |

### Slice 8e — Role dashboards thành workflow hub

**Trạng thái 2026-06-14:** ✅ Hoàn thành frontend. Manager có budget health/forecast + link budgets/approvals, Purchasing có workflow links PO/RFQ/GR/Invoice và vendor meters, Requester có status doughnut chart + CTA tạo PR theo permission. Recent PR rows chưa link detail vì projection hiện chỉ có `prNumber`, chưa có UUID route id.

| Tab | Nâng cấp |
|---|---|
| Manager | Budget meter rõ allocated/committed/spent/available, forecast run-out date, pending approval link sang `/approvals`, recent PR rows có link sang PR detail |
| Purchasing | PO pipeline stacked chart/list, workload cards link sang RFQ/GR/invoice, vendor performance matrix có score/on-time/pending orders |
| Requester | PR status distribution chart, department budget health, recent PR rows link sang PR detail, CTA tạo PR khi có `PR_CREATE` |

**Không thêm API mới:** Những link dùng mã định danh đã có trong row nếu frontend hiện có đủ route. Nếu row chưa có id route cần thiết, giữ text và note backend enrichment future.

### Slice 8f — Quick actions + reports UX

| Việc | Chi tiết |
|---|---|
| Quick action strip | Permission-aware cards: create PR, approvals, create PO, budgets, RFQ, GR, invoice, reports |
| Reports deep link | Quick action reports set `?tab=reports` |
| Report presets | Khi mở reports từ executive/manager/purchasing, preselect report type hợp lý |
| Job polling | Với job vừa tạo, poll trạng thái tới `COMPLETED/FAILED`; stop polling on destroy |
| Session persistence | Lưu `reportJobs` vào `sessionStorage` để reload page không mất job trong phiên hiện tại |
| Download UX | Disabled rõ khi chưa completed, loading theo job, expiresAt hiển thị nếu có |

### Slice 8g — i18n, SCSS, accessibility, responsive

| Việc | Tiêu chuẩn |
|---|---|
| i18n | Tất cả text mới trong `vi.json` và `en.json` |
| SCSS | Chỉ dùng CSS custom properties, không hardcode hex/rgb trong component SCSS |
| Responsive | 375px, 768px, 1024px, desktop; chart không overflow mobile |
| Accessibility | `aria-label` chart, keyboard focus cho tabs/quick actions, không dùng màu là tín hiệu duy nhất |
| Loading/empty/error | Skeleton cho dashboard, empty-state cho từng chart, fallback khi departments load fail |

### Slice 8h — Verification

| Lệnh | Kỳ vọng |
|---|---|
| `npm run build` trong `frontend/eprocure-web` | Pass; ghi rõ warning nếu chỉ là warning sẵn có |
| `git diff --check` | Pass |
| Browser test | Chỉ mở nếu user yêu cầu; nếu không, dừng ở build-level verify như các plan gần đây |

---

## 6. i18n Keys Cần Bổ Sung

Nhóm key dự kiến:

```json
"dashboard.quickActions.label": "Thao tác nhanh",
"dashboard.quick.createPr": "Tạo yêu cầu mua",
"dashboard.quick.approvalInbox": "Hộp thư duyệt",
"dashboard.quick.createPo": "Tạo đơn mua",
"dashboard.quick.viewBudgets": "Xem ngân sách",
"dashboard.quick.viewRfqs": "Xem RFQ",
"dashboard.quick.createGr": "Tạo phiếu nhận hàng",
"dashboard.quick.createInvoice": "Tạo hóa đơn",
"dashboard.quick.reports": "Xuất báo cáo",
"dashboard.data.lastUpdated": "Cập nhật lúc {{time}}",
"dashboard.data.cachedAt": "Cache lúc {{time}}",
"dashboard.data.stale": "Dữ liệu có thể đã cũ",
"dashboard.filter.allDepartments": "Tất cả phòng ban",
"dashboard.filter.departmentLoadFailed": "Không tải được danh sách phòng ban, có thể nhập UUID thủ công.",
"dashboard.chart.spent": "Đã chi",
"dashboard.chart.budget": "Ngân sách",
"dashboard.chart.prCount": "Số PR",
"dashboard.chart.noData": "Chưa có dữ liệu biểu đồ",
"dashboard.kpi.target": "Mục tiêu",
"dashboard.kpi.worstApprovers": "Approver trễ SLA nhiều nhất",
"dashboard.reports.jobExpiresAt": "Hết hạn lúc {{time}}",
"dashboard.reports.polling": "Đang xử lý báo cáo..."
```

Tên key cuối cùng có thể điều chỉnh theo cấu trúc hiện tại trong `vi.json`/`en.json`, nhưng không hardcode visible text trong template.

---

## 7. Acceptance Criteria

Plan #8 chỉ coi là hoàn thành khi:

- Dashboard build pass với chart dependency hoặc fallback chart implementation.
- Executive tab không còn CSS-only monthly/category/SLA chart.
- Department filter không còn bắt user nhớ UUID khi IAM departments load được.
- Có last updated/cache indicator và auto-refresh cleanup đúng.
- Quick actions theo permission xuất hiện và link đúng route hiện có.
- KPI deep-dive dùng cả `cycle.trend`, `cycle.byPriority`, `sla.byApproverRole`, `sla.worstApprovers`.
- Manager/Purchasing/Requester tabs có workflow links/CTA hợp lý, không chỉ là số liệu rời rạc.
- Reports tab có polling/persistence phiên hiện tại và download state rõ ràng.
- i18n VI/EN đầy đủ.
- `docs/planning/README.md` và `agent/memory/progress-tracker.md` được cập nhật khi hoàn thành.

---

## 8. Rủi Ro Và Cách Xử Lý

| Rủi ro | Cách xử lý |
|---|---|
| `ng2-charts` peer dependency chưa khớp Angular 21 | Thử build ngay sau install; nếu fail do compatibility, revert dependency và dùng SVG chart thuần trong plan này |
| Chart.js không đọc CSS variables | Resolve bằng `getComputedStyle`, không truyền `var(...)` vào canvas |
| Department API bị chặn permission với một số user | Hiển thị fallback text input UUID + thông báo i18n, không block dashboard |
| Auto-refresh gây request chồng | Guard bằng loading state hoặc timer chỉ gọi khi không loading |
| Report job list không có backend history endpoint | Persist sessionStorage cho job trong phiên hiện tại; backend history là future enhancement |
| SLA compliance đang breach-based | UI label/hint phải trung thực, không diễn giải như tỷ lệ hoàn tất tuyệt đối nếu backend chưa có denominator đầy đủ |
