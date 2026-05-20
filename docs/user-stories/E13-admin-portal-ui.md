# E13-A Admin Portal (User, RBAC, Org Chart UI)
## User Stories và Use Cases Giao diện Quản trị

---

## 1. Epic Goal

Cung cấp giao diện quản trị Web (Angular) trực quan, hiện đại, và bảo mật cao dành cho các Quản trị viên (Admin/HR Admin) để thực hiện:
1. **User Management UI:** Quản lý danh sách, thêm, sửa, và khóa/mở khóa tài khoản nhân sự.
2. **RBAC Configuration UI:** Quản lý danh sách vai trò (Roles) và cấu hình phân quyền (Permissions mapping).
3. **Org Chart UI (Cây chức vụ):** Hiển thị sơ đồ tổ chức phòng ban trực quan dạng hình cây và quản lý phòng ban (thêm/sửa/ẩn nút).

Epic này được ưu tiên triển khai sớm (ngay sau Epic E04) để đảm bảo dữ liệu nhân sự, vai trò, và cơ cấu tổ chức phòng ban có thể được thiết lập dễ dàng thông qua giao diện trước khi triển khai Approval Engine (E05).

---

## 2. Actors

| Actor | Vai trò |
|---|---|
| **System Admin** | Quản lý toàn bộ hệ thống, phân quyền vai trò (RBAC), quản lý phòng ban. |
| **HR Admin** | Quản lý thông tin nhân sự (Users), cập nhật trạng thái hoạt động của nhân viên. |
| **Employee** | Được tạo và phân quyền để sử dụng hệ thống theo đúng phòng ban và vai trò. |

---

## 3. User Stories

| ID | Story | Priority | API Endpoints Phục Vụ |
|---|---|---|---|
| **E13-A-US-001** | Là Admin, tôi muốn xem danh sách Users với phân trang, tìm kiếm, lọc theo phòng ban/trạng thái để dễ dàng quản lý. | MVP | `GET /api/v1/users` |
| **E13-A-US-002** | Là HR Admin, tôi muốn tạo mới User và gán thông tin phòng ban, vai trò để nhân viên mới có thể truy cập hệ thống. | MVP | `POST /api/v1/users` |
| **E13-A-US-003** | Là HR Admin, tôi muốn cập nhật thông tin cá nhân, phòng ban, và vai trò của User khi họ thay đổi vị trí công việc. | MVP | `PUT /api/v1/users/{id}` |
| **E13-A-US-004** | Là Admin, tôi muốn vô hiệu hóa (deactivate) hoặc khóa (lock) tài khoản của User khi họ nghỉ việc hoặc sai phạm mà không làm mất dữ liệu lịch sử (soft delete). | MVP | `PATCH /api/v1/users/{id}/status` |
| **E13-A-US-005** | Là Admin, tôi muốn xem danh sách các Roles hiện có và danh sách Permissions của từng Role. | MVP | `GET /api/v1/roles`, `GET /api/v1/permissions` |
| **E13-A-US-006** | Là Admin, tôi muốn thay đổi (gán thêm hoặc gỡ bỏ) các quyền (permissions) của một Role để điều chỉnh quyền hạn phòng ban. | MVP | `PUT /api/v1/roles/{code}/permissions` |
| **E13-A-US-007** | Là Admin, tôi muốn xem sơ đồ tổ chức (Org Tree) của công ty dưới dạng cây phân cấp trực quan để nắm bắt cơ cấu. | MVP | `GET /api/v1/org/departments` |
| **E13-A-US-008** | Là Admin, tôi muốn thêm mới một phòng ban con hoặc cập nhật tên/phòng ban cha trực tiếp trên giao diện cây tổ chức. | MVP | `POST /api/v1/org/departments`, `PUT /api/v1/org/departments/{id}` |

---

## 4. Giao diện & Use Cases Chi tiết (Frontend)

