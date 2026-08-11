<p align="center">
  <h1 align="center">🏢 eProcure Enterprise</h1>
  <p align="center">
    <strong>End-to-end Enterprise Procurement Management System</strong>
  </p>
  <p align="center">
    Digitize the entire internal procurement lifecycle — from purchase requests to payment settlement — with multi-level approvals, real-time budget control, and full audit trails.
  </p>
  <p align="center">
    <img src="https://img.shields.io/badge/Java-17-ED8B00?logo=openjdk&logoColor=white" alt="Java 17" />
    <img src="https://img.shields.io/badge/Spring_Boot-3.2-6DB33F?logo=springboot&logoColor=white" alt="Spring Boot 3.2" />
    <img src="https://img.shields.io/badge/Angular-21-DD0031?logo=angular&logoColor=white" alt="Angular 21" />
    <img src="https://img.shields.io/badge/PostgreSQL-15-4169E1?logo=postgresql&logoColor=white" alt="PostgreSQL 15" />
    <img src="https://img.shields.io/badge/Kafka-7.6-231F20?logo=apachekafka&logoColor=white" alt="Kafka" />
    <img src="https://img.shields.io/badge/Docker-Compose-2496ED?logo=docker&logoColor=white" alt="Docker Compose" />
  </p>
</p>

---

## 📑 Table of Contents

