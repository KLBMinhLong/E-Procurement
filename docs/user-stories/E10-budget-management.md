# E10 Budget Management
## User Stories và Use Cases

---

## 1. Epic Goal

Triển khai `finance-service` phần ngân sách để thay `BudgetCheckPort` fallback trong PR service, kiểm soát ngân sách theo phòng ban/năm/quý/GL account, ghi nhận commitment khi PR đi qua lifecycle, hỗ trợ cảnh báo vượt ngân sách, override và điều chuyển ngân sách.

Mục tiêu coding gần nhất không phải làm toàn bộ Finance/Invoice/Payment, mà là dựng budget foundation đủ chắc để luồng:

```
PR submit -> Finance budget check -> Approval -> PR approved/rejected/cancelled -> Finance commit/release budget
```

chạy bằng service thật.

---

## 2. Actors

| Actor | Vai trò |
|---|---|
| Requester | Submit PR và nhận kết quả budget check trong PR detail |
| PR Service | Gọi Finance để check budget trước khi submit |
| Finance Service | Tính available/committed/spent, ghi budget transactions |
| Finance Manager | Xem dashboard, duyệt override, điều chuyển ngân sách |
| Department Manager | Theo dõi ngân sách phòng ban |
| Notification Service | Nhận `finance.budget.warning` hoặc `finance.budget.exceeded` |

---

## 3. User Stories

| ID | Story | Priority | API/Event Reference |
|---|---|---|---|
| E10-US-001 | Là Finance Manager, tôi muốn xem danh sách budget theo phòng ban/năm/quý/GL để kiểm soát kế hoạch chi. | P1 | `GET /budgets` |
| E10-US-002 | Là Department Manager, tôi muốn xem dashboard budget real-time gồm allocated/committed/spent/available. | P1 | `GET /budgets/{id}/dashboard` |
| E10-US-003 | Là PR Service, tôi muốn check budget qua Finance Service khi submit PR thay vì fallback adapter. | P1 | Internal budget check contract |
| E10-US-004 | Là Finance Service, tôi muốn tentative commit ngân sách khi PR submit để tránh overspend do nhiều PR đồng thời. | P1 | `procurement.pr.submitted` |
| E10-US-005 | Là Finance Service, tôi muốn firm commit ngân sách khi PR được approve hoàn toàn. | P1 | `procurement.pr.approved` |
| E10-US-006 | Là Finance Service, tôi muốn release commitment khi PR bị reject/cancel hoặc requester cần sửa lại. | P1 | `procurement.pr.rejected`, `procurement.pr.cancelled`, `procurement.pr.changes-requested` |
| E10-US-007 | Là Finance Manager, tôi muốn nhận cảnh báo khi budget còn dưới 20% hoặc không đủ cho PR. | P1 | `finance.budget.warning`, `finance.budget.exceeded` |
| E10-US-008 | Là Finance Manager, tôi muốn phê duyệt budget override khi nghiệp vụ cho phép vượt ngân sách. | P2 | `PATCH /budgets/{id}/override-approval` |
| E10-US-009 | Là Finance Manager, tôi muốn điều chuyển ngân sách giữa budget lines để cân đối kế hoạch. | P2 | `PATCH /budgets/{id}/transfer` |
| E10-US-010 | Là Admin/Finance, tôi muốn có seed/demo active budgets để test end-to-end trên local. | P1 | Flyway seed/local profile |

---

## 4. Use Cases

### E10-UC-001: ListBudgetsUseCase

**Permission:** `BUDGET_VIEW_OWN_DEPT` hoặc `BUDGET_VIEW_ALL`.

**Main flow:**

```
1. Controller nhận filter departmentId, fiscalYear, quarter, status, page, size, sort.
2. UseCase resolve view scope theo permission.
3. Repository query finance.budgets với WHERE is_deleted = false.
4. Tính committed/spent/available từ budget_transactions.
5. Sort qua whitelist, không nối raw ORDER BY.
6. Return Page<BudgetDashboard>.
```

**Acceptance criteria:**

```
[ ] User chỉ thấy budget phòng ban của mình nếu chỉ có BUDGET_VIEW_OWN_DEPT.
[ ] BUDGET_VIEW_ALL được xem toàn bộ department.
[ ] Money trả về JSON string 4 decimals.
[ ] Pagination meta đúng API convention.
```

### E10-UC-002: GetBudgetDashboardUseCase

**Permission:** `BUDGET_VIEW_OWN_DEPT` hoặc `BUDGET_VIEW_ALL`.

