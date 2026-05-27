# Decision Log

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
