# tech-stack.md
## eProcure Enterprise — Tech Stack & Versions

> Quick reference khi cần biết version, import path hoặc cấu hình đặc thù.

---

## BACKEND (Java / Spring Boot)

```xml
<!-- pom.xml parent -->
<parent>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-parent</artifactId>
    <version>3.2.x</version>
</parent>

<!-- Java version -->
<java.version>17</java.version>

<!-- Key dependencies -->
spring-boot-starter-web
spring-boot-starter-security
spring-boot-starter-validation
spring-boot-starter-actuator
spring-boot-starter-data-redis   ← Lettuce (không dùng Jedis)
spring-boot-starter-log4j2       ← PHẢI exclude spring-boot-starter-logging

mybatis-spring-boot-starter:3.0.x
flyway-core:9.x
flyway-database-postgresql:9.x
HikariCP (built-in Spring Boot)

jackson-databind (built-in)
jackson-datatype-jsr310          ← Java 8 time module

<!-- Kafka -->
spring-kafka:3.x

<!-- Security / Auth -->
spring-security-oauth2-resource-server
keycloak-admin-client:23.x

<!-- Camunda (approval-service only) -->
camunda-bpm-spring-boot-starter:7.x
camunda-bpm-spring-boot-starter-rest:7.x

<!-- Jasper (analytics-service only) -->
jasperreports:6.21.x
poi:5.x  ← Apache POI cho Excel export

<!-- OpenTelemetry (Java Agent — NOT dependency) -->
# -javaagent:/app/agents/opentelemetry-javaagent-2.x.x.jar
# Auto-instrument: Spring, MyBatis, Redis, Kafka, JDBC

<!-- Lombok -->
lombok:1.18.x
mapstruct:1.5.x  ← Chỉ dùng nếu cần, ưu tiên ObjectMapper

<!-- Test -->
spring-boot-starter-test (includes JUnit 5, Mockito, AssertJ)
testcontainers:1.19.x  ← Cho integration test (PostgreSQL, Redis, Kafka)
```

### Exclude Logback (BẮT BUỘC trong mọi service)
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-web</artifactId>
    <exclusions>
        <exclusion>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-logging</artifactId>
        </exclusion>
    </exclusions>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-log4j2</artifactId>
</dependency>
```

### application.yml base config pattern
```yaml
server:
  port: ${SERVER_PORT:8080}

spring:
  application:
    name: ${SERVICE_NAME:unknown-service}
  profiles:
    active: ${SPRING_PROFILES_ACTIVE:dev}
  datasource:
    url: jdbc:postgresql://${DB_HOST:localhost}:${DB_PORT:5432}/${DB_NAME}
    username: ${DB_USER}
    password: ${DB_PASS}
    driver-class-name: org.postgresql.Driver
    hikari:
      minimum-idle:       ${DB_POOL_MIN:2}
      maximum-pool-size:  ${DB_POOL_MAX:10}
      idle-timeout:       300000
      connection-timeout: 20000
      max-lifetime:       1200000
      connection-init-sql: SET search_path TO ${DB_SCHEMA:public}
      pool-name:          ${SERVICE_NAME:svc}-HikariPool
  flyway:
    enabled: true
    locations: classpath:db/migration
    schemas: ${DB_SCHEMA}
    baseline-on-migrate: true
  data:
    redis:
      host: ${REDIS_HOST:localhost}
      port: ${REDIS_PORT:6379}
      password: ${REDIS_PASS:}
      database: ${REDIS_DB:0}
      lettuce:
        pool:
          max-active: ${REDIS_MAX_POOL:10}
          min-idle:   ${REDIS_MIN_IDLE:2}
  kafka:
    bootstrap-servers: ${KAFKA_BOOTSTRAP_SERVERS:localhost:9092}
    producer:
      key-serializer:   org.apache.kafka.common.serialization.StringSerializer
      value-serializer: org.springframework.kafka.support.serializer.JsonSerializer
      acks: all
      retries: 3
      enable-idempotence: true
    consumer:
      group-id:           ${SERVICE_NAME:svc}-group
      key-deserializer:   org.apache.kafka.common.serialization.StringDeserializer
      value-deserializer: org.springframework.kafka.support.serializer.JsonDeserializer
      auto-offset-reset:  earliest
      properties:
        spring.json.trusted.packages: "com.eprocure.*"

management:
  endpoints:
    web:
      exposure:
        include: health,info,prometheus
  endpoint:
    health:
      show-details: when-authorized
  metrics:
    export:
      prometheus:
        enabled: true

mybatis:
  mapper-locations: classpath:mapper/**/*.xml
  configuration:
    map-underscore-to-camel-case: true
    default-fetch-size: 100
    default-statement-timeout: 30
```

---

## FRONTEND (Angular)

```json
// package.json key deps
{
  "@angular/core": "^17.x",
  "@angular/router": "^17.x",
  "@angular/forms": "^17.x",
  "@angular/common": "^17.x",
  "@ngx-translate/core": "^15.x",
  "@ngx-translate/http-loader": "^8.x",
  "@stomp/stompjs": "^7.x",
  "sockjs-client": "^1.x",
  "rxjs": "^7.x",
  "typescript": "~5.2"
}
```

```typescript
// angular.json - key config
"budgets": [
  { "type": "initial", "maximumWarning": "1mb", "maximumError": "2mb" },
  { "type": "anyComponentStyle", "maximumWarning": "4kb", "maximumError": "8kb" }
]

