# UI Module Plan — Admin & System Config Portal

> Mục tiêu: hoàn thiện phần E13 còn thiếu sau khi các UI nghiệp vụ chính đã có. Plan này không chỉ thêm màn hình, mà phải kiểm tra lại backend/runtime contract vì `docs/api/admin-service.openapi.yaml` mô tả một `admin-service` riêng. Tính đến 2026-06-15, backend foundation đã có tại `services/admin-service` và cover health/config read-only, audit-log query/export/status/download.

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

### Contract còn thiếu UI hoặc chưa rõ backend

Nguồn: `docs/api/admin-service.openapi.yaml`

| Nhóm | Endpoint | Permission | Ghi chú |
|---|---|---|---|
| System Config | `GET /admin/config/services`, `GET/PUT /admin/config/services/{serviceName}` | `SYSTEM_CONFIG` | Endpoint nhạy cảm, có sensitive masking và TOTP confirmation |
| Restart/Key rotation | `POST /admin/config/services/{serviceName}/restart`, `POST /admin/config/encryption/rotate-key` | `SYSTEM_CONFIG` | High-risk action, phải có modal confirm + reason/TOTP |
| Audit log | `GET /admin/audit-log`, `POST /admin/audit-log/export`, `GET /admin/audit-log/export/{jobId}`, `GET /admin/audit-log/export/{jobId}/download` | `SYSTEM_AUDIT_VIEW` | Cần filter bắt buộc `from_time`, `to_time`, pagination/export, job polling/download |
| Catalog categories | `/admin/catalog/categories` | `ADMIN_CATALOG_MANAGE` | Có thể trùng với inventory catalog item UI; cần làm category admin riêng |
| Department admin | `/admin/departments` mutations | `ADMIN_DEPARTMENT_MANAGE` | Frontend hiện đang dùng `/org/departments`; cần align thật với IAM backend |
| System health | `GET /admin/health` | `SYSTEM_CONFIG` | Operational dashboard cho service/infrastructure health |
| Sessions | `GET /admin/sessions`, `PATCH /admin/sessions/{sessionId}/invalidate` | `SYSTEM_CONFIG` | Active sessions + forced logout |

### Mismatch / follow-up cần xử lý trước khi code lớn

- `services/admin-service` đã có runtime foundation: `/admin/config/services`, `/admin/config/services/{serviceName}`, `/admin/health`, `/admin/audit-log`, `/admin/audit-log/export`, `/admin/audit-log/export/{jobId}`, `/admin/audit-log/export/{jobId}/download`.
- Chưa có controller/backend cho `/admin/sessions`, config update/restart/rotate-key.
- `AdminOrgService` hiện gọi `/org/departments`, không phải `/admin/departments`.
- Một số admin capability đã nằm ở service khác:
  - IAM: users, roles, RBAC, org.
  - Notification: templates.
  - Inventory: catalog items.
  - Approval: approval rules.

Kết luận: plan này cần triển khai theo hai tầng:

1. **Contract truth slice:** đã xác minh và tạo backend foundation cho `admin-service`.
2. **UI slice theo capability có backend thật trước**, sau đó mới thêm backend/service mới cho các phần chưa tồn tại.

---

## 2. Nguyên Tắc Triển Khai

- Không tạo HTTP DELETE; deactivate/invalidate/restart/rotate đều là state transition.
- `SYSTEM_CONFIG` là quyền nhạy cảm: UI phải có cảnh báo, confirm code/TOTP, reason, disabled state rõ ràng.
- Không log hoặc hiển thị secret thật. GET config chỉ hiển thị masked sensitive values.
- POST/PUT/PATCH phải có `Idempotency-Key`; ưu tiên dùng `ApiService`.
- Mọi visible text dùng i18n VI/EN.
- SCSS dùng design tokens `var(--...)`.
- Không tạo dead link sidebar: chỉ thêm nav khi route/component/service đã tồn tại trong cùng slice.
- Nếu endpoint OpenAPI chưa có backend controller thật, không dựng UI giả gọi endpoint đó mà ghi rõ backend slice cần làm.

---

## 3. Slice Đề Xuất

### Slice 9a — Contract truth + implementation strategy

| Việc | Chi tiết |
|---|---|
| Backend inventory | ✅ `admin-service` mới tồn tại tại `services/admin-service`; gateway đã route `/api/v1/admin/*` sang port 8089 |
| Controller check | ✅ Có `/admin/config/services*`, `/admin/health`, `/admin/audit-log`, `/admin/audit-log/export`, export job status/download; chưa có `/admin/sessions`, `/admin/catalog/categories`, `/admin/departments` |
| Decision | ✅ Chọn hướng tạo `admin-service` riêng theo spec; các capability đã có ở IAM/notification/inventory/approval vẫn giữ service owner hiện tại |
| Docs | ✅ Cập nhật plan này, `agent/memory/decision-log.md`, `agent/memory/progress-tracker.md` |

