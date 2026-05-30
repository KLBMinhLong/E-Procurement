package com.eprocure.finance.application.port.in;

import com.eprocure.finance.domain.model.BudgetStatus;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

public record ListBudgetsQuery(
        UUID actorId,
        UUID actorDepartmentId,
        Set<String> permissions,
        UUID departmentId,
        Integer fiscalYear,
        Integer quarter,
        String glAccountCode,
        BudgetStatus status,
        int page,
        int size,
        String sort) {

    private static final int DEFAULT_PAGE = 1;
    private static final int DEFAULT_SIZE = 20;
    private static final int MAX_SIZE = 100;
    private static final String DEFAULT_SORT = "fiscalYear,desc";

    public ListBudgetsQuery {
        actorId = Objects.requireNonNull(actorId, "actorId must not be null");
        actorDepartmentId = Objects.requireNonNull(actorDepartmentId, "actorDepartmentId must not be null");
        permissions = permissions == null ? Set.of() : Set.copyOf(permissions);
        glAccountCode = normalizeGlAccountCode(glAccountCode);
        page = page < 1 ? DEFAULT_PAGE : page;
        size = size < 1 || size > MAX_SIZE ? DEFAULT_SIZE : size;
        sort = sort == null || sort.isBlank() ? DEFAULT_SORT : sort.trim();
    }

    public boolean hasPermission(String permission) {
        return permissions.contains(permission);
    }

    private static String normalizeGlAccountCode(String value) {
        return value == null || value.isBlank() ? null : value.trim().toUpperCase(Locale.ROOT);
    }
}
