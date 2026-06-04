# Decision Log

## [2026-06-01] E11 notification template admin

- Decision: Manage notification templates through `SYSTEM_CONFIG` guarded list/update/preview APIs and an Angular admin page at `/admin/notification-templates`.
- Reason: E11 needs a safe operational surface to change seeded EMAIL/IN_APP copy without altering historical notification rows or requiring database edits.
- Impact: Admins can filter templates, edit subject/body/isActive with `Idempotency-Key`, and preview escaped rendered output using sample JSON; frontend navigation exposes the page only to users with `SYSTEM_CONFIG`.
- Constraint: Template updates currently log action boundaries but do not yet write a separate immutable audit table; existing unread notification bodies remain unchanged because rendered rows store body snapshots.

## [2026-05-30] E11 notification-service first backend slice

- Decision: Scaffold `notification-service` as a Maven/Docker service on port 8088 with notification schema, in-app notification APIs, template rendering, Kafka business-event consumption, and event idempotency.
- Reason: E10 now publishes `finance.budget.warning` and `finance.budget.exceeded`; E11 needs a durable receiver before adding frontend bell, real WebSocket/STOMP delivery, or email dispatch.
- Impact: Notification-service persists `notifications`, `notification_templates`, and `event_processing_log`; users can list/count/read their own notifications using `NOTIFICATION_VIEW_OWN`; budget alerts route to configured finance recipients via `NOTIFICATION_BUDGET_ALERT_RECIPIENT_IDS`.
- Constraint: Brevo/email and template-admin work remains later E11 slices. Runtime verification showed `notification-service` needs a 0.25 CPU dev limit and longer Docker health start period when OTel is enabled.

## [2026-05-30] E11 realtime notification delivery

- Decision: Deliver in-app notification events through Spring STOMP endpoint `/ws/notifications` and user destination `/user/queue/notifications`, with the Angular shell bell consuming count/feed APIs plus realtime messages.
- Reason: Persisted notification APIs were already stable; realtime delivery should reuse gateway-authenticated user headers and keep push emission after transaction commit.
- Impact: Frontend users with `NOTIFICATION_VIEW_OWN` can see unread count, latest feed, read/read-all actions, and live push toasts in the shell.
- Constraint: WebSocket principal name is derived from gateway `X-User-ID`; direct service WebSocket calls without gateway headers are not a supported production auth path.

## [2026-05-30] E11 email dispatch outbox

- Decision: Handle `notification.email.send` as a Kafka-driven EMAIL outbox row in `notification.notifications`, then let a scheduled worker dispatch via a pluggable logging/Brevo `EmailSenderPort`.
- Reason: Email delivery must not block PR/Approval/Finance transactions, and failed sends need durable retry state rather than transient adapter logs.
- Impact: EMAIL notifications track `email_to`, provider message id, attempt timestamps, retry count, sanitized last error, and exhausted failures in `notification.email_dispatch_dead_letters`.
- Constraint: Dev/local default provider is `logging`; production should set `NOTIFICATION_EMAIL_PROVIDER=brevo`, `BREVO_API_KEY`, and `EMAIL_FROM_ADDRESS` through ENV only.

## [2026-05-30] E10 budget alert event publisher

- Decision: Add a finance `BudgetAlertService` with a `BudgetAlertEventPublisher` port, Kafka publisher, and logging fallback for `finance.budget.warning` and `finance.budget.exceeded`.
- Reason: Budget check, PR budget commitments, and budget transfers need one consistent policy point for low-budget alerts instead of duplicating threshold logic across use cases.
- Impact: Budget check emits warning/exceeded alerts for projected results, tentative/firm commits emit after successful ledger mutation, and transfers emit for the source budget after allocation movement. Kafka publishes after transaction commit when finance Kafka integration is enabled.
- Constraint: Notification-service consumption and user-facing realtime delivery remain E11; this slice only publishes the finance-side event contract.

## [2026-05-30] Finance Kafka listener startup is non-fatal

- Decision: Finance-service disables Spring Kafka listener auto-startup and starts listener containers after `ApplicationReadyEvent` through a guarded starter.
- Reason: Finance APIs, Flyway, and `/actuator/health` must remain available when Kafka is temporarily unavailable or its Docker container exits after the application has been built.
- Impact: Missing Kafka DNS/broker now logs `[KAFKA] Finance Kafka listeners not started` without aborting the Spring context; `FINANCE_KAFKA_AUTO_STARTUP=false` can disable the background listener start in local diagnostics.
- Constraint: This preserves eventual Kafka integration semantics, but if Kafka is down the service will not consume PR budget events until the listener start is retried by restarting the service or toggling runtime deployment.

## [2026-05-30] E10 budget override and transfer actions

- Decision: Implement budget override and transfer as `PATCH` state-changing finance-service actions with required `Idempotency-Key`, Redis replay cache, DB idempotency keys, and auditable `budget_overrides`/`budget_transfers` tables.
- Reason: These actions change approval/audit or allocation state and must follow the repo convention for state transitions while preventing duplicate mutation on retries.
- Impact: Override approvals persist an audit record and do not mutate `allocated_amount`; override amounts above the configured threshold are rejected with `FIN_004`. Transfers lock source/target budget rows in stable UUID order, validate active same-year/same-currency budgets, adjust allocated amounts atomically, and write balanced `TRANSFER_OUT`/`TRANSFER_IN` ledger rows.
- Constraint: High-threshold CEO/CFO approval is represented as a guarded failure path for now; notification publishing for `finance.budget.exceeded` remains a later integration slice.

