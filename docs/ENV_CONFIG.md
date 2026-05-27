# ENV_CONFIG
## eProcure Enterprise — Biến Môi Trường & Cấu Hình Hệ Thống

---

> **Version:** 1.0.0  
> **Nguyên tắc:**  
> - KHÔNG commit giá trị sensitive lên Git (dùng `.env.example` với placeholder)  
> - Giá trị sensitive chỉ tồn tại trong Docker secrets / CI/CD vault / môi trường runtime  
> - Mọi service đọc config qua biến môi trường, KHÔNG hardcode  
> - Trang Admin Config Portal cho phép xem/sửa qua UI (che sensitive values)

---

## 1. QUYẾT ĐỊNH THIẾT KẾ

```
application.yml          ← Cấu hình chung, không sensitive, commit được
application-local.yml    ← Dev chạy ngoài Docker (localhost endpoints)
application-dev.yml      ← Dev chạy trong Docker (log DEBUG, encryption OFF)
application-prod.yml     ← Production (log INFO, encryption ON, full features)

Biến môi trường ghi đè application.yml qua cú pháp:
  ${ENV_VAR_NAME:default_value}
```

---

## 2. BIẾN MÔI TRƯỜNG CHUNG (ALL SERVICES)

| Biến | Ví dụ (dev) | Bắt buộc | Mô tả |
|---|---|---|---|
| `TZ` | `Asia/Ho_Chi_Minh` | ✅ | Timezone cho JVM và container |
| `SPRING_PROFILES_ACTIVE` | `dev` | ✅ | Profile: `local` / `dev` / `prod` |
| `SERVICE_NAME` | `pr-service` | ✅ | Tên service, dùng cho log và tracing |
| `OTEL_SERVICE_NAME` | `pr-service` | ✅ | OpenTelemetry service name |
| `OTEL_EXPORTER_OTLP_ENDPOINT` | `http://tempo:4317` | ✅ | Tempo OTLP endpoint |
| `OTEL_EXPORTER_OTLP_PROTOCOL` | `grpc` | ✅ | Giao thức OTLP |
| `LOG_LEVEL_APP` | `DEBUG` | ❌ | Log level cho package `com.eprocure` |
| `LOG_LEVEL_ROOT` | `INFO` | ❌ | Root log level |
| `ENCRYPTION_ENABLED` | `false` | ✅ | Bật/tắt RSA+AES payload encryption |
| `X_API_KEY` | `secret-internal-key` | ✅ | Key cho inter-service auth (Gateway inject) |
| `SERVER_PORT` | `8080` | ❌ | HTTP port (mặc định 8080) |

---

## 3. API GATEWAY

| Biến | Ví dụ (dev) | Prod | Mô tả |
|---|---|---|---|
| `GATEWAY_PORT` | `8080` | `8080` | Port public |
| `RATE_LIMIT_USER_RPM` | `100` | `100` | Requests/phút/user |
| `RATE_LIMIT_AUTH_RPM` | `10` | `10` | Login attempt/phút/IP |
| `RATE_LIMIT_UPLOAD_RPM` | `10` | `10` | Upload/phút/user |
| `RATE_LIMIT_INTERSERVICE_RPM` | `1000` | `1000` | Inter-service/phút/service |
| `IAM_SERVICE_URL` | `http://iam-service:8081` | `http://iam-service:8081` | IAM endpoint để verify token |
| `PR_SERVICE_URL` | `http://pr-service:8082` | `http://pr-service:8082` | PR endpoint nội bộ/gateway upstream |
| `PR_INTERNAL_API_KEY` | `change-me-internal-api-key` | `***SENSITIVE***` | Shared key cho PR internal endpoints |
| `APPROVAL_SERVICE_URL` | `http://approval-service:8083` | `http://approval-service:8083` | |
| `FINANCE_SERVICE_URL` | `http://finance-service:8084` | `http://finance-service:8084` | |
| `INVENTORY_SERVICE_URL` | `http://inventory-service:8085` | `http://inventory-service:8085` | |
| `VENDOR_SERVICE_URL` | `http://vendor-service:8086` | `http://vendor-service:8086` | |
| `ANALYTICS_SERVICE_URL` | `http://analytics-service:8087` | `http://analytics-service:8087` | |
| `NOTIFICATION_SERVICE_URL` | `http://notification-service:8088` | `http://notification-service:8088` | |
| `ADMIN_SERVICE_URL` | `http://admin-service:8089` | `http://admin-service:8089` | |
| `CORS_ALLOWED_ORIGINS` | `http://localhost:4200` | `https://app.eprocure.vn` | CORS whitelist |

