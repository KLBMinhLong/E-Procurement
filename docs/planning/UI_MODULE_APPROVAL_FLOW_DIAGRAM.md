# Kế hoạch UI — Approval Flow Diagram & Process Visualization

> **Mục tiêu:** Thay thế và nâng cấp cách hiển thị tiến trình duyệt hiện tại (CSS text timeline thô) thành một component trực quan, nhất quán trên cả 3 màn hình sử dụng: Approval Detail, PR Detail sidebar, và PR Detail standalone section.

---

## 1. Hiện trạng

### 1.1 Approval Detail — `/approvals/:taskId`

**File:** `features/approvals/pages/approval-detail/approval-detail.component.html`

**Cách hiển thị hiện tại:**
```html
<!-- Trong sidebar "Right Sidebar Column" -->
<div class="card sidebar-card process-card">
  <h2 class="card__title">{{ 'approvals.detail.processHeader' | translate }}</h2>
  <div class="timeline">
    @for (step of process()?.steps; track step.stepIndex; let last = $last) {
      <div class="timeline-item" [class.timeline-item--active]="step.status === 'PENDING'">
        <div class="timeline-indicator">
          <div class="timeline-dot" [class]="'timeline-dot--' + step.status.toLowerCase()"></div>
          @if (!last) { <div class="timeline-line"></div> }
        </div>
        <div class="timeline-content">
          <strong>Step {{ step.stepIndex + 1 }}: {{ step.approverRole }}</strong>
          <ep-badge [tone]="statusTone[step.status]" ... />
          <span>{{ step.approver.fullName }}</span>
          <blockquote>"{{ step.comment }}"</blockquote>
          <time>{{ formatDate(step.actedAt) }}</time>
        </div>
      </div>
    }
  </div>
</div>
```

**Vấn đề:**
- Timeline chỉ là danh sách dọc với chấm CSS — không thấy tổng quan ngay lập tức
- Đặt trong sidebar nhỏ (right column) — bị ép chiều rộng, khó đọc khi có 4-5 steps
- Không có visual indicator "bạn đang ở bước X/Y" nổi bật ở đầu trang
- `BYPASS` và `FORWARDED` status không có giải thích — không biết ai đã forward cho ai, lý do bypass là gì
- Không highlight bước đang chờ action của approver hiện tại

**Data source:** `process()` signal → `ApprovalProcessDetail` từ `approvalsService.getProcessDetail(entityType, entityId)`

---

### 1.2 PR Detail — `/procurement/:id`

**File:** `features/procurement/pages/pr-detail/pr-detail.component.html`

**Cách hiển thị hiện tại:**
```html
<!-- Trong aside sidebar -->
<ep-card tone="raised">
  <div class="approval-timeline">
    @for (step of process.steps; track step.stepIndex) {
      <div class="timeline-item" [class.timeline-item--active]="step.stepIndex === process.currentStep">
        <div class="timeline-item__marker">
          @if (step.status === 'APPROVED') { <ep-icon name="check" class="icon-success" /> }
          @else if (step.status === 'REJECTED') { <ep-icon name="x" class="icon-danger" /> }
          @else { <span class="dot"></span> }
        </div>
        <div class="timeline-item__content">
          <span>{{ step.approverRole }}</span>
          <ep-badge [tone]="..." />
          <span>{{ step.approver?.fullName }}</span>
          <!-- SLA bar cho PENDING step -->
          @if (step.status === 'PENDING') {
            <ep-sla-bar [assignedAt]="step.assignedAt" [deadline]="step.slaDeadline" />
          }
          <div>{{ step.comment }}</div>
        </div>
      </div>
    }
  </div>
</ep-card>
```

**Vấn đề:**
- Khác biệt hoàn toàn với cách render trong approval detail — không nhất quán
- Dùng `step.stepIndex === process.currentStep` để highlight active nhưng approval detail dùng `step.status === 'PENDING'`
- Cùng dữ liệu `ApprovalProcessDetail` nhưng 2 component render 2 kiểu khác nhau
- Không có bước lifecycle tổng thể của PR (DRAFT → SUBMITTED → PENDING_APPROVAL → APPROVED)

