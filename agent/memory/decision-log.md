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

## [2026-05-17] E02 IAM admin user and RBAC management API

- Decision: Implement admin user/profile, role, and permission APIs against the IAM database profile tables; new users are created as `PENDING_VERIFY` with `keycloak_username = username`.
- Reason: The current OpenAPI `CreateUserRequest` has no credential/password provisioning contract, so Keycloak account creation must remain a separate identity provisioning flow.
- Impact: Admins can list, create, view, update, status-change users, replace user roles, create roles, update role permissions, and list permission codes using `ADMIN_USER_*` and `ADMIN_ROLE_MANAGE` permission codes.
- Constraint: `Idempotency-Key` is validated as a required UUID v4 at UseCase entry; full Redis replay caching remains a cross-cutting idempotency implementation task.

## [2026-05-17] E02 organization and approver resolution API

- Decision: Add organization read endpoints guarded by `ORG_VIEW` and approver resolution guarded by `ORG_APPROVER_RESOLVE`.
- Reason: Approval Engine needs a permission-code protected way to resolve approver candidates without hardcoding roles in downstream services.
- Impact: IAM exposes `/api/v1/org/departments`, `/api/v1/org/departments/{id}/members`, and `/api/v1/org/approvers`; approver resolution scopes to department ancestors and descendants, excludes `requester_id` when supplied, and returns `IAM_034` if no candidate is available.
- Constraint: Delegation-aware substitution remains deferred to the P1 delegation API slice because the current response schema only returns `UserSummary`.

## [2026-05-17] E02 delegation management API

- Decision: Implement user-owned delegation management with `GET /delegations`, `POST /delegations`, and `PATCH /delegations/{id}/revoke`, all guarded by `DELEGATION_MANAGE`.
- Reason: Approvers need a controlled way to create and revoke approval delegation periods before the approval engine consumes delegation data.
- Impact: Delegations enforce distinct delegator/delegate users, active-user checks, non-overlapping active periods, max value precision, and same-or-higher org-level delegation based on `org_nodes.path` depth.
- Constraint: Applying delegation during approver resolution remains deferred until the approver lookup API includes amount/category context, otherwise `maxValue` and category limits cannot be enforced safely.

## [2026-05-17] E02 IAM TOTP two-factor authentication

- Decision: Implement TOTP setup/confirm and post-login verification with a short-lived HttpOnly `ep_2fa` challenge cookie before issuing the real `ep_session` cookie.
- Reason: A user with 2FA enabled must not receive a full session immediately after password verification; the second factor must complete before session creation and single-session revocation.
- Impact: `/users/me/two-factor/enable`, `/users/me/two-factor/confirm`, and `/auth/two-factor/verify` are implemented. TOTP secrets are AES-GCM encrypted with `TOTP_SECRET_ENCRYPTION_KEY`, and backup codes are returned once while only hashes are stored.
- Constraint: Backup-code login is deferred because the current OpenAPI verification contract accepts only six-digit TOTP codes.

## [2026-05-17] E02 Google OAuth login

- Decision: Implement Google OAuth as a browser redirect flow with one-time Redis-backed state and an HttpOnly `ep_oauth_state` cookie.
- Reason: OAuth callback must validate anti-CSRF state before exchanging the authorization code or issuing an internal opaque session.
- Impact: `/auth/oauth/google` redirects to Google, `/auth/oauth/google/callback` validates state/code, fetches Google userinfo, links `google_oauth_id` to an existing IAM user by email, and then issues `ep_session` or `ep_2fa` if local 2FA is enabled.
- Constraint: Google OAuth does not auto-create IAM users; accounts must already exist in IAM to preserve admin-controlled RBAC and org assignment.