---

## 4. IAM SERVICE

### 4.1 Database
| Biến | Dev | Prod | Mô tả |
|---|---|---|---|
| `IAM_DB_HOST` | `postgres` | `postgres-iam.internal` | PostgreSQL host |
| `IAM_DB_PORT` | `5432` | `5432` | PostgreSQL port |
| `IAM_DB_NAME` | `db_iam` | `db_iam` | Database name |
| `IAM_DB_SCHEMA` | `iam` | `iam` | Schema name |
| `IAM_DB_USER` | `iam_user` | `iam_user` | DB username |
| `IAM_DB_PASS` | `iam_pass_dev` | `***SENSITIVE***` | DB password |
| `IAM_DB_POOL_MIN` | `2` | `5` | HikariCP min pool |
| `IAM_DB_POOL_MAX` | `5` | `20` | HikariCP max pool |

### 4.2 Redis
| Biến | Dev | Prod | Mô tả |
|---|---|---|---|
| `REDIS_HOST` | `redis` | `redis-cluster.internal` | Redis host |
| `REDIS_PORT` | `6379` | `6379` | Redis port |
| `REDIS_PASS` | `` | `***SENSITIVE***` | Redis password (rỗng = không cần) |
| `REDIS_DB` | `0` | `0` | Redis DB index |
| `SESSION_TTL_HOURS` | `8` | `8` | Session TTL (sliding window, giờ) |
| `PERM_CACHE_TTL_MINUTES` | `15` | `15` | Permission cache TTL (phút) |
| `IDEMPOTENCY_TTL_HOURS` | `24` | `24` | Idempotency key TTL (giờ) |

### 4.3 Keycloak
| Biến | Dev | Prod | Mô tả |
|---|---|---|---|
| `KEYCLOAK_URL` | `http://keycloak:8080` | `https://auth.eprocure.internal` | Keycloak base URL |
| `KEYCLOAK_REALM` | `eprocure` | `eprocure` | Realm name |
| `KEYCLOAK_CLIENT_ID` | `eprocure-iam` | `eprocure-iam` | Client ID |
| `KEYCLOAK_CLIENT_SECRET` | `change-me-keycloak-client-secret` | `***SENSITIVE***` | Client secret, bắt buộc đặt trong `.env`; dùng cho realm import và IAM direct grant |
| `KEYCLOAK_ADMIN_USER` | `admin` | `***SENSITIVE***` | Admin username |
| `KEYCLOAK_ADMIN_PASS` | `admin` | `***SENSITIVE***` | Admin password |
| `IAM_PROVIDER_BASE_URL` | `http://iam-service:8081` | `http://iam-service:8081` | Base URL Keycloak provider dùng để gọi IAM internal API; truyền qua env vào realm import |
| `IAM_PROVIDER_TIMEOUT_SECONDS` | `3` | `3` | HTTP timeout cho Keycloak provider khi gọi IAM internal API |
| `IAM_INTERNAL_API_KEY` | `change-me-internal-api-key` | `***SENSITIVE***` | Shared key giữa Keycloak provider/approval-service và IAM internal endpoints |

### 4.4 Encryption (RSA)
| Biến | Dev | Prod | Mô tả |
|---|---|---|---|
| `ENCRYPTION_ENABLED` | `false` | `true` | Bật mã hóa payload |
| `RSA_KEY_SIZE` | `2048` | `2048` | RSA key size bits |
| `RSA_PRIVATE_KEY` | *(empty)* | `***SENSITIVE***` | RSA private key (PEM, base64) |
| `RSA_KEY_VERSION` | `v2025-01` | `v2025-01` | Key version label |

### 4.5 OAuth Google
| Biến | Dev | Prod | Mô tả |
|---|---|---|---|
| `GOOGLE_CLIENT_ID` | `xxx.apps.googleusercontent.com` | `***SENSITIVE***` | Google OAuth Client ID |
| `GOOGLE_CLIENT_SECRET` | `***SENSITIVE***` | `***SENSITIVE***` | Google OAuth Secret |
| `GOOGLE_REDIRECT_URI` | `http://localhost:8081/api/v1/auth/oauth/google/callback` | `https://api.eprocure.vn/api/v1/auth/oauth/google/callback` | OAuth callback URL |
| `GOOGLE_OAUTH_STATE_TTL_MINUTES` | `5` | `5` | TTL state chống CSRF cho OAuth callback |

