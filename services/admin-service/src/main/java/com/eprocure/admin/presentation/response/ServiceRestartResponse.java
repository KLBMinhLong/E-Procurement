package com.eprocure.admin.presentation.response;

import java.time.Instant;
import java.util.UUID;

public record ServiceRestartResponse(
        UUID actionId,
        String status,
        int estimatedDowntimeSeconds,
        Instant triggeredAt,
        boolean applied) {
}
