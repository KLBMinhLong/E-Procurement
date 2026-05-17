# E01 Infrastructure Setup
## User Stories và Use Cases

---

## 1. Epic Goal

Cung cấp môi trường local/dev chạy được nền tảng eProcure với Docker Compose, PostgreSQL multi database/schema, Redis, Kafka, Keycloak, NGINX gateway và monitoring profile tùy chọn, đủ nhẹ cho máy dev 8GB RAM.

---

## 2. Actors

| Actor | Vai trò |
|---|---|
| Developer | Chạy stack local để code/test service |
| DevOps Engineer | Quản lý compose, env, resource limits, healthchecks |
| Backend Service | Kết nối DB, Redis, Kafka, Keycloak |
| Frontend App | Gọi API qua gateway/NGINX |

---

## 3. User Stories

| ID | Story | Priority |
|---|---|---|
| E01-US-001 | Là Developer, tôi muốn chạy toàn bộ hạ tầng bằng một lệnh để bắt đầu code nhanh. | MVP |
| E01-US-002 | Là Backend Service, tôi muốn PostgreSQL có nhiều database/schema đúng ownership để service không dùng chung schema `public`. | MVP |
| E01-US-003 | Là IAM/PR/Approval Service, tôi muốn Redis sẵn sàng cho session, permission cache và idempotency. | MVP |
| E01-US-004 | Là service event-driven, tôi muốn Kafka có topic chuẩn để publish/consume event nghiệp vụ. | MVP |
| E01-US-005 | Là IAM Service, tôi muốn Keycloak realm được seed để verify credential mà không lưu user business data trong Keycloak. | MVP |
| E01-US-006 | Là Frontend App, tôi muốn NGINX/API gateway route `/api/v1/*` và WSS đến service đúng port. | MVP |
| E01-US-007 | Là DevOps Engineer, tôi muốn monitoring stack bật bằng profile riêng để không làm quá tải máy 8GB. | P1 |
| E01-US-008 | Là Developer, tôi muốn env files rõ ràng, không commit secret, profile `local/dev/prod` nhất quán. | MVP |

---

## 4. Use Cases

### E01-UC-001: StartLocalInfrastructure

**Trigger:** Developer chạy `docker compose up -d`.

**Preconditions:**

```
- Docker Desktop đang chạy.
- .env.local tồn tại từ .env.example.
- Ports 5432, 6379, 9092, 8080-8089 không bị chiếm hoặc đã override.
```

**Main flow:**

```
1. Compose tạo network nội bộ eprocure-net.
2. PostgreSQL khởi động với healthcheck.
3. Redis khởi động với memory limit.
4. Kafka khởi động và topic init script chạy.
5. Keycloak khởi động và import realm.
6. NGINX/gateway khởi động sau khi upstream health.
7. Healthcheck tổng báo các container nền đạt trạng thái healthy.
```

**Alternate/error flows:**

```
- Nếu port conflict, compose fail rõ service/port.
- Nếu DB chưa healthy, service phụ thuộc không start hoặc retry.
- Nếu monitoring profile không bật, Prometheus/Grafana/Loki/Tempo không chạy.
```

**Acceptance criteria:**

```
[ ] `docker compose up -d` chạy thành công trên máy 8GB.
[ ] `docker compose ps` hiển thị core services healthy.
[ ] Resource limits không vượt budget trong AGENTS.md.
[ ] Monitoring chỉ chạy khi dùng `--profile monitoring`.
```

### E01-UC-002: InitializePostgreSQLDatabases

**Trigger:** PostgreSQL container chạy lần đầu.

**Main flow:**

```
1. Tạo databases: db_iam, db_procurement, db_finance, db_inventory, db_vendor, db_notification, db_audit, db_camunda.
2. Tạo schemas: iam, pr, approval, finance, inventory, vendor, notification, audit, camunda.
3. Enable extension `pgcrypto` nếu cần UUID `gen_random_uuid()`.
4. Tạo service users/roles theo least privilege.
5. Không tạo object nghiệp vụ trong schema `public`.
```

**Acceptance criteria:**

```
[ ] Mỗi service có DB/schema đúng tài liệu.
[ ] `public` không chứa table nghiệp vụ.
[ ] Init script idempotent, chạy lại không lỗi.
```

### E01-UC-003: InitializeKafkaTopics

**Trigger:** Kafka broker healthy.

**Main flow:**

```
1. Init script tạo topic trong `agent/knowledge/kafka-topics.md`.
2. Topic có partitions/replication phù hợp local dev.
3. Script bỏ qua topic đã tồn tại.
```

**Acceptance criteria:**

```
[ ] Có topic `procurement.pr.submitted`, `procurement.pr.approved`, `procurement.pr.rejected`.
[ ] Có topic `approval.step.assigned`, `approval.sla.breached`.
[ ] Topic init không fail khi chạy lại.
```

### E01-UC-004: ImportKeycloakRealm

**Trigger:** Keycloak container start.

**Main flow:**

```
1. Import realm eprocure.
2. Seed client nội bộ cho IAM.
3. Seed test users tối thiểu: requester, manager, director, finance, admin.
4. Keycloak chỉ phục vụ credential verification.
```

**Acceptance criteria:**

```
[ ] IAM có thể verify credential với Keycloak.
[ ] User business data vẫn thuộc IAM database.
[ ] Secret không commit hardcoded trong repo.
```

### E01-UC-005: RouteThroughGateway

**Trigger:** Frontend gọi `/api/v1/*`.

**Main flow:**

```
1. NGINX/gateway nhận request.
2. Route `/api/v1/auth/*`, `/api/v1/users/*`, `/api/v1/roles/*`, `/api/v1/org/*` đến IAM.
3. Route PR, approval, finance, inventory, vendor, analytics, notification, admin theo service port.
4. Preserve Cookie, request id, trace headers.
```

**Acceptance criteria:**

```
[ ] Gateway route đúng service port trong AGENTS.md.
[ ] WSS notification route sẵn sàng cho E11.
[ ] Không log cookie/token raw.
```

---

## 5. Technical Deliverables

```
docker-compose.yml
docker-compose.monitoring.yml hoặc compose profile monitoring
.env.example
infra/postgres/init/*.sql
infra/kafka/init-topics.*
infra/keycloak/realm-eprocure.json
infra/nginx/nginx.conf
infra/prometheus/prometheus.yml
infra/grafana/provisioning/*
README setup section
```

---

## 6. Story-Level Checklist

```
[ ] Mọi container có TZ=Asia/Ho_Chi_Minh.
[ ] Java service memory Xmx không vượt 75% memory limit.
[ ] Healthcheck có start_period phù hợp.
[ ] Secret chỉ qua env file, không hardcode.
[ ] PostgreSQL không dùng schema public cho nghiệp vụ.
[ ] Kafka topic names đúng convention.
[ ] Monitoring optional bằng profile.
```
