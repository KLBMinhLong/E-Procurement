package com.eprocure.analytics.domain.model.dashboard;

import java.time.Instant;

public record SlaWarning(
        String taskId,
        String prNumber,
        Instant slaDeadline,
        boolean overdue) {

    public SlaWarning {
        taskId = taskId == null ? "" : taskId.trim();
        prNumber = prNumber == null ? "" : prNumber.trim();
    }
}
