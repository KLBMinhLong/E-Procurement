# Decision Log

## [2026-05-17] E01 local infrastructure baseline

- Decision: Tạo Docker Compose nền ở chế độ infra-only gồm PostgreSQL, Redis, Zookeeper/Kafka, Kafka topic init, Keycloak realm import, NGINX gateway skeleton và monitoring profile Prometheus/Grafana/Loki/Tempo.
- Reason: Các Spring/Angular service image chưa tồn tại; đưa app containers vào compose lúc này sẽ làm stack fail. App service containers sẽ được thêm khi skeleton từng service được tạo.
- Impact: Developer có thể chạy hạ tầng trước bằng `docker compose up -d postgres redis zookeeper kafka kafka-init keycloak nginx-gateway`; monitoring bật riêng bằng `--profile monitoring`.
- Constraint: Keycloak custom provider chưa được implement trong E01; chỉ có realm/dev users để phục vụ IAM credential verification ở E02.
