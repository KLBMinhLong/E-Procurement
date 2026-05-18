# eProcure Enterprise

This repository contains the eProcure Enterprise documentation, agent playbooks and implementation workspace.

## Local Infrastructure

The E01 foundation stack is defined in `docker-compose.yml`.

```powershell
Copy-Item .env.example .env
docker compose up -d postgres redis kafka kafka-init keycloak nginx-gateway
```

Optional monitoring stack:

```powershell
docker compose --profile monitoring up -d prometheus grafana loki tempo
```

Core endpoints:

| Component | URL |
|---|---|
| Gateway health | http://localhost:8080/health |
| Keycloak | http://localhost:18080 |
| Kafka external bootstrap | localhost:19092 |
| Grafana | http://localhost:3000 |
| Prometheus | http://localhost:9090 |

## Development Rules

Read `AGENTS.md` before changing code. The key invariants are: no HTTP DELETE, domain POJO only, permission-code authorization, UseCase transactions, BigDecimal money, opaque tokens, mandatory Idempotency-Key for POST/PUT/PATCH, and no sensitive logs.
