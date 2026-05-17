## SK-30 · Docker Resource Budget

### Trigger
Agent cần phân bổ resource Docker theo budget.

### Inputs Required
- Tổng RAM/CPU của máy
- Danh sách services
- Target limits per service

### Rules
```
[R1] Tổng resource không vượt budget máy
[R2] Service nặng (Camunda, Kafka) cần ưu tiên
[R3] JVM flags dùng để giới hạn heap
```

### Resource Allocation Table (8GB RAM machine)

| Service | CPU Limit | RAM Limit | Notes |
|---|---|---|---|
| nginx-gateway | 0.25 | 128M | Reverse proxy only |
| iam-service | 0.25 | 512M | Auth + RBAC |
| pr-service | 0.25 | 512M | Core business |
| approval-engine | 0.50 | 768M | Camunda BPMN — needs more |
| finance-service | 0.25 | 512M | Budget + Invoice |
| inventory-service | 0.25 | 256M | Lighter service |
| notification-service | 0.10 | 256M | Lightweight |
| vendor-service | 0.25 | 512M | RFQ + Vendor |
| postgresql | 0.50 | 512M | Shared cluster |
| redis | 0.10 | 256M | Session + Cache |
| kafka | 0.50 | 512M | Includes Zookeeper |
| keycloak | 0.25 | 512M | Auth provider |
| prometheus | 0.10 | 256M | Metrics scraper |
| grafana | 0.10 | 128M | Dashboard |
| loki | 0.10 | 128M | Log aggregation |
| tempo | 0.10 | 128M | Tracing backend |
| **TOTAL** | **~3.85** | **~5.84GB** | Within 4-5GB Docker budget |

### JVM Flags cho Spring Boot (giảm memory footprint)
```yaml
environment:
  - JAVA_OPTS=-Xms128m -Xmx384m -XX:+UseG1GC -XX:MaxGCPauseMillis=200
              -XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0
              -Djava.security.egd=file:/dev/./urandom
```

### Checklist
```
[ ] Tổng RAM/CPU nằm trong budget
[ ] Limits set cho từng service
[ ] JVM flags giới hạn heap
```
