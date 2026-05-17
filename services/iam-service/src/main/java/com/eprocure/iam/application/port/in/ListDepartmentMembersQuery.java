package com.eprocure.iam.application.port.in;

import java.util.UUID;

public record ListDepartmentMembersQuery(UUID departmentId, int page, int size) {
    public ListDepartmentMembersQuery {
        page = Math.max(page, 1);
        size = Math.max(1, Math.min(size, 100));
    }

    public int offset() {
        return (page - 1) * size;
    }
}