## [2026-05-30] E10 budget dashboard/list read model

- Decision: Implement public budget read APIs as read-only finance-service use cases with department-scoped filtering, aggregate ledger projections, and Redis dashboard cache evicted by budget ledger write use cases.
- Reason: Finance and department managers need a stable read model for allocated/committed/spent/available before override/transfer workflows are added, and cached dashboards must not bypass permission checks or remain stale after PR lifecycle events.
- Impact: `GET /api/v1/budgets` and `GET /api/v1/budgets/{id}/dashboard` are guarded by `BUDGET_VIEW_OWN_DEPT` or `BUDGET_VIEW_ALL`; own-department users cannot view other departments, while view-all users can filter all departments. MyBatis sorts through whitelist XML mapping.
- Constraint: Department names/top-category analytics and budget override/transfer mutations remain later E10 slices.

## [2026-05-30] E10 finance-service budget check foundation

- Decision: Start E10 with a narrow `finance-service` budget foundation and an internal `GET /internal/budgets/check` endpoint consumed by purchase-request-service.
- Reason: PR submit currently depends on a fallback `BudgetCheckPort`; replacing that fake dependency is higher priority than starting RFQ/PO because it protects the financial control boundary of the existing PR approval flow.
- Impact: `finance-service` becomes a Maven/Docker service on port 8084 with budget ledger schema and local seed budgets. PR service can use `FinanceBudgetCheckAdapter` when `PR_FINANCE_INTEGRATION_ENABLED=true`, while local fallback remains available by config.
- Constraint: Kafka commit/release budget transactions, public budget dashboard APIs, override and transfer flows remain later E10 slices.

## [2026-05-30] E10 budget ledger event source

- Decision: Publish `procurement.pr.approved`, `procurement.pr.rejected`, and `procurement.pr.changes-requested` from purchase-request-service after the approval callback updates PR state, while finance-service consumes those topics plus `procurement.pr.submitted` and `procurement.pr.cancelled`.
- Reason: Approval-service currently applies approval results through synchronous internal PR callbacks and does not own the final persisted PR status; publishing after PR commit keeps budget ledger events aligned with committed PR state.
- Impact: Finance writes immutable `budget_transactions` for tentative commit, firm commit, and release with `finance.event_processing_log` idempotency by event id. Topic registry now documents PR service as the status-event publisher after approval callback.
- Constraint: Budget warning/exceeded producer and public dashboard/list APIs remain later E10 slices.

## [2026-05-28] E05 SLA timer and escalation

- Decision: Add a scheduled `SlaEscalationUseCase` in approval-service to scan overdue pending approval steps, mark `is_escalated`, persist `escalated_from`, reassign to another eligible approver for the same role when IAM returns one, and publish `approval.sla.breached`.
- Reason: E05 needs durable SLA breach handling after deadlines are already calculated and persisted; the existing schema includes `is_escalated`/`escalated_from`, and overdue task scanning can be replay-safe by filtering non-escalated pending steps.
- Impact: Approval tasks remain actionable in inbox after escalation because status stays `PENDING`, while details can show `isEscalated=true`. Kafka mode publishes a transaction-after-commit SLA breach event; local fallback logs the same event without requiring Kafka.
- Constraint: Escalation target resolution currently selects another eligible candidate for the same approver role from IAM and falls back to notifying the original approver when no alternate candidate exists. Delegation-aware substitution and admin approval rule CRUD remain later E05 slices.

## [2026-05-28] E03 Frontend Breadcrumb i18n Collision and Dynamic Route Parameter Support

- Decision: Resolve translation override by merging duplicate `"route"` JSON blocks in `vi.json` and `en.json` into a nested structure, and upgrade `EpBreadcrumbComponent` to use progressive cumulative path mapping with Regex support for dynamic UUIDs/IDs.
- Reason: The duplicate root-level `"route"` key in translations caused the second block to override the first, leading to raw `"route.dashboard"` strings and `"[object Object]"` outputs. Static word-by-word path lookup could not handle nested routes or dynamic ID parameters (e.g. `/procurement/98b50e2d...`), showing ugly raw UUIDs on UI breadcrumbs.
- Impact: All breadcrumb items now render perfectly localized labels. Dynamic routes (PR details, Approval details) map automatically to generic keys (`route.pr.detail`, `route.approvals.detail`) while preserving precise navigation. Build validation completed successfully with exit code 0.
- Constraint: Relies on `ngx-translate` nested path resolution parser.

## [2026-05-27] E05 Backend Approval Inbox, Counts & Task Detail API

- Decision: Implement `/api/v1/approval/inbox`, `/api/v1/approval/inbox/count`, and `/api/v1/approval/tasks/{taskId}` endpoints in approval-service, resolved through custom MyBatis projections and UserResolverPort Feign integrations.
- Reason: User interfaces require high-performance, paginated inbox querying with multi-criteria dynamic filtering, dashboard priority counters, and detailed historic process steps for active workflows.
- Impact: Secure, permission-code protected REST APIs allow actors to search tasks by type, amount range, priority, overdue status, and sorted SLA deadlines. The usecase caches resolved employee profiles in memory to prevent N+1 remote user details calls.
- Constraint: Real-time SSE / WebSocket notifications and the Angular visual inbox screen are separate slices.

## [2026-05-27] E05 PR pending approval status callback