### E13-A-UC-001: Quản lý và Tìm kiếm Người dùng (User Directory UI)
* **Quyền truy cập:** `ADMIN_USER_VIEW` hoặc `ADMIN_USER_MANAGE`
* **Luồng chính:**
  1. Admin truy cập màn hình `/admin/users`.
  2. Giao diện tải danh sách người dùng thông qua `ep-table` với các cột: Họ tên, Email, Phòng ban, Trạng thái (Active/Inactive/Locked), Vai trò, và Cột Thao tác.
  3. Hỗ trợ thanh bộ lọc `ep-filter-bar`:
     * Tìm kiếm text (Họ tên/Email/Username).
     * Dropdown lọc theo phòng ban (lấy danh sách phòng ban từ API).
     * Dropdown lọc theo Trạng thái.
  4. Hỗ trợ phân trang: Chuyển trang, thay đổi kích thước trang (10, 20, 50 dòng).

### E13-A-UC-002: Thêm mới và Cập nhật Người dùng (User Form Modal)
* **Quyền truy cập:** `ADMIN_USER_MANAGE`
* **Luồng chính:**
  1. Admin bấm nút "Thêm nhân viên" hoặc nút "Sửa" trên dòng thông tin nhân viên.
  2. Hệ thống hiển thị một `ep-modal` chứa Form điền thông tin nhân viên:
     * Họ tên (required)
     * Email (required, regex validate email)
     * Username (required, chỉ cho nhập chữ thường, số, dấu gạch dưới)
     * Số điện thoại (optional, validate số điện thoại VN)
     * Phòng ban (Dropdown dạng cây hoặc Select - required)
     * Vai trò (Checkbox list hoặc Select đa chọn - required)
  3. Khi gửi (Submit):
     * Validate toàn bộ phía client.
     * Gửi request lên IAM Service với `Idempotency-Key` (UUIDv4) tự sinh trong interceptor.
     * Trả về thông báo thành công dạng Toast, đóng modal và reload danh sách.

### E13-A-UC-003: Khóa / Vô hiệu hóa tài khoản (User Status Switch)
* **Quyền truy cập:** `ADMIN_USER_MANAGE`
* **Luồng chính:**
  1. Tại cột "Trạng thái" hoặc cột "Thao tác" trên bảng người dùng, Admin bấm nút chuyển đổi hoặc icon hành động Khóa/Mở khóa.
  2. Hệ thống hiển thị modal xác nhận (`ep-modal` loại warning): *"Bạn có chắc chắn muốn khóa/mở khóa tài khoản của nhân viên [Họ tên] không?"*.
  3. Sau khi xác nhận, gửi yêu cầu `PATCH /api/v1/users/{id}/status` với body chứa trạng thái mong muốn (`ACTIVE`, `INACTIVE`, `LOCKED`).
  4. Reload lại bảng và hiển thị Toast thành công.

### E13-A-UC-004: Quản lý Phân Quyền RBAC (Role-Permission Matrix UI)
* **Quyền truy cập:** `ADMIN_ROLE_MANAGE`
* **Luồng chính:**
  1. Admin truy cập màn hình `/admin/rbac`.
  2. Bố cục chia làm hai cột (Split Layout):
     * **Cột bên trái:** Danh sách các Vai trò (`Roles` - VD: REQUESTER, MANAGER, DIRECTOR, ACCOUNTANT,...).
     * **Cột bên phải:** Bảng danh sách Permissions được gom nhóm theo Module nghiệp vụ (PR, PO, GR, BUDGET, ORG, ADMIN). Mỗi permission có checkbox để tích chọn/bỏ chọn.
  3. Khi chọn một Role bên trái:
     * Cột bên phải tải danh sách quyền hiện tại của Role đó và tự động đánh dấu tích (checked).
  4. Admin tích/bỏ tích các Permission và bấm nút "Lưu thay đổi".
  5. Hệ thống gửi yêu cầu `PUT /api/v1/roles/{code}/permissions` với danh sách mã quyền mới được cấu hình.
  6. Hiển thị thông báo thành công và tự động xóa cache phân quyền (`role-perm:{roleCode}`) ở phía backend.

