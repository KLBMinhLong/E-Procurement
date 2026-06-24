package com.eprocure.admin.application.port.in;

import java.util.Objects;
import java.util.UUID;

public record GetAuditExportJobQuery(
        UUID actorId,
        UUID jobId) {

    public GetAuditExportJobQuery {
        actorId = Objects.requireNonNull(actorId, "actorId must not be null");
        jobId = Objects.requireNonNull(jobId, "jobId must not be null");
    }
}
