# E03 UI Shell & Design System
## User Stories và Use Cases

---

## 1. Epic Goal

Xây Angular 17 shell và design system `ep-*` đủ để triển khai nhanh các màn hình nghiệp vụ PR/Approval, đảm bảo i18n VI/EN, auth cookie, permission guard, error handling và Idempotency-Key cho state-changing requests.

---

## 2. Actors

| Actor | Vai trò |
|---|---|
| Employee | Đăng nhập và dùng các màn nghiệp vụ |
| Requester | Tạo/xem PR |
| Approver | Xử lý approval inbox |
| Admin | Vào màn quản trị khi có permission |
| Frontend Developer | Tái sử dụng component chuẩn |

---

## 3. User Stories

| ID | Story | Priority |
|---|---|---|
| E03-US-001 | Là Developer, tôi muốn Angular app skeleton có routing, layout, env config và lint/test setup. | MVP |
| E03-US-002 | Là User, tôi muốn login page dùng auth flow RSA+AES/cookie để vào hệ thống. | MVP |
| E03-US-003 | Là User, tôi muốn app shell có sidebar/topbar/breadcrumb/profile/lang switcher nhất quán. | MVP |
| E03-US-004 | Là Developer, tôi muốn design tokens và shared components `ep-*` để không hardcode màu/text/UI pattern. | MVP |
| E03-US-005 | Là Frontend, tôi muốn HTTP interceptor tự gửi cookie, request id và Idempotency-Key. | MVP |
| E03-US-006 | Là User, tôi muốn lỗi API hiển thị thống nhất và không expose technical detail. | MVP |
| E03-US-007 | Là Product Owner, tôi muốn `/ui-showcase` để kiểm tra component states. | MVP |
| E03-US-008 | Là User, tôi muốn chuyển VI/EN và mọi text hiển thị qua translate key. | MVP |
| E03-US-009 | Là Admin/Approver, tôi muốn route và action button ẩn/hiện theo permission. | MVP |
| E03-US-010 | Là User, tôi muốn nhận notification realtime khi approval task/status thay đổi. | P1 |

---

## 4. Use Cases

### E03-UC-001: BootstrapAngularShell

**Main flow:**

```
1. Tạo Angular 17 project.
2. Cấu hình SCSS, route lazy loading, environments.
3. Cài ngx-translate.
4. Thiết lập app layout route group: auth, workspace, admin.
5. Cấu hình lint/test/build scripts.
```

**Acceptance criteria:**

```
[ ] App build được.
[ ] Mọi component dùng ChangeDetectionStrategy.OnPush.
[ ] Không hardcode màu ngoài design token CSS variables.
```

### E03-UC-002: LoginPageFlow

**Main flow:**

```
1. User mở /login.
2. FE gọi GET /api/v1/auth/public-key.
3. User nhập username/password.
4. FE encrypt payload nếu encryption enabled.
5. FE gọi POST /api/v1/auth/login với withCredentials=true.
6. IAM set cookie ep_session.
7. FE gọi /users/me để hydrate user context.
8. Route đến dashboard mặc định theo permission.
```

**Acceptance criteria:**

```
[ ] Password không log ra console.
[ ] Token không đọc bằng JavaScript.
[ ] Login loading/error states rõ.
[ ] Text dùng translate pipe/key.
```

### E03-UC-003: RenderAppShell

**Main flow:**

```
1. AuthGuard kiểm tra user context.
2. Sidebar hiển thị nav theo permission.
3. Topbar hiển thị user summary, language switcher, logout.
4. Breadcrumb theo active route.
5. Router outlet render feature page.
```

**Acceptance criteria:**

```
[ ] Route không có permission bị redirect hoặc hiển thị forbidden.
[ ] Layout responsive desktop/mobile.
[ ] Không có text chồng lấn trong sidebar/topbar.
```

### E03-UC-004: BuildCoreComponents

**Components MVP:**

```
ep-button
ep-badge
ep-card
ep-table
ep-modal
ep-form-field
ep-amount
ep-sla-bar
ep-stat-card
ep-avatar
ep-icon
ep-approval-action
ep-filter-bar
ep-breadcrumb
ep-lang-switcher
ep-empty-state
ep-skeleton
```

**Acceptance criteria:**

```
[ ] Component prefix là ep-.
[ ] Inputs/outputs typed rõ.
[ ] States gồm loading, disabled, error nếu phù hợp.
[ ] Không hardcode string tiếng Việt trong template.
[ ] ep-table hỗ trợ pagination meta.
[ ] ep-amount format money string/BigDecimal-safe.
```

### E03-UC-005: ApiClientInterceptors

**Main flow:**

```
1. Mọi HTTP request set withCredentials=true.
2. POST/PUT/PATCH tự thêm Idempotency-Key UUID v4 nếu caller chưa set.
3. Error interceptor map ApiResponse error thành UI error model.
4. Auth error 401 redirect login.
5. Forbidden 403 hiển thị permission error.
```

**Acceptance criteria:**

```
[ ] Idempotency-Key giữ nguyên khi retry cùng user intent.
[ ] Không thêm Idempotency-Key cho GET.
[ ] Không log response chứa token/secret.
```

### E03-UC-006: PermissionGuardAndDirective

**Main flow:**

```
1. User context chứa permissions từ /users/me.
2. Route config khai báo requiredPermission.
3. Guard chặn route nếu thiếu permission.
4. Directive `*epHasPermission` ẩn action button nếu thiếu permission.
```

**Acceptance criteria:**

```
[ ] Check permission code, không check role.
[ ] Missing permission không làm crash page.
[ ] Admin nav chỉ hiện với ADMIN_* hoặc SYSTEM_* permissions.
```

---

## 5. Technical Deliverables

```
frontend/eprocure-web Angular app
src/app/core/auth/*
src/app/core/http/*
src/app/core/permissions/*
src/app/layout/*
src/app/shared/components/ep-*
src/assets/i18n/vi.json
src/assets/i18n/en.json
src/styles/_tokens.scss
src/styles.scss
src/app/ui-showcase/*
```

---

## 6. MVP Acceptance Checklist

```
[ ] Login -> /users/me -> shell route chạy được.
[ ] withCredentials=true trong mọi API call.
[ ] POST/PUT/PATCH có Idempotency-Key.
[ ] Component dùng OnPush.
[ ] Subscription dùng takeUntilDestroyed(this.destroyRef).
[ ] Text hiển thị có translate key.
[ ] CSS dùng var(--...) cho màu sắc.
[ ] Permission guard dùng permission code.
```
