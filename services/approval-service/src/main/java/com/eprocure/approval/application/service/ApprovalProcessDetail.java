package com.eprocure.approval.application.service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ApprovalProcessDetail(
        UUID id,
        String entityType,
        UUID entityId,
        String entityNumber,
        String status,
        int currentStepIndex,
        List<StepDetail> steps,
        Instant startedAt,
        Instant completedAt) {

    public record StepDetail(
            int stepIndex,
            String requiredPermission,
            Approver approver,
            UUID delegateId,
            String status,
            String action,
            String comment,
            Instant slaDeadline,
            Instant assignedAt,
            Instant actedAt,
            boolean isEscalated) {
    }

    public record Approver(UUID id, String fullName) {
    }
}
