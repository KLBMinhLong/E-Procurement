# eProcure Infrastructure

Local infrastructure for E01.

## Start core stack

```powershell
Copy-Item .env.example .env
docker compose up -d postgres redis zookeeper kafka kafka-init keycloak nginx-gateway
```

## Start monitoring profile

```powershell
docker compose --profile monitoring up -d prometheus grafana loki tempo
```

## Useful URLs

| Service | URL |
|---|---|
| Gateway health | http://localhost:8080/health |
| Keycloak | http://localhost:18080 |
| Kafka external bootstrap | localhost:19092 |
| Grafana | http://localhost:3000 |
| Prometheus | http://localhost:9090 |

## Keycloak dev users

All users use the local-only password `Password@123`.

```
requester
manager
director
finance
admin
```

IAM still owns user business data, roles, permissions and sessions. Keycloak is only for credential verification.
