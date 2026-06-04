package com.eprocure.analytics.presentation.response;

import java.time.Instant;

public record SlaWarningResponse(
        String taskId,
        String prNumber,
        Instant slaDeadline,
        boolean isOverdue) {
}