- Decision: Approval-service now calls PR service internal API to move a submitted PR to `PENDING_APPROVAL` after the approval process is persisted.
- Reason: E05-UC-001 needs the submit-to-approval path to reflect the business state in PR service, not only create approval records.
- Impact: PR service exposes `PATCH /internal/purchase-requests/{id}/pending-approval` guarded by `X-Internal-Api-Key` and `Idempotency-Key`; approval-service uses the approval process id as the idempotency key and shares `PR_INTERNAL_API_KEY`.
- Constraint: Final PR status callbacks for approved/rejected/changes-requested decisions and durable retry/outbox for cross-service callback failures remain later slices.

## [2026-05-27] E05 Kafka event wiring for approval start

- Decision: Wire approval-service to consume `procurement.pr.submitted` as JSON, start approval processes from the consumed event, and publish initial `approval.step.assigned` events after process creation.
- Reason: E05-UC-001 requires the PR submitted event to create the approval process and notify downstream notification-service about assigned first-wave approval tasks.
- Impact: `PrSubmittedEvent` now carries `title` and `categories` so approval rules can evaluate category add-ons. Docker Compose runs PR/Approval with Kafka integration enabled by default, while local profiles can keep fallback logging enabled when Kafka is unavailable.
- Constraint: Approval action result producers and durable outbox retry for failed Kafka publish remain later slices.

## [2026-05-27] E05 Start approval process from PR submitted event

- Decision: Add `StartApprovalProcessUseCase` to idempotently consume PR submitted event data, resolve the approval chain, start the matching Camunda BPMN process, and persist `approval_processes` plus initial `approval_steps`.
- Reason: The approval engine needs a durable process boundary before inbox queries, action APIs, SLA timers, and PR status callbacks can be implemented.
- Impact: Standard PRs start `pr-approval-process`; emergency PRs start `emergency-approval`. Duplicate event IDs are skipped, and a second RUNNING process for the same purchase request is marked skipped through `event_processing_log`.
- Constraint: Kafka listener wiring, `approval.step.assigned` publishing, PR status callback, delegation substitution, task inbox, and approve/reject/request-changes/forward actions remain later E05 slices.

## [2026-05-27] E05 SLA business-hours calculation

- Decision: Add `SlaDeadlineCalculator` and `BusinessHoursCalendar` to calculate approval deadlines from resolved step SLA hours.
- Reason: Approval tasks need deterministic `slaDeadline` values before process persistence, inbox sorting, warning flags, and Camunda timer escalation can be implemented.
- Impact: Normal/urgent PR deadlines advance only through configured business hours `08:00-17:30` on `MON-FRI`; emergency PR deadlines use continuous 24/7 hours. Resolved approval-chain steps now include `slaDeadline`.
- Constraint: Holiday calendars, persisted SLA breach timers, warning events, and escalation target resolution remain later E05 slices.

## [2026-05-27] E05 Approval chain resolution

- Decision: Add `ResolveApprovalChainUseCase` in approval-service with `OrgApproverPort`, IAM REST adapter, and an IAM internal `/internal/org/approvers` endpoint protected by `X-Internal-Api-Key`.
- Reason: Approval Engine consumes PR submitted events without a browser session, so it needs a service-to-service approver resolver while preserving the public permission-code protected `/api/v1/org/approvers` contract for user-facing calls.
- Impact: Approval chain resolution now selects the matching approval rule, calls IAM for candidates per approver role, excludes requester candidates, and returns `APR_002` for no approver or `APR_004` for unresolved SoD conflicts.
- Constraint: This slice resolves the chain only; Camunda process start, persisted approval_process/approval_steps records, delegation substitution, and inbox/action APIs remain separate E05 slices.

## [2026-05-27] E05 Approval Engine foundation

- Decision: Scaffold `approval-service` as a Maven reactor module with Spring Boot 3.2, Camunda BPMN starter `7.21.0`, PostgreSQL schema `approval`, baseline approval rule migrations, and initial BPMN files for standard and emergency PR approval.
- Reason: E05 is the next MVP slice after E13-A; it needs a buildable service boundary, persisted rule matrix, and deployable BPMN resources before Kafka consumption, approver resolution, inbox, and action endpoints are added.
- Impact: Docker Compose can build/run `approval-service` on port `8083`; `SelectApprovalRuleUseCase` selects primary value/emergency/default rules and appends matching category rules such as software/SaaS or CAPEX.
- Constraint: This slice does not yet start Camunda process instances from `PrSubmittedEvent`, resolve approvers from IAM, or expose inbox/action APIs.

## [2026-05-24] Codex project personalization baseline

- Decision: Add `.codex/CODEX_CONTEXT.md` as the concise Codex bootstrap file, add the missing `agent/memory/domain-glossary.md`, and align `AGENTS.md` with the actual `.cursor/rules/*.mdc` filenames.
- Reason: Future Codex runs need a short, reliable entrypoint after `AGENTS.md` and must not fail context loading because a referenced glossary/rule file is missing or renamed.
- Impact: Agents can load project context faster, route tasks by layer, use the correct command matrix, and follow the same terminology/status/permission/event vocabulary.
- Constraint: `AGENTS.md` remains the master orchestration source; `.codex/CODEX_CONTEXT.md` is a compact working profile, not a replacement for domain docs, ADRs, or OpenAPI specs.

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

## [2026-05-20] E13-A Admin Portal (User, RBAC, Org Chart UI) Priority Shift

