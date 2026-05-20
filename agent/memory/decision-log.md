# Decision Log

## [2026-05-17] E01 local infrastructure baseline

- Decision: Tạo Docker Compose nền ở chế độ infra-only gồm PostgreSQL, Redis, Kafka (KRaft), Kafka topic init, Keycloak realm import, NGINX gateway skeleton và monitoring profile Prometheus/Grafana/Loki/Tempo.
- Reason: Các Spring/Angular service image chưa tồn tại; đưa app containers vào compose lúc này sẽ làm stack fail. App service containers sẽ được thêm khi skeleton từng service được tạo.
- Impact: Developer có thể chạy hạ tầng trước bằng `docker compose up -d postgres redis kafka kafka-init keycloak nginx-gateway`; monitoring bật riêng bằng `--profile monitoring`.
- Constraint: Keycloak custom provider được hoàn thiện ở lát cắt E01/E02 sau; dev users nằm trong IAM DB, không seed credential cục bộ trong Keycloak.

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

## [2026-05-17] E02 IAM forgot/reset password

- Decision: Implement forgot/reset password with persisted SHA-256 reset-token hashes, password-history checks, IAM BCrypt(password + userId) credential reset, password-history records, and active session revocation including Redis cache eviction.
- Reason: Reset password must not expose whether an email exists, must not persist raw reset tokens, and must keep IAM as the credential source federated through the Keycloak User Storage SPI.
- Impact: `/auth/forgot-password` and `/auth/reset-password` are public endpoints with required `Idempotency-Key`; reset tokens live for `RESET_TOKEN_TTL_MINUTES` and all active user sessions are invalidated after a successful reset.
- Constraint: The production email sender remains behind `PasswordResetDeliveryPort`; the current IAM slice includes a non-sensitive stub until E11 notification-service/Brevo adapter is available.

## [2026-05-17] E01 Keycloak IAM User Storage SPI

- Decision: Add `eprocure-keycloak-provider` as a Keycloak User Storage SPI module and configure the realm to federate users from IAM through `/internal/keycloak/**`.
- Reason: Keycloak must verify credentials without owning user business data or local seeded credentials; IAM remains the source for users, password hashes, status, roles and sessions.
- Impact: Keycloak image is now built from `infra/keycloak/Dockerfile`, installs the provider JAR, and calls IAM with `X-Internal-Api-Key`; IAM exposes internal lookup and credential verification endpoints excluded from request encryption.
- Constraint: Existing local Keycloak volumes may still contain old seeded users; recreate the local Keycloak/PostgreSQL data when validating the new realm import path.

## [2026-05-17] E02 IAM logging rule alignment

- Decision: Standardize IAM Java loggers on Log4j2 imports and add the masking helpers referenced by `.cursor/rules/logging.mdc`.
- Reason: The runtime already uses `spring-boot-starter-log4j2`, but older slices still imported SLF4J, which conflicts with the always-applied logging rule and creates noisy review churn for future features.
- Impact: Existing log messages keep their layer prefixes while logger creation now uses `LogManager.getLogger(...)`; `LogMaskingUtil` now supports phone, token, and name masking.
- Constraint: This is a mechanical rule-alignment change only; log message content and levels are intentionally unchanged except for helper availability.

## [2026-05-18] E02 RBAC session and role-permission cache consistency

- Decision: IAM session cache stores user identity and role codes only; permission authorities are resolved on each authentication from `role-perm:{roleCode}` cache with DB fallback.
- Reason: Caching a permission snapshot inside session lets a user keep stale authorities after an admin changes a role's permissions.
- Impact: Updating role permissions refreshes `role-perm:{roleCode}`; updating a user's roles evicts active session cache for that user so the next request reloads current roles from DB.
- Constraint: Redis remains cache-only; IAM DB role and permission tables are still the source of truth.

## [2026-05-18] E03 Angular 21 frontend baseline

- Decision: Standardize frontend documentation on Angular 21 with standalone-first components, signals, and control flow blocks.
- Reason: Align frontend guidance with the modern Angular baseline before any UI code is scaffolded.
- Impact: E03 UI shell and design-system docs reference Angular 21, standalone app config/routes, and signal-driven view state.
- Constraint: Exact package versions will be finalized when the frontend workspace is generated.

## [2026-05-18] E01 Kafka KRaft mode (no ZooKeeper)

- Decision: Switch local Kafka to KRaft mode and remove ZooKeeper from the compose stack.
- Reason: Kafka supports KRaft natively, reducing dev infra complexity and resource usage.
- Impact: Core stack runs with `docker compose up -d postgres redis kafka kafka-init keycloak nginx-gateway`; KRaft uses `KAFKA_CLUSTER_ID` persisted in the Kafka volume.
- Constraint: If changing `KAFKA_CLUSTER_ID` or switching modes, delete the Kafka data volume to reformat storage.

## [2026-05-18] E03 Angular UI shell foundation

- Decision: Scaffold `frontend/eprocure-web` as an Angular 21 standalone app and use `ep-*` shared components backed by `@lucide/angular`, ngx-translate, functional guards, and functional HTTP interceptors.
- Reason: E03 needs a reusable shell/design-system foundation before PR/Approval screens so feature pages do not duplicate layout, i18n, permission, and request conventions.
- Impact: `/ui-showcase` is public for visual QA; authenticated workspace routes use `AuthGuard` and permission-code based `PermissionGuard`; POST/PUT/PATCH receive `Idempotency-Key` automatically and all requests use `withCredentials=true`.
- Constraint: The frontend RSA+AES request/response encryption adapter is still pending; the current login flow keeps the feature flag/hook but sends plain payload when local/dev encryption is disabled.

