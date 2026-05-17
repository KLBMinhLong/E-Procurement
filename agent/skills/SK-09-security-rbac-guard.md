## SK-09 · Security & RBAC Guard

### Trigger
Agent thêm phân quyền cho endpoint hoặc method mới.

### Inputs Required
- Permission codes cần dùng
- Ownership rule (owner/approver)
- Security service method name

### Rules
```
[R1] @PreAuthorize("hasAuthority('PERMISSION_CODE')") — KHÔNG dùng hasRole()
[R2] Permission codes: {ENTITY}_{ACTION} — VD: PR_CREATE, PR_APPROVE_L1, PR_VIEW_ANY
[R3] Ownership check kết hợp: @PreAuthorize + @prSecurityService.isOwner(...)
[R4] Permission data lưu trong DB (role_permissions table), load vào Redis khi start
[R5] Role-Permission Mapping trong Redis: key = perm:{userId}, value = Set<String>
[R6] Controller luôn check permission — không tin FE tự filter
[R7] Phân tách VIEW_OWN vs VIEW_ANY, EDIT_OWN vs EDIT_ANY rõ ràng
```

### Permission Code Registry (PR Service)
```java
package com.eprocure.pr.common.constant;

/**
 * Permission codes cho PR Service.
 * Lưu trong DB: iam.permissions.code
 */
public final class PrPermissions {

    private PrPermissions() {}

    // === Purchase Request ===
    public static final String PR_CREATE          = "PR_CREATE";
    public static final String PR_SUBMIT          = "PR_SUBMIT";
    public static final String PR_VIEW_OWN        = "PR_VIEW_OWN";
    public static final String PR_VIEW_DEPT       = "PR_VIEW_DEPT";
    public static final String PR_VIEW_ANY        = "PR_VIEW_ANY";
    public static final String PR_EDIT_OWN_DRAFT  = "PR_EDIT_OWN_DRAFT";
    public static final String PR_CANCEL_OWN      = "PR_CANCEL_OWN";
    public static final String PR_CANCEL_ANY      = "PR_CANCEL_ANY";

    // === Approval ===
    public static final String PR_APPROVE_L1      = "PR_APPROVE_L1";
    public static final String PR_APPROVE_L2      = "PR_APPROVE_L2";
    public static final String PR_APPROVE_L3      = "PR_APPROVE_L3";
    public static final String PR_APPROVE_FINANCE = "PR_APPROVE_FINANCE";
    public static final String PR_REJECT          = "PR_REJECT";
    public static final String PR_REQUEST_CHANGES = "PR_REQUEST_CHANGES";
    public static final String PR_FORWARD         = "PR_FORWARD";

    // === Admin ===
    public static final String PR_MANAGE_CATALOG  = "PR_MANAGE_CATALOG";
    public static final String PR_VIEW_REPORTS    = "PR_VIEW_REPORTS";
    public static final String PR_EXPORT          = "PR_EXPORT";
}
```

### Security Service (Ownership Check)
```java
@Service
public class PrSecurityService {

    private final PurchaseRequestRepository repository;

    /**
     * Kiểm tra người dùng có phải là chủ sở hữu của PR không.
     * Dùng trong @PreAuthorize expression.
     */
    public boolean isOwner(UUID prId, Authentication auth) {
        UUID userId = extractUserId(auth);
        return repository.findById(prId)
            .map(pr -> pr.getRequesterId().equals(userId))
            .orElse(false);
    }

    /**
     * Kiểm tra người dùng có phải là approver được assign không.
     */
    public boolean isAssignedApprover(UUID approvalTaskId, Authentication auth) {
        UUID userId = extractUserId(auth);
        return approvalTaskRepository.findById(approvalTaskId)
            .map(task -> task.getAssignedTo().equals(userId))
            .orElse(false);
    }
}

// Sử dụng trong Controller:
@PatchMapping("/{id}/cancel")
@PreAuthorize("hasAuthority('PR_CANCEL_ANY') or " +
              "(hasAuthority('PR_CANCEL_OWN') and @prSecurityService.isOwner(#id, authentication))")
public ResponseEntity<?> cancel(@PathVariable UUID id, ...) { ... }
```

### Checklist
```
[ ] @PreAuthorize dùng permission code
[ ] Có phân tách VIEW_OWN/VIEW_ANY
[ ] Ownership check kết hợp với permission
```
