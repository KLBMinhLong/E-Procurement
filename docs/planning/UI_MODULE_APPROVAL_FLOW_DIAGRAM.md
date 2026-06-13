# Kế hoạch UI — Approval Flow Diagram & Process Visualization

> **Mục tiêu:** Chuẩn hóa cách hiển thị tiến trình duyệt trên Approval Detail và PR Detail bằng shared components có thể tái sử dụng, giúp approver/requester scan nhanh vị trí hiện tại trong workflow và toàn bộ lifecycle của PR.

---

## 0. Rà soát lại theo code hiện tại — 2026-06-13

Plan này là plan kế tiếp sau `UI_MODULE_FINANCE_BUDGET.md`. Rà soát code hiện tại cho thấy hướng làm vẫn đúng, nhưng plan cũ có một số giả định cần chỉnh để khớp frontend/backend thật.

| Nhóm | Trạng thái thực tế | Điều chỉnh scope |
|---|---|---|
| Path Approval | Code thật nằm ở `features/approvals/...`, không phải `features/approval/...` | Dùng đúng `features/approvals/pages/approval-detail/*`, `features/approvals/models/approvals.model.ts`, `features/approvals/services/approvals.service.ts`. |
| Route Approval Detail | Route thật là `/approvals/:id`, param tên `id` là task id | Link từ PR detail về inbox dùng `/approvals`; link từ approval detail sang PR dùng `/procurement/:entityId` khi `entityType === 'PURCHASE_REQUEST'`. |
| Approval step detail API | OpenAPI `ApprovalProcessDetail.steps` hiện có: `stepIndex`, `approverRole`, `approver`, `delegateId`, `status`, `action`, `comment`, `slaDeadline`, `assignedAt`, `actedAt`, `isEscalated` | Không thêm bắt buộc `forwardedTo`/`bypassReason` vào model. Nếu cần note forward/skipped thì render từ `action`, `delegateId`, `comment`, `isEscalated`; backend không trả tên người forward tới. |
| Step status | Backend hiện có `PENDING`, `APPROVED`, `REJECTED`, `ESCALATED`, `SKIPPED`, `FORWARDED`; PR detail summary cũng cùng union | Không dùng `BYPASS` trong slice này. Dùng `SKIPPED` cho bước bỏ qua. Không đưa `CHANGES_REQUESTED` vào step status nếu API chưa trả ở step-level. |
| Model khác nhau | Approval Detail dùng `ApprovalStepDetail`; PR Detail dùng `ApprovalStepSummary` trong `purchase-request.model.ts` | Tạo shared component nhận kiểu view model linh hoạt `ApprovalStepView` với field optional, không ép sửa API model nếu không cần. |
| current step | Approval detail có `currentStepIndex`; PR detail có `currentStep` | Component nhận `currentStepIndex`; từng page map từ field hiện có. Nếu null thì tự tìm step `PENDING`. |
| SLA | Đã có `EpSlaBarComponent` | `ep-approval-steps` dùng lại `ep-sla-bar`, không tạo progress bar mới. |
| Icons | `app.config.ts` đã register `check`, `x`, `clock`, `send`, `circle-off`, `shield-alert`, `workflow`, `external-link`, `inbox` | Dùng các icon đã register để tránh build fail. Nếu muốn icon mới như `forward`/`skip-forward`, phải add vào `app.config.ts` cùng slice. |
| i18n status | Đã có `approval.status.*` trong `vi.json`/`en.json` cho `PENDING/APPROVED/REJECTED/ESCALATED/SKIPPED/FORWARDED/...` | Component dùng lại `approval.status.*`; chỉ thêm key cho labels/hints mới. |
| UI style | Skill UI gợi ý data-dense dashboard; dự án đã có Enterprise Dark Command Center tokens | Ưu tiên design system hiện tại của dự án: dense, scannable, CSS vars, Lucide icons; không đổi palette/font theo external suggestion. |

**Thứ tự thực hiện đề xuất:** 4a shared `ep-approval-steps` → 4b wire Approval Detail → 4c shared `ep-pr-lifecycle` → 4d wire PR Detail → 4e links/navigation polish → 4f i18n + build verify.

**Trạng thái triển khai 2026-06-13:** ✅ Slice 4a hoàn thành ở mức build: đã tạo shared component `ep-approval-steps` với horizontal/vertical compact modes, empty state, current-step highlight, status icon/tone, detail expansion, compact comments, SLA bar reuse, responsive layout, reduced-motion pulse và i18n `approval.steps.*`.

