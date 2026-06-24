package com.eprocure.admin.application.service;

import com.eprocure.admin.domain.model.AdminConfigActionStatus;
import java.time.Instant;
import java.util.UUID;

public record ServiceRestartResult(
        UUID actionId,
        AdminConfigActionStatus status,
        int estimatedDowntimeSeconds,
        Instant triggeredAt,
        boolean applied,
        boolean replayed) {
}
