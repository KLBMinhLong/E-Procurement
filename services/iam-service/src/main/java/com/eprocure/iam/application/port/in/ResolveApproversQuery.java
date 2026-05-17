package com.eprocure.iam.application.port.in;

import java.util.UUID;

public record ResolveApproversQuery(String roleCode, UUID departmentId, UUID requesterId) {
    public ResolveApproversQuery {
        if (roleCode != null) {
            roleCode = roleCode.trim().toUpperCase();
        }
    }
}