**Main flow:**

```
1. Load budget by id, is_deleted = false.
2. Verify actor có quyền xem department của budget.
3. Aggregate transactions:
   - committed = SUM(COMMIT_TENTATIVE + COMMIT_FIRM - RELEASE)
   - spent = SUM(SPEND)
   - available = allocated - committed - spent
4. Tính availablePercent, burnRatePerMonth, forecastExhaustedAt.
5. Cache dashboard Redis key budget:{deptId}:{year}:{quarter} TTL 5 phút.
6. Return BudgetDashboard.
```

**Acceptance criteria:**

```
[ ] Available = allocated - committed - spent.
[ ] Không return null nếu budget không tồn tại; throw FIN_* not found.
[ ] Cache miss đọc DB, cache hit không làm sai permission scope.
[ ] NUMERIC(19,4)/BigDecimal được dùng xuyên suốt.
```

### E10-UC-003: CheckBudgetUseCase

**Trigger:** PR service gọi sync trước khi submit PR.

**Proposed internal contract cần thêm vào `finance-service.openapi.yaml` trước hoặc cùng lúc code:**

```
GET /internal/budgets/check
Headers: X-Internal-Api-Key, X-Request-ID
Query: department_id, fiscal_year, amount, currency, gl_account_code optional
Response data: allocated, committed, spent, available, status PASS|WARNING|FAIL, warningMessage
```

**Main flow:**

```
1. Validate X-Internal-Api-Key.
2. Resolve active budget by departmentId, fiscalYear, quarter/current period, glAccountCode.
3. Calculate available = allocated - committed - spent.
4. Calculate availableAfter = available - requestAmount.
5. Return:
   - PASS nếu availableAfter >= 20% allocated.
   - WARNING nếu availableAfter >= 0 và < 20% allocated.
   - FAIL nếu availableAfter < 0.
6. Publish finance.budget.warning/exceeded only when policy says business alert is needed; budget check response itself stays sync.
```

**Alternate/error flows:**

```
- Không có active budget -> FIN_BUDGET_NOT_FOUND hoặc FAIL theo policy đã chốt.
- Currency mismatch -> FIN_CURRENCY_MISMATCH.
- Amount <= 0 -> validation error.
```

**Acceptance criteria:**

```
[ ] PR service có thể thay FallbackBudgetCheckAdapter bằng FinanceBudgetCheckAdapter.
[ ] Check budget là read-only, không tạo transaction.
[ ] Không dùng POST cho read-only check chỉ vì cần nhiều tham số.
[ ] Result map tương thích `BudgetCheckResult` hiện có của PR service.
```

### E10-UC-004: TentativeCommitBudgetUseCase

**Trigger:** Consume `procurement.pr.submitted`.

**Main flow:**

```
1. Check event idempotency by eventId.
2. Resolve active budget for PR department/year/GL.
3. Ensure no existing COMMIT_TENTATIVE transaction for reference PURCHASE_REQUEST/prId.
4. Insert immutable budget_transaction type COMMIT_TENTATIVE.
5. Recalculate budget state.
6. Publish finance.budget.warning nếu remaining < 20%.
```

**Acceptance criteria:**

```
[ ] Duplicate event does not double commit.
[ ] budget_transactions immutable, no soft delete.
[ ] Commit is written after PR submitted so concurrent PR submissions see committed amount.
[ ] Failure is logged with [EXCEPTION] and routed to retry/DLQ strategy.
```

### E10-UC-005: FirmCommitBudgetUseCase

**Trigger:** Consume `procurement.pr.approved`.

**Main flow:**

```
1. Check event idempotency by eventId.
2. Find tentative commitment by PR id.
3. If tentative exists, convert by inserting COMMIT_FIRM and compensating RELEASE for tentative if needed by ledger model.
4. If tentative missing but PR approved event is valid, create COMMIT_FIRM once.
5. Publish finance.budget.warning if budget is now near exhaustion.
```

**Acceptance criteria:**

```
[ ] Approval does not duplicate committed value.
[ ] Ledger remains auditable; do not update/delete old transaction rows.
[ ] PR approved event can arrive once and produce exactly one firm commitment effect.
```

### E10-UC-006: ReleaseBudgetCommitmentUseCase

**Trigger:** Consume `procurement.pr.rejected`, `procurement.pr.cancelled`, `procurement.pr.changes-requested`.

**Main flow:**

```
1. Check event idempotency.
2. Find active tentative/firm commitment by PR reference.
3. Insert RELEASE transaction for committed amount still held.
4. Return no-op if no commitment exists.
```

