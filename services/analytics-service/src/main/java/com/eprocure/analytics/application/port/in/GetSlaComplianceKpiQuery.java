package com.eprocure.analytics.application.port.in;

import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

public record GetSlaComplianceKpiQuery(
        UUID actorId,
        LocalDate fromDate,
        LocalDate toDate) {

    public GetSlaComplianceKpiQuery {
        actorId = Objects.requireNonNull(actorId, "actorId must not be null");
        fromDate = Objects.requireNonNull(fromDate, "fromDate must not be null");
        toDate = Objects.requireNonNull(toDate, "toDate must not be null");
    }
}
