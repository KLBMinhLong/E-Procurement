# Error History (Những lỗi đã xảy ra — agent phải tránh)

## [2026-06-05] Bug: Analytics service startup failed with UnsatisfiedDependencyException on LocalReportFileRenderer

- Symptom: Analytics service failed to start during context initialization. The log showed: `UnsatisfiedDependencyException: Error creating bean with name 'processReportExportJobsUseCase' ... Unsatisfied dependency expressed through constructor parameter 2: Error creating bean with name 'localReportFileRenderer' ... Failed to instantiate [com.eprocure.analytics.infrastructure.report.LocalReportFileRenderer]: No default constructor found`.
- Root cause: `LocalReportFileRenderer` had two constructors: one taking a `String` (with `@Value`) and another package-private constructor taking a `Path` (used for tests). Because there were multiple constructors and neither was annotated with `@Autowired`, Spring could not resolve which constructor to use for dependency injection and tried to fall back to a default (no-args) constructor, which did not exist.
- Fix: Simplified `LocalReportFileRenderer` to a single constructor that accepts a `String` (annotated with `@Value`) and converts it to a `Path` inside the constructor. Updated the test class `LocalReportFileRendererTest` to instantiate it with `tempDir.toString()`.
- Prevention: Prefer defining a single constructor in Spring bean classes to avoid injection ambiguity. If multiple constructors are required, explicitly annotate the injection target constructor with `@Autowired` or equivalent.

## [2026-06-01] Bug: Reset password email link opened frontend not-found

- Symptom: Brevo delivered forgot-password email, but opening `http://localhost:4200/reset-password?token=...` showed the Angular not-found page even though the HTTP request returned 200.
- Root cause: The IAM password reset email used `/reset-password`, but Angular only defined `/forgot-password`; the SPA served `index.html`, then the wildcard route rendered not-found.
- Fix: Add a public `/reset-password` Angular route and reset password form that posts `resetToken`, `newPassword`, and `confirmPassword` to `POST /api/v1/auth/reset-password`.
- Prevention: Any email deep link produced by backend configuration must have a matching public Angular route and should be verified with the generated URL before accepting the flow.

## [2026-06-01] Bug: Notification email consumer rejected IAM password reset events

- Symptom: Forgot-password mail flow produced repeated notification-service logs: `[KAFKA] Skip invalid notification event | topic=notification.email.send | reason=timestamp is required`, followed by `KafkaMessageListenerContainer - Error handler threw an exception`.
- Root cause: IAM published `PasswordResetEmailEvent.timestamp` as `Instant`; Spring Kafka serialized it as numeric epoch seconds with fractional nanos, while notification-service required `timestamp` to be a textual ISO instant and rethrew malformed events, causing retry/error-handler noise.
- Fix: Make IAM password reset event timestamp a string ISO instant, allow notification consumer to parse legacy numeric epoch-second timestamps, and skip malformed notification events without rethrowing so poison records do not block the partition.
- Prevention: Kafka event envelope tests must verify serialized field shape for cross-service events, especially time fields and other non-primitive Java types.

## [2026-05-30] Bug: Notification-service startup failed while parsing MyBatis UUID mappings

- Symptom: Notification container stayed unhealthy and restarted; logs showed `Failed to parse mapping resource [mapper/NotificationMapper.xml]` and `No typehandler found for property id`.
- Root cause: The new notification module used UUID columns in MyBatis XML/result mappings but did not register a PostgreSQL UUID type handler.
- Fix: Add `UuidTypeHandler` under `notification.infrastructure.persistence.typehandler` and configure `mybatis.type-handlers-package`.
- Prevention: Every new Spring/MyBatis service with UUID columns must include the UUID type handler before Docker runtime verification, not just compile-time tests.

## [2026-05-30] Bug: Finance health failed when Kafka DNS was unavailable

- Symptom: Finance container logged `SYS_001` on `/actuator/health` with `No WebApplicationContext found`, then the Spring context failed while starting `internalKafkaListenerEndpointRegistry`.
- Root cause: Spring Kafka listener containers auto-started during application context startup. When Docker Kafka had exited and `kafka:9092` no longer resolved, constructing the Kafka consumer threw `ConfigException`, aborting the whole Finance Spring context.
- Fix: Set `spring.kafka.listener.auto-startup=false`, start Finance Kafka listeners after `ApplicationReadyEvent` in a guarded background starter, catch startup failures, expose `FINANCE_KAFKA_AUTO_STARTUP`, and increase the Finance Docker health start period to match observed Java/OTel startup time.
- Prevention: Non-critical Kafka consumers in dev/local must not be part of the blocking HTTP application startup path; verify `/actuator/health` with Kafka unavailable before accepting service container changes.