---

### 1.3 Không có lifecycle status tracker

Không có màn hình nào (PR detail hay approval detail) thể hiện vị trí hiện tại trong **full lifecycle** của PR từ đầu đến cuối. Người dùng không biết bước tiếp theo là gì.

---

## 2. Vấn đề cần giải quyết

| # | Vấn đề | Ảnh hưởng |
|---|---|---|
| 1 | Không có "you are here" indicator rõ ràng ở đầu trang | Approver mất 3-5 giây để tìm mình đang cần làm gì |
| 2 | Timeline chỉ là danh sách dọc, không scan được nhanh | Khó thấy toàn cảnh khi có 5+ steps |
| 3 | 2 cách render khác nhau cho cùng dữ liệu | Code không nhất quán, khó bảo trì |
| 4 | BYPASS/FORWARDED không giải thích | Approver không hiểu tại sao bước đó bị skip |
| 5 | Không có PR lifecycle tracker | Requester không biết PR đang ở giai đoạn nào của cả quy trình |
| 6 | Không có link "Xem PR đầy đủ" từ approval detail | Approver phải mở tab mới tự tìm |

---

## 3. Giải pháp — Component `ep-approval-steps`

### 3.1 Thiết kế component mới

**File cần tạo:** `shared/components/ep-approval-steps/ep-approval-steps.component.ts`

**Mục đích:** Shared component dùng chung ở cả approval-detail và pr-detail, thay thế 2 cách render riêng lẻ hiện tại.

**Input signals:**
```typescript
@Component({ selector: 'ep-approval-steps', ... })
export class EpApprovalStepsComponent {
  // Required
  readonly steps = input.required<ApprovalStep[]>();
  
  // Optional
  readonly currentStepIndex = input<number | null>(null);  // highlight step hiện tại
  readonly viewMode = input<'horizontal' | 'vertical'>('horizontal');
  readonly showSlaBar = input<boolean>(true);
  readonly compact = input<boolean>(false);  // dùng trong sidebar (vertical compact)
}
```

**Interface `ApprovalStep` (unified):**
```typescript
export interface ApprovalStep {
  stepIndex: number;
  approverRole: string;
  approver: {
    id: string | null;
    fullName: string | null;
  };
  status: 'PENDING' | 'APPROVED' | 'REJECTED' | 'CHANGES_REQUESTED' | 'FORWARDED' | 'BYPASS' | 'SKIPPED' | 'ESCALATED';
  comment: string | null;
  actedAt: string | null;
  assignedAt: string | null;
  slaDeadline: string | null;
  slaRemainingHours: number | null;
  forwardedTo?: { id: string; fullName: string } | null;   // khi FORWARDED
  bypassReason?: string | null;                            // khi BYPASS
}
```

---

### 3.2 Horizontal Stepper (dùng ở đầu trang approval detail)

**Layout:**
```
[Step 1]──────[Step 2]──────[Step 3 ●CURRENT]──────[Step 4]
  APPROVED      APPROVED       PENDING               (waiting)
  Manager L1    Director       CFO                   Board
  Nguyễn A      Trần B         (chưa assign)
  ✓ 2h ago      ✓ 1h ago       ⏱ 6h còn lại
```

**Visual rules:**
- Step đã xong (APPROVED): circle màu `var(--color-success)`, icon `check`, opacity 100%
- Step đang pending (PENDING): circle màu `var(--color-warning)`, pulsing ring animation
- Step hiện tại của user đang xem: outer ring `var(--color-accent)` dày hơn
- Step chưa tới: circle màu `var(--color-neutral-subtle)`, text mờ
- Step REJECTED: circle màu `var(--color-danger)`, icon `x`, line connector sau đó đổi màu đỏ
- Step FORWARDED: circle màu `var(--color-info)`, icon `forward`, tooltip/note tên người được forward tới
- Step BYPASS: circle màu `var(--color-neutral)`, icon `skip-forward`, tooltip lý do bypass
- Connector line: gradient từ màu step trước sang màu step tiếp theo

