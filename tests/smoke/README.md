# E15-A Runtime/API Smoke Pack

Smoke pack nay kiem tra baseline runtime qua Docker, gateway, auth, Kafka, DB va API contract hien co.

## Scope

- Docker compose service health: infra + gateway + backend services.
- HTTP health: gateway va tung service `/actuator/health`.
- API flow: login -> create PR -> submit -> approve -> manual PO -> send PO -> GR -> invoice -> match -> approve -> payment -> notification count -> analytics dashboard/KPI.

RFQ award khong nam trong smoke flow mac dinh vi E15-A chon nhanh manual PO path theo contract "RFQ/award hoac manual PO".

## Seed Actors

Flyway seeds them actor local/dev cho smoke:

| Username | Role | Purpose |
|---|---|---|
| `requester` | `REQUESTER` | Create and submit PR |
| `manager` | `MANAGER` | Approve one-step PR |
| `purchasing` | `PURCHASING` | Manual PO and send-to-vendor |
| `warehouse` | `WAREHOUSE` | Goods receipt |
| `accountant` | `ACCOUNTANT` | Invoice, match, approve, payment |

Set `E15_SMOKE_PASSWORD` to the documented local dev password before running the mutation flow.

## Run

```powershell
$env:PR_SERVICE_PORT = '58082'
$env:APPROVAL_SERVICE_PORT = '58083'
$env:FINANCE_SERVICE_PORT = '58084'
$env:GATEWAY_PORT = '58080'
$env:PR_INTEGRATION_FALLBACK_ENABLED = 'false'
$env:APPROVAL_INTEGRATION_FALLBACK_ENABLED = 'false'
$env:OTEL_TRACES_EXPORTER = 'none'
$env:IAM_SERVICE_URL = 'http://iam-service:8081'
$env:PR_SERVICE_URL = 'http://pr-service:8082'
$env:APPROVAL_SERVICE_URL = 'http://approval-service:8083'
$env:FINANCE_SERVICE_URL = 'http://finance-service:8084'
$env:INVENTORY_SERVICE_URL = 'http://inventory-service:8085'
$env:VENDOR_SERVICE_URL = 'http://vendor-service:8086'
$env:ANALYTICS_SERVICE_URL = 'http://analytics-service:8087'
$env:NOTIFICATION_SERVICE_URL = 'http://notification-service:8088'

docker compose up -d --build `
  postgres redis kafka kafka-init keycloak `
  iam-service finance-service pr-service approval-service `
  vendor-service inventory-service notification-service analytics-service nginx-gateway

$env:E15_GATEWAY_URL = 'http://localhost:58080'
$env:PR_SERVICE_URL = 'http://localhost:58082'
$env:APPROVAL_SERVICE_URL = 'http://localhost:58083'
$env:FINANCE_SERVICE_URL = 'http://localhost:58084'
$env:E15_SMOKE_PASSWORD = '<local-dev-password>'
node .\tests\smoke\e15-runtime-smoke.mjs
```

Useful modes:

```powershell
node .\tests\smoke\e15-runtime-smoke.mjs --health-only
node .\tests\smoke\e15-runtime-smoke.mjs --flow-only
node .\tests\smoke\e15-runtime-smoke.mjs --skip-docker
```

Useful environment overrides:

| Variable | Default |
|---|---|
| `E15_GATEWAY_URL` | `http://localhost:8080` |
| `PR_SERVICE_URL` | `http://localhost:8082` |
| `APPROVAL_SERVICE_URL` | `http://localhost:8083` |
| `FINANCE_SERVICE_URL` | `http://localhost:8084` |
| `E15_SMOKE_PASSWORD` | Required for mutation flow |
| `E15_VENDOR_ID` | `80000000-0000-0000-0000-000000000101` |
| `E15_WAREHOUSE_ID` | `81000000-0000-4000-8000-000000000001` |
| `E15_POLL_TIMEOUT_MS` | `180000` |
| `E15_POLL_INTERVAL_MS` | `3000` |
| `E15_REQUEST_TIMEOUT_MS` | `30000` |

## Notes

- Existing Docker volumes are fine; new Flyway migrations apply when the affected services start.
- The command above uses alternate host ports to avoid collisions with locally running Java services, while containers still call each other by Docker DNS.
- `OTEL_TRACES_EXPORTER=none` keeps smoke runs independent from the optional monitoring profile. Compose defaults still use OTLP when this override is not set.
- The script prints step status only. It does not print cookies or passwords.
- Every POST/PATCH request sends an `Idempotency-Key`.