## [2026-05-30] Bug: Finance budget dashboard returned 500 after MyBatis aggregate query

- Symptom: `GET /api/v1/budgets` and `/api/v1/budgets/{id}/dashboard` returned `SYS_001`/HTTP 500 after finance-service was rebuilt, while the aggregate SQL itself worked in PostgreSQL.
- Root cause: `BudgetRepositoryImpl` used `domainObjectMapper.convertValue()` from `BudgetLedgerSummaryDbEntity` to `BudgetLedgerSummary`, but the finance `domainObjectMapper` was field-only and ignored the entity's public computed getters `getAllocated()`, `getCommitted()`, and `getSpent()`. The domain record constructor therefore received `null` `Money` values.
- Fix: Allow public getters in the qualified `domainObjectMapper` and add a regression test proving `BudgetLedgerSummaryDbEntity` converts to the domain record with `Money` value objects intact.
- Prevention: When infrastructure entities expose domain value objects through computed getters, keep the qualified domain ObjectMapper able to read those getters or add a focused conversion test before relying on runtime mapper behavior.

## [2026-05-30] Bug: PR detail UI showed `[object Object] VND` and incomplete line item values

- Symptom: Purchase request detail rendered budget money as `[object Object] VND`, showed missing quantity/unit price values, and exposed long raw UUIDs in operational panels.
- Root cause: Backend `PurchaseRequestDetailResponse.LineItemResponse` omitted quantity, unit price, GL account, catalog/specification fields although OpenAPI/frontend expected them; frontend `ep-amount` only accepted primitive values while budget check returns `Money` objects.
- Fix: Expanded PR detail response line item payload, aligned OpenAPI budget money fields with `Money`, allowed `ep-amount` to format `Money` objects, and rebuilt PR list/detail UI around compact operational panels with shortened IDs and safe optional approval data.
- Prevention: Keep frontend response models and OpenAPI schemas synchronized with backend records, and make shared value-formatting components accept the canonical API value objects directly.

## [2026-05-30] Bug: Approval rule create/update failed when binding PostgreSQL array columns

- Root cause: `ApprovalRuleDbEntity` stored `categories`, `departmentIds`, and `priorities` as `Object`, while `ApprovalRuleMapper` only declared `jdbcType=ARRAY`. MyBatis delegated to the generic object handler, so the PostgreSQL driver tried to create a `JAVA_OBJECT` array instead of `varchar[]` or `uuid[]`.
- Fix: Add explicit MyBatis array type handlers for `varchar[]` and `uuid[]`, wire them into approval rule result mappings and insert/update bindings, and keep repository conversion tolerant of collection values.
- Prevention: PostgreSQL array columns must declare an explicit element-type handler in MyBatis; do not rely on bare `jdbcType=ARRAY` for domain-specific arrays such as UUID lists.

## [2026-05-30] Bug: Approval rules UI failed because seeded rules had no step templates

- Root cause: `V2__seed_default_approval_rules.sql` inserted baseline rules in a data-modifying CTE, then the main `INSERT ... SELECT` tried to join `approval.approval_rules` in the same statement. PostgreSQL executes the query with one snapshot, so the main query could not see rows inserted by the CTE when the database was fresh, resulting in 10 rules and 0 `approval_rule_steps`.
- Fix: Add `V3__backfill_default_approval_rule_steps.sql` to insert the baseline step templates for existing active baseline rules with `ON CONFLICT DO NOTHING`.
- Prevention: When a migration needs to seed parent and child rows in one statement, join child rows against the CTE `RETURNING` output or split the work into separate statements/migrations; verify seed counts on a clean DB before accepting the migration.

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

## [2026-05-30] Bug: Approval rules table clipped actions when approval chains were long

- Root cause: The approval rules table rendered every approval step card directly inside the `Chuỗi duyệt` column. Long chains increased row height and consumed horizontal space, causing the action buttons to wrap and clip labels such as `Vô hiệu hóa`.
- Fix: Replace the table cell with a compact approval-chain summary and move the full ordered step/SLA view into a modal diagram opened by `Xem sơ đồ`; keep row action buttons in a no-wrap action group.
- Prevention: Dense operational tables should show bounded summaries for expandable workflow data, while full workflow diagrams belong in detail drawers/modals rather than primary rows.