// tsconfig.json
"strict": true,
"strictTemplates": true,
"noImplicitAny": true
```

---

## INFRASTRUCTURE

| Component | Image | Version | Port (dev) |
|---|---|---|---|
| PostgreSQL | postgres:15-alpine | 15.x | 5432 |
| Redis | redis:7-alpine | 7.x | 6379 |
| Kafka | confluentinc/cp-kafka | 7.5.0 | 9092 |
| Zookeeper | confluentinc/cp-zookeeper | 7.5.0 | 2181 |
| Keycloak | quay.io/keycloak/keycloak | 23.0 | 8180 |
| Prometheus | prom/prometheus | v2.47.0 | 9090 |
| Grafana | grafana/grafana | 10.1.0 | 3000 |
| Loki | grafana/loki | 2.9.0 | 3100 |
| Tempo | grafana/tempo | 2.2.0 | 4317 |
| NGINX | nginx:1.25-alpine | 1.25.x | 80/443 |

---

## JVM STARTUP FLAGS (Dockerfile)

```bash
JAVA_OPTS="\
  -Xms128m \
  -Xmx384m \
  -XX:+UseContainerSupport \
  -XX:MaxRAMPercentage=75.0 \
  -XX:+UseG1GC \
  -XX:+OptimizeStringConcat \
  -Djava.security.egd=file:/dev/./urandom \
  -javaagent:/app/agents/opentelemetry-javaagent.jar"

# OTel config (ENV)
OTEL_SERVICE_NAME=${SERVICE_NAME}
OTEL_EXPORTER_OTLP_ENDPOINT=http://tempo:4317
OTEL_EXPORTER_OTLP_PROTOCOL=grpc
OTEL_TRACES_SAMPLER=always_on        # dev
OTEL_TRACES_SAMPLER=parentbased_traceidratio  # prod
OTEL_TRACES_SAMPLER_ARG=0.1          # prod: 10% sampling
```

---

## SECURITY LIBS

```java
// Password hashing
BCrypt (spring-security-crypto, built-in)
cost factor = 12

// RSA + AES Encryption
java.security.KeyPairGenerator   ← RSA-2048 key generation
javax.crypto.Cipher              ← RSA/ECB/OAEPWithSHA-256AndMGF1Padding
javax.crypto.SecretKey           ← AES-256-GCM
javax.crypto.spec.GCMParameterSpec ← IV 12 bytes cho GCM

// TOTP (2FA)
// Library: dev.samstevens.totp:totp:1.7.1
// hoặc: com.warrenstrange:googleauth:1.5.0

// Google OAuth
spring-security-oauth2-client (built-in Spring Security)
```

---

## JASPER REPORT

```java
// Locations
src/main/resources/reports/
├── pr-summary.jrxml           ← PR summary report
├── budget-vs-plan.jrxml
├── vendor-scorecard.jrxml
├── audit-trail.jrxml
└── shared/
    ├── header.jrxml           ← Common header subreport
    └── footer.jrxml

// Compile + fill pattern
JasperReport compiled = JasperCompileManager.compileReport(inputStream);
JasperPrint print = JasperFillManager.fillReport(compiled, params, dataSource);

// Export PDF
JasperExportManager.exportReportToPdfFile(print, outputPath);

// Export Excel (JRXlsxExporter)
JRXlsxExporter exporter = new JRXlsxExporter();
exporter.setExporterInput(new SimpleExporterInput(print));
exporter.setExporterOutput(new SimpleOutputStreamExporterOutput(outputStream));
exporter.exportReport();
```

---

## CAMUNDA BPMN KEY APIs

```java
// Inject trong approval-service
@Autowired private RuntimeService runtimeService;
@Autowired private TaskService taskService;
@Autowired private HistoryService historyService;
@Autowired private ManagementService managementService;

// Start process
ProcessInstance pi = runtimeService.startProcessInstanceByKey(
    "pr-approval-process",
    Map.of("prId", prId.toString(), "totalAmount", totalAmount.toPlainString())
);

// Get task cho approver
List<Task> tasks = taskService.createTaskQuery()
    .taskAssignee(userId.toString())
    .processDefinitionKey("pr-approval-process")
    .list();

// Complete task
taskService.complete(taskId, Map.of("approved", true, "comment", comment));

// BPMN files location
src/main/resources/bpmn/
├── pr-approval-process.bpmn
├── rfq-process.bpmn
├── po-process.bpmn
└── emergency-approval.bpmn
```

---

## REDIS KEY PATTERNS

```
session:{tokenHash}               → SessionData JSON with userId, expiresAt, roles only (TTL 8h sliding)
role-perm:{roleCode}              → Set<permissionCode> (TTL PERM_CACHE_TTL_MINUTES)
idempotent:{idempotencyKey}       → CachedResponse JSON (TTL 24h)
kafka-consumed:{messageId}        → "1" (TTL 48h, Kafka dedup)
budget:{deptId}:{year}:{quarter}  → BudgetDashboard JSON (TTL 5min)
dashboard:manager:{userId}        → ManagerDashboard JSON (TTL 5min)
dashboard:executive:{userId}      → ExecDashboard JSON (TTL 5min)
reset-token:{tokenHash}           → optional cache; iam.password_reset_tokens is source of truth (TTL 15min)
login-attempts:{email}            → Integer count (TTL 30min)
```

---

## FLYWAY MIGRATION LOCATIONS

```
services/iam-service/src/main/resources/db/migration/
services/pr-service/src/main/resources/db/migration/
services/approval-service/src/main/resources/db/migration/
services/finance-service/src/main/resources/db/migration/
services/inventory-service/src/main/resources/db/migration/
services/vendor-service/src/main/resources/db/migration/
services/notification-service/src/main/resources/db/migration/
services/admin-service/src/main/resources/db/migration/
```
