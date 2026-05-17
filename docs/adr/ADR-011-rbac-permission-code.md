## ADR-011 — RBAC với Permission Code, không hardcode Role

**Status:** Accepted  
**Date:** 2025-01  

### Quyết định

Không dùng `hasRole('MANAGER')` trong code. Thay bằng permission code:

```java
// ❌ SAI
@PreAuthorize("hasRole('MANAGER')")

// ✅ ĐÚNG  
@PreAuthorize("hasAuthority('PR_APPROVE_L1')")
```

**Permission Code naming convention:**
```
{RESOURCE}_{ACTION}[_{SCOPE}]

Ví dụ:
PR_CREATE          — Tạo Purchase Request
PR_APPROVE_L1      — Duyệt PR cấp 1 (Manager)
PR_APPROVE_L2      — Duyệt PR cấp 2 (Director)
PR_APPROVE_L3      — Duyệt PR cấp 3 (C-Level)
PO_CREATE          — Tạo Purchase Order
PO_VIEW_ALL        — Xem tất cả PO (không chỉ của mình)
BUDGET_OVERRIDE    — Duyệt vượt ngân sách
ADMIN_USER_MANAGE  — Quản lý người dùng
SYSTEM_CONFIG      — Truy cập trang cấu hình hệ thống
```

**Role-Permission mapping:** Lưu trong DB, cache Redis theo key `role-perm:{roleCode}`.