**Trạng thái triển khai 2026-06-13:** ✅ Slice 4b hoàn thành ở mức build: Approval Detail đã có workflow overview full-width, sidebar process card dùng lại `ep-approval-steps` vertical compact thay timeline riêng, nút mở PR đầy đủ khi task là `PURCHASE_REQUEST`, và i18n `approvals.detail.processOverview/viewFullPr`.

**Trạng thái triển khai 2026-06-13:** ✅ Slice 4c hoàn thành ở mức build: đã tạo shared component `ep-pr-lifecycle` với lifecycle stages, terminal node cho `REJECTED/CANCELLED/CHANGES_REQUESTED`, state complete/current/upcoming/blocked, responsive layout, export shared và i18n `pr.lifecycle.*`.

**Trạng thái triển khai 2026-06-13:** ✅ Slice 4d hoàn thành ở mức build: PR Detail đã render `ep-pr-lifecycle` ngay sau page header, sidebar approval dùng lại `ep-approval-steps` vertical compact thay timeline thủ công, thêm link Approval Inbox có guard permission khi PR `PENDING_APPROVAL`, và i18n `pr.detail.approval.viewInbox`.

**Trạng thái triển khai 2026-06-13:** ✅ Slice 4e hoàn thành ở mức build: đã polish navigation/UX cho Approval Detail và PR Detail, đảm bảo empty workflow hiển thị rõ, link PR/Approval Inbox chỉ render đúng điều kiện, và `ep-approval-steps` có keyboard toggle/aria-label cho step detail kể cả compact mode.

---

## 1. Hiện trạng

### 1.1 Approval Detail — `/approvals/:id`

**Files thực tế:**
- `frontend/eprocure-web/src/app/features/approvals/pages/approval-detail/approval-detail.component.ts`
- `frontend/eprocure-web/src/app/features/approvals/pages/approval-detail/approval-detail.component.html`
- `frontend/eprocure-web/src/app/features/approvals/pages/approval-detail/approval-detail.component.scss`

**Cách hiển thị hiện tại:**
- Header có task number và action buttons approve/reject/request changes/forward.
- Main grid có basic info, line items, attachments.
- Sidebar có requester, SLA card và process card.
- Process card đang render timeline thủ công bằng `.timeline`, `.timeline-item`, `.timeline-dot`.

**Nguồn dữ liệu:**
- `taskDetail = approvalsService.getTaskDetail(taskId)`
- `process = approvalsService.getProcessDetail(entityType, entityId)`
- Model: `ApprovalProcessDetail.steps: ApprovalStepDetail[]`

**Vấn đề:**
- Timeline chỉ nằm trong sidebar nên không có overview rõ ở đầu trang.
- Timeline approval detail và PR detail render bằng hai bộ HTML/SCSS khác nhau.
- Active step logic đang dựa vào `step.status === 'PENDING'`, trong khi API đã có `currentStepIndex`.
- Step status `ESCALATED/SKIPPED/FORWARDED` chưa có visual treatment rõ.
- Không có link trực tiếp sang PR đầy đủ dù task đã có `entityId`.

### 1.2 PR Detail — `/procurement/:id`

**Files thực tế:**
- `frontend/eprocure-web/src/app/features/procurement/pages/pr-detail/pr-detail.component.ts`
- `frontend/eprocure-web/src/app/features/procurement/pages/pr-detail/pr-detail.component.html`
- `frontend/eprocure-web/src/app/features/procurement/pages/pr-detail/pr-detail.component.scss`

**Cách hiển thị hiện tại:**
- Sidebar approval card render timeline thủ công bằng `.approval-timeline`, `.timeline-item`.
- Active step logic dùng `step.stepIndex === process.currentStep`.
- Có SLA bar cho step pending.
- Không có PR lifecycle tracker full-width.

**Nguồn dữ liệu:**
- `pr = PurchaseRequestService.getById(id)`
- `pr.approvalProcess?: ApprovalProcess`
- Model: `ApprovalProcess.steps: ApprovalStepSummary[]`

**Vấn đề:**
- Requester không thấy PR đang ở đâu trong toàn lifecycle: draft, submitted, pending approval, approved, converted, closed/terminal.
- HTML/SCSS timeline trùng concept nhưng không tái sử dụng.
- Không có link tiện lợi về Approval Inbox khi PR đang pending approval và user có quyền duyệt.

