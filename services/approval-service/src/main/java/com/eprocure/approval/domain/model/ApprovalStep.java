package com.eprocure.approval.domain.model;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public class ApprovalStep {
    private final UUID id;
    private final UUID processId;
    private final int stepIndex;
    private final ApprovalStepType stepType;
    private final String approverRole;
    private final UUID approverId;
    private final UUID delegateId;
    private final ApprovalStepStatus status;
    private final Instant slaDeadline;
    private final Instant assignedAt;
    private final String camundaTaskId;

    private ApprovalStep(
            UUID id,
            UUID processId,
            int stepIndex,
            ApprovalStepType stepType,
            String approverRole,
            UUID approverId,
            UUID delegateId,
            ApprovalStepStatus status,
            Instant slaDeadline,
            Instant assignedAt,
            String camundaTaskId) {
        this.id = Objects.requireNonNull(id, "id must not be null");
        this.processId = Objects.requireNonNull(processId, "processId must not be null");
        if (stepIndex < 1) {
            throw new IllegalArgumentException("stepIndex must be positive");
        }
        this.stepIndex = stepIndex;
        this.stepType = Objects.requireNonNull(stepType, "stepType must not be null");
        if (approverRole == null || approverRole.isBlank()) {
            throw new IllegalArgumentException("approverRole must not be blank");
        }
        this.approverRole = approverRole.trim().toUpperCase();
        this.approverId = Objects.requireNonNull(approverId, "approverId must not be null");
        this.delegateId = delegateId;
        this.status = Objects.requireNonNull(status, "status must not be null");
        this.slaDeadline = Objects.requireNonNull(slaDeadline, "slaDeadline must not be null");
        this.assignedAt = Objects.requireNonNull(assignedAt, "assignedAt must not be null");
        this.camundaTaskId = camundaTaskId == null || camundaTaskId.isBlank() ? null : camundaTaskId.trim();
    }

    public static ApprovalStep pending(
            UUID processId,
            int stepIndex,
            ApprovalStepType stepType,
            String approverRole,
            UUID approverId,
            Instant slaDeadline,
            Instant assignedAt) {
        return new ApprovalStep(
                UUID.randomUUID(),
                processId,
                stepIndex,
                stepType,
                approverRole,
                approverId,
                null,
                ApprovalStepStatus.PENDING,
                slaDeadline,
                assignedAt,
                null);
    }

    public UUID getId() {
        return id;
    }

    public UUID getProcessId() {
        return processId;
    }

    public int getStepIndex() {
        return stepIndex;
    }

    public ApprovalStepType getStepType() {
        return stepType;
    }

    public String getApproverRole() {
        return approverRole;
    }

    public UUID getApproverId() {
        return approverId;
    }

    public Optional<UUID> getDelegateId() {
        return Optional.ofNullable(delegateId);
    }

    public ApprovalStepStatus getStatus() {
        return status;
    }

    public Instant getSlaDeadline() {
        return slaDeadline;
    }

    public Instant getAssignedAt() {
        return assignedAt;
    }

    public Optional<String> getCamundaTaskId() {
        return Optional.ofNullable(camundaTaskId);
    }
}
