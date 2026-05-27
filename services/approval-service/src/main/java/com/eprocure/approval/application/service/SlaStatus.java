package com.eprocure.approval.application.service;

import java.time.Instant;

public record SlaStatus(
        Instant deadlineAt,
        Double remainingHours,
        boolean isOverdue,
        boolean isWarning) {

    public static SlaStatus calculate(Instant deadlineAt, Instant now) {
        boolean isOverdue = now.isAfter(deadlineAt);
        Double remainingHours = null;
        boolean isWarning = false;
        if (!isOverdue) {
            long remainingMillis = deadlineAt.toEpochMilli() - now.toEpochMilli();
            remainingHours = (double) remainingMillis / (1000.0 * 60.0 * 60.0);
            isWarning = remainingHours < 4.0;
        }
        return new SlaStatus(deadlineAt, remainingHours, isOverdue, isWarning);
    }
}
