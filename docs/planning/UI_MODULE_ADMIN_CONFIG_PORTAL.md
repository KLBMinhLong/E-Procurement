# UI Module Plan — Admin & System Config Portal

> Mục tiêu: hoàn thiện phần E13 còn thiếu sau khi các UI nghiệp vụ chính đã có. Plan này đã được rà lại theo frontend route hiện tại, `services/admin-service` runtime code, gateway route, permission seed, và `docs/api/admin-service.openapi.yaml`.

**Cập nhật 2026-06-16:** backend `admin-service` đã có đủ foundation cho health/config read/update/restart/key rotation, audit-log query/export/status/download, active sessions list/invalidate, catalog category admin facade, và department mutation facade. OpenAPI đã document audit side effects/idempotency replay. Từ thời điểm này, trọng tâm chuyển sang frontend implementation.

---

## 1. Hiện Trạng Rà Soát

### Frontend đã có

| Khu vực | Route | Trạng thái |
|---|---|---|
| User management | `/admin/users` | ✅ Có UI CRUD/lock/reset password |
| Role management | `/admin/roles` | ✅ Có UI |
| RBAC matrix | `/admin/rbac` | ✅ Có UI |
| Org chart | `/admin/org-chart` | ✅ Có tree UI đọc/tạo/sửa theo service hiện tại |
| Notification templates | `/admin/notification-templates` | ✅ Có UI qua notification-service |
| Approval rules | `/approvals/rules` | ✅ Có UI, redirect từ `/admin/approval-rules` |

### Backend contract hiện có cho frontend

Nguồn đã đối chiếu: `docs/api/admin-service.openapi.yaml`, `services/admin-service/src/main/java/com/eprocure/admin/presentation/controller`, `docker-compose.yml`, `infra/nginx/templates/default.conf.template`, IAM permission seeds.

| Nhóm | Endpoint | Permission | UI cần làm |
|---|---|---|---|
| System health | `GET /admin/health` | `SYSTEM_CONFIG` | Operational dashboard dense, service/infra health, retry |
| System config read | `GET /admin/config/services`, `GET /admin/config/services/{serviceName}` | `SYSTEM_CONFIG` | Config list/detail, masked sensitive values |
| System config mutation | `PUT /admin/config/services/{serviceName}` | `SYSTEM_CONFIG` | Modal TOTP + reason + variable diff; kết quả `PENDING_MANUAL_APPLY` |
| Restart/key rotation | `POST /admin/config/services/{serviceName}/restart`, `POST /admin/config/encryption/rotate-key` | `SYSTEM_CONFIG` | High-risk confirm modal; không mô tả là đã apply runtime |
| Audit log | `GET /admin/audit-log` | `SYSTEM_AUDIT_VIEW` | Required from/to filter, pagination, detail drawer |
| Audit export | `POST /admin/audit-log/export`, `GET /admin/audit-log/export/{jobId}`, `GET /admin/audit-log/export/{jobId}/download` | `SYSTEM_AUDIT_VIEW` | Create job, poll, download XLSX, replay-aware UX |
| Active sessions | `GET /admin/sessions`, `PATCH /admin/sessions/{sessionId}/invalidate` | `SYSTEM_CONFIG` | Session table, user filter, invalidate confirm reason |
| Catalog categories | `/admin/catalog/categories*` | `ADMIN_CATALOG_MANAGE` | Category taxonomy admin, not inventory item catalog |
| Department mutations | `/admin/departments*` | `ADMIN_DEPARTMENT_MANAGE` | Move org mutation calls to audited admin facade; keep `/org/departments` for read tree |

### Frontend gaps phải xử lý trước khi code sâu

- Parent route `/admin` guard hiện mới gồm `ADMIN_USER_VIEW`, `ADMIN_ROLE_MANAGE`, `SYSTEM_CONFIG`, `ADMIN_APPROVAL_RULE`; cần thêm `SYSTEM_AUDIT_VIEW`, `ADMIN_CATALOG_MANAGE`, `ADMIN_DEPARTMENT_MANAGE`, `ORG_VIEW` để không chặn user có quyền child route.
- Shell nav chưa có System Config, Audit Log, System Health, Sessions, Catalog Category Admin.
- `frontend/eprocure-web/src/app/features/admin/models/admin.model.ts` chưa có model cho admin-service contracts.
- Chưa có service frontend cho `admin-service`; nên thêm service riêng theo API `/api/v1/admin/*`.
- `AdminOrgService` đang mutate `/org/departments`; sau slice department alignment phải dùng `/admin/departments*` cho create/update/deactivate để có audit row bất biến.
- `ApiService` hiện không có helper blob download; audit export download cần dùng direct `HttpClient` với `responseType: 'blob'` hoặc mở rộng `ApiService` có chủ đích.
- Existing `/inventory/catalog` là inventory item catalog. `/admin/catalog-categories` phải là taxonomy admin riêng, tránh trộn item stock/catalog UI.

