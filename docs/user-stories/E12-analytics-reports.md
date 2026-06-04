# E12 Analytics & Reports

## Scope

E12 cung cấp dashboard/KPI/report cho quản lý và finance. Foundation đầu tiên chỉ dựng `analytics-service` và executive dashboard read model để frontend/gateway có API thật; event ingestion, KPI nâng cao và Jasper export làm ở các slice sau.

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

## Next Coding Slices

1. Executive dashboard foundation.
2. Event consumers/projection jobs to populate analytics snapshots from finance/vendor/approval/inventory events.
3. Manager, purchasing, requester dashboards.
4. KPI endpoints: cycle time and SLA compliance.
5. Async report export jobs and Jasper templates.