**Kỳ vọng:** không code UI gọi endpoint chưa tồn tại.

### Slice 9b — Admin Config shell + route/nav foundation

| Việc | Chi tiết |
|---|---|
| Models/service | Tạo `admin-config.model.ts`, `admin-config.service.ts` theo contract đã xác minh |
| Routes | Thêm lazy routes chỉ cho phần có backend thật |
| Nav | Thêm nav `System Config`, `Audit Log`, `System Health`, `Sessions` theo permission |
| i18n | Thêm `nav.systemConfig`, `nav.auditLog`, `nav.systemHealth`, `nav.sessions`, route keys |

### Slice 9c — System Health page

| Việc | Chi tiết |
|---|---|
| Page | `/admin/health` dense operational dashboard |
| UI | Overall status, service table, infra PostgreSQL/Redis/Kafka panels |
| State | Loading/empty/error/retry |
| Access | `SYSTEM_CONFIG` |

Đây là slice UI an toàn nhất nếu backend health endpoint có thật hoặc được thêm trước.

### Slice 9d — Service Config read-only + risky mutation workflow

| Việc | Chi tiết |
|---|---|
| List/detail | Service config list, status summary, variables table |
| Sensitive values | Masked values only; không có “reveal secret” nếu backend không có endpoint riêng |
| Update config | Modal yêu cầu `confirmationCode`, `changeReason`, `requiresRestart` |
| Restart | Modal riêng, reason + confirmation code |
| Rotate key | Cảnh báo high-risk; chỉ enable khi backend thật và permission đúng |

### Slice 9e — Audit Log query + export

| Việc | Chi tiết |
|---|---|
| Filters | from/to required, actor/entity/action/service/success filters |
| Table | actor, action, entity, service, requestId, success/error, occurredAt |
| Detail drawer | old/new values pretty JSON, description |
| Export | POST export job, polling status và download XLSX đã có backend; disabled rõ khi filter invalid; hiển thị `QUEUED/PROCESSING/COMPLETED/FAILED` sau khi tạo |
| Access | `SYSTEM_AUDIT_VIEW` |

### Slice 9f — Active Sessions

| Việc | Chi tiết |
|---|---|
| List | user, ip, userAgent, createdAt, lastActivity |
| Filter | userId + pagination |
| Invalidate | PATCH invalidate với reason, idempotency, confirm modal |
| Safety | Không hiển thị token/session secret |

### Slice 9g — Catalog Category Admin

| Việc | Chi tiết |
|---|---|
| List | categories tree/table, include inactive toggle |
| Create/update | code/name/parent/special approval/RFQ threshold/CAPEX |
| Deactivate | PATCH deactivate, disabled khi category có active constraints nếu backend trả lỗi |
| Integration | Không trùng item catalog UI; đây là category taxonomy admin |

### Slice 9h — Department Admin mutation alignment

| Việc | Chi tiết |
|---|---|
| Contract align | So sánh `/org/departments` hiện có với `/admin/departments` OpenAPI |
| UI | Extend org chart with create/update/deactivate only if backend supports it |
| Safety | Deactivate must explain active employee constraint |

### Slice 9i — Verification

| Lệnh | Kỳ vọng |
|---|---|
| `npm run build` trong `frontend/eprocure-web` | Pass; ghi rõ warning cũ nếu còn |
| Targeted backend tests nếu thêm endpoint | `mvn -pl services/{service} test` |
| `git diff --check` | Pass |
| Browser test | Chỉ mở nếu user yêu cầu |

---

## 4. Thứ Tự Thực Hiện Đúng Nhất

1. Slice 9a — Contract truth.
2. Nếu backend endpoint đã có: 9b + 9c health page trước.
3. Nếu backend endpoint chưa có: tạo tiếp backend foundation cho audit/session trước khi UI.
4. Audit log và sessions làm sau health/config vì cần chuẩn hóa pagination/filter/export.
5. Catalog category và department mutation làm cuối vì dễ trùng với Inventory/IAM UI hiện có.

---

## 5. Acceptance Criteria

- Không còn route/nav admin trỏ vào màn hình không tồn tại.
- E13 Admin & Config Portal có kế hoạch rõ theo backend thật, không dựa vào OpenAPI aspirational.
- Mọi action nhạy cảm có reason/confirmation code/idempotency.
- Config values sensitive không bị expose.
- Audit/session pages có filter/loading/empty/error states.
- i18n VI/EN đầy đủ.
- Build pass và tracker được cập nhật theo từng slice.