- Decision: Tách phần giao diện Admin quản lý người dùng, phân quyền RBAC và Sơ đồ tổ chức (Org Tree) lên trước và gom nhóm thành Epic E13-A để triển khai ngay.
- Reason: Việc xây dựng giao diện Admin cho phép thiết lập và quản lý dữ liệu nhân sự, vai trò, phòng ban trực quan trên trình duyệt trước khi triển khai hệ thống phê duyệt Approval Engine (E05) - vốn phụ thuộc trực tiếp vào cấu trúc sơ đồ tổ chức (Org Chart) và ma trận phân quyền để phê duyệt.
- Impact: Toàn bộ module Admin frontend bao gồm User CRUD, Role-Permission Matrix, và Org Chart UI sẽ được xây dựng trước Epic E05. Các API nghiệp vụ từ IAM Service (E02) đã hoàn thiện sẽ được gọi trực tiếp.
- Constraint: Các chức năng quản trị cấu hình rules phê duyệt nâng cao (Approval rules) và quản lý catalog danh mục hệ thống vẫn sẽ được giữ lại trong Epic E13 gốc để triển khai sau cùng với các tính năng tương ứng.

## [2026-05-21] E13-A Admin User Form Modal Vertical Layout Optimization

- Decision: Tối ưu hóa layout modal "Thêm/Sửa tài khoản mới" (`user-form-modal`) để chống tràn dọc màn hình (vertical overflow) bằng cách giới hạn max-height của modal panel theo viewport, cố định phần Header và Footer (form actions), và tạo vùng nội dung scroll mượt (.user-form__body). Đồng thời mở rộng chiều rộng của modal trên desktop (`max-width: 48rem`) để tạo layout 2 cột thoáng đãng.
- Reason: Form thêm tài khoản chứa nhiều trường thông tin và lưới danh sách roles lớn, dẫn tới tràn dọc vượt quá chiều cao màn hình ở các độ phân giải thấp hoặc zoom cao, làm ẩn các nút Lưu/Hủy (Action buttons).
- Impact: Giao diện modal trên desktop hiển thị lưới 2 cột chuyên nghiệp, giảm chiều cao cần thiết. Toàn bộ các trường input và thẻ chọn role scroll trơn tru bên trong body của form, trong khi tiêu đề và nút thao tác chính luôn cố định và có thể thao tác ngay lập tức, cải thiện đáng kể trải nghiệm người dùng (UX).
- Constraint: Đã cấu hình độc lập qua custom CSS variables của CSS system, không gây ảnh hưởng tới cấu trúc chung của các modal khác trong hệ thống.

## [2026-05-21] E13-A Resolve Missing action.submit Translation Key

- Decision: Bổ sung translation key `submit` vào đối tượng `action` trong hai tệp ngôn ngữ `vi.json` (Việt) và `en.json` (Anh) lần lượt là "Lưu thay đổi" và "Save Changes".
- Reason: Các nút bấm trong modal Sửa tài khoản, Đổi mật khẩu và Sửa phòng ban bị lỗi hiển thị thô chữ `action.submit` thay vì dịch sang tiếng Việt hoặc tiếng Anh do thiếu key dịch tương ứng trong tệp cấu hình i18n.
- Impact: Toàn bộ nút lưu/gửi trong các modal quản lý hệ thống Admin hiển thị đúng ngôn ngữ bản địa chuyên nghiệp, khắc phục hoàn toàn lỗi giao diện thô i18n.
- Constraint: Đảm bảo độ nhất quán toàn bộ i18n cho frontend.

## [2026-05-21] E13-A Add Confirm Password Field to Reset Password Modal

- Decision: Bổ sung trường "Nhập lại mật khẩu" (`confirmPassword`) vào biểu mẫu đặt lại mật khẩu (`user-reset-password-modal`) và triển khai bộ kiểm duyệt Reactive Form chéo (`passwordMatchValidator`) để đảm bảo tính trùng khớp.
- Reason: Nâng cao độ an toàn và tin cậy cho thao tác quản trị khi đổi mật khẩu cho người dùng, loại bỏ hoàn toàn rủi ro gõ nhầm mật khẩu của quản trị viên do không có đối chiếu.
- Impact: Biểu mẫu hiển thị thêm trường nhập liệu xác nhận mật khẩu có kiểm tra trực quan thời gian thực. Nút "Lưu thay đổi" sẽ bị khóa (disabled) cho tới khi cả hai trường mật khẩu được nhập khớp nhau và tuân thủ đúng định dạng độ phức tạp mật khẩu.
- Constraint: Không ảnh hưởng tới cơ chế API backend do giá trị payload gửi đi vẫn là chuỗi mật khẩu đã kiểm duyệt chuẩn xác.

## [2026-05-21] E03 Premium Visual Redesign for User Profile Screen

- Decision: Thiết kế lại toàn bộ giao diện Profile người dùng (`profile.component.html` và `.scss`) theo cấu trúc dashboard 2 cột cao cấp, kết hợp hiệu ứng radial glowing, avatar halo viền gradient, danh sách metadata chi tiết có icon thông tin động, bộ chọn preset avatar dạng grid tròn có thanh trượt cuộn, và tab đổi biểu mẫu kiểu dáng filter chip vàng/hổ phách đặc trưng.
- Reason: Giao diện Profile cũ thiết kế đơn giản, thô sơ và sử dụng các biến CSS không tồn tại, lệch chuẩn so với bộ nhận diện tối ưu và cực kỳ thẩm mỹ của eProcure Enterprise.
- Impact: Trải nghiệm người dùng được nâng tầm tối đa với các hiệu ứng micro-animations mượt mà khi đổi tab và chọn avatar. Toàn bộ các trường readonly được hiển thị trực quan riêng biệt tránh nhầm lẫn, và đảm bảo tương thích 100% với cơ chế OnPush và i18n đa ngôn ngữ của hệ thống.
- Constraint: Không làm thay đổi logic xử lý reactive forms của Component TypeScript hiện có, giúp bảo toàn tính toàn vẹn nghiệp vụ.