---

## 2. Giải pháp tổng thể

Tạo 2 shared components:

1. `ep-approval-steps`
   - Dùng chung cho Approval Detail và PR Detail.
   - Hỗ trợ horizontal overview và vertical compact timeline.
   - Nhận view model linh hoạt, không phụ thuộc chặt vào một feature model.
   - Dùng `ep-badge`, `ep-icon`, `ep-sla-bar`, `ep-empty-state`.

2. `ep-pr-lifecycle`
   - Dùng trong PR Detail để thể hiện lifecycle tổng thể của Purchase Request.
   - Tách biệt khỏi approval steps vì approval chỉ là một giai đoạn trong lifecycle.
   - Dùng `PrStatus` hiện có, không cần API mới.

---

## 3. Slice 4a — Shared Component `ep-approval-steps`

**Files cần tạo:**
```
frontend/eprocure-web/src/app/shared/components/ep-approval-steps/
  ep-approval-steps.component.ts
  ep-approval-steps.component.html
  ep-approval-steps.component.scss
```

**Cập nhật export:**
```
frontend/eprocure-web/src/app/shared/shared.index.ts
```

**Interface view model trong component:**
```typescript
export type ApprovalStepStatus =
  | 'PENDING'
  | 'APPROVED'
  | 'REJECTED'
  | 'ESCALATED'
  | 'SKIPPED'
  | 'FORWARDED';

export interface ApprovalStepView {
  stepIndex: number;
  approverRole: string;
  approver?: { id?: string | null; fullName?: string | null } | null;
  status: ApprovalStepStatus;
  action?: 'APPROVE' | 'REJECT' | 'REQUEST_CHANGES' | 'FORWARD' | null;
  comment?: string | null;
  actedAt?: string | null;
  assignedAt?: string | null;
  slaDeadline?: string | null;
  slaRemainingHours?: number | null;
  delegateId?: string | null;
  isEscalated?: boolean;
}
```

**Inputs:**
```typescript
readonly steps = input.required<ReadonlyArray<ApprovalStepView>>();
readonly currentStepIndex = input<number | null>(null);
readonly viewMode = input<'horizontal' | 'vertical'>('horizontal');
readonly showSlaBar = input(true);
readonly compact = input(false);
```

**Computed/helper cần có:**
- `resolvedCurrentStepIndex`: dùng input nếu có, fallback step đầu tiên `PENDING`.
- `statusTone(status): EpBadgeTone`.
- `statusIcon(status): string` dùng icon đã register:
  - `APPROVED` → `check`
  - `REJECTED` → `x`
  - `PENDING` → `clock`
  - `FORWARDED` → `send`
  - `SKIPPED` → `circle-off`
  - `ESCALATED` → `shield-alert`
- `formatDateTime(iso)` dùng `Intl.DateTimeFormat('vi-VN', ...)` theo pattern hiện có.
- `approverLabel(step)` fallback `approval.steps.waitingAssign`.

**Render behavior:**
- Khi `steps.length === 0`, render compact empty state hoặc text note bằng i18n.
- Horizontal mode:
  - Render step nodes + connector line.
  - Bước current có outer ring.
  - Click/keyboard Enter trên step toggle detail panel.
  - Detail panel show approver, assignedAt, deadline, actedAt, comment/action note.
- Vertical compact mode:
  - Render dense list, không accordion.
  - Hiển thị comment chỉ khi có comment hoặc status terminal.
  - Hiển thị `ep-sla-bar` cho step `PENDING` nếu có `assignedAt` và `slaDeadline`.

**SCSS rules:**
- Dùng CSS custom properties `var(--color-success)`, `var(--color-warning)`, `var(--color-danger)`, `var(--color-accent)`, `var(--color-info)`, `var(--color-surface)`.
- Không hardcode hex/rgb.
- `@media (max-width: 768px)` ép horizontal thành vertical.
- Pulse ring cho pending phải bọc trong `@media (prefers-reduced-motion: no-preference)`.
- Focus visible phải có `box-shadow: var(--focus-ring)`.

---

## 4. Slice 4b — Wire Approval Detail

**Files sửa:**
- `features/approvals/pages/approval-detail/approval-detail.component.ts`
- `features/approvals/pages/approval-detail/approval-detail.component.html`
- `features/approvals/pages/approval-detail/approval-detail.component.scss`

