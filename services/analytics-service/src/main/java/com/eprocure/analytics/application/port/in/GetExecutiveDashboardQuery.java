package com.eprocure.analytics.application.port.in;

import java.util.Objects;
import java.util.UUID;

public record GetExecutiveDashboardQuery(
        UUID actorId,
        int fiscalYear,
        Integer quarter) {

    public GetExecutiveDashboardQuery {
        actorId = Objects.requireNonNull(actorId, "actorId must not be null");
        if (fiscalYear < 2000) {
            throw new IllegalArgumentException("fiscalYear is invalid");
        }
        if (quarter != null && (quarter < 1 || quarter > 4)) {
            throw new IllegalArgumentException("quarter must be 1..4");
        }
    }
}
