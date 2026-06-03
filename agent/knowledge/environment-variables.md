# Environment Variables

## Common
| Variable | Default (dev) | Notes |
|---|---|---|
| TZ | Asia/Ho_Chi_Minh | Required for all containers |
| SPRING_PROFILES_ACTIVE | dev | local/dev/prod |
| ENCRYPTION_ENABLED | false | true in prod |
| API_KEY | dev-internal-key-xxx | internal service auth |

## Database
| Variable | Notes |
|---|---|
| DB_HOST | PostgreSQL host |
| DB_PORT | 5432 |
| DB_NAME | db_iam / db_procurement / db_finance / ... |
| DB_USER | per-service DB user |
| DB_PASS | per-service DB password |
| DB_SCHEMA | primary schema |

## Redis
| Variable | Notes |
|---|---|
| REDIS_HOST | Redis host |
| REDIS_PORT | 6379 |
| REDIS_PASS | Redis auth password |
| REDIS_DB | DB index |

## Kafka
| Variable | Notes |
|---|---|
| KAFKA_BOOTSTRAP_SERVERS | kafka:9092 |
| KAFKA_CONSUMER_GROUP_ID | {service-name}-group |
| KAFKA_AUTO_OFFSET_RESET | earliest |
| FINANCE_KAFKA_ENABLED | Enables Finance Kafka consumers/producers |
| FINANCE_KAFKA_AUTO_STARTUP | Starts Finance Kafka listeners after ApplicationReady and keeps HTTP health independent from temporary broker DNS/startup failures |
| INVENTORY_KAFKA_TOPIC_GR_CREATED | Topic used by inventory-service to publish completed Goods Receipts |

## Keycloak (IAM only)
| Variable | Notes |
|---|---|
| KEYCLOAK_URL | http://keycloak:8080 |
| KEYCLOAK_REALM | eprocure |
| KEYCLOAK_CLIENT_ID | eprocure-iam |
| KEYCLOAK_CLIENT_SECRET | Required secret from `.env`; do not hardcode in realm JSON |
| IAM_PROVIDER_BASE_URL | Base URL from `.env` used by Keycloak provider -> IAM internal endpoints |
| IAM_PROVIDER_TIMEOUT_SECONDS | HTTP timeout for Keycloak provider -> IAM internal endpoints |
| IAM_INTERNAL_API_KEY | Shared key for Keycloak provider and approval-service -> IAM internal endpoints |
| GOOGLE_CLIENT_ID | Google OAuth client ID |
| GOOGLE_CLIENT_SECRET | Google OAuth client secret |
| GOOGLE_REDIRECT_URI | http://localhost:8081/api/v1/auth/oauth/google/callback |
| GOOGLE_OAUTH_STATE_TTL_MINUTES | State TTL for Google OAuth callback validation |

## Security
| Variable | Notes |
|---|---|
| ENCRYPTION_RSA_PRIVATE_KEY | base64 RSA private key (BE) |
| ENCRYPTION_RSA_PUBLIC_KEY | base64 RSA public key (BE) |
| TOTP_SECRET_ENCRYPTION_KEY | AES key for TOTP secret |
| TWO_FACTOR_ISSUER | Display name in authenticator apps |
| TWO_FACTOR_CHALLENGE_TTL_MINUTES | TTL for pending 2FA challenge cookie |

## Observability
| Variable | Notes |
|---|---|
| OTEL_SERVICE_NAME | service name |
| OTEL_EXPORTER_OTLP_ENDPOINT | http://tempo:4317 |
| OTEL_EXPORTER_OTLP_PROTOCOL | grpc |
| PROMETHEUS_ENABLED | true |

## JVM
| Variable | Default | Notes |
|---|---|---|
| JAVA_OPTS | -Xms128m -Xmx384m -XX:+UseG1GC -XX:MaxRAMPercentage=75.0 | per service tuning |
