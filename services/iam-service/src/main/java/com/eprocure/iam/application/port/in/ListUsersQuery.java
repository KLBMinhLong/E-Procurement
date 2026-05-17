package com.eprocure.iam.application.port.in;

import com.eprocure.iam.domain.model.SortDirection;
import com.eprocure.iam.domain.model.UserSort;
import com.eprocure.iam.domain.model.UserStatus;
import java.util.UUID;

public record ListUsersQuery(
        int page,
        int size,
        String sort,
        UserStatus status,
        UUID departmentId,
        String role,
        String query) {

    public ListUsersQuery {
        page = Math.max(page, 1);
        size = Math.max(1, Math.min(size, 100));
        sort = normalizeSort(sort);
        role = normalize(role);
        query = normalize(query);
    }

    public int offset() {
        return (page - 1) * size;
    }

    public UserSort sortField() {
        return UserSort.from(sort.split(",", 2)[0]);
    }

    public SortDirection sortDirection() {
        String[] parts = sort.split(",", 2);
        return parts.length == 2 ? SortDirection.from(parts[1]) : SortDirection.DESC;
    }

    private static String normalizeSort(String value) {
        return value == null || value.isBlank() ? "createdAt,desc" : value.trim();
    }

    private static String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
