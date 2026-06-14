package com.eprocure.iam.application.port.in;

import java.util.UUID;

public record ResolveApproversQuery(String roleCode, String permissionCode, UUID departmentId, UUID requesterId) {
    public ResolveApproversQuery {
        if (roleCode != null) {
            roleCode = roleCode.trim().toUpperCase();
        }
        if (permissionCode != null) {
            permissionCode = permissionCode.trim().toUpperCase();
        }
    }

    public static ResolveApproversQuery byRole(String roleCode, UUID departmentId, UUID requesterId) {
        return new ResolveApproversQuery(roleCode, null, departmentId, requesterId);
    }

    public static ResolveApproversQuery byPermission(String permissionCode, UUID departmentId, UUID requesterId) {
        return new ResolveApproversQuery(null, permissionCode, departmentId, requesterId);
    }
}