## [2026-06-07] Bug: Runtime smoke path lacked valid seed actors and requester budget

- Root cause: Unit-tested service foundations had enough isolated seed data, but the Docker E2E path had no dedicated Purchasing/Warehouse/Accountant users for RBAC-gated endpoints and no active finance budget for the seeded requester department `22222222-2222-2222-2222-222222222222`.
- Fix: Add IAM Flyway `V8__seed_e15_smoke_actors.sql` for smoke roles/users/permissions and finance Flyway `V10__seed_procurement_smoke_budget.sql` for active PROCUREMENT budget.
- Prevention: Full-product smoke must include deterministic actor and reference-data seeds for every role in the product backbone, then verify through gateway auth instead of bypassing service security.

## [2026-06-07] Bug: Runtime smoke exposed missing catalog and inventory reference data

- Symptom: PR creation or GR completion failed in Docker even though isolated service tests compiled.
- Root cause: The smoke flow used a deterministic office package item, but PR catalog and Inventory item seeds did not contain matching active data across both services.
- Fix: Add PR Flyway `V5__seed_e15_smoke_catalog_item.sql` and Inventory Flyway `V6__seed_e15_smoke_inventory_item.sql` for `E15-OFFICE-KIT`.
- Prevention: E2E smoke data must cover every cross-service reference used by the flow, not only user and budget records.

## [2026-06-07] Bug: PR submit could not boot with integration fallback disabled

- Symptom: Docker PR service startup failed when smoke env disabled the broad integration fallback flag.
- Root cause: `FallbackInventoryCheckAdapter` was conditional on the same fallback flag, but there is not yet a real PR-to-Inventory availability adapter implementing `InventoryCheckPort`.
- Fix: Keep the fallback inventory adapter registered so submit PR can boot; E15 smoke separately verifies Inventory runtime through goods receipt completion.
- Prevention: Do not bind fallback bean existence to a feature flag until the production adapter exists, and record the gap as follow-up rather than letting runtime smoke fail at Spring context startup.

## [2026-06-07] Bug: Runtime PATCH callbacks failed with default JDK HTTP client

- Symptom: Approval and Finance callbacks to PR failed at runtime with invalid HTTP method errors for PATCH.
- Root cause: Spring `RestClient` used the default request factory, which did not support PATCH in this Docker runtime path.
- Fix: Configure Approval and Finance RestClient customization with `JdkClientHttpRequestFactory`.
- Prevention: Any service-to-service adapter using PATCH must have an explicit request factory and be covered by Docker smoke, not just unit-level mock verification.

## [2026-06-07] Bug: Approval inbox returned null task ids after runtime approval start

- Symptom: The smoke script could see a pending approval row but could not perform the action because `taskId` was null.
- Root cause: The inbox projection preferred Camunda task id, while the runtime fallback path persisted approval steps before a Camunda task id was always available.
- Fix: Include `stepId` in `PendingTaskProjection` and return `camundaTaskId` first, falling back to `stepId.toString()`.
- Prevention: API action identifiers must always be non-null and durable even when the workflow engine-specific task id is delayed or absent.

## [2026-06-07] Bug: Finance runtime service URL and budget release references diverged from E2E flow

- Symptom: Manual PO creation initially tried the wrong PR service DNS name, and payment confirmation later failed on duplicate budget ledger release reference.
- Root cause: Finance config did not prefer the compose `PR_SERVICE_URL` used by the smoke runtime, and payment reused the earlier PR approval release reference instead of a payment-stage reference.
- Fix: Resolve purchase-request callback base URL from `PR_SERVICE_URL` first, then release payment commitments by `PURCHASE_ORDER/{poId}`.
- Prevention: Runtime smoke env must use one service URL vocabulary, and budget ledger reference types must be unique per lifecycle transition.

## [2026-06-07] Bug: Analytics cycle-time KPI failed on nullable UUID filter

- Symptom: `GET /api/v1/kpi/cycle-time` returned HTTP 500 with PostgreSQL `could not determine data type of parameter` when `department_id` was omitted.
- Root cause: MyBatis bound a nullable UUID parameter into `? IS NULL OR pr.department_id = ?`; PostgreSQL could not infer the type of the null placeholder.
- Fix: Cast the nullable `departmentId` placeholders to UUID in all cycle-time KPI queries.
- Prevention: PostgreSQL nullable UUID filters in annotation/XML mappers should cast placeholders or use dynamic SQL branches, and dashboard smoke should include the omitted-filter path.

