package com.eprocure.analytics.domain.model.projection;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record ApprovalStepAssignedProjection(
        AnalyticsEventMetadata eventMetadata,
        UUID processId,
        UUID approvalStepId,
        UUID purchaseRequestId,
        String prNumber,
        String priority,
        int stepIndex,
        String stepType,
        String approverRole,
        UUID approverId,
        Instant assignedAt,
        Instant slaDeadline) {

    public ApprovalStepAssignedProjection {
        eventMetadata = Objects.requireNonNull(eventMetadata, "eventMetadata must not be null");
        processId = Objects.requireNonNull(processId, "processId must not be null");
        approvalStepId = Objects.requireNonNull(approvalStepId, "approvalStepId must not be null");
        purchaseRequestId = Objects.requireNonNull(purchaseRequestId, "purchaseRequestId must not be null");
        prNumber = requireText(prNumber, "prNumber");
        priority = requireText(priority, "priority");
        if (stepIndex < 1) {
            throw new IllegalArgumentException("stepIndex must be positive");
        }
        stepType = requireText(stepType, "stepType");
        approverRole = requireText(approverRole, "approverRole");
        approverId = Objects.requireNonNull(approverId, "approverId must not be null");
        assignedAt = Objects.requireNonNull(assignedAt, "assignedAt must not be null");
        slaDeadline = Objects.requireNonNull(slaDeadline, "slaDeadline must not be null");
    }

    private static String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value.trim();
    }
}