**TS changes:**
```typescript
readonly currentStepIndex = computed(() => this.process()?.currentStepIndex ?? null);

readonly approvalSteps = computed(() => this.process()?.steps ?? []);

canOpenPurchaseRequest(): boolean {
  return this.task()?.entityType === 'PURCHASE_REQUEST' && Boolean(this.task()?.entityId);
}

navigateToPurchaseRequest(): void {
  const entityId = this.task()?.entityId;
  if (entityId) this.router.navigate(['/procurement', entityId]);
}
```

**Template changes:**
- Thêm overview full-width ngay sau header:
```html
@if (process()?.steps?.length) {
  <section class="process-overview" aria-labelledby="process-overview-heading">
    <h2 id="process-overview-heading" class="sr-only">
      {{ 'approvals.detail.processOverview' | translate }}
    </h2>
    <ep-approval-steps
      [steps]="approvalSteps()"
      [currentStepIndex]="currentStepIndex()"
      viewMode="horizontal"
      [showSlaBar]="true" />
  </section>
}
```

- Trong basic info card, thêm link PR đầy đủ chỉ khi `entityType === 'PURCHASE_REQUEST'`:
```html
@if (canOpenPurchaseRequest()) {
  <ep-button variant="ghost" size="sm" icon="external-link" (click)="navigateToPurchaseRequest()">
    {{ 'approvals.detail.viewFullPr' | translate }}
  </ep-button>
}
```

- Thay toàn bộ `.timeline` trong sidebar bằng:
```html
<ep-approval-steps
  [steps]="approvalSteps()"
  [currentStepIndex]="currentStepIndex()"
  viewMode="vertical"
  [compact]="true"
  [showSlaBar]="true" />
```

**SCSS cleanup:**
- Xóa hoặc ngưng dùng `.timeline*` thủ công sau khi replacement xong.
- Thêm `.process-overview` card style theo existing surface token.

---

## 5. Slice 4c — Shared Component `ep-pr-lifecycle`

**Files cần tạo:**
```
frontend/eprocure-web/src/app/shared/components/ep-pr-lifecycle/
  ep-pr-lifecycle.component.ts
  ep-pr-lifecycle.component.html
  ep-pr-lifecycle.component.scss
```

**Cập nhật export:**
```
frontend/eprocure-web/src/app/shared/shared.index.ts
```

**Inputs:**
```typescript
readonly status = input<PrStatus | null>(null);
readonly approvalStatus = input<string | null>(null);
```

**Lifecycle stages:**
```typescript
[
  'DRAFT',
  'SUBMITTED',
  'PENDING_APPROVAL',
  'APPROVED',
  'CONVERTED_TO_PO',
  'CLOSED'
]
```

**Terminal statuses:**
- `REJECTED`
- `CANCELLED`
- `CHANGES_REQUESTED`

**Behavior:**
- Stage hiện tại highlight `var(--color-accent)`.
- Stage đã qua highlight success.
- Nếu terminal status, render terminal node rõ và không đánh dấu các stage sau là complete.
- `CHANGES_REQUESTED` quay về requester, nên hiển thị như terminal/action-needed tạm thời, không phải completed.
- Dùng i18n keys `pr.lifecycle.*` để label ngắn, không hardcode visible text.

---

## 6. Slice 4d — Wire PR Detail

**Files sửa:**
- `features/procurement/pages/pr-detail/pr-detail.component.ts`
- `features/procurement/pages/pr-detail/pr-detail.component.html`
- `features/procurement/pages/pr-detail/pr-detail.component.scss`

**TS changes:**
```typescript
readonly approvalSteps = computed(() => this.pr()?.approvalProcess?.steps ?? []);
readonly currentApprovalStep = computed(() => this.pr()?.approvalProcess?.currentStep ?? null);
```

**Template changes:**
- Thêm lifecycle bar ngay sau page header và trước metric strip/main grid:
```html
@if (pr(); as data) {
  <ep-pr-lifecycle
    [status]="data.status"
    [approvalStatus]="data.approvalProcess?.status ?? null" />
}
```

- Thay sidebar `.approval-timeline` thủ công bằng:
```html
<ep-approval-steps
  [steps]="approvalSteps()"
  [currentStepIndex]="currentApprovalStep()"
  viewMode="vertical"
  [compact]="true"
  [showSlaBar]="true" />
```