### E13-A-UC-005: Sơ đồ Cây Tổ chức Trực quan (Org Chart UI)
* **Quyền truy cập:** `ORG_VIEW` hoặc `ADMIN_USER_MANAGE`
* **Luồng chính:**
  1. Admin truy cập màn hình `/admin/org-chart`.
  2. Hệ thống hiển thị cơ cấu tổ chức dạng cây phân cấp (Tree View) hoặc đồ họa Node-Link:
     * Mỗi nút là một phòng ban (Department) có tên phòng ban, mã phòng ban, và tên Trưởng bộ phận (Manager).
     * Khi click vào một nút, hệ thống mở panel phụ bên phải hiển thị danh sách tất cả Users thuộc phòng ban đó.
  3. Cung cấp các nút hành động nhanh tại mỗi phòng ban:
     * **Nút "+":** Thêm phòng ban con (Mở Modal điền tên và mã phòng ban con, mặc định chọn nút hiện tại làm nút cha).
     * **Nút "Sửa":** Thay đổi tên hoặc chỉ định lại Trưởng bộ phận (Manager).
     * **Nút "Ẩn/Xóa":** Soft delete phòng ban (gửi yêu cầu deactive/delete nếu không còn nhân viên nào trực thuộc).

---

## 5. Technical Deliverables

### Angular Frontend (`frontend/eprocure-web`)
1. **Admin Lazy-Loaded Module:** 
   * `/src/app/features/admin/admin.routes.ts`
   * `/src/app/features/admin/pages/user-management/` (Danh sách + Form User)
   * `/src/app/features/admin/pages/rbac/` (Phân quyền Roles/Permissions)
   * `/src/app/features/admin/pages/org-chart/` (Sơ đồ cây tổ chức)
2. **Components & Components Reuse:**
   * Sử dụng `ep-table` phục vụ phân trang, lọc và sắp xếp.
   * Sử dụng `ep-modal` phục vụ form pop-up thêm/sửa và modal cảnh báo khóa user.
   * Sử dụng `@lucide/angular` cho các biểu tượng quản trị (UserCog, ShieldAlert, GitFork, Lock, Key, etc.).
3. **Admin HTTP Services:**
   * `/src/app/core/services/admin-user.service.ts` (Tương tác API `/users`)
   * `/src/app/core/services/admin-rbac.service.ts` (Tương tác API `/roles` và `/permissions`)
   * `/src/app/core/services/admin-org.service.ts` (Tương tác API `/org/departments`)
4. **Guards & Permissions:**
   * Gắn `PermissionGuard` cho các Route quản trị tương ứng để chặn truy cập trái phép trực tiếp trên trình duyệt.

---

## 6. Kế hoạch Triển khai Lát cắt Dọc (Implementation Checklist)

- [ ] **Bước 1: Khởi tạo Module Admin và Routing**
  * Định nghĩa cấu hình route `/admin` bảo vệ bởi `AuthGuard` và `PermissionGuard` (`ADMIN_USER_MANAGE` hoặc `ADMIN_ROLE_MANAGE`).
- [ ] **Bước 2: Cài đặt HTTP Services cho Admin**
  * Xây dựng các method CRUD user, map role-permission, load phòng ban dạng cây từ backend `iam-service`.
- [ ] **Bước 3: Phát triển Giao diện Quản lý Người dùng (User Directory UI)**
  * Hiển thị bảng user phân trang bằng `ep-table`.
  * Form modal thêm/sửa user tích hợp validate email, phone.
  * Xử lý gửi `Idempotency-Key` và hiển thị Toast kết quả.
- [ ] **Bước 4: Phát triển Giao diện Phân quyền (RBAC Matrix UI)**
  * Layout chia cột chọn Role và bảng tích chọn Permission nhóm theo chức năng.
  * API integration để cập nhật quyền của Role và invalidate Redis cache.
- [ ] **Bước 5: Phát triển Giao diện Sơ đồ Phòng ban (Org Chart UI)**
  * Sử dụng cấu trúc đệ quy hoặc thư viện cây để hiển thị sơ đồ phân cấp phòng ban từ dữ liệu parent/child.
  * Form modal thêm phòng ban con và chỉ định Manager.
- [ ] **Bước 6: Tích hợp Ngôn ngữ i18n (VI/EN)**
  * Bổ sung nhãn quản trị vào các file JSON ngôn ngữ `vi.json` và `en.json`.
