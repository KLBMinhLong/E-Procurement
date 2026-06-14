package com.eprocure.approval.application.port.out;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

public interface OrgApproverPort {
    List<ApproverCandidate> resolveApprovers(ResolveApproverQuery query);

    record ResolveApproverQuery(
            String requiredPermission,
            UUID departmentId,
            UUID requesterId) {
        public ResolveApproverQuery {
            if (requiredPermission == null || requiredPermission.isBlank()) {
                throw new IllegalArgumentException("requiredPermission must not be blank");
            }
            requiredPermission = requiredPermission.trim().toUpperCase();
            departmentId = Objects.requireNonNull(departmentId, "departmentId must not be null");
            requesterId = Objects.requireNonNull(requesterId, "requesterId must not be null");
        }
    }

    record ApproverCandidate(
            UUID id,
            String employeeCode,
            String username,
            String fullName,
            String email,
            UUID departmentId) {
        public ApproverCandidate {
            id = Objects.requireNonNull(id, "id must not be null");
        }
    }
}
