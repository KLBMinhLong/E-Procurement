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

## Keycloak (IAM only)
| Variable | Notes |
|---|---|
| KEYCLOAK_URL | http://keycloak:8080 |
| KEYCLOAK_REALM | eprocure |
| KEYCLOAK_CLIENT_ID | eprocure-backend |
| KEYCLOAK_CLIENT_SECRET | secret |

## Security
| Variable | Notes |
|---|---|
| ENCRYPTION_RSA_PRIVATE_KEY | base64 RSA private key (BE) |
| ENCRYPTION_RSA_PUBLIC_KEY | base64 RSA public key (BE) |
| TOTP_SECRET_ENCRYPTION_KEY | AES key for TOTP secret |

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