### 4.6 Session & Security
| Biến | Dev | Prod | Mô tả |
|---|---|---|---|
| `MAX_LOGIN_ATTEMPTS` | `5` | `5` | Số lần sai tối đa trước khi lock |
| `LOCK_DURATION_MINUTES` | `30` | `30` | Thời gian lock tài khoản |
| `PASSWORD_MIN_LENGTH` | `8` | `8` | Độ dài mật khẩu tối thiểu |
| `RESET_TOKEN_TTL_MINUTES` | `15` | `15` | TTL của forgot-password token |
| `PASSWORD_RESET_FRONTEND_URL` | `http://localhost:4200/reset-password` | FE domain | URL FE nhận reset token từ email |
| `TWO_FACTOR_ISSUER` | `eProcure Dev` | `eProcure` | Tên hiển thị trong Authenticator app |
| `TOTP_SECRET_ENCRYPTION_KEY` | `MDEy...` | `***SENSITIVE***` | Base64 AES key dùng để mã hóa TOTP secret trong DB |
| `TWO_FACTOR_CHALLENGE_TTL_MINUTES` | `5` | `5` | TTL cookie challenge trước khi xác minh 2FA |
| `COOKIE_DOMAIN` | `localhost` | `eprocure.vn` | Domain của HttpOnly cookie |
| `COOKIE_SECURE` | `false` | `true` | Require HTTPS cho cookie |
| `COOKIE_SAMESITE` | `Lax` | `Strict` | SameSite policy |

---

## 5. PR SERVICE

### 5.1 Database
| Biến | Dev | Prod | Mô tả |
|---|---|---|---|
| `PR_DB_HOST` | `postgres` | `postgres-proc.internal` | |
| `PR_DB_PORT` | `5432` | `5432` | |
| `PR_DB_NAME` | `db_procurement` | `db_procurement` | |
| `PR_DB_SCHEMA` | `pr` | `pr` | |
| `PR_DB_USER` | `pr_user` | `pr_user` | |
| `PR_DB_PASS` | `pr_pass_dev` | `***SENSITIVE***` | |
| `PR_DB_POOL_MIN` | `2` | `5` | |
| `PR_DB_POOL_MAX` | `5` | `20` | |

### 5.2 Business Rules
| Biến | Dev | Prod | Mô tả |
|---|---|---|---|
| `PR_NUMBER_PREFIX` | `PR` | `PR` | Prefix số PR |
| `PR_ATTACHMENT_MAX_SIZE_MB` | `10` | `10` | Giới hạn file upload (MB) |
| `PR_EMERGENCY_MONTHLY_LIMIT` | `3` | `3` | Số lần Emergency PR/phòng/tháng |
| `BUDGET_WARNING_THRESHOLD_PCT` | `20` | `20` | % ngân sách còn lại → warning |
| `INVENTORY_CHECK_ENABLED` | `true` | `true` | Bật kiểm tra tồn kho khi submit |
| `FILE_STORAGE_PATH` | `/data/attachments` | `/data/attachments` | Thư mục lưu file |
| `IAM_SERVICE_URL` | `http://iam-service:8081` | `http://iam-service:8081` | Để check user/dept |
| `FINANCE_SERVICE_URL` | `http://finance-service:8084` | `http://finance-service:8084` | Budget check |
| `INVENTORY_SERVICE_URL` | `http://inventory-service:8085` | `http://inventory-service:8085` | Stock check |
| `PR_INTEGRATION_FALLBACK_ENABLED` | `false` trong Docker Compose, `true` local mặc định | `false` | `false` để publish Kafka `procurement.pr.submitted`; `true` chỉ log event khi chạy local không có Kafka |

---

## 6. APPROVAL ENGINE SERVICE

### 6.1 Database
| Biến | Dev | Prod | Mô tả |
|---|---|---|---|
| `APPROVAL_DB_HOST` | `postgres` | `postgres-proc.internal` | |
| `APPROVAL_DB_NAME` | `db_procurement` | `db_procurement` | Cùng DB với PR |
| `APPROVAL_DB_SCHEMA` | `approval` | `approval` | Schema riêng |
| `APPROVAL_DB_USER` | `approval_user` | `approval_user` | |
| `APPROVAL_DB_PASS` | `approval_pass_dev` | `***SENSITIVE***` | |

