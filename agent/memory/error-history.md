# Error History (Những lỗi đã xảy ra — agent phải tránh)

## [2026-05-27] Bug: Compilation failure due to custom Java Record accessor and PageMeta builder constraints

- Root cause:
  1. Defining custom methods in a Java record that match component names but return different types (e.g., custom `priority()` returning `Optional<String>` instead of `String` component type) violates Java record compilation rules.
  2. Direct call to `PageMeta` multi-param constructor failed because the signature requires multiple boolean flags (`isFirst`, `isLast`) rather than just pagination bounds.
  3. Adding new interface methods (`findPendingTasks`, `countPendingTasks`) to `ApprovalProcessRepository` broke compiler compatibility in unit tests due to missing overrides in `FakeApprovalProcessRepository`.
- Fix:
  1. Renamed custom accessor methods to `*Opt()` (e.g. `priorityOpt()`).
  2. Invoked static builder `PageMeta.of(...)` which dynamically computes metadata.
  3. Added stub implementations to test-level mock repositories.
- Prevention: Ensure Java records only override accessors with matching type signatures, use static factory methods for complex object initialization, and maintain test mock implementation alignment during interface expansions.

## [2026-05-27] Bug: Camunda Desktop Modeler could not display approval BPMN files

- Root cause: The BPMN resources contained executable process definitions only and did not include BPMN Diagram Interchange metadata (`bpmndi:BPMNDiagram`, `BPMNPlane`, `BPMNShape`, `BPMNEdge`). Camunda Engine can parse process XML without DI, but Camunda Desktop Modeler requires DI coordinates to render an editable diagram.
- Fix: Add full BPMN DI metadata and Vietnamese display names to `pr-approval-process.bpmn` and `emergency-approval.bpmn`, while keeping stable executable process ids for deployment.
- Prevention: Add `BpmnDiagramResourceTest` so every BPMN resource must parse with Camunda BPMN model API and include diagram, plane, shape, and edge metadata before it is accepted.

## [2026-05-27] Bug: IAM logs showed colors but blank trace/span context

- Root cause: IAM Docker image did not run the OpenTelemetry Java agent, and Log4j2 pattern read `traceId`/`spanId` MDC keys while OTel Java agent log correlation exposes `trace_id`/`span_id`.
- Fix: Copy `infra/otel/opentelemetry-javaagent.jar` into IAM/PR Docker images, add `-javaagent:/app/agents/opentelemetry-javaagent.jar`, configure OTLP trace export env vars in Docker Compose, and update Log4j2 patterns to print `trace=%X{trace_id} span=%X{span_id}`.
- Prevention: For every Spring service, observability requires both the Java agent at runtime and Log4j2 MDC keys aligned with OTel standard `trace_id` and `span_id`.

## [2026-05-27] Bug: Tempo restarted and Grafana trace UI returned 500/502

- Root cause: Tempo ran with `mem_limit: 128m` and `cpus: 0.10`; local compaction/query/ingest exceeded that limit, Docker killed the container with OOM exit 137, and Java services timed out exporting spans to `tempo:4317`.
- Fix: Increase Tempo local monitoring budget to `cpus: 0.25`, `mem_limit: 512m`, add reservations, and add `/ready` healthcheck so Grafana/OTel stability is visible in `docker compose ps`.
- Prevention: Tempo needs a larger budget than lightweight sidecars when trace ingestion and Grafana Explore are enabled; do not cap it at 128M in the monitoring profile.

## [2026-05-18] Bug: Fresh Docker recreate failed after Keycloak provider merge

- Root cause: IAM Docker build copied only `services/iam-service/pom.xml`, while the root Maven reactor now references `infra/keycloak/eprocure-keycloak-provider`.
- Fix: Copy the provider module POM in `services/iam-service/Dockerfile` before dependency resolution.
- Prevention: When adding a Maven reactor module, update every Dockerfile that runs root-level Maven goals.

## [2026-05-18] Bug: Keycloak realm import rejected User Storage component

- Root cause: `realm-eprocure.json` exported `providerType`, which Keycloak 24 does not accept in `ComponentExportRepresentation` during import.
- Fix: Remove `providerType` from the user-storage component; keep `providerId`, `name`, `subType`, and config.
- Prevention: Validate realm JSON with a fresh volume recreate, not only against an already-imported local realm.

## [2026-05-18] Bug: IAM startup failed at `sessionMapper` / `sqlSessionTemplate`

