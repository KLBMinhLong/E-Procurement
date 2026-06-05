# E12 Analytics & Reports

## Scope

E12 cung cấp dashboard/KPI/report cho quản lý và finance. Foundation đầu tiên dựng `analytics-service`, executive dashboard read model, projection ingestion từ các business event đã có contract rõ ràng, KPI API contract, và async report job storage; KPI nâng cao và Jasper export làm ở các slice sau.

## User Stories

### E12-US-001 Executive Dashboard Foundation

As a C-level user, I want to view executive procurement KPIs so that I can monitor company spend, budget utilization, approval SLA, and top vendors.

Acceptance:
- `GET /api/v1/dashboard/executive` requires `REPORT_VIEW`.
- Optional filters: `fiscal_year`, `quarter`.
- Response follows analytics OpenAPI shape: KPI cards, spend by department/category, monthly trend, approval SLA, top vendors, and `cachedAt`.
- Data comes from `analytics` read-model snapshots, not direct cross-service table reads.
- If no fresh snapshot exists, API returns an empty dashboard with zero KPIs instead of failing the shell.
- Dashboard snapshot TTL defaults to 5 minutes.

### E12-US-002 Analytics Read Model Bootstrap

As a platform engineer, I want analytics-service to own its read-model database so that future consumers can populate dashboard/report projections without violating service DB boundaries.

Acceptance:
- `analytics-service` runs on port `8087`.
- PostgreSQL has `db_analytics` and schema `analytics`.
- Flyway V1 creates executive dashboard snapshot tables with audit fields, soft delete fields, `TIMESTAMPTZ`, `NUMERIC(19,4)`, and active indexes.
- Docker Compose, `.env.example`, root Maven, and Docker builders know the new module.

### E12-US-003 Executive Dashboard Projection Ingestion

As an analytics service, I want to consume business events into my own read model so that executive dashboard data is populated without cross-service database reads.

Acceptance:
- Consume `procurement.po.issued`, `finance.invoice.matched`, and `approval.sla.breached`.
- Store event processing records by `eventId` so Kafka replay does not duplicate projections.
- Store issued PO, PO line, matched invoice, and approval SLA breach fact rows in schema `analytics`.
- Refresh annual and quarterly executive dashboard snapshots after a supported event is recorded.
- Snapshot metrics currently populated from available contracts: total issued PO spend, approved PR count from issued PO facts, category spend, monthly trend, top vendors, SLA breach count, and SLA breach cycle hours.
- RFQ savings and department spend remain zero/empty until source events provide baseline price and department allocation data.

### E12-US-004 Role Dashboard API Foundation

As manager, purchasing, and requester users, I want stable dashboard API contracts so that the frontend can route each role to a real backend endpoint while data projections are completed incrementally.

Acceptance:
- `GET /api/v1/dashboard/manager` requires `BUDGET_VIEW_OWN_DEPT` or `BUDGET_VIEW_ALL`.
- `GET /api/v1/dashboard/purchasing` requires `PO_VIEW_ALL`.
- `GET /api/v1/dashboard/requester` requires `PR_VIEW_OWN`.
- Responses follow the analytics OpenAPI shape for each role dashboard.
- Until role-specific read models exist, APIs return zero/empty structured data instead of failing.
- Purchasing dashboard populates issued PO count, issued PO total, matched invoice count, and vendor pending-order rows from analytics-owned projection tables.
- Manager/requester dashboard data and RFQ/GR-specific purchasing metrics remain follow-up until source projections are available.

### E12-US-005 Async Report Export Job Foundation

As a report user, I want to create and poll async report jobs so that heavy PDF/Excel generation can be handled outside the request thread.

Acceptance:
- `POST /api/v1/reports/export` requires `REPORT_EXPORT` and `Idempotency-Key`.
- A new request stores a `QUEUED` job in `analytics.report_export_jobs`.
- Replaying the same `Idempotency-Key` for the same user returns the existing job with `Idempotency-Replayed: true`.
- `GET /api/v1/reports/jobs/{jobId}` returns the current job status for the requesting user.
- `GET /api/v1/reports/jobs/{jobId}/download` returns a business error until an export worker produces a file.

### E12-US-006 KPI API Foundation

As a report viewer, I want stable KPI endpoints for cycle time and approval SLA so that dashboards can integrate with analytics-service while source projections mature.

Acceptance:
- `GET /api/v1/kpi/cycle-time` requires `REPORT_VIEW`.
- Required filters: `from_date`, `to_date`; optional filter: `department_id`.
- Response follows OpenAPI shape with `avgCycleHours`, `medianCycleHours`, `p95CycleHours`, `target`, `byPriority`, and `trend`.
- Because current events do not carry a reliable PR submitted/created timestamp for PR to PO cycle time, cycle-time returns a zero/empty foundation response with target `48.00` until PR lifecycle projection is added.
- `GET /api/v1/kpi/sla-compliance` requires `REPORT_VIEW`.
- Required filters: `from_date`, `to_date`.
- SLA response reads `analytics.approval_sla_breach_projections` and exposes overdue count plus average breached action hours by approver role.
- Overall and row compliance percentages stay `0` until analytics has a completion/on-time denominator event or projection.
- Worst approver rows expose temporary masked approver identifiers (`approver:{uuid-prefix}`) until IAM/user name projection is available.
- Both endpoints validate `from_date <= to_date` and return `VAL_001` for invalid ranges.

## Next Coding Slices

1. Jasper/PDF/Excel worker implementation for queued report jobs.
2. PR lifecycle projection for real PR to PO cycle-time metrics.
3. Approval completion/on-time projection for true SLA compliance percentages.
4. Manager/requester dashboard data projections and RFQ/GR-specific purchasing metrics.