**Acceptance criteria:**

```
[ ] Reject/cancel/changes-requested releases budget exactly once.
[ ] No-op path is safe for events produced before finance-service was enabled.
[ ] Release transaction references original PR id.
```

### E10-UC-007: ApproveBudgetOverrideUseCase

**Permission:** `BUDGET_OVERRIDE`.

**Endpoint:** `PATCH /budgets/{id}/override-approval`.

**Main flow:**

```
1. Check Idempotency-Key.
2. Load budget and PR context.
3. Validate overrideAmount and overrideReason length.
4. Enforce override threshold policy:
   - Manager/Finance approval for normal override.
   - CEO/CFO approval if overage exceeds configured percentage.
5. Persist override approval audit record.
6. Return envelope and optionally publish finance.budget.exceeded.
```

**Acceptance criteria:**

```
[ ] Override does not mutate allocated amount silently.
[ ] Override is auditable by actor, PR id, amount, reason.
[ ] 30%+ overage follows explicit approval path, not hidden bypass.
```

### E10-UC-008: TransferBudgetUseCase

**Permission:** `BUDGET_TRANSFER_APPROVE`.

**Endpoint:** `PATCH /budgets/{id}/transfer`.

**Main flow:**

```
1. Check Idempotency-Key.
2. Load source and target budget lines.
3. Validate same currency/fiscal year unless policy allows cross-period transfer.
4. Validate source available >= amount.
5. Insert budget transfer record plus balanced ledger transactions (`TRANSFER_OUT` and `TRANSFER_IN`) and adjust source/target allocated amounts atomically.
6. Return updated source/target dashboard.
```

**Acceptance criteria:**

```
[ ] Transfer is not allowed from CLOSED budget.
[ ] Source and target transactions are balanced.
[ ] Reason is required and audit logged.
```

---

## 5. Frontend Use Cases

```
BudgetDashboardPage: list budgets, filter department/year/quarter/status, show allocated/committed/spent/available.
BudgetDetailPage: real-time dashboard, warning state, transaction timeline.
BudgetOverrideModal: approve override with reason and amount.
BudgetTransferModal: move amount source -> target with reason.
```

Acceptance:

```
[ ] Mọi text dùng translate key.
[ ] `ep-amount` dùng cho tất cả money.
[ ] Dashboard dùng `ep-stat-card`/`ep-table`, không tạo landing page.
[ ] POST/PUT/PATCH/PATCH-aligned actions giữ Idempotency-Key ổn định khi retry.
```

---

## 6. Technical Deliverables

```
services/finance-service Maven module
Dockerfile + docker-compose service on port 8084
Flyway migrations for finance.budgets, finance.budget_transactions, finance.budget_transfers
Domain: Budget, BudgetTransaction, BudgetTransfer, Money
UseCases: ListBudgets, GetBudgetDashboard, CheckBudget, TentativeCommit, FirmCommit, ReleaseCommitment, ApproveOverride, TransferBudget
Ports: PrEventConsumer, BudgetWarningPublisher, IdempotencyService, InternalApiKeyGuard
Adapters: MyBatis repositories, Kafka consumers/producers, Redis dashboard cache
PR service adapter: FinanceBudgetCheckAdapter replacing fallback when configured
OpenAPI updates for internal budget check and any PATCH alignment
Unit tests for budget calculation, idempotent event handling, commit/release ledger
```

---

## 7. First Coding Slice Recommendation

```
1. Scaffold finance-service module with Log4j2, Flyway, MyBatis, Redis/Kafka config and Docker resource limit.
2. Create budget schema migration plus local seed active budgets.
3. Implement Budget domain + CheckBudgetUseCase + internal GET /internal/budgets/check.
4. Add PR FinanceBudgetCheckAdapter behind feature flag; fallback remains local/dev safety net.
5. Verify mvn -pl services/finance-service,services/purchase-request-service test.
```

This slice directly removes the highest-risk fake dependency in current PR submit flow without prematurely building PO/invoice/payment.

---

## 8. Readiness Checklist

```
[ ] Finance OpenAPI internal budget check contract added.
[x] Decision made on POST vs PATCH for override/transfer actions: use `PATCH`.
[ ] Error codes FIN_* for budget not found, insufficient budget, currency mismatch, duplicate commitment documented.
[ ] Local seed budget data exists for requester department used in demo.
[ ] Budget event idempotency key strategy is eventId-based.
```
