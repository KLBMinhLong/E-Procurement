# E02 IAM Service
## User Stories và Use Cases

---

## 1. Epic Goal

Cung cấp xác thực, opaque session token, RBAC permission cache, user/role management, org chart và delegation để các service khác có thể bảo vệ API bằng permission code và resolve approver đúng tổ chức.

---

## 2. Actors

| Actor | Vai trò |
|---|---|
| Employee | Đăng nhập, xem hồ sơ, đổi mật khẩu |
| Approver | Dùng permission và delegation trong approval |
| Admin | Quản lý users, roles, permissions, org chart |
| Gateway | Verify session và inject user context |
| Downstream Service | Tin vào `X-User-ID` và internal `X-Api-Key` từ gateway |

---

## 3. User Stories

| ID | Story | Priority | API Reference |
|---|---|---|---|
| E02-US-001 | Là Frontend, tôi muốn lấy RSA public key để mã hóa login payload. | MVP | `GET /auth/public-key` |
| E02-US-002 | Là Employee, tôi muốn đăng nhập bằng username/password và nhận HttpOnly cookie. | MVP | `POST /auth/login` |
| E02-US-003 | Là Employee, tôi muốn đăng xuất để token bị revoke ngay. | MVP | `POST /auth/logout` |
| E02-US-004 | Là Employee, tôi muốn xem hồ sơ và permissions hiện tại. | MVP | `GET /users/me` |
| E02-US-005 | Là Admin, tôi muốn tạo/cập nhật/khoá user để quản trị nhân sự hệ thống. | MVP | `/users`, `/users/{id}`, `/users/{id}/status` |
| E02-US-006 | Là Admin, tôi muốn quản lý roles và permission mapping. | MVP | `/roles`, `/roles/{code}/permissions`, `/permissions` |
| E02-US-007 | Là Approval Engine, tôi muốn resolve approver theo department/org để tạo approval chain. | MVP | `GET /org/approvers` |
| E02-US-008 | Là Approver, tôi muốn ủy quyền phê duyệt trong khoảng thời gian hợp lệ. | P1 | `/delegations` |
| E02-US-009 | Là Employee, tôi muốn bật và xác nhận 2FA để tăng bảo mật. | P1 | `/auth/two-factor/verify`, `/users/me/two-factor/*` |
| E02-US-010 | Là Employee, tôi muốn forgot/reset password an toàn. | P1 | `/auth/forgot-password`, `/auth/reset-password` |
| E02-US-011 | Là Employee, tôi muốn đăng nhập Google OAuth khi doanh nghiệp bật SSO. | P2 | `/auth/oauth/google` |

---

## 4. Use Cases

### E02-UC-001: GetPublicKeyUseCase

**Preconditions:** RSA key pair active trong IAM config.

**Main flow:**

```
1. FE gọi GET /api/v1/auth/public-key.
2. IAM trả publicKey, keyVersion, algorithm.
3. FE dùng public key để encrypt login payload.
```

**Acceptance criteria:**

```
[ ] Response không yêu cầu auth.
[ ] Không trả private key.
[ ] Có keyVersion để rotate key về sau.
```

### E02-UC-002: LoginUseCase

**Preconditions:**

```
- User tồn tại và không bị LOCKED/INACTIVE.
- Payload được decrypt nếu ENCRYPTION_ENABLED=true.
- Keycloak realm đã có credential tương ứng.
```

**Main flow:**

```
1. Validate request.
2. Verify credential với Keycloak.
3. Invalidate old active session của user trong Redis và DB.
4. Generate opaque token 64 chars.
5. Save session:{token} vào Redis và sessions table.
6. Load permissions từ role mapping, cache user-perm:{userId}.
7. Set-Cookie ep_session; HttpOnly; Secure; SameSite=Strict; Path=/.
8. Return ApiResponse với user summary, roles, permissions, requiresTwoFactor nếu cần.
```

**Alternate/error flows:**

```
- Sai credential -> tăng failed attempts, trả IAM_001/IAM_002 theo convention.
- User bị LOCKED sau 5 lần sai -> trả IAM_LOCKED.
- requiresTwoFactor=true -> set cookie pending, không issue full session.
```

**Acceptance criteria:**

```
[ ] Token không xuất hiện trong response body.
[ ] Token là opaque 64 chars, không JWT.
[ ] Login mới revoke session cũ ngay.
[ ] Không log password/token/encryptedPayload.
[ ] Auth login được whitelist khỏi Idempotency-Key nếu spec quyết định như hiện tại.
```

### E02-UC-003: LogoutUseCase

**Main flow:**