### 6.2 Camunda BPMN
| Biến | Dev | Prod | Mô tả |
|---|---|---|---|
| `CAMUNDA_DB_HOST` | `postgres` | `postgres-camunda.internal` | DB riêng cho Camunda |
| `CAMUNDA_DB_NAME` | `db_camunda` | `db_camunda` | |
| `CAMUNDA_DB_SCHEMA` | `camunda` | `camunda` | |
| `CAMUNDA_DB_USER` | `camunda_user` | `camunda_user` | |
| `CAMUNDA_DB_PASS` | `camunda_pass_dev` | `***SENSITIVE***` | |
| `CAMUNDA_ADMIN_USER` | `admin` | `***SENSITIVE***` | Camunda admin user |
| `CAMUNDA_ADMIN_PASS` | `admin` | `***SENSITIVE***` | |
| `CAMUNDA_HISTORY_TTL` | `P180D` | `P365D` | ISO8601 duration — TTL history |

### 6.3 IAM Integration
| Biến | Dev | Prod | Mô tả |
|---|---|---|---|
| `IAM_SERVICE_URL` | `http://iam-service:8081` | `https://iam.eprocure.internal` | Approval-service gọi IAM internal API để resolve approver |
| `IAM_INTERNAL_API_KEY` | `change-me-internal-api-key` | `***SENSITIVE***` | Shared key cho `/internal/org/approvers` |
| `APPROVAL_INTEGRATION_FALLBACK_ENABLED` | `false` trong Docker Compose, `true` local mặc định | `false` | `false` để consume/publish Kafka; `true` tắt consumer và chỉ log `approval.step.assigned` |

### 6.4 PR Integration
| Biến | Dev | Prod | Mô tả |
|---|---|---|---|
| `PR_SERVICE_URL` | `http://pr-service:8082` | `https://pr.eprocure.internal` | Approval-service gọi PR internal API để cập nhật trạng thái PR |
| `PR_INTERNAL_API_KEY` | `change-me-internal-api-key` | `***SENSITIVE***` | Shared key cho `/internal/purchase-requests/*` |

### 6.5 SLA & Escalation
| Biến | Dev | Prod | Mô tả |
|---|---|---|---|
| `SLA_WARNING_AHEAD_HOURS` | `4` | `4` | Gửi cảnh báo trước SLA bao nhiêu giờ |
| `SLA_CHECK_INTERVAL_MINUTES` | `15` | `15` | Tần suất check SLA (Camunda timer) |
| `SLA_ZONE_ID` | `Asia/Ho_Chi_Minh` | `Asia/Ho_Chi_Minh` | Timezone dùng khi cộng giờ làm việc |
| `BUSINESS_HOURS_START` | `08:00` | `08:00` | Giờ bắt đầu làm việc |
| `BUSINESS_HOURS_END` | `17:30` | `17:30` | Giờ kết thúc làm việc |
| `BUSINESS_DAYS` | `MON,TUE,WED,THU,FRI` | `MON,TUE,WED,THU,FRI` | Ngày làm việc |
| `EMERGENCY_SLA_HOURS` | `2` | `2` | SLA cho Manager khi EMERGENCY |
| `EMERGENCY_L2_SLA_HOURS` | `4` | `4` | SLA cho Director khi EMERGENCY |

---

## 7. FINANCE SERVICE

| Biến | Dev | Prod | Mô tả |
|---|---|---|---|
| `FINANCE_DB_HOST` | `postgres` | `postgres-fin.internal` | |
| `FINANCE_DB_NAME` | `db_finance` | `db_finance` | |
| `FINANCE_DB_SCHEMA` | `finance` | `finance` | |
| `FINANCE_DB_USER` | `finance_user` | `finance_user` | |
| `FINANCE_DB_PASS` | `finance_pass_dev` | `***SENSITIVE***` | |
| `FINANCE_DB_POOL_MIN` | `2` | `5` | |
| `FINANCE_DB_POOL_MAX` | `5` | `20` | |
| `BUDGET_OVERRIDE_MAX_PCT` | `30` | `30` | Override tối đa 30% — vượt cần CEO |
| `THREE_WAY_MATCH_TOLERANCE_PCT` | `5` | `5` | Dung sai 5% số lượng/giá |
| `INVOICE_DUE_WARNING_DAYS` | `7` | `7` | Cảnh báo trước hạn thanh toán N ngày |
| `BUDGET_CACHE_TTL_MINUTES` | `5` | `5` | Cache ngân sách dashboard (phút) |