## [2026-05-22] E02 Google OAuth Callback Redirect Flow Redesign

- Decision: Thay thế việc trả về phản hồi JSON thô (`ApiResponse<LoginResponse>` hoặc `BusinessException`) trực tiếp trên trình duyệt của người dùng tại endpoint Callback Google OAuth `/api/v1/auth/oauth/google/callback` bằng cơ chế chuyển hướng HTTP 302 về trang frontend (`http://localhost:4200/login`) kèm theo các tham số truy vấn thích hợp (`error`, `requiresTwoFactor`).
- Reason: Khi người dùng nhấp vào "Đăng nhập Google", trình duyệt thực hiện điều hướng toàn trang thay vì yêu cầu AJAX/HTTP. Việc trả về chuỗi JSON thô khi đăng nhập lỗi (ví dụ lỗi `IAM_030` - User not found) hoặc khi đăng nhập thành công nhưng yêu cầu 2FA sẽ hiển thị trang trắng chứa chuỗi JSON thô cực kỳ thiếu chuyên nghiệp (unprofessional) và gây ngắt quãng trải nghiệm của người dùng.
- Impact:
  1. **Success Flow (Không bật 2FA)**: Backend tự động thiết lập Cookie Session và chuyển hướng trình duyệt về trang chủ `/` (Dashboard). Hệ thống tự động xác thực thông qua Cookie an toàn.
  2. **Success Flow (Có bật 2FA)**: Backend thiết lập Cookie 2FA Temporary Challenge và chuyển hướng trình duyệt về `/login?requiresTwoFactor=true`. Giao diện đăng nhập frontend tự động bắt tham số và hiển thị biểu mẫu nhập mã OTP/Mã dự phòng cao cấp để hoàn tất xác thực.
  3. **Failure Flow (Tài khoản chưa được liên kết - `IAM_030` hoặc lỗi khác)**: Backend chuyển hướng trình duyệt về `/login?error=IAM_030`. Giao diện đăng nhập frontend hiển thị thông báo lỗi thân thiện được nội địa hóa chi tiết (i18n): *"Tài khoản Google này chưa được liên kết với bất kỳ người dùng nào trên hệ thống..."*, giúp người dùng hiểu chính xác vấn đề.
- Constraint: Duy trì hoàn hảo cấu trúc bảo mật Cookie Secure/SameSite của eProcure Enterprise mà không cần can thiệp hay thay đổi cấu hình API Gateway hoặc cơ chế xác thực hiện có.

## [2026-05-27] E05 Approve/Reject/RequestChanges/Forward approval actions

- Decision: Implement `ApprovalTaskActionUseCase` with four domain-level actions (APPROVE, REJECT, REQUEST_CHANGES, FORWARD) on `ApprovalProcess`, cross-service PR status callbacks via `PurchaseRequestStatusPort`, and corresponding `ApplyPurchaseRequestApprovalResultUseCase` in `purchase-request-service`.
- Reason: E05 tracker requires approval task actions as the next vertical slice after start-process and PR-pending-approval-callback are complete. Actions must transition process/step state, propagate final results to PR service, and emit `approval.step.assigned` events for newly assigned steps.
- Impact: `ApprovalTaskController` exposes 4 PATCH endpoints (`/{taskId}/approve`, `/reject`, `/request-changes`, `/forward`) guarded by permission codes. Domain `ApprovalProcess` enforces SoD (requester ≠ approver), sequential step progression, parallel step skipping on reject, and forward-to-eligible-candidate validation via `OrgApproverPort`. PR service receives approval results through internal PATCH endpoints (`/internal/purchase-requests/{id}/approved|rejected|changes-requested`) guarded by `X-Internal-Api-Key`. All actions are idempotent via `IdempotencyService`. 26 approval-service tests and 38 PR-service tests pass (109 total across all modules).
- Constraint: SLA timer escalation, delegation-aware substitution, admin CRUD for approval rules, and frontend inbox/task-detail screens remain deferred to later E05 slices.

## [2026-05-28] E05 Delegation-aware approval assignment

- Decision: Resolve active approval delegation during approval chain resolution by calling IAM internal delegation lookup and persisting `delegate_id` on generated approval steps while keeping the original approver as delegator.
- Reason: Approval inbox/detail/action flows already understand `delegate_id`, but newly started processes did not populate it from IAM delegation data.
- Impact: IAM exposes `GET /internal/org/delegations/active` guarded by `X-Internal-Api-Key`; Approval Engine uses `DelegationResolutionPort` to pass delegator, requester, requester department, amount, currency, and categories before creating steps. Delegated tasks appear in the delegate inbox and can be acted on through existing task authorization. IAM and Approval targeted tests pass: 47 IAM tests, 30 Approval tests.
- Constraint: Delegation is resolved at process start. Existing in-flight approval steps are not retroactively reassigned if a delegation is created or revoked later.