```
1. Lấy token từ HttpOnly cookie.
2. Mark session inactive/revoked trong DB.
3. Delete session:{token} và user-perm:{userId}.
4. Clear cookie ep_session.
5. Return ApiResponse success.
```

**Acceptance criteria:**

```
[ ] Logout là POST, không DELETE.
[ ] Token bị revoke ngay cả khi Redis delete fail thì DB backup vẫn ghi nhận.
[ ] Response clear cookie.
```

### E02-UC-004: GetCurrentUserUseCase

**Main flow:**

```
1. Gateway/IAM verify token.
2. Load user detail từ IAM DB.
3. Load permissions từ Redis cache hoặc DB.
4. Return user profile, roles, permissions.
```

**Acceptance criteria:**

```
[ ] Không return passwordHash/twoFactorSecret/token.
[ ] Permission cache TTL đúng config.
[ ] User deleted/inactive không được coi là authenticated.
```

### E02-UC-005: ManageUserUseCases

**Use cases:**

```
CreateUserUseCase
UpdateUserUseCase
ChangeUserStatusUseCase
AssignUserRolesUseCase
ListUsersUseCase
GetUserByIdUseCase
```

**Rules:**

```
- @PreAuthorize dùng ADMIN_USER_MANAGE, ADMIN_USER_VIEW, ADMIN_ROLE_MANAGE.
- POST/PUT/PATCH có Idempotency-Key.
- Không HTTP DELETE; khóa user bằng PATCH /users/{id}/status.
- Email/phone được validate bằng Value Object.
- Password hash dùng BCrypt(password + userId_salt, cost=12).
```

**Acceptance criteria:**

```
[ ] Admin không thể tạo username/email trùng.
[ ] Status transition hợp lệ: PENDING_VERIFY -> ACTIVE -> INACTIVE/LOCKED.
[ ] Audit log cho tạo/sửa/khoá user và assign role.
```

### E02-UC-006: ManageRolePermissionUseCases

**Use cases:**

```
ListRolesUseCase
CreateRoleUseCase
UpdateRolePermissionsUseCase
ListPermissionsUseCase
```

**Rules:**

```
- Permission code là source of truth cho @PreAuthorize.
- Không hardcode role trong business code.
- System role không thể soft delete/deactivate nếu đang được dùng.
- Cập nhật permission phải evict role-perm:{roleCode} và user-perm:* liên quan.
```

### E02-UC-007: ResolveApproverUseCase

**Consumer:** Approval Engine.

**Input:**

```
departmentId
requesterId
requiredApproverRole hoặc permission code
amount/category/priority context nếu cần
```

**Main flow:**

```
1. Load department and org chain.
2. Resolve manager/director/finance/C-level theo role/permission.
3. Exclude requester để enforce Separation of Duties.
4. Apply active delegation nếu có.
5. Return approver candidates với delegatedFrom nếu applicable.
```

**Acceptance criteria:**

```
[ ] Requester không được resolve thành approver của PR chính họ.
[ ] Delegation chỉ active trong startAt/endAt và không vượt maxValue.
[ ] Không return null nếu không tìm thấy approver; trả error rõ.
```

### E02-UC-008: ManageDelegationUseCases

**Use cases:**

```
CreateDelegationUseCase
ListMyDelegationsUseCase
RevokeDelegationUseCase
```

**Rules:**

```
- Revoke dùng PATCH /delegations/{id}/revoke.
- delegateId.orgLevel >= delegatorId.orgLevel.
- Không được ủy quyền vượt quyền bản thân.
- Có audit log khi tạo/sửa/revoke.
```

---

## 5. Technical Deliverables

```
iam-service Spring Boot project
Flyway migrations: users, roles, permissions, user_roles, sessions, delegations, departments, org_nodes
Domain: User, Role, Department, OrgNode, Delegation, Email, PhoneNumber
UseCases: Login, Logout, GetMe, ManageUser, ManageRolePermission, ResolveApprover, ManageDelegation
Redis adapters: SessionStore, PermissionCache, IdempotencyStore
Keycloak adapter: CredentialVerificationPort
Controllers matching iam-service.openapi.yaml
Unit tests for domain/application
```

---

## 6. MVP Acceptance Checklist

```
[ ] Login sets HttpOnly Secure SameSite=Strict cookie.
[ ] New login invalidates old session.
[ ] GET /users/me returns permissions.
[ ] Admin can create user and assign role.
[ ] Approval Engine can resolve approver from org.
[ ] No JWT token generation.
[ ] No token/password/secret logs.
[ ] POST/PUT/PATCH endpoints enforce Idempotency-Key except explicitly whitelisted auth flow.
```