---

## 8. INVENTORY SERVICE

| Biến | Dev | Prod | Mô tả |
|---|---|---|---|
| `INVENTORY_DB_HOST` | `postgres` | `postgres-inv.internal` | |
| `INVENTORY_DB_NAME` | `db_inventory` | `db_inventory` | |
| `INVENTORY_DB_SCHEMA` | `inventory` | `inventory` | |
| `INVENTORY_DB_USER` | `inventory_user` | `inventory_user` | |
| `INVENTORY_DB_PASS` | `inv_pass_dev` | `***SENSITIVE***` | |
| `INVENTORY_DB_POOL_MIN` | `2` | `3` | |
| `INVENTORY_DB_POOL_MAX` | `5` | `10` | |
| `STOCK_LOCK_TIMEOUT_MS` | `5000` | `5000` | Timeout pessimistic lock khi update stock |
| `GR_QUANTITY_TOLERANCE_PCT` | `10` | `10` | Dung sai số lượng GR so với PO |

---

## 9. VENDOR SERVICE

| Biến | Dev | Prod | Mô tả |
|---|---|---|---|
| `VENDOR_DB_HOST` | `postgres` | `postgres-vnd.internal` | |
| `VENDOR_DB_NAME` | `db_vendor` | `db_vendor` | |
| `VENDOR_DB_SCHEMA` | `vendor` | `vendor` | |
| `VENDOR_DB_USER` | `vendor_user` | `vendor_user` | |
| `VENDOR_DB_PASS` | `vendor_pass_dev` | `***SENSITIVE***` | |
| `VENDOR_DB_POOL_MIN` | `2` | `3` | |
| `VENDOR_DB_POOL_MAX` | `5` | `10` | |
| `RFQ_MIN_VENDORS` | `2` | `2` | Số vendor tối thiểu cần mời trong RFQ |
| `RFQ_REMINDER_HOURS_BEFORE` | `24` | `24` | Gửi reminder trước deadline N giờ |

---

## 10. ANALYTICS SERVICE

| Biến | Dev | Prod | Mô tả |
|---|---|---|---|
| `ANALYTICS_DB_HOST` | `postgres` | `postgres-proc.internal` | Read-replica (nếu có) |
| `ANALYTICS_DB_NAME` | `db_procurement` | `db_procurement` | |
| `ANALYTICS_DB_USER` | `analytics_ro` | `analytics_ro` | Read-only user |
| `ANALYTICS_DB_PASS` | `analytics_pass_dev` | `***SENSITIVE***` | |
| `DASHBOARD_CACHE_TTL_MINUTES` | `5` | `5` | TTL cache dashboard data |
| `REPORT_JOB_TIMEOUT_MINUTES` | `10` | `10` | Timeout job export báo cáo |
| `REPORT_DOWNLOAD_TTL_HOURS` | `24` | `24` | File download link TTL |
| `REPORT_STORAGE_PATH` | `/data/reports` | `/data/reports` | Thư mục lưu file báo cáo |
| `JASPER_TEMPLATE_PATH` | `classpath:reports/` | `classpath:reports/` | Path JasperReport templates |

---

## 11. NOTIFICATION SERVICE