## [2026-05-28] E05 Admin approval rule CRUD

- Decision: Add backend admin APIs for listing, creating, updating, and deactivating approval rules under `/api/v1/approvals/rules`, guarded by `ADMIN_APPROVAL_RULE`.
- Reason: E05 required admin rule management after approval execution, inbox, SLA, and delegation were complete.
- Impact: Rule mutations are idempotent, audit-logged, and update `approval_rules` plus replacement `approval_rule_steps` through the approval repository. OpenAPI and error-code docs now include the rule mutation contract; approval-service tests pass with 36 tests.
- Constraint: Deactivate only sets `is_active=false`; it does not hard-delete or retroactively alter already running approval processes.

## [2026-05-28] E05 Approval rule admin frontend closure

- Decision: Add `/approvals/rules` as the approval rule management UI guarded by `ADMIN_APPROVAL_RULE`, with admin navigation entry and a compatibility redirect from `/admin/approval-rules`.
- Reason: E05 frontend had inbox/detail/actions, but the story map also required `ApprovalRuleAdminPage` to operate the backend rule CRUD added for E05 closure.
- Impact: Admin users can list, search, create, update, and deactivate approval rules with condition and step editors. The approvals module route guard now admits either approver permissions or `ADMIN_APPROVAL_RULE`; child routes still enforce their own narrower permissions. Frontend build passes.
- Constraint: Visual browser QA was not run because no Browser MCP tool was available in this session; verification is limited to Angular production build.

## [2026-06-01] E02 IAM password reset notification adapter

- Decision: Replace the Docker/prod password reset email stub path with a Kafka delivery adapter that publishes `notification.email.send` events carrying `templateEventType=PASSWORD_RESET` for notification-service email outbox/dispatch.
- Reason: E11 notification-service now owns transactional email rendering and retry, so IAM should only issue/reset tokens and publish a delivery request after transaction commit.
- Impact: `IAM_PASSWORD_RESET_DELIVERY_MODE=kafka` enables the adapter in Docker/prod; local remains `logging` by default. The adapter publishes after commit, masks logs, and avoids logging the raw reset token or reset URL.
- Constraint: Reset URL necessarily carries the raw reset token inside the Kafka payload for email delivery; logs and error messages must continue to mask token-like values. Response encryption remains deferred to E14 because E02 only has the backend request-decryption/public-key contract.

## [2026-06-02] E06 Vendor master foundation

- Decision: Implement `vendor-service` first as Vendor master + AVL foundation before RFQ, with vendor categories stored as JSONB array and exposed as the OpenAPI `categories` array.
- Reason: RFQ creation depends on a reliable approved-vendor list and category filtering; implementing RFQ before Vendor master would require stubs or duplicated validation.
- Impact: `vendor-service` owns `vendors`, `vendor_contacts`, and `vendor_scores`; API foundation exposes list/create/detail/approve vendor endpoints guarded by `VENDOR_VIEW`, `VENDOR_CREATE`, and `VENDOR_APPROVE`. Docker Compose adds service port 8086 with light-service resource limits.
- Constraint: RFQ tables/use cases and PR-service validation port remain deferred to the next E06 slice.

## [2026-06-02] E06 RFQ source validation contract

- Decision: Let `purchase-request-service` expose internal `GET /internal/purchase-requests/{id}/rfq-source`, and let `vendor-service` validate `APPROVED` PR status plus AVL vendors before creating RFQ snapshots.
- Reason: RFQ must be created from canonical PR data without duplicating PR state in vendor-service or trusting client-provided line items.
- Impact: RFQ create now snapshots PR line items into `vendor.rfq_line_items`, stores vendor invitations in `vendor.rfq_invitations`, and exposes create/list/detail/close RFQ APIs guarded by `RFQ_CREATE`, `RFQ_VIEW`, and `RFQ_EVALUATE`.
- Constraint: Quote submission, scoring/evaluation, and award remain the next E06 implementation slice.

## [2026-06-02] E06 RFQ quote and award foundation

- Decision: Add `vendor_quotes` plus `vendor_quote_line_items` as structured quote persistence, and expose quote submit/evaluate/award endpoints under `/api/v1/rfq`.
- Reason: RFQ evaluation and award need auditable quote line items with monetary precision; evaluating/awarding without persisted quotes would require stubs and break the PO handoff path.
- Impact: `POST /rfq/{id}/quotes` records a vendor quote, `POST /rfq/{id}/quotes/{quoteId}/evaluate` stores score/note, and `POST /rfq/{id}/award` sets RFQ `AWARDED` with awarded vendor/quote. Submit quote requires open RFQ, invited AVL vendor, matching RFQ line items, and unexpired submission deadline.
- Constraint: PO creation/event publication after award is deferred to the next E06/finance-inventory handoff slice.

## [2026-06-02] E06 RFQ award handoff event

- Decision: Publish `procurement.rfq.awarded` from `vendor-service` after RFQ award commits, using a logging fallback locally and Kafka when `VENDOR_KAFKA_ENABLED=true`.
- Reason: Finance PO creation needs an auditable event payload, but implementing full PO persistence/API in finance-service is a separate slice from RFQ award.
- Impact: The event carries RFQ/PR ids, awarded vendor/quote, amount/currency, payment terms, award reason, and line item snapshot including PR line item id, category, quantity, unit price, and total price.
- Constraint: finance-service consumer and PO persistence/API remain the next implementation step.

## [2026-06-03] E07 PO foundation from RFQ award

