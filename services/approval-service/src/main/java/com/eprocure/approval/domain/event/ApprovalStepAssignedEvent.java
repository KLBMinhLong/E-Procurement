package com.eprocure.approval.domain.event;

import com.eprocure.approval.domain.model.ApprovalStepType;
import com.eprocure.approval.domain.model.PurchaseRequestPriority;
import java.time.Instant;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

public record ApprovalStepAssignedEvent(
        String eventId,
        String eventType,
        String version,
        String source,
        Instant timestamp,
        UUID traceId,
        Payload payload) {

    public static ApprovalStepAssignedEvent create(UUID traceId, Instant timestamp, Payload payload) {
        return new ApprovalStepAssignedEvent(
                UUID.randomUUID().toString(),
                "APPROVAL_STEP_ASSIGNED",
                "1.0",
                "approval-service",
                timestamp,
                traceId == null ? UUID.randomUUID() : traceId,
                payload);
    }

    public ApprovalStepAssignedEvent {
        eventId = requireText(eventId, "eventId");
        eventType = requireText(eventType, "eventType");
        version = requireText(version, "version");
        source = requireText(source, "source");
        timestamp = Objects.requireNonNull(timestamp, "timestamp must not be null");
        traceId = Objects.requireNonNull(traceId, "traceId must not be null");
        payload = Objects.requireNonNull(payload, "payload must not be null");
    }

    public record Payload(
            UUID processId,
            UUID approvalStepId,
            UUID purchaseRequestId,
            String prNumber,
            PurchaseRequestPriority priority,
            int stepIndex,
            ApprovalStepType stepType,
            String approverRole,
            UUID approverId,
            Instant assignedAt,
            Instant slaDeadline) {
        public Payload {
            processId = Objects.requireNonNull(processId, "processId must not be null");
            approvalStepId = Objects.requireNonNull(approvalStepId, "approvalStepId must not be null");
            purchaseRequestId = Objects.requireNonNull(purchaseRequestId, "purchaseRequestId must not be null");
            prNumber = requireText(prNumber, "prNumber");
            priority = Objects.requireNonNull(priority, "priority must not be null");
            if (stepIndex < 1) {
                throw new IllegalArgumentException("stepIndex must be positive");
            }
            stepType = Objects.requireNonNull(stepType, "stepType must not be null");
            approverRole = requireText(approverRole, "approverRole").toUpperCase(Locale.ROOT);
            approverId = Objects.requireNonNull(approverId, "approverId must not be null");
            assignedAt = Objects.requireNonNull(assignedAt, "assignedAt must not be null");
            slaDeadline = Objects.requireNonNull(slaDeadline, "slaDeadline must not be null");
        }
    }

    private static String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value.trim();
    }
}
