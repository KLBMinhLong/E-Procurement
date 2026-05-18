# eProcure Infrastructure

Local infrastructure for E01.

## Start core stack

```powershell
Copy-Item .env.example .env
docker compose up -d postgres redis kafka kafka-init keycloak nginx-gateway
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

## Keycloak IAM provider

The Keycloak image is built from `infra/keycloak/Dockerfile`, which installs the `eprocure-iam-user-storage` User Storage SPI provider. Keycloak does not store local eProcure users or passwords; it federates lookups and password checks to IAM internal endpoints with `X-Internal-Api-Key`.

## Dev users

Dev users are seeded in the IAM database. All users use the local-only password `Password@123`.

```
requester
manager
director
finance
admin
```

IAM still owns user business data, roles, permissions, password hashes and sessions. Keycloak is only for credential verification through the custom provider.

If your local Keycloak/PostgreSQL volume was created before this provider existed, recreate the local data before validating realm import; old volumes can still contain the previous local Keycloak users.
