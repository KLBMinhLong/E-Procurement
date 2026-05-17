# E05 Approval Engine
## User Stories và Use Cases

---

## 1. Epic Goal

Tự động tạo và điều phối quy trình phê duyệt PR bằng Camunda BPMN, áp dụng approval rules theo giá trị/category/priority, enforce Separation of Duties, hỗ trợ delegation, SLA, escalation và các hành động approve/reject/request changes/forward.

---

## 2. Actors

| Actor | Vai trò |
|---|---|
| Approval Engine | Consumer event PR submitted, tạo process/task |
| Manager/Director/Finance/C-Level | Approver xử lý task |
| Requester | Theo dõi trạng thái PR và xử lý changes requested |
| Admin | Cấu hình approval rules |
| Notification Service | Nhận event assigned/SLA breached/result |

---

## 3. User Stories

| ID | Story | Priority | API/Event Reference |
|---|---|---|---|
| E05-US-001 | Là Approval Engine, tôi muốn nhận `PrSubmittedEvent` và start process phê duyệt. | MVP | `procurement.pr.submitted` |
| E05-US-002 | Là hệ thống, tôi muốn chọn approval rule theo amount/category/department/priority. | MVP | ApprovalRule |
| E05-US-003 | Là hệ thống, tôi muốn resolve approver từ org chart và enforce requester != approver. | MVP | IAM `GET /org/approvers` |
| E05-US-004 | Là Approver, tôi muốn xem inbox task đang chờ duyệt. | MVP | `GET /approvals/inbox` |
| E05-US-005 | Là Approver, tôi muốn xem chi tiết task và PR context trước khi quyết định. | MVP | `GET /approvals/tasks/{taskId}` |
| E05-US-006 | Là Approver, tôi muốn approve task để process chuyển bước tiếp theo hoặc hoàn tất. | MVP | `PATCH /approvals/tasks/{taskId}/approve` |
| E05-US-007 | Là Approver, tôi muốn reject task với comment để PR bị từ chối. | MVP | `PATCH /approvals/tasks/{taskId}/reject` |
| E05-US-008 | Là Approver, tôi muốn request changes để requester bổ sung thông tin. | MVP | `PATCH /approvals/tasks/{taskId}/request-changes` |
| E05-US-009 | Là Approver, tôi muốn forward task cho người khác khi hợp lệ. | P1 | `PATCH /approvals/tasks/{taskId}/forward` |
| E05-US-010 | Là hệ thống, tôi muốn theo dõi SLA và escalation khi task quá hạn. | P1 | `approval.sla.breached` |
| E05-US-011 | Là Admin, tôi muốn tạo/sửa/deactivate approval rules. | MVP | `/approvals/rules` |
| E05-US-012 | Là Approver, tôi muốn UI inbox/detail/action rõ deadline, delegated state và comment. | MVP | Angular |

---

## 4. Use Cases

### E05-UC-001: StartApprovalProcessUseCase

**Trigger:** Consume `procurement.pr.submitted`.

**Preconditions:**

```
- Event envelope valid.
- PR payload chứa id, prNumber, requesterId, departmentId, totalAmount, priority, categories.
- Event chưa được xử lý trước đó.
```

**Main flow:**

```
1. Check event idempotency by eventId.
2. Select approval rule chain.
3. Resolve approvers via IAM OrgApproverPort.
4. Enforce requester != approver for every step.
5. Create ApprovalProcess aggregate.
6. Start Camunda process instance `pr-approval-process.bpmn` hoặc `emergency-approval.bpmn`.
7. Create first ApprovalStep task(s).
8. Publish `approval.step.assigned`.
9. Notify PR service that status can move to PENDING_APPROVAL if needed.
```

**Alternate/error flows:**

```
- No matching rule -> use DEFAULT rule or throw APR_RULE_NOT_FOUND.
- No approver -> APR_APPROVER_NOT_FOUND and process not started.
- SoD violation unresolved -> APR_CONFLICT_OF_INTEREST.
- Duplicate event -> skip without duplicate process.
```

