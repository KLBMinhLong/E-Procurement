package com.eprocure.approval.domain.repository;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record PendingTaskProjection(
        String camundaTaskId,
        UUID processId,
        String entityType,
        UUID entityId,
        String entityNumber,
        String entityTitle,
        UUID requesterId,
        UUID requesterDepartmentId,
        BigDecimal totalAmount,
        String currency,
        String priority,
        int stepIndex,
        String stepType,
        Instant slaDeadline,
        UUID approverId,
        UUID delegateId,
        Instant assignedAt) {
}
