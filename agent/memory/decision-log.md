# Decision Log

## [2026-05-17] E01 local infrastructure baseline

- Decision: Tạo Docker Compose nền ở chế độ infra-only gồm PostgreSQL, Redis, Zookeeper/Kafka, Kafka topic init, Keycloak realm import, NGINX gateway skeleton và monitoring profile Prometheus/Grafana/Loki/Tempo.
- Reason: Các Spring/Angular service image chưa tồn tại; đưa app containers vào compose lúc này sẽ làm stack fail. App service containers sẽ được thêm khi skeleton từng service được tạo.
- Impact: Developer có thể chạy hạ tầng trước bằng `docker compose up -d postgres redis zookeeper kafka kafka-init keycloak nginx-gateway`; monitoring bật riêng bằng `--profile monitoring`.
- Constraint: Keycloak custom provider chưa được implement trong E01; chỉ có realm/dev users để phục vụ IAM credential verification ở E02.

## [2026-05-17] E02 IAM auth/session foundation

- Decision: Scaffold IAM Service theo Clean Architecture với domain POJO, UseCase transaction boundary, MyBatis repositories, Flyway IAM core schema, Keycloak password verification, opaque 64-char session token, Redis session cache và HttpOnly `ep_session` cookie.
- Reason: Các service tiếp theo cần auth/RBAC ổn định trước khi triển khai PR, approval và frontend shell. Login trả token qua cookie, response body không chứa token.
- Impact: `/api/v1/auth/public-key`, `/api/v1/auth/login`, `/api/v1/auth/logout` và `/api/v1/users/me` là lát cắt chạy được đầu tiên của E02; admin user/role/delegation/2FA/OAuth/forgot-password sẽ đi các feature branch sau.
- Constraint: RSA+AES encryption interceptor chưa được implement; endpoint public-key hiện trả configured public key để giữ API contract cho bước hardening tiếp theo.

## [2026-05-17] E02 RSA+AES request decryption

- Decision: Implement RSA+AES request decryption as a high-priority `OncePerRequestFilter` guarded by `ENCRYPTION_ENABLED`, instead of `HandlerInterceptor`.
- Reason: `HandlerInterceptor` cannot replace the servlet request body consumed by `@RequestBody`; a filter can wrap `HttpServletRequest` with decrypted JSON without changing controllers.
- Impact: When encryption is enabled, POST/PUT/PATCH JSON requests must send `encryptedPayload`, `encryptedAesKey`, `iv`, and `keyVersion`; the filter decrypts to plaintext JSON before controller validation. GET, actuator, and `/api/v1/auth/public-key` are skipped.
- Constraint: Response encryption remains deferred until the frontend public-key exchange contract is explicit.