- Decision: Let `finance-service` consume `procurement.rfq.awarded` and create a DRAFT PO snapshot with nullable delivery details, using `finance.event_processing_log` and unique RFQ/source-event indexes for idempotency.
- Reason: RFQ award events do not carry delivery address/deadline, but the system needs an inspectable PO record before the purchasing workflow can complete send/issue actions.
- Impact: `finance.purchase_orders` and `finance.po_line_items` persist RFQ/PR/vendor/quote snapshots; `GET /api/v1/purchase-orders` and `GET /api/v1/purchase-orders/{id}` expose the generated PO under `PO_VIEW_OWN`/`PO_VIEW_ALL`.
- Constraint: Manual PO create/edit, send/cancel, and `procurement.po.issued` publication remain E07 follow-up work.

## [2026-06-03] E07 PO action workflow

- Decision: Add draft edit, send, and cancel actions to `finance-service`; send publishes `procurement.po.issued` plus a `notification.email.send` request for vendor email dispatch.
- Reason: RFQ-award-created DRAFT POs need delivery details before issue, and Inventory/Notification need a durable PO issued event after send.
- Impact: `PATCH /api/v1/purchase-orders/{id}`, `POST /api/v1/purchase-orders/{id}/send`, and `PATCH /api/v1/purchase-orders/{id}/cancel` are idempotent via `Idempotency-Key`. Notification-service now subscribes to `procurement.po.issued` and seeds an IN_APP `PO_ISSUED` template.
- Constraint: Direct/manual `POST /api/v1/purchase-orders` remains deferred until the approved-PR direct-PO source contract is finalized.

## [2026-06-03] E08 Inventory PO issued snapshot foundation

- Decision: Start E08 by adding `inventory-service` and consuming `procurement.po.issued` into `inventory.purchase_order_snapshots` plus `inventory.purchase_order_line_snapshots`, guarded by `inventory.event_processing_log`.
- Reason: Goods Receipt needs a durable issued-PO source before stock receipt can be implemented, while the PO issued event currently carries item name/category snapshots but not canonical inventory item codes.
- Impact: `inventory-service` is now a Maven/Docker/Compose service on port 8085 with Flyway V1 inventory foundation, Kafka listener delayed until application ready, and unit-tested idempotent PO snapshot recording.
- Constraint: GR create/complete APIs must resolve or capture catalog item codes in the next slice before updating `stock_entries` and publishing `inventory.gr.created`.

## [2026-06-03] E08 Goods Receipt draft API from PO snapshots

- Decision: Add DRAFT Goods Receipt create/list/detail APIs backed by issued PO snapshots, with DB-level `idempotency_key` on `inventory.goods_receipts`.
- Reason: Warehouse users need a persisted GR draft before stock receipt-in can safely update inventory balances. PO issued events still do not carry canonical `itemCode`, so GR line `itemCode` remains nullable until the complete/stock slice resolves catalog mapping.
- Impact: `POST /api/v1/goods-receipts` validates PO snapshot, active warehouse, line membership, duplicate lines, and quantity tolerance. `GET /api/v1/goods-receipts` and `GET /api/v1/goods-receipts/{id}` expose DRAFT GR data under `GR_VIEW`.
- Constraint: Complete GR, stock entry updates, `RECEIPT_IN` stock movements, and `inventory.gr.created` publication remain the next E08 slice.

## [2026-06-03] E08 Complete Goods Receipt receipt-in

- Decision: Complete GR resolves missing catalog `itemCode` from active inventory items by exact PO line snapshot match (`itemName`, `categoryCode`, `unit`) before stock posting.
- Reason: `procurement.po.issued` currently carries PO line item name/category/unit but not canonical inventory `itemCode`, while `stock_entries` and immutable `stock_movements` require non-null `item_code`.
- Impact: `POST /api/v1/goods-receipts/{id}/complete` is idempotent through `completed_idempotency_key`, only completes DRAFT GR, updates `stock_entries`, writes `RECEIPT_IN` movements, captures resolved GR line `item_code`, and publishes `inventory.gr.created` after commit.
- Constraint: If no active catalog item can be resolved, completion fails with `INV_001` instead of writing ambiguous stock. Stock list/movement and issue-out APIs remain the next E08 slice.

## [2026-06-03] E08 Stock query API projections

- Decision: Stock read APIs use a dedicated `StockRepository` and read projections for current stock and movement history, exposed with `GR_VIEW` on `GET /api/v1/items/{itemCode}/stock`, `GET /api/v1/warehouses/{id}/stock`, and `GET /api/v1/stock/movements`.
- Reason: Querying stock should not expand the Goods Receipt repository responsibility, and FE needs a stable read contract before implementing issue-out workflows.
- Constraint: `stock_movements` currently stores only `performed_by`, so `performedBy.fullName` falls back to the UUID string until IAM/user snapshot integration is added. Issue-out remains the next E08 slice.

## [2026-06-03] E08 Issue-out stock API

- Decision: `POST /api/v1/stock/issue-out` records an idempotent `inventory.stock_issue_out_requests` header, atomically decrements `stock_entries`, and appends `ISSUE_OUT` rows in `stock_movements` with negative ledger quantities.
- Reason: Multi-line issue-out needs replay-safe request grouping and must fail/rollback when any line lacks sufficient stock.
- Constraint: The API validates active item and warehouse locally; recipient identity is captured as a UUID snapshot and can be enriched by IAM integration later.

## [2026-06-03] E09 Invoice foundation