**Behavior:**
- Click vào step → expand/collapse detail panel phía dưới stepper (accordion)
- Panel chi tiết step: comment (nếu có), actedAt, forwardedTo (nếu FORWARDED), bypassReason (nếu BYPASS)
- Responsive: trên mobile (< 768px) tự chuyển sang vertical layout

---

### 3.3 Vertical Compact Timeline (dùng trong sidebar)

**Layout dọc trong sidebar (compact mode):**
```
● ✓  Manager L1 — Nguyễn A          APPROVED
│    Comment: "Đồng ý, đúng ngân sách"   2h ago
│
● ⏱  Director — Trần B               PENDING ← CURRENT
│    Hạn: 23/06 17:00 [===----] 6h còn
│
○    CFO                              WAITING
     (chưa giao)
```

**Khi `compact = true`:** Bỏ accordion, chỉ hiển thị comment nếu có status cuối (REJECTED/CHANGES_REQUESTED), rút gọn tối đa.

---

### 3.4 PR Lifecycle Tracker (riêng biệt với approval steps)

**Mục đích:** Hiển thị vị trí của PR trong toàn bộ procurement lifecycle — khác với approval steps (chỉ là một giai đoạn trong lifecycle).

**Component:** `ep-pr-lifecycle` hoặc thêm section trong PR detail template

**Stages:**
```
[DRAFT] → [SUBMITTED] → [PENDING_APPROVAL] → [APPROVED] → [CONVERTED_TO_PO / RFQ] → [CLOSED]
                                                                    ↓
                                                             [REJECTED]
                                                             [CANCELLED]
                                                             [CHANGES_REQUESTED]
```

**Visual:**
- Horizontal step bar, tương tự breadcrumb nhưng dạng status pipeline
- Stage hiện tại highlight bằng `var(--color-accent)`
- Stage đã qua: màu `var(--color-success)` + icon check nhỏ
- Stage terminal (REJECTED/CANCELLED): màu `var(--color-danger)`, hiển thị thay cho các stage sau

**Đặt vị trí:** Ngay dưới page header trong PR detail, trên metric strip, rộng full-width

---

## 4. Thay đổi trong các màn hình hiện có

### 4.1 Approval Detail — thay đổi cần thiết

**Thêm horizontal stepper ở đầu trang** (sau page header, trước main grid):
```html
<!-- Thêm trước <div class="detail-grid"> -->
<section class="process-overview" aria-labelledby="process-overview-heading">
  <h2 id="process-overview-heading" class="sr-only">{{ 'approvals.detail.processOverview' | translate }}</h2>
  <ep-approval-steps
    [steps]="process()?.steps ?? []"
    [currentStepIndex]="currentStepIndex()"
    viewMode="horizontal"
    [showSlaBar]="true" />
</section>
```

**Tính `currentStepIndex`:**
```typescript
readonly currentStepIndex = computed(() => {
  const steps = this.process()?.steps ?? [];
  const idx = steps.findIndex(s => s.status === 'PENDING');
  return idx >= 0 ? idx : null;
});
```

**Sidebar process card** — giữ lại nhưng dùng `ep-approval-steps` compact vertical thay cho HTML thủ công:
```html
<ep-approval-steps
  [steps]="process()?.steps ?? []"
  [currentStepIndex]="currentStepIndex()"
  viewMode="vertical"
  [compact]="true" />
```

**Thêm link "Xem PR đầy đủ"** trong basic-info card:
```html
<a [routerLink]="['/procurement', task()?.entityId]" class="view-full-link">
  <ep-icon name="external-link" [size]="14" />
  {{ 'approvals.detail.viewFullPr' | translate }}
</a>
```

---

### 4.2 PR Detail — thay đổi cần thiết

**Thêm PR lifecycle tracker** ngay sau `.page-header` và trước `.metric-strip`:
```html
<div class="pr-lifecycle-bar">
  <ep-pr-lifecycle [status]="pr()?.status" [approvalProcess]="pr()?.approvalProcess" />
</div>
```