| Biến | Dev | Prod | Mô tả |
|---|---|---|---|
| `NOTIFICATION_DB_HOST` | `postgres` | `postgres-ntf.internal` | |
| `NOTIFICATION_DB_NAME` | `db_notification` | `db_notification` | |
| `NOTIFICATION_DB_SCHEMA` | `notification` | `notification` | |
| `NOTIFICATION_DB_USER` | `notif_user` | `notif_user` | |
| `NOTIFICATION_DB_PASS` | `notif_pass_dev` | `***SENSITIVE***` | |
| `NOTIFICATION_DB_POOL_MIN` | `1` | `2` | |
| `NOTIFICATION_DB_POOL_MAX` | `3` | `10` | |
| `EMAIL_ENABLED` | `false` | `true` | Bật gửi email thực (tắt ở dev) |
| `BREVO_API_KEY` | *(empty)* | `***SENSITIVE***` | Brevo SMTP API Key |
| `BREVO_SMTP_HOST` | `smtp-relay.brevo.com` | `smtp-relay.brevo.com` | |
| `BREVO_SMTP_PORT` | `587` | `587` | |
| `BREVO_SMTP_USER` | `dev@eprocure.vn` | `noreply@eprocure.vn` | |
| `BREVO_SMTP_PASS` | *(empty)* | `***SENSITIVE***` | |
| `EMAIL_FROM_NAME` | `eProcure Dev` | `eProcure` | Tên hiển thị người gửi |
| `EMAIL_FROM_ADDRESS` | `dev@eprocure.vn` | `noreply@eprocure.vn` | |
| `EMAIL_RETRY_MAX` | `3` | `3` | Số lần retry khi gửi lỗi |
| `EMAIL_RETRY_DELAY_SECONDS` | `30` | `30` | Delay giữa các lần retry |
| `WEBSOCKET_ALLOWED_ORIGINS` | `http://localhost:4200` | `https://app.eprocure.vn` | WebSocket CORS |
| `NOTIFICATION_RETENTION_DAYS` | `90` | `90` | Giữ notification bao nhiêu ngày |

---

## 12. ADMIN SERVICE

| Biến | Dev | Prod | Mô tả |
|---|---|---|---|
| `ADMIN_DB_HOST` | `postgres` | `postgres-audit.internal` | |
| `ADMIN_DB_NAME` | `db_audit` | `db_audit` | |
| `ADMIN_DB_SCHEMA` | `audit` | `audit` | |
| `ADMIN_DB_USER` | `audit_user` | `audit_user` | |
| `ADMIN_DB_PASS` | `audit_pass_dev` | `***SENSITIVE***` | |
| `AUDIT_LOG_RETENTION_YEARS` | `5` | `5` | Giữ audit log 5 năm |
| `SYSTEM_CONFIG_2FA_REQUIRED` | `false` | `true` | 2FA bắt buộc cho SYSTEM_CONFIG |

---

## 13. KAFKA (SHARED)

| Biến | Dev | Prod | Mô tả |
|---|---|---|---|
| `KAFKA_BOOTSTRAP_SERVERS` | `kafka:9092` | `kafka-1:9092,kafka-2:9092,kafka-3:9092` | Broker list |
| `KAFKA_SECURITY_PROTOCOL` | `PLAINTEXT` | `SASL_SSL` | Security protocol |
| `KAFKA_SASL_MECHANISM` | *(empty)* | `PLAIN` | SASL mechanism (prod) |
| `KAFKA_SASL_USERNAME` | *(empty)* | `***SENSITIVE***` | SASL username |
| `KAFKA_SASL_PASSWORD` | *(empty)* | `***SENSITIVE***` | SASL password |
| `KAFKA_GROUP_ID` | `{service-name}-group` | `{service-name}-group` | Consumer group |
| `KAFKA_AUTO_OFFSET_RESET` | `earliest` | `earliest` | |
| `KAFKA_ENABLE_IDEMPOTENCE` | `true` | `true` | Producer idempotence |
| `KAFKA_ACKS` | `all` | `all` | Producer acks |
| `KAFKA_RETRIES` | `3` | `5` | Producer retries |
| `KAFKA_CONSUMED_CACHE_TTL_HOURS` | `48` | `48` | TTL Kafka idempotency key (Redis) |

### Topics cần tạo trước:
```bash
kafka-topics --create --topic procurement.pr.submitted      --partitions 3 --replication-factor 1
kafka-topics --create --topic procurement.pr.approved       --partitions 3 --replication-factor 1
kafka-topics --create --topic procurement.pr.rejected       --partitions 3 --replication-factor 1
kafka-topics --create --topic approval.step.assigned        --partitions 3 --replication-factor 1
kafka-topics --create --topic approval.sla.breached         --partitions 3 --replication-factor 1
kafka-topics --create --topic finance.budget.warning        --partitions 3 --replication-factor 1
kafka-topics --create --topic procurement.po.issued         --partitions 3 --replication-factor 1
kafka-topics --create --topic inventory.gr.created          --partitions 3 --replication-factor 1
kafka-topics --create --topic finance.invoice.matched       --partitions 3 --replication-factor 1
kafka-topics --create --topic procurement.emergency.abuse   --partitions 1 --replication-factor 1
```