- Decision: Start E09 in finance-service with idempotent invoice create/list/detail, backed by `finance.invoices` and `finance.invoice_line_items`.
- Reason: 3-way match needs a durable invoice aggregate before comparing PO, GR, and invoice amounts/quantities.
- Constraint: Invoice creation validates PO existence and vendor match, but does not run 3-way match yet. Match/approve/dispute/payment remain follow-up E09 slices.

## [2026-06-03] E09 Invoice 3-way match foundation

- Decision: Finance stores `inventory.gr.created` as `finance.goods_receipt_snapshots` and `finance.goods_receipt_line_snapshots`, then matches invoice lines by `poLineItemId` against PO lines and aggregated GR received quantities.
- Reason: 3-way match must be deterministic and should not query inventory-service storage directly across service boundaries.
- Impact: `POST /api/v1/invoices/{id}/match` is idempotent with `match_idempotency_key`, returns `MATCHED`/`MISMATCHED`/`PARTIAL`, updates invoice match fields, and publishes `finance.invoice.matched` only for fully matched invoices.
- Constraint: Invoice create now requires each line to carry `poLineItemId`; approve/dispute/payment actions remain follow-up E09 slices.

## [2026-06-03] E09 Invoice approve, dispute, and payment actions

- Decision: Add idempotent invoice approve/dispute/payment actions in finance-service with dedicated action idempotency keys and a `finance.payments` audit table.
- Reason: Payment state must be replay-safe and auditable after 3-way match, while approval/dispute transitions need explicit state guards before an invoice can be paid.
- Impact: `POST /api/v1/invoices/{id}/approve` moves `MATCHED` invoices to `APPROVED`; `POST /api/v1/invoices/{id}/dispute` moves `MISMATCHED` invoices to `DISPUTED`; `POST /api/v1/invoices/{id}/confirm-payment` records a confirmed payment and marks the invoice `PAID`.
- Constraint: Confirm payment currently requires paid amount to equal invoice total amount and does not update budget spent ledger until PO/invoice carries a reliable budget reference.

## [2026-06-04] E12 Analytics service foundation

- Decision: Start analytics-service as a standalone Spring Boot module on port 8087 with its own `db_analytics` / `analytics` read-model schema.
- Reason: Executive dashboards need stable API contracts without direct cross-service table reads; future event consumers can populate projections asynchronously.
- Impact: Root Maven, Docker Compose, PostgreSQL bootstrap, `.env.example`, and service Dockerfiles include analytics-service. `GET /api/v1/dashboard/executive` is guarded by `REPORT_VIEW` and reads fresh executive dashboard snapshots, falling back to an empty dashboard when no snapshot exists.
- Constraint: Snapshot population, manager/purchasing/requester dashboards, KPI endpoints, and Jasper report export remain later E12 slices.

## [2026-06-04] E12 Analytics projection ingestion foundation

- Decision: Populate executive dashboard read models through analytics-owned projections from `procurement.po.issued`, `finance.invoice.matched`, and `approval.sla.breached`.
- Reason: These topics already expose stable payloads for spend, vendor/category/monthly trend, invoice match facts, and SLA breach facts without cross-service DB reads.
- Impact: analytics-service now has Kafka listener config, an event-processing idempotency log, PO/invoice/SLA fact tables, and snapshot refresh logic for annual and quarterly executive dashboard data.
- Constraint: RFQ savings and department spend stay zero/empty until upstream events include baseline quote savings and department allocation fields.

## [2026-06-04] E12 Role dashboard API foundation

- Decision: Add manager, purchasing, and requester dashboard endpoints as stable analytics-service API contracts returning structured zero/empty data.
- Reason: OpenAPI already defines the role dashboard surfaces, but role-specific source projections are not reliable enough yet for real data aggregation.
- Impact: `/api/v1/dashboard/manager`, `/api/v1/dashboard/purchasing`, and `/api/v1/dashboard/requester` are guarded by permission codes and can be integrated by the Angular shell without 404s.
- Constraint: Role dashboard data projections remain a follow-up slice once source events/read models exist for budgets, approval inbox, PO pipeline, RFQ, GR, invoices pending match, and requester PR stats.

## [2026-06-04] E12 Purchasing dashboard projection data

- Decision: Populate purchasing dashboard with analytics-owned issued PO and matched invoice projection facts.
- Reason: `po_issued_projections` and `invoice_matched_projections` are already available and do not require cross-service DB reads.
- Impact: `/api/v1/dashboard/purchasing` now returns issued PO count, issued PO total, matched invoice count, sent-to-vendor pipeline count, and vendor pending-order rows from analytics projections.
- Constraint: Open RFQ, GR pending, invoice pending-match, true delivery timeliness, and vendor quality score remain zero until analytics-service has RFQ, GR, invoice-pending, and vendor-score projection sources.

## [2026-06-04] E12 Async report export job foundation

- Decision: Add analytics-owned report export job storage and API foundation with DB-scoped idempotency.
- Reason: `POST /reports/export` is a mutating endpoint and must be replay-safe before Jasper/PDF/Excel workers are added.
- Impact: `analytics.report_export_jobs` stores queued export jobs; `POST /api/v1/reports/export`, `GET /api/v1/reports/jobs/{jobId}`, and download guard endpoints are available under `REPORT_EXPORT`.
- Constraint: Jobs remain `QUEUED` until a later worker slice renders files and marks jobs `COMPLETED` or `FAILED`.