**Sidebar approval card** — thay HTML thủ công bằng `ep-approval-steps`:
```html
<!-- Xóa toàn bộ <div class="approval-timeline"> hiện tại -->
<ep-approval-steps
  [steps]="approvalSteps()"
  [currentStepIndex]="currentApprovalStep()"
  viewMode="vertical"
  [compact]="true"
  [showSlaBar]="true" />
```

**Thêm "Xem trong Approval Inbox"** khi status = PENDING_APPROVAL và user có quyền duyệt:
```html
@if (pr()?.status === 'PENDING_APPROVAL' && canViewApprovalInbox()) {
  <a [routerLink]="['/approvals']" class="link-cta">
    <ep-icon name="inbox" [size]="14" />
    {{ 'pr.detail.approval.viewInbox' | translate }}
  </a>
}
```

---

## 5. Models/interfaces cần thêm

**Trong `features/approvals/models/approvals.model.ts`** — thêm `forwardedTo` và `bypassReason`:
```typescript
export interface ApprovalProcessStep {
  stepIndex: number;
  approverRole: string;
  approver: { id: string | null; fullName: string | null };
  status: 'PENDING' | 'APPROVED' | 'REJECTED' | 'CHANGES_REQUESTED' | 'FORWARDED' | 'BYPASS' | 'SKIPPED' | 'ESCALATED';
  comment: string | null;
  actedAt: string | null;
  assignedAt: string | null;
  slaDeadline: string | null;
  slaRemainingHours: number | null;
  forwardedTo: { id: string; fullName: string } | null;
  bypassReason: string | null;
}
```

---

## 6. i18n keys cần thêm

```json
"approvals.detail.processOverview": "Tổng quan tiến trình",
"approvals.detail.viewFullPr": "Xem PR đầy đủ",
"approvals.detail.step.forwardedTo": "Đã chuyển tiếp cho {name}",
"approvals.detail.step.bypassReason": "Bỏ qua: {reason}",
"approvals.detail.step.waitingAssign": "Chờ giao việc",
"approval.step.status.BYPASS": "Bỏ qua",
"approval.step.status.SKIPPED": "Bỏ qua",
"approval.step.status.ESCALATED": "Đã leo thang",
"approval.step.status.FORWARDED": "Đã chuyển tiếp",
"pr.detail.approval.viewInbox": "Xem trong hộp thư duyệt",
"pr.lifecycle.DRAFT": "Nháp",
"pr.lifecycle.SUBMITTED": "Đã nộp",
"pr.lifecycle.PENDING_APPROVAL": "Đang duyệt",
"pr.lifecycle.APPROVED": "Đã duyệt",
"pr.lifecycle.CONVERTED_TO_PO": "Đã tạo PO",
"pr.lifecycle.REJECTED": "Bị từ chối",
"pr.lifecycle.CANCELLED": "Đã hủy",
"pr.lifecycle.CLOSED": "Đã đóng"
```

---

## 7. Nguyên tắc triển khai

- `ep-approval-steps` và `ep-pr-lifecycle` là standalone components đặt trong `shared/components/`
- Không duplicate logic: cả 2 màn hình đều dùng cùng component với input khác nhau
- CSS của stepper: dùng CSS custom properties `var(--color-success)`, `var(--color-warning)`, `var(--color-danger)`, `var(--color-accent)` — không hardcode hex
- Animation pulsing cho PENDING step: `@keyframes pulse` trong SCSS của component, tuân thủ `prefers-reduced-motion`
- Horizontal → vertical responsive tại `768px` breakpoint
- `viewMode="horizontal"` chỉ có ý nghĩa ở viewport ≥ 768px; dưới 768px luôn render vertical
- Step tooltip (BYPASS reason, FORWARDED to) dùng `title` attribute cơ bản — không cần custom tooltip component
- Khi `steps` là mảng rỗng hoặc null: render `ep-empty-state` nhỏ với text "Chưa bắt đầu quy trình duyệt"
