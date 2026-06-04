# E12 Analytics & Reports

## Scope

E12 cung cấp dashboard/KPI/report cho quản lý và finance. Foundation đầu tiên dựng `analytics-service`, executive dashboard read model, và projection ingestion từ các business event đã có contract rõ ràng; KPI nâng cao và Jasper export làm ở các slice sau.

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

## Next Coding Slices

1. Executive dashboard foundation.
2. Executive dashboard event projection ingestion.
3. Manager, purchasing, requester dashboards.
4. KPI endpoints: cycle time and SLA compliance.
5. Async report export jobs and Jasper templates.