- [Overview](#-overview)
- [Architecture](#-architecture)
- [Tech Stack](#-tech-stack)
- [Service Map](#-service-map)
- [Project Structure](#-project-structure)
- [Prerequisites](#-prerequisites)
- [Quick Start](#-quick-start)
- [Service Endpoints](#-service-endpoints)
- [Dev Users](#-dev-users)
- [Key Design Decisions](#-key-design-decisions)
- [Security Model](#-security-model)
- [Approval Matrix](#-approval-matrix)
- [API Conventions](#-api-conventions)
- [Observability](#-observability)
- [Testing](#-testing)
- [Documentation Index](#-documentation-index)
- [Contributing](#-contributing)
- [License](#-license)

---

## 🎯 Overview

**eProcure** is a microservices-based enterprise procurement platform designed for organizations with **200+ employees**. It replaces manual email/Excel/verbal-approval workflows with a fully automated, auditable system.

### Core Business Flow

```
📝 Employee creates PR  →  ✅ Multi-level approval (Camunda BPMN)  →  📋 Purchasing creates PO
        ↓                                                                        ↓
   Budget check                                                         Vendor delivers goods
   Inventory check                                                              ↓
                                                                   📦 Warehouse receives (GR)
                                                                              ↓
                                                                   💰 3-Way Match (PO + GR + Invoice)
                                                                              ↓
                                                                      💳 Payment settlement
```

### Business KPIs

| Metric | Before | Target |
|---|---|---|
| PR → PO cycle time | 5–7 days | < 2 days |
| Real-time budget overrun detection | 0% | 100% |
| Actions with full audit trail | ~30% | 100% |
| Duplicate purchases (items already in stock) | N/A | < 5% |
| Approvals within SLA | N/A | > 85% |

---

## 🏗 Architecture

eProcure follows a **Microservices + Clean Architecture + Event-Driven** pattern.

```
                         ┌─────────────────────────┐
                         │    Angular Frontend      │
                         │  (NGINX, HTTPS, SCSS)    │
                         └────────────┬─────────────┘
                                      │ HTTPS / WSS
                         ┌────────────▼─────────────┐
                         │     NGINX API Gateway     │
                         │  (rate limit, routing,    │
                         │   X-Api-Key injection)    │
                         └──┬────┬────┬────┬────┬───┘
                            │    │    │    │    │
           ┌────────────────┘    │    │    │    └────────────────┐
           │              ┌─────┘    │    └─────┐               │
  ┌────────▼────────┐ ┌───▼─────────▼─────────┐ │  ┌────────────▼──────┐
  │  IAM Service    │ │   PR Service           │ │  │ Admin Service     │
  │  :8081          │ │   :8082                │ │  │ :8089             │
  │  Auth, RBAC,    │ │   Purchase Request,    │ │  │ System Config,    │
  │  Org Chart      │ │   Budget Check         │ │  │ Audit Log         │
  └────────┬────────┘ └──────────┬─────────────┘ │  └───────────────────┘
           │                     │               │
           │          ┌──────────▼──────────┐ ┌──▼────────────────┐
           │          │  Approval Engine    │ │ Vendor Service    │
           │          │  :8083             │ │ :8086             │
           │          │  Camunda BPMN,     │ │ Vendor Master,    │
           │          │  SLA, Escalation   │ │ RFQ, AVL          │
           │          └──────────┬─────────┘ └──────────┬────────┘
           │                     │                      │
           │          ┌──────────▼──────────────────────▼────────┐
           │          │           Kafka Event Bus                │
           │          └───┬──────────┬──────────┬───────────┬───┘
           │              │          │          │           │
  ┌────────▼──────────┐ ┌─▼──────────▼─┐ ┌─────▼──────┐ ┌─▼──────────────┐
  │ Finance Service   │ │ Inventory    │ │ Analytics  │ │ Notification   │
  │ :8084             │ │ Service      │ │ Service    │ │ Service        │
  │ Budget, PO,       │ │ :8085        │ │ :8087      │ │ :8088          │
  │ Invoice, Payment  │ │ Stock, GR    │ │ Dashboard, │ │ Email, In-app, │
  └───────────────────┘ │ Catalog      │ │ KPI, Rpt   │ │ WebSocket      │
                        └──────────────┘ └────────────┘ └────────────────┘

  ┌─────────────────────────────────────────────────────────────────────┐
  │                     Infrastructure Layer                            │
  │  PostgreSQL 15 │ Redis 7 │ Keycloak │ Kafka │ JasperReports        │
  │  Prometheus │ Grafana │ Loki │ Tempo │ OpenTelemetry               │
  └─────────────────────────────────────────────────────────────────────┘
```

### Clean Architecture (per service)

```
  presentation/        →  application/        →  domain/         ←  infrastructure/
  ─────────────            ──────────────          ──────────          ────────────────
  Controller               UseCase                POJO (pure)        RepositoryImpl
  Request/Response DTO      Command/Port           Entity             MyBatis Mapper
  @PreAuthorize            @Transactional          ValueObject        Kafka Producer
                                                   Repository (if)    Redis Adapter
```

> **Rule:** Domain layer is **pure POJO** — zero Spring/Jakarta/MyBatis annotations.

---

## 🛠 Tech Stack

### Backend

| Layer | Technology | Version |
|---|---|---|
| Language | Java | 17 LTS |
| Framework | Spring Boot | 3.2.x |
| SQL Mapping | MyBatis | 3.0.4 |
| DB Migration | Flyway | Version-based |
| Connection Pool | HikariCP | Default |
| Auth Provider | Keycloak | 24.0 (Custom User Storage SPI) |
| Cache | Redis | 7 (Lettuce) |
| Message Broker | Apache Kafka (KRaft) | 7.6.1 |
| Workflow Engine | Camunda BPMN | 7.21 |
| Reporting | JasperReports | PDF + Excel |
| Realtime | WebSocket (STOMP) | — |
| Email | Brevo SMTP | Template-based |

### Frontend

| Layer | Technology | Version |
|---|---|---|
| Framework | Angular | 21.x |
| Styling | SCSS + CSS Custom Properties | — |
| Icons | Lucide Angular | 1.16+ |
| i18n | ngx-translate | 17.x (VI / EN) |
| Charts | Chart.js + ng2-charts | 4.5 / 10.0 |
| WebSocket | @stomp/stompjs | 7.3+ |
| Build Tool | Angular CLI | 21.x |
| Test Runner | Vitest | 4.x |

### Infrastructure

| Component | Technology | Notes |
|---|---|---|
| Database | PostgreSQL | 15+ Alpine, multi-DB, multi-schema |
| Cache | Redis | 7 Alpine, LRU eviction |
| Message Bus | Kafka | KRaft mode (no Zookeeper) |
| Identity | Keycloak | Custom provider — credential verification only |
| Gateway | NGINX | Reverse proxy, rate limiting, CORS |
| Containers | Docker + Docker Compose | Full local stack |
| CI/CD | Jenkins | Pipeline as code |

### Observability

| Component | Technology | Notes |
|---|---|---|
| Logging | Log4j2 | Custom layout, colored terminal output |
| Tracing | OpenTelemetry → Tempo | Distributed tracing across services |
| Metrics | Micrometer → Prometheus → Grafana | Pre-built dashboards |
| Log Aggregation | Log4j2 → Loki → Grafana | Centralized log exploration |

---

## 🗺 Service Map

| Service | Port | Database | Schema | Description |
|---|---|---|---|---|
| **iam-service** | 8081 | `db_iam` | `iam` | Authentication, RBAC, Org Chart, Sessions, 2FA, OAuth |
| **purchase-request-service** | 8082 | `db_procurement` | `pr` | PR lifecycle, budget/inventory check, catalog |
| **approval-service** | 8083 | `db_procurement` | `approval` | Camunda BPMN workflow, SLA, escalation, delegation |
| **finance-service** | 8084 | `db_finance` | `finance` | Budget, PO, Invoice, 3-way match, Payments |
| **inventory-service** | 8085 | `db_inventory` | `inventory` | Catalog, Stock, Goods Receipt, Issue-out |
| **vendor-service** | 8086 | `db_vendor` | `vendor` | Vendor master, RFQ, Quotes, AVL |
| **analytics-service** | 8087 | `db_analytics` | `analytics` | Executive/Manager/Purchasing dashboards, KPI, Reports |
| **notification-service** | 8088 | `db_notification` | `notification` | Email (Brevo), In-app, WebSocket realtime |
| **admin-service** | 8089 | `db_audit` | `audit` | System config, Audit log, Service health |

---

## 📁 Project Structure

```
E-Procurement/
├── AGENTS.md                    # AI Agent orchestration playbook
├── README.md                    # ← You are here
├── pom.xml                      # Maven parent POM (multi-module)
├── docker-compose.yml           # Full local infrastructure
├── .env.example                 # Environment variables template
│
├── services/                    # Backend microservices (Java/Spring Boot)
│   ├── iam-service/
│   ├── purchase-request-service/
│   ├── approval-service/
│   ├── finance-service/
│   ├── inventory-service/
│   ├── vendor-service/
│   ├── analytics-service/
│   ├── notification-service/
│   └── admin-service/
│
├── frontend/
│   └── eprocure-web/            # Angular 21 SPA
│
├── infra/                       # Infrastructure configs
│   ├── postgres/                # Init scripts (multi-DB creation)
│   ├── redis/
│   ├── kafka/                   # Topic creation scripts
│   ├── keycloak/                # Realm + Custom User Storage SPI
│   ├── nginx/                   # Gateway configs (dev + prod)
│   ├── prometheus/
│   ├── grafana/
│   ├── loki/
│   ├── tempo/
│   └── otel/                    # OpenTelemetry config
│
├── docs/                        # Project documentation
│   ├── PROJECT_BRIEF.md         # Business requirements & scope
│   ├── DOMAIN_MODEL.md          # Entities, aggregates, domain events
│   ├── DATABASE_SCHEMA.md       # Full DDL, index strategy
│   ├── CODING_GUIDE.md          # Conventions, naming, checklist
│   ├── ENV_CONFIG.md            # All environment variables
│   ├── adr/                     # 13 Architecture Decision Records
│   ├── api/                     # OpenAPI 3.0 specs (9 services)
│   ├── user-stories/            # User stories per epic
│   └── planning/                # Sprint planning docs
│
├── agent/                       # AI Agent context & knowledge base
│   ├── memory/                  # Project context, progress, decisions
│   ├── knowledge/               # Deep-dive technical references
│   ├── skills/                  # Executable skill templates (SK-01 to SK-30)
│   └── tasks/                   # Task playbooks (new feature, bug fix, etc.)
│
└── tests/
    └── smoke/                   # Runtime API smoke tests (Node.js)
```

---

## 📋 Prerequisites

| Requirement | Version |
|---|---|
| Docker & Docker Compose | Latest stable |
| Java JDK | 17 LTS |
| Node.js | 22+ (for frontend) |
| Maven | 3.9+ |
| Available RAM | 8 GB minimum (Docker uses ~5 GB) |

---

## 🚀 Quick Start

### 1. Clone & configure environment

```powershell
git clone https://github.com/KLBMinhLong/E-Procurement.git
cd E-Procurement
Copy-Item .env.example .env
```

> ⚠️ Review `.env` and update secrets (API keys, Keycloak client secret) before proceeding.

### 2. Start infrastructure stack

```powershell
# Core services (PostgreSQL, Redis, Kafka, Keycloak, NGINX Gateway)
docker compose up -d postgres redis kafka kafka-init keycloak nginx-gateway
```

### 3. Start application services

```powershell
# All backend services
docker compose up -d iam-service pr-service approval-service finance-service ^
  inventory-service vendor-service analytics-service notification-service admin-service
```

### 4. Start monitoring (optional)

```powershell
docker compose --profile monitoring up -d prometheus grafana loki tempo
```

### 5. Start frontend

```powershell
cd frontend/eprocure-web
npm install
npm start
# → http://localhost:4200
```

### 6. Build all backend services

```powershell
# From project root
mvn clean package -DskipTests
```

---

## 🌐 Service Endpoints

### Application

| Component | URL |
|---|---|
| **Frontend (Angular)** | http://localhost:4200 |
| **Gateway Health** | http://localhost:8080/health |
| **IAM Service** | http://localhost:8081 |
| **PR Service** | http://localhost:8082 |
| **Approval Service** | http://localhost:8083 |
| **Finance Service** | http://localhost:8084 |
| **Inventory Service** | http://localhost:8085 |
| **Vendor Service** | http://localhost:8086 |
| **Analytics Service** | http://localhost:8087 |
| **Notification Service** | http://localhost:8088 |
| **Admin Service** | http://localhost:8089 |

### Infrastructure

| Component | URL |
|---|---|
| **Keycloak Admin** | http://localhost:18080 |
| **PostgreSQL** | localhost:5432 |
| **Redis** | localhost:6379 |
| **Kafka Bootstrap** | localhost:19092 |
| **Grafana** | http://localhost:3000 |
| **Prometheus** | http://localhost:9090 |

---

## 👤 Dev Users

All dev users are seeded via IAM Flyway migrations. Default password for all accounts:

```
Password@123
```

| Username | Role | Primary Actions |
|---|---|---|
| `requester` | Requester | Create & track purchase requests |
| `manager` | Manager | L1 approval, department budget view |
| `director` | Director | L2 approval, analytics |
| `finance` | Finance / Accountant | Budget approval, invoice matching |
| `purchasing` | Purchasing Officer | Create PO, run RFQ, track delivery |
| `warehouse` | Warehouse Staff | Goods receipt, stock management |
| `accountant` | Accountant | Invoice processing, payment |
| `admin` | System Admin | User management, approval rules |
| `superadmin` | Super Admin | Full system configuration |

> **Note:** Keycloak is configured with a Custom User Storage SPI that delegates credential verification to IAM Service. Keycloak does **not** store user data locally.

---

## 🔑 Key Design Decisions

This project has [13 Architecture Decision Records](docs/adr/) documenting major design choices:

| # | Decision | Rationale |
|---|---|---|
| ADR-001 | Microservices + Clean Architecture | Independent deployment, domain isolation |
| ADR-002 | Custom Opaque Token (not JWT) | Instant revocation, no token data leakage |
| ADR-003 | RSA + AES Hybrid Encryption | Defense-in-depth for payload security |
| ADR-004 | Soft Delete Strategy | Complete audit trail, no data loss |
| ADR-005 | MyBatis over JPA/Hibernate | Full SQL control, avoid N+1, Clean Arch compatible |
| ADR-006 | Camunda BPMN 7 | Visual workflow, SLA, escalation patterns |
| ADR-007 | Observability Stack (OTel + Grafana) | Unified tracing, metrics, and logs |
| ADR-008 | PostgreSQL Multi-Schema | Logical isolation per service |
| ADR-009 | Angular Design System (ep-* components) | Consistent UI, design tokens |
| ADR-010 | Spring Profile Strategy (local/dev/prod) | Environment-specific configuration |
| ADR-011 | RBAC Permission Codes | No hardcoded roles, flexible authorization |
| ADR-012 | Mandatory Idempotency-Key | Safe retries, prevent duplicate operations |
| ADR-013 | Timezone & Financial Precision | TIMESTAMPTZ + NUMERIC(19,4) |

### Core Invariants

```
① No HTTP DELETE endpoints — soft delete everywhere
② Domain layer is pure POJO — zero framework annotations
③ @PreAuthorize uses permission codes — never hardcode roles
④ @Transactional belongs in UseCase layer only
⑤ Money is always BigDecimal (Java) / NUMERIC(19,4) (DB)
⑥ Never return null from public methods — use Optional<T>
⑦ Logs never contain passwords, tokens, secrets, or keys
⑧ Auth token is opaque (64 chars) — not JWT
⑨ Idempotency-Key required for every POST/PUT/PATCH
⑩ ObjectMapper for domain ↔ entity conversion — no manual field mapping
```

---

## 🔒 Security Model

### Authentication Flow

```
 Client                  Gateway              IAM Service           Keycloak
   │                        │                      │                    │
   │── POST /auth/login ───▶│── forward ──────────▶│                    │
   │   (RSA+AES encrypted)  │                      │── verify cred ────▶│
   │                        │                      │◀── OK ─────────────│
   │                        │                      │ Generate opaque    │
   │                        │                      │ token → Redis + DB │
   │◀── Set-Cookie: ep_session (HttpOnly) ─────────│                    │
   │                        │                      │                    │
   │── Cookie (every req) ─▶│── Redis lookup ─────▶│                    │
   │                        │◀── user + perms ─────│                    │
   │                        │── inject headers ────▶ Downstream Service │
```

### Security Features

- **Opaque Token:** Random 64-char string (not JWT), stored in Redis + DB
- **Single Session:** New login immediately invalidates previous session
- **HttpOnly Cookie:** `ep_session` with `Secure + SameSite=Strict`
- **RSA+AES Encryption:** Hybrid payload encryption (toggle via `ENCRYPTION_ENABLED`)
- **Separation of Duties:** PR creator ≠ PR approver ≠ Payment processor
- **2FA (TOTP):** Optional two-factor authentication
- **Google OAuth:** SSO integration
- **Password:** BCrypt with per-user salt, cost=12

---

## ✅ Approval Matrix

Dynamic, rule-based approval chains powered by Camunda BPMN:

| PR Value (VND) | Approval Chain |
|---|---|
| < 5M | Manager |
| 5M – 20M | Manager → Finance |
| 20M – 50M | Manager → Director → Finance |
| 50M – 200M | Manager → Director → CFO *(+ mandatory RFQ)* |
| 200M – 500M | Manager → Director → CEO → CFO |
| > 500M | Manager → Director → Board *(parallel)* → CFO |
| **Emergency** | Manager + Director *(parallel, 2h SLA, 24/7)* |

**Special Rules:**
- IT Software/SaaS: adds CISO + IT Manager regardless of value
- CAPEX items: adds Finance Director + CEO regardless of value
- Emergency > 3 times/department/month → abuse alert

---

## 📡 API Conventions

```
Base URL:      /api/v1/{resource}
Auth:          HttpOnly Cookie "ep_session"
Idempotency:   Idempotency-Key header required for POST/PUT/PATCH
Pagination:    page starts at 1 (not 0)
Money:         String in JSON ("70000000.0000"), BigDecimal in Java
No DELETE:     Use PATCH .../cancel | .../deactivate | .../revoke
```

### Standard Response Envelope

```json
{
  "success": true,
  "code": "SUCCESS",
  "message": null,
  "data": { ... },
  "meta": { "page": 1, "size": 20, "totalElements": 150, "totalPages": 8 },
  "timestamp": "2026-08-11T10:30:00.000Z",
  "requestId": "req-abc-123"
}
```

### Error Codes

Each service has its own error prefix: `IAM_`, `PR_`, `APR_`, `FIN_`, `INV_`, `VND_`, `GW_`, `SYS_`, `VAL_`

### Kafka Topics

| Topic | Description |
|---|---|
| `procurement.pr.submitted` | PR submitted for approval |
| `procurement.pr.approved` | PR fully approved |
| `procurement.pr.rejected` | PR rejected |
| `approval.step.assigned` | Approval task assigned to approver |
| `approval.sla.breached` | Approval step exceeded SLA |
| `procurement.po.issued` | PO sent to vendor |
| `procurement.rfq.awarded` | RFQ winner selected |
| `inventory.gr.created` | Goods receipt completed |
| `finance.invoice.matched` | Invoice 3-way match successful |
| `finance.budget.warning` | Budget below 20% threshold |
| `notification.email.send` | Email dispatch request |

---

## 📊 Observability

### Log Format (Layer Prefixes)

```
[REQUEST]    → HTTP filter: method, URL, IP, status, duration
[CONTROLLER] → Controller entry with masked userId
[ACTION]     → UseCase Start/Step/Complete with key=value
[REPO]       → Repository operation (DEBUG level)
[CACHE]      → Redis hit/miss/put/evict (DEBUG level)
[TOKEN]      → Token validation failure (WARN)
[SECURITY]   → Unauthorized access attempt (WARN)
[AUDIT]      → Immutable audit trail (INFO, separate file)
[EXCEPTION]  → Error with code (WARN/ERROR)
```

### Monitoring Stack

| Tool | URL | Purpose |
|---|---|---|
| **Grafana** | http://localhost:3000 | Dashboards, log exploration |
| **Prometheus** | http://localhost:9090 | Metrics collection |
| **Tempo** | http://localhost:3200 | Distributed tracing |
| **Loki** | http://localhost:3100 | Log aggregation |

> Start the monitoring stack with: `docker compose --profile monitoring up -d`

---

## 🧪 Testing

| Type | Tool | Target |
|---|---|---|
| Unit Tests | JUnit 5 + Mockito | domain/ (80%+), application/ (70%+) |
| Frontend Tests | Vitest | Component + service tests |
| Integration Tests | Postman / Newman | API contract validation |
| Performance Tests | JMeter | Load & stress testing |
| Smoke Tests | Node.js script | Full workflow verification |

### Run Tests

```powershell
# Backend unit tests (all services)
mvn test

# Single service
mvn -pl services/iam-service test

# Frontend
cd frontend/eprocure-web
npm test

# Smoke test (requires Docker stack running)
node tests/smoke/e15-runtime-smoke.mjs
```

---

## 📚 Documentation Index

| Document | Description |
|---|---|
| [`docs/PROJECT_BRIEF.md`](docs/PROJECT_BRIEF.md) | Business requirements, scope, epics, KPIs |
| [`docs/DOMAIN_MODEL.md`](docs/DOMAIN_MODEL.md) | Entities, aggregates, domain events, business rules |
| [`docs/DATABASE_SCHEMA.md`](docs/DATABASE_SCHEMA.md) | Full DDL, index strategy, schema layout |
| [`docs/CODING_GUIDE.md`](docs/CODING_GUIDE.md) | Conventions, naming standards, checklists |
| [`docs/ENV_CONFIG.md`](docs/ENV_CONFIG.md) | All environment variables reference |
| [`docs/adr/`](docs/adr/) | 13 Architecture Decision Records |
| [`docs/api/`](docs/api/) | OpenAPI 3.0 specs for all 9 services |
| [`docs/user-stories/`](docs/user-stories/) | User stories & use cases per epic |
| [`AGENTS.md`](AGENTS.md) | AI Agent orchestration playbook |
| [`infra/README.md`](infra/README.md) | Infrastructure setup details |

---

## 🤝 Contributing

### Before You Start

1. **Read [`AGENTS.md`](AGENTS.md)** — Contains all project invariants and development rules
2. **Read [`docs/CODING_GUIDE.md`](docs/CODING_GUIDE.md)** — Conventions and naming standards
3. **Check [`docs/adr/`](docs/adr/)** — Understand architectural decisions before proposing changes

### Development Rules

- ❌ No `HTTP DELETE` endpoints — use soft delete via `PATCH`
- ❌ No Spring/Jakarta annotations in `domain/` package
- ❌ No hardcoded roles in `@PreAuthorize` — use permission codes
- ❌ No `@Transactional` in Repository or Controller — only in UseCase
- ❌ No `float`/`double` for money — use `BigDecimal` / `NUMERIC(19,4)`
- ❌ No `return null` from public methods — use `Optional<T>`
- ❌ No passwords, tokens, or secrets in logs
- ✅ Every `POST`/`PUT`/`PATCH` must handle `Idempotency-Key`
- ✅ Every `SELECT` must include `WHERE is_deleted = false`
- ✅ All Angular text uses `| translate` pipe — no hardcoded strings
- ✅ All Angular colors use CSS Custom Properties — no hardcoded values

### Branch Strategy

```
main        ← Production-ready code
develop     ← Integration branch
feature/*   ← Feature branches (from develop)
bugfix/*    ← Bug fix branches (from develop)
hotfix/*    ← Critical fixes (from main)
```

### Definition of Done

- [ ] Code reviewed by at least 1 other developer
- [ ] Unit test coverage ≥ 70% for service layer
- [ ] No hardcoded strings in Angular templates
- [ ] `@PreAuthorize` uses permission codes only
- [ ] Logs contain no sensitive data
- [ ] OpenAPI spec updated (if API changed)
- [ ] Flyway migration script included (if schema changed)
- [ ] Docker resource limits configured appropriately

---

## 📄 License

This project is proprietary software. All rights reserved.

---

<p align="center">
  Built with ❤️ by the eProcure Engineering Team
</p>
