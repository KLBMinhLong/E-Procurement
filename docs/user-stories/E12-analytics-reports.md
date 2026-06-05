# E12 Analytics & Reports

## Scope

E12 cung cấp dashboard/KPI/report cho quản lý và finance. Foundation đầu tiên dựng `analytics-service`, executive dashboard read model, projection ingestion từ các business event đã có contract rõ ràng, KPI API contract, async report job storage, Jasper template rendering cho các report đã có dataset, và các lát cắt dataset nâng cao sẽ hoàn thiện tiếp theo.

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
- Consume `procurement.pr.submitted`, `procurement.po.issued`, `finance.invoice.matched`, and `approval.sla.breached`.
- Store event processing records by `eventId` so Kafka replay does not duplicate projections.
- Store submitted PR, issued PO, PO line, matched invoice, and approval SLA breach fact rows in schema `analytics`.
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
- A scheduled worker claims `QUEUED` jobs, marks them `PROCESSING`, renders local PDF/XLSX files, then marks jobs `COMPLETED` or `FAILED`.
- `GET /api/v1/reports/jobs/{jobId}/download` streams the generated file for the requesting user after completion.
- Download returns a business error while the file is not ready, expired, missing, or not owned by the requester.
- Current renderer includes projection-backed summary rows for `PO_SUMMARY`, `PR_SUMMARY`, `SLA_COMPLIANCE`, `THREE_WAY_MATCH`, `CYCLE_TIME_ANALYSIS`, and `VENDOR_SCORECARD`.
- Projection-backed report datasets honor supported export filters: `fromDate`/`toDate`, `fiscalYear`/`quarter`, `vendorId`, and `categoryCode` where the source projection has matching columns.
- PDF exports use JasperReports templates with report-type-specific titles, metadata/filter summaries, and metric tables.
- XLSX exports use Apache POI workbooks with styled sections, frozen table headers, filters, and fixed business-friendly column widths.
- Unsupported report types explicitly render foundation rows that identify the missing projection contract.
- Filters requiring missing source fields such as `departmentId`/`status` remain follow-up and are not exposed in the current report export API contract.

### E12-US-006 KPI API Foundation

As a report viewer, I want stable KPI endpoints for cycle time and approval SLA so that dashboards can integrate with analytics-service while source projections mature.

Acceptance:
- `GET /api/v1/kpi/cycle-time` requires `REPORT_VIEW`.
- Required filters: `from_date`, `to_date`; optional filter: `department_id`.
- Response follows OpenAPI shape with `avgCycleHours`, `medianCycleHours`, `p95CycleHours`, `target`, `byPriority`, and `trend`.
- Cycle-time reads `analytics.pr_submitted_projections` joined to `analytics.po_issued_projections` by `prId` to expose average, median, p95, priority breakdown, and weekly trend for PR-to-PO hours.
- Cycle-time returns zero/empty metrics with target `48.00` when no linked PR submitted and PO issued projections exist in the requested period.
- `GET /api/v1/kpi/sla-compliance` requires `REPORT_VIEW`.
- Required filters: `from_date`, `to_date`.
- SLA response reads `analytics.approval_sla_breach_projections` and exposes overdue count plus average breached action hours by approver role.
- Overall and row compliance percentages stay `0` until analytics has a completion/on-time denominator event or projection.
- Worst approver rows expose temporary masked approver identifiers (`approver:{uuid-prefix}`) until IAM/user name projection is available.
- Both endpoints validate `from_date <= to_date` and return `VAL_001` for invalid ranges.

## Next Coding Slices

1. Projection-backed datasets for the remaining report types beyond `PO_SUMMARY`, `PR_SUMMARY`, `SLA_COMPLIANCE`, `THREE_WAY_MATCH`, `CYCLE_TIME_ANALYSIS`, and `VENDOR_SCORECARD`.
2. Source contracts for `departmentId`/`status` report filters, RFQ savings, GR pending metrics, and department spend.
3. Docker/Flyway/Kafka end-to-end verification for analytics projections and report worker outputs.
