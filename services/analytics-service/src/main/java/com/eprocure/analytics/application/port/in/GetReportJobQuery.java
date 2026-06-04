package com.eprocure.analytics.application.port.in;

import java.util.Objects;
import java.util.UUID;

public record GetReportJobQuery(
        UUID actorId,
        UUID jobId) {

    public GetReportJobQuery {
        actorId = Objects.requireNonNull(actorId, "actorId must not be null");
        jobId = Objects.requireNonNull(jobId, "jobId must not be null");
    }
}