Kết luận: plan hiện đã sẵn sàng để code frontend. Không còn backend blocker cho các page E13 chính, ngoại trừ runtime executor/secret-manager apply là boundary vận hành có chủ ý và UI phải hiển thị là pending manual apply.

---

## 2. Nguyên Tắc Triển Khai

- Không tạo HTTP DELETE; deactivate/invalidate/restart/rotate đều là state transition.
- `SYSTEM_CONFIG` là quyền nhạy cảm: UI phải có cảnh báo, TOTP/confirmation code, reason, disabled state rõ ràng.
- Không hiển thị secret thật. GET config chỉ hiển thị masked sensitive values; không có nút reveal nếu backend không có endpoint riêng.
- POST/PUT/PATCH dựa vào `idempotencyInterceptor` hoặc idempotency key explicit khi cần replay tracking.
- Mọi visible text dùng i18n VI/EN.
- SCSS dùng design tokens `var(--...)`, không hardcode màu.
- Component mới phải `ChangeDetectionStrategy.OnPush`, standalone, signals/computed, `takeUntilDestroyed`.
- Không tạo dead link sidebar: nav chỉ thêm cùng slice với route/component hoạt động.
- UI là operational console: dense, scan nhanh, bảng/filter/action rõ ràng, không landing/hero.

---

## 3. Slice Đề Xuất

### Slice 9a — Contract truth + implementation strategy

| Việc | Chi tiết |
|---|---|
| Backend inventory | ✅ `admin-service` tồn tại tại `services/admin-service`; gateway route `/api/v1/admin/*` sang port 8089 |
| Controller check | ✅ Có config read/update/restart, rotate key, health, audit query/export/status/download, sessions list/invalidate, catalog categories, departments |
| Permission check | ✅ IAM seeds có `SYSTEM_CONFIG`, `SYSTEM_AUDIT_VIEW`, `ADMIN_CATALOG_MANAGE`, `ADMIN_DEPARTMENT_MANAGE` |
| Contract sync | ✅ OpenAPI đã document audit side effects/idempotency replay |
| Decision | ✅ UI dùng service-owner boundary: IAM/PR/Admin facade; không gọi cross-DB |

**Kỳ vọng:** không code UI giả; tất cả page trong plan gọi endpoint thật hoặc service owner hiện có.

### Slice 9b — Admin operations foundation

| Việc | Chi tiết |
|---|---|
| Models | Thêm `admin-operations.model.ts` hoặc mở rộng admin model có typed contract cho ServiceConfig, health, audit, session, category, department |
| Service | Thêm `admin-operations.service.ts` gọi `/admin/*`; download dùng blob-safe method |
| Parent route guard | Mở rộng `/admin` required permissions: `ADMIN_USER_VIEW`, `ADMIN_ROLE_MANAGE`, `SYSTEM_CONFIG`, `SYSTEM_AUDIT_VIEW`, `ADMIN_APPROVAL_RULE`, `ADMIN_CATALOG_MANAGE`, `ADMIN_DEPARTMENT_MANAGE`, `ORG_VIEW` |
| Route keys | Thêm i18n route/nav keys cho từng page, nhưng nav link chỉ bật khi page trong slice đã có component |
| Shared helpers | Status tone mapping, date/time formatting, JSON pretty helper nếu cần |

### Slice 9c — System Health page

| Việc | Chi tiết |
|---|---|
| Route/nav | `/admin/health`, nav `System Health`, permission `SYSTEM_CONFIG` |
| Page | Dense operational dashboard |
| UI | Overall status strip, service table, PostgreSQL/Redis/Kafka panels, checkedAt, response time |
| State | Loading skeleton, empty/error/retry, stale checkedAt warning |
| Verify | `npm run build` |

Đây là slice code đầu tiên nên làm vì ít mutation, kiểm route/nav/service foundation nhanh nhất.

### Slice 9d — Service Config + high-risk actions