**Acceptance criteria:**

```
[ ] Camunda process id stored in ApprovalProcess.
[ ] No duplicate process for same PR/event.
[ ] SoD enforced before task assignment.
[ ] Event publish uses standard envelope.
```

### E05-UC-002: SelectApprovalRuleUseCase

**Main flow:**

```
1. Load active ApprovalRule records where is_deleted = false.
2. Filter by ruleType conditions: VALUE, CATEGORY, DEPARTMENT, DEFAULT, priority.
3. Sort by priority desc.
4. Merge additive category rules if configured, e.g. SOFTWARE/SAAS adds CISO + IT_MANAGER.
5. Return ordered step templates.
```

**Rule matrix baseline:**

```
VALUE_UNDER_5M      -> Manager
VALUE_5M_20M        -> Manager -> Finance
VALUE_20M_50M       -> Manager -> Director -> Finance
VALUE_50M_200M      -> Manager -> Director -> CFO, RFQ required
VALUE_200M_500M     -> Manager -> Director -> CEO -> CFO
VALUE_OVER_500M     -> Manager -> Director -> BOD parallel -> CFO
EMERGENCY           -> Manager + Director parallel -> PostAudit
CAT_IT_SOFTWARE     -> + CISO + IT_MANAGER
CAT_CAPEX           -> + FinanceDirector + CEO
```

**Acceptance criteria:**

```
[ ] Money compare uses BigDecimal.
[ ] Rules are soft-deletable/deactivatable, not hard deleted.
[ ] Active highest priority rule wins unless additive rule explicitly configured.
```

### E05-UC-003: GetApprovalInboxUseCase

**Permission:** one or more approval permissions such as `PR_APPROVE_L1`, `PR_APPROVE_L2`, `PR_APPROVE_FINANCE`, `PR_APPROVE_EMERGENCY`.

**Main flow:**

```
1. Load current user id and permissions.
2. Query pending tasks assigned to user or active delegation.
3. Filter by query params priority/entity_type/min_amount/is_overdue.
4. Sort by SLA deadline whitelist.
5. Return paginated ApprovalTaskSummary.
```

**Acceptance criteria:**

```
[ ] Delegated tasks show delegatedFrom.
[ ] Overdue/warning SLA flags correct.
[ ] Pagination meta correct.
[ ] Query filters is_deleted = false.
```

### E05-UC-004: GetTaskDetailUseCase

**Main flow:**

```
1. Verify user can view/act on task.
2. Load ApprovalTask, ApprovalProcess and PR context snapshot.
3. Return details, SLA, previous steps, allowed actions.
```

**Acceptance criteria:**

```
[ ] User cannot open task not assigned/delegated unless has admin/audit permission.
[ ] Sensitive fields not returned.
[ ] Allowed actions match permission and task status.
```

### E05-UC-005: ApproveTaskUseCase

**Permission:** matching approval permission for the step.

**Main flow:**

```
1. Check idempotency.
2. Load task by camunda task id.
3. Validate task PENDING and assigned/delegated to actor.
4. Enforce actor != requester.
5. Record APPROVE action/comment.
6. Complete Camunda task.
7. If next step exists, assign next step and publish approval.step.assigned.
8. If process complete, mark process COMPLETED and publish `procurement.pr.approved`.
9. Cache idempotency response.
```

**Acceptance criteria:**

```
[ ] Approve endpoint is PATCH and requires Idempotency-Key.
[ ] Duplicate approve with same key replays result.
[ ] Completing final step publishes approved event once.
[ ] Audit log records actor, task, entity, result.
```

### E05-UC-006: RejectTaskUseCase

**Main flow:**

```
1. Check idempotency.
2. Validate task pending and actor assignment.
3. Require non-empty rejection comment.
4. Record REJECT action.
5. Cancel/complete Camunda process as rejected.
6. Mark ApprovalProcess COMPLETED.
7. Publish `procurement.pr.rejected`.
```

**Acceptance criteria:**

```
[ ] Rejection comment is required.
[ ] PR service receives rejected event exactly once.
[ ] Pending sibling/next tasks are no longer actionable.
```