- Root cause: MyBatis XML result mapping for delegation UUID fields did not declare a PostgreSQL UUID type handler, so mapper creation failed and cascaded into `SessionAuthenticationFilter` dependency creation.
- Fix: Add `UuidTypeHandler`, register it in `application.yml`, and declare explicit UUID mappings in `DelegationMapper.xml`.
- Prevention: For XML mapper result maps using UUID columns, declare `javaType`, `jdbcType=OTHER`, and the UUID type handler explicitly.

## [2026-05-18] Bug: IAM startup failed on `TotpService` constructor binding

- Root cause: `TotpService` had both a production issuer constructor and a package-private test constructor, so Spring could not infer the bean constructor safely.
- Fix: Keep `TotpService` as an application service class and expose it through `TotpServiceConfig` with the configured issuer value.
- Prevention: Prefer explicit config beans when a service class needs multiple constructors for test seams.

## [2026-05-18] Bug: Login request JSON failed to deserialize after domain mapper customization

- Root cause: The field-visibility `domainObjectMapper` became the only `ObjectMapper` bean, so Spring MVC used it for API records such as `LoginRequest`.
- Fix: Add a primary API `ObjectMapper` and keep the field-visibility mapper under `domainObjectMapper`.
- Prevention: Keep domain/entity conversion mappers qualified so MVC serialization keeps normal Jackson record support.

## [2026-05-18] Bug: Keycloak direct grant returned `Account is not fully set up`

- Root cause: The custom user adapter exposed profile getters but did not expose first name, last name, email, and emailVerified through Keycloak attributes used by required actions/profile checks.
- Fix: Override `getFirstAttribute` and `getAttributes` in `EprocureIamUserAdapter`.
- Prevention: Validate Keycloak User Storage SPI with a direct-grant token request after realm recreate.

## [2026-05-18] Bug: Login failed while inserting session IP address

- Root cause: `iam.sessions.ip_address` is PostgreSQL `INET`, but `SessionMapper.save` bound the value as VARCHAR.
- Fix: Cast the bound IP string with `CAST(#{session.ipAddress,jdbcType=VARCHAR} AS INET)` in the insert statement.
- Prevention: Match mapper parameter binding to PostgreSQL-specific column types during local end-to-end login tests.

## [2026-05-18] Bug: Permission updates did not affect active sessions immediately

- Root cause: IAM session cache stored permission codes as a snapshot, so a user could keep stale authorities after an admin changed role-permission mapping.
- Fix: Store only role codes in session cache, resolve authorities via `role-perm:{roleCode}` cache on each authentication, refresh role-perm cache when role permissions change, and evict active session cache when user roles change.
- Prevention: Never cache user permission snapshots inside session data; permission checks must flow through role-permission cache with DB fallback.

## [2026-05-19] Bug: PR service runtime failed on UUID and record conversion

- Root cause: `purchase-request-service` initially lacked a PostgreSQL UUID MyBatis type handler, and the field-only `domainObjectMapper` disabled record creator visibility for nested value objects during domain-to-DB conversion.
- Fix: Add `UuidTypeHandler`, register `mybatis.type-handlers-package`, and enable `PropertyAccessor.CREATOR` on `domainObjectMapper`.
- Prevention: Validate new service slices with Docker runtime plus API calls, not only unit tests; keep field-based domain/entity conversion mappers able to construct record value objects.

## [2026-05-22] Bug: Raw JSON displayed on browser during Google OAuth callback and 2FA required logins

- Root cause:
  1. Google OAuth Callback was returning direct JSON payload response (`ApiResponse<LoginResponse>` or `BusinessException` for `IAM_030`) during standard browser-initiated full-page navigation. This left users looking at raw JSON text on a blank page.
  2. When a user with 2FA enabled successfully logged in using standard credentials or Google OAuth, the backend returned a successful HTTP 200 response with `requiresTwoFactor = true` but did not perform a redirect or provide the frontend with a direct path to navigate to the 2FA verification panel, causing the login page to sit idle.
- Fix:
  1. Updated Google OAuth callback handler to catch successful authentications and exceptions, performing an HTTP 302 redirect back to the Angular frontend (`http://localhost:4200/login`) with appropriate query parameters (`?error=IAM_030` or `?requiresTwoFactor=true`).
  2. Implemented query parameter interception in Angular `LoginComponent` to automatically show localized error alerts (for `IAM_030` / "Tài khoản Google chưa liên kết") and auto-switch the UI view to the premium OTP and Backup Code verification panel when `requiresTwoFactor=true` is detected.
- Prevention: Ensure standard full-page browser callback flows always return HTTP 302 redirects instead of raw API responses, and ensure all multi-factor auth states trigger explicit frontend navigation cues.
