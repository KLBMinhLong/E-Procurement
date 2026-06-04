package com.eprocure.analytics.application.port.in;

import java.util.Objects;
import java.util.UUID;

public record GetRoleDashboardQuery(
        UUID actorId,
        UUID departmentId) {

    public GetRoleDashboardQuery {
        actorId = Objects.requireNonNull(actorId, "actorId must not be null");
    }
}