## [2026-05-18] E03 frontend request encryption adapter

- Decision: Add an Angular functional encryption interceptor that encrypts JSON POST/PUT/PATCH request bodies with AES-256-GCM and wraps the AES key with the IAM RSA-OAEP-SHA-256 public key.
- Reason: Encryption must remain cross-cutting at the HTTP layer so feature services do not duplicate security code and so login/logout/idempotent state changes all follow the same contract.
- Impact: When `ENCRYPTION_ENABLED=true`, frontend sends `{ encryptedPayload, encryptedAesKey, iv, keyVersion }`; the public key is fetched from `/auth/public-key` via `HttpBackend` and cached only in memory. `SYS_003` clears the key cache.
- Constraint: Response body decryption is intentionally not implemented because IAM currently only decrypts requests; response encryption remains a later hardening slice once the backend response filter contract is implemented.

## [2026-05-18] Keycloak realm env placeholders and login polish

- Decision: Move Keycloak realm client secret, IAM provider base URL, and provider timeout to environment-driven placeholders and remove Java fallback to the fixed IAM service URL.
- Reason: Realm import and provider runtime config must not persist development secrets or topology-specific URLs in source-controlled code.
- Impact: `.env` must provide `KEYCLOAK_CLIENT_SECRET`, `IAM_PROVIDER_BASE_URL`, and `IAM_INTERNAL_API_KEY`; `IAM_PROVIDER_TIMEOUT_SECONDS` defaults to 3 seconds in the local sample. Login UI now centers the brand/action area and exposes Google login, forgot-password, and password visibility actions.
- Constraint: Existing Keycloak volumes keep previously imported realm values; recreate Keycloak/PostgreSQL volumes when validating the new realm import.

## [2026-05-19] E04 Purchase Request service foundation

- Decision: Scaffold `purchase-request-service` as a Maven module with Spring Boot port 8082, PR datasource/Flyway config, Dockerfile, compose service, PR/catalog/attachment foundation migration, and domain POJO aggregate models.
- Reason: E04 needs a runnable backend and stable domain/schema boundary before adding Create/Submit/Update/Cancel use cases and controllers.
- Impact: Docker can start `pr-service` without Kafka; Flyway creates schema objects under `pr`, and domain tests cover Money precision, line item totals, draft creation, submit, cancel, and requester ownership checks.
- Constraint: Controllers, MyBatis repositories, idempotency replay, Redis adapters, budget/inventory ports, and Kafka publishing remain deferred to the next E04 slices.

## [2026-05-19] E04 Create Purchase Request command slice

- Decision: Implement Create PR as the first vertical slice with gateway header authentication, permission-code `PR_CREATE`, Redis-backed idempotency replay, MyBatis inserts, and a small catalog-category seed migration.
- Reason: Submit/update/cancel depend on a persisted draft PR path and a repeatable POST contract with `Idempotency-Key`.
- Impact: `POST /api/v1/purchase-requests` creates draft PRs, stores line items, returns `ApiResponse<CreatedPurchaseRequestResponse>`, and replays cached responses with `Idempotency-Replayed: true`.
- Constraint: Gateway is assumed to pass `X-User-ID`, `X-Department-ID`, and `X-Permissions`; list/detail/update/submit/cancel, catalog APIs, attachments, and Kafka events remain later E04 slices.

## [2026-05-19] E04 Submit Purchase Request command slice

- Decision: Implement Submit PR through `BudgetCheckPort`, `InventoryCheckPort`, and `PrSubmittedEventPublisher`, with local fallback adapters enabled by default through `eprocure.pr.integration.fallback-enabled`.
- Reason: Submit must persist budget/inventory snapshots and emit the domain event without coupling the PR service to Finance, Inventory, or Kafka transports before those epics are implemented.
- Impact: `PATCH /api/v1/purchase-requests/{id}/submit` validates requester/status/idempotency, stores `budget_check_status` plus `inventory_check`, transitions PR to `SUBMITTED`, and replays cached submit responses with `Idempotency-Replayed: true`.
- Constraint: Real Finance/Inventory integrations, Kafka transport publishing, submit-with-override, approval process creation, and PR detail/list APIs remain later E04/E05 slices.

## [2026-05-20] E04 UpdatePR + CancelPR + GetPR list/detail slice

- Decision: Implement UpdatePR (PUT /{id}), CancelPR (PATCH /{id}/cancel), GetPR Detail (GET /{id}), and GetPR List (GET /) as a single vertical slice completing the E04 PR CRUD lifecycle.
- Reason: After Create and Submit, Update and Cancel complete the requester's workflow. GetPR list/detail are required before the frontend can render the PR module.
- Impact: `updateWithLineItems()` replaces line items atomically (soft-delete old + insert new); `findByFilter()`/`countByFilter()` delegate to an XML mapper with dynamic WHERE clause and whitelist ORDER BY. ViewScope (OWN/DEPARTMENT/ALL) is resolved from the principal's granted authorities in the controller. `ApiResponse.ok()` and `successWithMeta()` factory methods added. 26 tests pass.
- Constraint: File attachment upload, catalog API, Kafka real transport, and PR frontend pages remain deferred to subsequent E04 slices.