---

## 14. REDIS (SHARED)

| Biến | Dev | Prod | Mô tả |
|---|---|---|---|
| `REDIS_HOST` | `redis` | `redis-master.internal` | |
| `REDIS_PORT` | `6379` | `6379` | |
| `REDIS_PASS` | *(empty)* | `***SENSITIVE***` | |
| `REDIS_DB` | `0` | `0` | DB index (0=session, 1=cache, 2=idempotency) |
| `REDIS_MAX_POOL` | `10` | `50` | Lettuce max connections |
| `REDIS_MIN_IDLE` | `2` | `5` | Min idle connections |
| `REDIS_TIMEOUT_MS` | `3000` | `3000` | Command timeout |

---

## 15. OBSERVABILITY (SHARED)

| Biến | Dev | Prod | Mô tả |
|---|---|---|---|
| `OTEL_SERVICE_NAME` | `{service-name}` | `{service-name}` | |
| `OTEL_EXPORTER_OTLP_ENDPOINT` | `http://tempo:4317` | `http://tempo.monitoring:4317` | Tempo |
| `OTEL_EXPORTER_OTLP_PROTOCOL` | `grpc` | `grpc` | |
| `OTEL_TRACES_SAMPLER` | `always_on` | `parentbased_traceidratio` | Sampling strategy |
| `OTEL_TRACES_SAMPLER_ARG` | `1.0` | `0.1` | Sample 10% ở prod |
| `MANAGEMENT_PROMETHEUS_ENABLED` | `true` | `true` | Expose /actuator/prometheus |
| `PROMETHEUS_HOST` | `prometheus` | `prometheus.monitoring` | |
| `LOKI_URL` | `http://loki:3100` | `http://loki.monitoring:3100` | Loki push URL |
| `GRAFANA_URL` | `http://grafana:3000` | `https://grafana.eprocure.internal` | |
| `GRAFANA_ADMIN_PASS` | `admin` | `***SENSITIVE***` | |

---

## 16. NGINX (FRONTEND)

| Biến | Dev | Prod | Mô tả |
|---|---|---|---|
| `NGINX_PORT_HTTP` | `80` | `80` | HTTP port (redirect sang HTTPS) |
| `NGINX_PORT_HTTPS` | `443` | `443` | HTTPS port |
| `SSL_CERT_PATH` | *(empty)* | `/etc/nginx/ssl/cert.pem` | TLS certificate |
| `SSL_KEY_PATH` | *(empty)* | `/etc/nginx/ssl/key.pem` | TLS private key |
| `API_BASE_URL` | `http://api-gateway:8080` | `http://api-gateway:8080` | Backend URL cho proxy |
| `ANGULAR_API_URL` | `http://localhost:8080/api/v1` | `https://api.eprocure.vn/api/v1` | Angular env var |
| `ANGULAR_WS_URL` | `ws://localhost:8088/ws` | `wss://api.eprocure.vn/ws` | WebSocket URL |

---

## 17. FILE .env.example (ROOT)

