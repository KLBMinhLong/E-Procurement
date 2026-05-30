package com.eprocure.finance.application.port.in;

import java.util.Objects;
import java.util.Set;
import java.util.UUID;

public record GetBudgetDashboardQuery(
        UUID actorId,
        UUID actorDepartmentId,
        Set<String> permissions,
        UUID budgetId) {

    public GetBudgetDashboardQuery {
        actorId = Objects.requireNonNull(actorId, "actorId must not be null");
        actorDepartmentId = Objects.requireNonNull(actorDepartmentId, "actorDepartmentId must not be null");
        permissions = permissions == null ? Set.of() : Set.copyOf(permissions);
        budgetId = Objects.requireNonNull(budgetId, "budgetId must not be null");
    }

    public boolean hasPermission(String permission) {
        return permissions.contains(permission);
    }
}
