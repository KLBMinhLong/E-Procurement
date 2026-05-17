## ADR-007 — Log4j2 + OpenTelemetry Java Agent

**Status:** Accepted  
**Date:** 2025-01  

### Bối cảnh
Cần observability đầy đủ: structured logging, distributed tracing, metrics. Mặc định Spring Boot dùng Logback — cần thay thế.

### Quyết định

**Logging:** Log4j2 với custom XML layout
- Bỏ hoàn toàn Logback (exclude khỏi Spring Boot starter)
- Custom PatternLayout với màu ANSI cho dev console
- JSON layout cho production (dễ ingest vào Loki)
- Log theo tầng với level khác nhau (FILTER/CONTROLLER/SERVICE/REPO/CACHE/SECURITY/AUDIT)
- Audit log ghi vào file riêng biệt (không thể sửa/xoá)

**Tracing:** OpenTelemetry Java Instrumentation Agent
- `-javaagent:opentelemetry-javaagent.jar` trong Dockerfile
- Auto-instrument Spring Boot, MyBatis, Kafka, Redis
- Export traces sang Tempo

**Metrics:** Micrometer → Prometheus → Grafana

**Log stack:** Loki (aggregation) + Grafana (query/dashboard)

```xml
<!-- log4j2.xml excerpt -->
<Console name="CONSOLE" target="SYSTEM_OUT">
  <PatternLayout pattern="%highlight{%d{HH:mm:ss.SSS} [%t] %-5level} %cyan{[%logger{1}]} - %msg%n"/>
</Console>
<RollingFile name="AUDIT" fileName="logs/audit.log">
  <JsonTemplateLayout/>  <!-- Immutable audit, append-only -->
</RollingFile>
```

### Hậu quả
- (+) Unified observability stack: log + trace + metric
- (+) Trace ID tự động inject vào log → dễ debug
- (-) Phải exclude Logback khỏi tất cả dependency
- (-) OTel agent thêm ~50ms startup time và ~10% CPU overhead nhỏ