```dotenv
# ═══════════════════════════════════════════════════════════
# eProcure Enterprise — Environment Variables Template
# Copy file này thành .env và điền giá trị thực
# KHÔNG commit file .env lên Git
# ═══════════════════════════════════════════════════════════

# ── Timezone ────────────────────────────────────────────────
TZ=Asia/Ho_Chi_Minh
SPRING_PROFILES_ACTIVE=dev

# ── Encryption ──────────────────────────────────────────────
ENCRYPTION_ENABLED=false
RSA_KEY_VERSION=v2025-01
RSA_PRIVATE_KEY=

# ── Inter-service Auth ───────────────────────────────────────
X_API_KEY=change-me-internal-api-key

# ── PostgreSQL (dev: single instance) ───────────────────────
POSTGRES_HOST=postgres
POSTGRES_PORT=5432
POSTGRES_ADMIN_USER=postgres
POSTGRES_ADMIN_PASS=postgres_dev

# IAM DB
IAM_DB_NAME=db_iam
IAM_DB_USER=iam_user
IAM_DB_PASS=iam_pass_dev

# PR & Approval DB
PR_DB_NAME=db_procurement
PR_DB_USER=pr_user
PR_DB_PASS=pr_pass_dev

# Finance DB
FINANCE_DB_NAME=db_finance
FINANCE_DB_USER=finance_user
FINANCE_DB_PASS=finance_pass_dev

# Inventory DB
INVENTORY_DB_NAME=db_inventory
INVENTORY_DB_USER=inventory_user
INVENTORY_DB_PASS=inv_pass_dev

# Vendor DB
VENDOR_DB_NAME=db_vendor
VENDOR_DB_USER=vendor_user
VENDOR_DB_PASS=vendor_pass_dev

# Notification DB
NOTIFICATION_DB_NAME=db_notification
NOTIFICATION_DB_USER=notif_user
NOTIFICATION_DB_PASS=notif_pass_dev

# Audit DB
AUDIT_DB_NAME=db_audit
AUDIT_DB_USER=audit_user
AUDIT_DB_PASS=audit_pass_dev

# Camunda DB
CAMUNDA_DB_NAME=db_camunda
CAMUNDA_DB_USER=camunda_user
CAMUNDA_DB_PASS=camunda_pass_dev

# ── Redis ────────────────────────────────────────────────────
REDIS_HOST=redis
REDIS_PORT=6379
REDIS_PASS=

# ── Kafka ────────────────────────────────────────────────────
KAFKA_BOOTSTRAP_SERVERS=kafka:9092

# ── Keycloak ─────────────────────────────────────────────────
KEYCLOAK_URL=http://keycloak:8080
KEYCLOAK_REALM=eprocure
KEYCLOAK_CLIENT_ID=eprocure-iam
KEYCLOAK_CLIENT_SECRET=change-me-keycloak-secret
KEYCLOAK_ADMIN_USER=admin
KEYCLOAK_ADMIN_PASS=admin
IAM_PROVIDER_BASE_URL=http://iam-service:8081
IAM_PROVIDER_TIMEOUT_SECONDS=3
IAM_INTERNAL_API_KEY=change-me-internal-api-key

# ── IAM 2FA ─────────────────────────────────────────────────
TWO_FACTOR_ISSUER=eProcure Dev
TOTP_SECRET_ENCRYPTION_KEY=MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=
TWO_FACTOR_CHALLENGE_TTL_MINUTES=5

# ── Google OAuth ─────────────────────────────────────────────
GOOGLE_CLIENT_ID=
GOOGLE_CLIENT_SECRET=
GOOGLE_REDIRECT_URI=http://localhost:8081/api/v1/auth/oauth/google/callback
GOOGLE_OAUTH_STATE_TTL_MINUTES=5

# ── Brevo SMTP ───────────────────────────────────────────────
EMAIL_ENABLED=false
BREVO_API_KEY=
BREVO_SMTP_PASS=
EMAIL_FROM_ADDRESS=dev@eprocure.vn
EMAIL_FROM_NAME=eProcure Dev

# ── OpenTelemetry ────────────────────────────────────────────
OTEL_EXPORTER_OTLP_ENDPOINT=http://tempo:4317
OTEL_EXPORTER_OTLP_PROTOCOL=grpc

# ── Grafana ──────────────────────────────────────────────────
GRAFANA_ADMIN_PASS=admin
```

---

## 18. CHECKLIST TRƯỚC KHI DEPLOY PRODUCTION

```
✅ Tất cả *_PASS, *_SECRET, *_KEY đã thay placeholder bằng giá trị thực mạnh
✅ ENCRYPTION_ENABLED=true
✅ COOKIE_SECURE=true
✅ COOKIE_SAMESITE=Strict
✅ EMAIL_ENABLED=true + Brevo API key hợp lệ
✅ KAFKA_SECURITY_PROTOCOL=SASL_SSL
✅ OTEL_TRACES_SAMPLER_ARG=0.1 (không sample 100% ở prod)
✅ SSL_CERT_PATH và SSL_KEY_PATH trỏ đến cert thực
✅ CORS_ALLOWED_ORIGINS chỉ chứa domain production
✅ SYSTEM_CONFIG_2FA_REQUIRED=true
✅ LOG_LEVEL_APP=INFO (không để DEBUG ở prod)
✅ SPRING_PROFILES_ACTIVE=prod
✅ Tất cả Kafka topics đã được tạo với replication-factor >= 2
✅ Redis có password
✅ PostgreSQL không dùng user postgres mặc định cho app
✅ File .env KHÔNG được commit lên Git
```