### E05-UC-007: RequestChangesUseCase

**Main flow:**

```
1. Check idempotency.
2. Validate task pending and permission `PR_REQUEST_CHANGES`.
3. Require comment with requested changes.
4. Record REQUEST_CHANGES action.
5. Pause/cancel approval process according to BPMN.
6. Publish `procurement.pr.changes-requested`.
```

**Acceptance criteria:**

```
[ ] PR becomes CHANGES_REQUESTED in PR service.
[ ] Requester can edit and resubmit.
[ ] Original approval history remains auditable.
```

### E05-UC-008: ForwardTaskUseCase

**Priority:** P1.

**Main flow:**

```
1. Check idempotency.
2. Validate actor can forward.
3. Validate target approver has required permission and is not requester.
4. Update task assignee/delegate metadata.
5. Publish approval.step.assigned for new assignee.
```

**Acceptance criteria:**

```
[ ] Cannot forward to requester.
[ ] Cannot forward to user without required permission.
[ ] Forward history is auditable.
```

### E05-UC-009: SlaEscalationUseCase

**Priority:** P1.

**Main flow:**

```
1. Camunda timer detects SLA breach.
2. Load overdue ApprovalStep.
3. Mark step isEscalated.
4. Resolve escalation target.
5. Assign or notify escalation target.
6. Publish `approval.sla.breached`.
```

**Acceptance criteria:**

```
[ ] SLA uses business hours unless EMERGENCY.
[ ] Emergency SLA uses 24/7 2h/4h rule.
[ ] Escalation event does not expose sensitive comment data.
```

### E05-UC-010: ManageApprovalRulesUseCases

**Use cases:**

```
ListApprovalRulesUseCase
CreateApprovalRuleUseCase
UpdateApprovalRuleUseCase
DeactivateApprovalRuleUseCase
```

**Rules:**

```
- Permission `ADMIN_APPROVAL_RULE`.
- POST/PUT/PATCH require Idempotency-Key.
- Deactivate uses PATCH /approvals/rules/{id}/deactivate.
- Rule changes audit logged.
```

---

## 5. Frontend Use Cases

```
ApprovalInboxPage: task table, filters, SLA sorting, count badge.
ApprovalTaskDetailPage: PR context, approval timeline, comment box, action buttons.
ApprovalRuleAdminPage: list/create/update/deactivate rules.
```

Acceptance:

```
[ ] Action buttons controlled by permission code.
[ ] ep-sla-bar shows warning/overdue.
[ ] Approve/reject/request changes use stable Idempotency-Key per click intent.
[ ] No hardcoded visible text; translate keys only.
```

---

## 6. Technical Deliverables

```
approval-service Spring Boot project
Camunda BPMN: pr-approval-process.bpmn, emergency-approval.bpmn
Flyway migrations: approval_processes, approval_steps, approval_rules, event_processing_log
Domain: ApprovalProcess, ApprovalStep, ApprovalRule, ApprovalCondition
UseCases: StartProcess, SelectRule, ResolveChain, Inbox, TaskDetail, Approve, Reject, RequestChanges, Forward, SlaEscalation, ManageRules
Ports: OrgApproverPort, PrStatusEventPublisher, NotificationEventPublisher
Kafka: PrSubmittedEvent consumer, approval result producers
Controllers matching approval-service.openapi.yaml
Angular approval inbox/detail/admin rules pages
Unit tests domain/application and BPMN smoke tests
```

---

## 7. MVP Acceptance Checklist

```
[ ] PrSubmittedEvent creates approval process and first task.
[ ] Approval rule matrix covers baseline value ranges.
[ ] Requester is never assigned as approver.
[ ] Approver can list inbox and open task detail.
[ ] Approver can approve/reject/request changes.
[ ] Final approval/rejection publishes event to PR service.
[ ] No HTTP DELETE.
[ ] POST/PUT/PATCH require Idempotency-Key.
[ ] Admin can deactivate rule via PATCH, not DELETE.
```