## [2026-06-14] Bug: PR create estimated total did not refresh while editing line items

- Symptom: `/procurement/create` accepted quantity/unit price input but the estimated total stayed stale, making users think the create action was not working.
- Root cause: Angular `computed()` values read `FormArray`/`FormControl` values directly; reactive form controls are not signals, so the computed summary and urgency conditional did not re-evaluate reliably under `OnPush`.
- Fix: Add a form revision signal driven by `form.valueChanges`, use it for PR create totals and urgency display, move line-total calculation out of the template, and show validation feedback when submit is attempted with invalid fields.
- Prevention: Any Angular `computed()` that derives from reactive forms must depend on a signal bridge such as `lineRevision`/`formRevision`; do not read form controls directly inside computed values without a signal dependency.

## [2026-06-14] Bug: PR submit did not create usable approval workflow for permission-rich users

- Symptom: Submitted PRs could stay without a visible approval workflow, and approver resolution depended on seeded roles such as `MANAGER` even when the user had broader permissions like `SUPER_ADMIN`.
- Root cause: Approval rules stored role-like values and approval-service called IAM with `role=...`; IAM seeds also granted some approval permissions too broadly, so permission eligibility and workflow routing diverged.
- Fix: Convert approval rules/steps to `requiredPermission`, route approval-service internal calls with `permission=...`, add IAM permission-based approver lookup, reset/reseed approval schema, add IAM V11/V12 migrations for Super Admin approval permissions plus cleanup of seeded approval grants, expose approval process detail endpoint, and make PR detail load workflow directly from approval-service.
- Prevention: Approval workflow routing must be permission-driven; roles are only a way to grant permissions, not approval rule values. PR detail should read workflow state from approval-service instead of assuming purchase-request-service embeds `approvalProcess`.

## [2026-06-14] Bug: Approval workflow timeline rendered wrong step numbers and poor responsive layout

- Symptom: The approval workflow card showed duplicate/wrong labels such as `Bước 2`, highlighted approved steps as current, used short/misaligned connector lines in both vertical and horizontal layouts, and did not show a process name.
- Root cause: The shared Angular workflow component treated backend `stepIndex` as zero-based even though approval-service returns one-based indexes, keyed expanded state only by `stepIndex`, anchored connector lines inside the marker button, and marked any matching index as current even when the step was already terminal. The process API also did not expose `stepType`, so the UI could not distinguish sequential and parallel steps.
- Fix: Render one-based `stepIndex` directly, track steps with a composite key, restrict current styling to pending steps, move connectors to the article layout, add translated permission/type badges, expose `stepType` from approval-service process detail, and show process names on PR/approval detail screens.
- Prevention: Workflow UI should treat backend ordering semantics as contract data, verify horizontal and vertical layouts together, and keep process metadata in the detail API rather than deriving it from generic section headings.

## [2026-06-14] Bug: Additive approval category rule ran in parallel with the primary value rule

- Symptom: A newly submitted PR with a software/SaaS category could create active parallel approvals for `PR_APPROVE_L1` and `PR_APPROVE_L3`, even though the visible value rule did not define that parallel step.
- Root cause: Approval rule selection correctly merged additive category rules, but the process builder persisted `sourceStepIndex` from each source rule. Additive category rules often start at source step `1`, so their first step collided with the primary rule step `1` and was treated as parallel.
- Fix: Compute runtime step sequence with an offset per applied rule, preserve same-index parallel steps within one rule such as emergency approval, and persist the runtime sequence into `approval_steps.step_index`.
- Prevention: Any approval rule merge must distinguish source rule step index from runtime workflow step index, with tests covering both intentional parallel steps and additive rule offsetting.

## [2026-06-14] Bug: Approval rule editor required manual permission code entry

- Symptom: Creating or editing approval rules forced admins to type `requiredPermission` manually, making it easy to mistype a permission code that the approval engine cannot route.
- Root cause: The Angular approval rule editor loaded IAM permissions but exposed them only through an HTML `datalist`, which still behaves like free text and does not clearly show available approval permissions.
- Fix: Replace manual permission entry with a real select control populated from IAM approval permissions, keep fallback `PR_APPROVE_*` options, and preserve existing rule permission codes when editing.
- Prevention: Business-critical code fields should use bounded controls backed by system reference data, with free text reserved only for descriptions and comments.