- Thêm link về Approval Inbox khi PR đang chờ duyệt và user có permission approve:
```html
@if (data.status === 'PENDING_APPROVAL') {
  <ep-button
    *epHasPermission="['PR_APPROVE_L1', 'PR_APPROVE_L2', 'PR_APPROVE_L3', 'PR_APPROVE_FINANCE', 'PR_APPROVE_EMERGENCY']"
    variant="ghost"
    size="sm"
    icon="inbox"
    routerLink="/approvals">
    {{ 'pr.detail.approval.viewInbox' | translate }}
  </ep-button>
}
```

**SCSS cleanup:**
- Xóa hoặc ngưng dùng `.approval-timeline`, `.timeline-item*` sau khi replacement xong.
- Thêm wrapper `.pr-lifecycle-panel` nếu cần spacing.

---

## 7. Slice 4e — Navigation/UX Polish

**Mục tiêu:** Hoàn thiện các chi tiết nhỏ để component dùng được trong cả hai context.

**Checklist:**
- Approval Detail:
  - Link "Xem PR đầy đủ" chỉ render cho `PURCHASE_REQUEST`.
  - Không render link nếu `entityId` thiếu.
  - Empty process có message rõ.
- PR Detail:
  - Link "Xem trong hộp thư duyệt" chỉ render khi status `PENDING_APPROVAL` và user có permission approval.
  - Lifecycle vẫn render đúng khi PR `DRAFT` chưa có approval process.
- Shared components:
  - Keyboard access: step node là button, Enter/Space toggle detail.
  - Không dùng `title` thay cho nội dung quan trọng duy nhất; tooltip/title chỉ phụ trợ.
  - Không dùng màu là tín hiệu duy nhất: có icon + label.

---

## 8. Slice 4f — i18n + Build Verify

**i18n keys cần thêm/cập nhật trong `vi.json` và `en.json`:**
```json
{
  "approvals": {
    "detail": {
      "processOverview": "Tổng quan tiến trình",
      "viewFullPr": "Xem PR đầy đủ"
    }
  },
  "approval": {
    "steps": {
      "emptyTitle": "Chưa có bước phê duyệt",
      "emptyMessage": "Quy trình duyệt chưa được khởi tạo hoặc chưa trả dữ liệu.",
      "waitingAssign": "Chờ giao việc",
      "assignedAt": "Giao lúc",
      "deadline": "Hạn xử lý",
      "actedAt": "Xử lý lúc",
      "comment": "Ý kiến",
      "noComment": "Không có ghi chú",
      "forwarded": "Đã chuyển tiếp",
      "skipped": "Đã bỏ qua",
      "escalated": "Đã chuyển cấp",
      "stepNumber": "Bước {{step}}"
    }
  },
  "pr": {
    "detail": {
      "approval": {
        "viewInbox": "Xem trong hộp thư duyệt"
      }
    },
    "lifecycle": {
      "title": "Vòng đời yêu cầu mua hàng",
      "DRAFT": "Nháp",
      "SUBMITTED": "Đã nộp",
      "PENDING_APPROVAL": "Đang duyệt",
      "APPROVED": "Đã duyệt",
      "CONVERTED_TO_PO": "Đã tạo PO",
      "CLOSED": "Đã đóng",
      "REJECTED": "Bị từ chối",
      "CANCELLED": "Đã hủy",
      "CHANGES_REQUESTED": "Cần bổ sung"
    }
  }
}
```

**Verify:**
```powershell
cd frontend/eprocure-web
npm run build
```

Không mở browser test nếu người dùng vẫn yêu cầu dừng ở mức build.

---

## 9. Nguyên tắc triển khai

- Component mới: standalone, `ChangeDetectionStrategy.OnPush`.
- Local state dùng `signal()` / `computed()`.
- Subscription mới nếu có phải dùng `takeUntilDestroyed(this.destroyRef)`.
- Không thêm HTTP endpoint hoặc service mới trong plan này.
- Không sửa backend/OpenAPI vì dữ liệu hiện tại đủ cho visual flow.
- Không tạo field frontend bắt buộc nếu backend chưa trả (`forwardedTo`, `bypassReason`).
- Mọi visible text dùng translate pipe/key i18n.
- CSS dùng `var(--...)`; không hardcode hex/rgb.
- Dùng Lucide icons đã register hoặc cập nhật `app.config.ts` nếu thêm icon mới.
- Không duplicate timeline HTML mới trong feature pages; mọi timeline approval dùng `ep-approval-steps`.
- Không dùng emoji.
- Build pass mới coi slice hoàn thành.
