package com.eprocure.approval.application.service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record ApprovalTaskSummary(
        String taskId,
        UUID processId,
        String entityType,
        UUID entityId,
        String entityNumber,
        String entityTitle,
        Requester requester,
        BigDecimal totalAmount,
        String currency,
        String priority,
        int stepIndex,
        String stepType,
        SlaStatus sla,
        boolean isDelegated,
        DelegatedFrom delegatedFrom,
        Instant assignedAt) {

    public record Requester(UUID id, String fullName, String department) {
    }

    public record DelegatedFrom(UUID id, String fullName) {
    }
}