| Việc | Chi tiết |
|---|---|
| Route/nav | `/admin/system-config`, nav `System Config`, permission `SYSTEM_CONFIG` |
| List/detail | Service cards/table, status summary, variables table |
| Sensitive values | Masked values only; không có reveal |
| Update config | Modal có variable diff, `confirmationCode`, `changeReason`, `requiresRestart` |
| Restart | Modal riêng có reason + confirmation code |
| Rotate key | Cảnh báo high-risk, keySize selector, confirmation code |
| Result UX | Hiển thị `PENDING_MANUAL_APPLY`, `applied=false`, actionId; không nói runtime đã đổi thật |

### Slice 9e — Audit Log query + export

| Việc | Chi tiết |
|---|---|
| Route/nav | `/admin/audit-log`, nav `Audit Log`, permission `SYSTEM_AUDIT_VIEW` |
| Filters | from/to required, actor/entity/action/service/success filters |
| Table | actor, action, entity, service, requestId, success/error, occurredAt |
| Detail drawer | old/new values pretty JSON, description, endpoint/http method |
| Export | POST export job, poll status, download XLSX khi `COMPLETED`; disable rõ khi filter invalid |
| Replay UX | Nếu backend trả replay job, không tạo duplicate card/job trong UI |

### Slice 9f — Active Sessions

| Việc | Chi tiết |
|---|---|
| Route/nav | `/admin/sessions`, nav `Sessions`, permission `SYSTEM_CONFIG` |
| List | userName/fullName, ipAddress, userAgent, createdAt, lastActivity, expiresAt |
| Filter | userId + pagination; nếu cần UX tốt hơn có thể mở lookup sau, không tự gọi API chưa có |
| Invalidate | Confirm modal có reason; PATCH idempotent |
| Safety | Không hiển thị token/session secret; reason không cần hiển thị lại sau action |

### Slice 9g — Catalog Category Admin

| Việc | Chi tiết |
|---|---|
| Route/nav | `/admin/catalog-categories`, nav `Catalog Categories`, permission `ADMIN_CATALOG_MANAGE` |
| List | Category tree/table, include inactive toggle, itemCount, status |
| Create/update | code/name/parent/special approval/RFQ threshold/CAPEX |
| Deactivate | PATCH deactivate, disabled/copy rõ khi backend trả conflict |
| Integration | Không trùng `/inventory/catalog`; page này quản trị taxonomy mua sắm |

### Slice 9h — Department Admin mutation alignment

| Việc | Chi tiết |
|---|---|
| Route | Giữ `/admin/org-chart` cho read tree và mutation UX |
| Read | Tiếp tục dùng `/org/departments` nếu cần tree hiện tại |
| Mutation | Chuyển create/update sang `/admin/departments*`, thêm deactivate |
| Contract align | `parentId/headUserId/glAccountPrefix` theo admin-service; không dùng `parentCode/managerId` cho mutation mới |
| Safety | Deactivate phải giải thích active employee/child department constraint khi lỗi 409 |

### Slice 9i — Verification

| Lệnh | Kỳ vọng |
|---|---|
| `npm run build` trong `frontend/eprocure-web` | Pass; ghi rõ warning cũ nếu còn |
| `git diff --check` | Pass |
| Backend tests | Chỉ chạy nếu frontend change phát hiện cần chỉnh backend |
| Browser test | Chỉ mở nếu user yêu cầu |

---

## 4. Thứ Tự Thực Hiện Đúng Nhất

1. Slice 9b + 9c: foundation + System Health page.
2. Slice 9d: System Config read/mutation workflow, vì đây là core `SYSTEM_CONFIG`.
3. Slice 9e: Audit Log query/export để kiểm chứng audit rows từ các mutation.
4. Slice 9f: Active Sessions và invalidate.
5. Slice 9g: Catalog Category Admin.
6. Slice 9h: Department mutation alignment trong Org Chart.
7. Slice 9i: final build/diff verification và cập nhật tracker.

---

## 5. Acceptance Criteria

- Không còn route/nav admin trỏ vào màn hình không tồn tại.
- Parent `/admin` route không chặn sai user có quyền child route.
- E13 Admin & Config Portal bám backend thật, không dựa vào OpenAPI aspirational.
- Mọi action nhạy cảm có reason/confirmation code/idempotency.
- Config values sensitive không bị expose, không có reveal secret giả.
- Pending runtime actions hiển thị đúng là `PENDING_MANUAL_APPLY`.
- Audit/session pages có filter/loading/empty/error states.
- i18n VI/EN đầy đủ.
- Build pass và tracker được cập nhật theo từng slice.
