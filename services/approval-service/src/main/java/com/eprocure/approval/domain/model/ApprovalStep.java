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
    private final String requiredPermission;
    private UUID approverId;
    private UUID delegateId;
    private ApprovalStepStatus status;
    private ApprovalAction action;
    private String comment;
    private final Instant slaDeadline;
    private final Instant assignedAt;
    private Instant actedAt;
    private boolean escalated;
    private UUID escalatedFrom;
    private String camundaTaskId;

    private ApprovalStep(
            UUID id,
            UUID processId,
            int stepIndex,
            ApprovalStepType stepType,
            String requiredPermission,
            UUID approverId,
            UUID delegateId,
            ApprovalStepStatus status,
            ApprovalAction action,
            String comment,
            Instant slaDeadline,
            Instant assignedAt,
            Instant actedAt,
            boolean escalated,
            UUID escalatedFrom,
            String camundaTaskId) {
        this.id = Objects.requireNonNull(id, "id must not be null");
        this.processId = Objects.requireNonNull(processId, "processId must not be null");
        if (stepIndex < 1) {
            throw new IllegalArgumentException("stepIndex must be positive");
        }
        this.stepIndex = stepIndex;
        this.stepType = Objects.requireNonNull(stepType, "stepType must not be null");
        if (requiredPermission == null || requiredPermission.isBlank()) {
            throw new IllegalArgumentException("requiredPermission must not be blank");
        }
        this.requiredPermission = requiredPermission.trim().toUpperCase();
        this.approverId = Objects.requireNonNull(approverId, "approverId must not be null");
        this.delegateId = delegateId;
        this.status = Objects.requireNonNull(status, "status must not be null");
        this.action = action;
        this.comment = normalizeOptional(comment);
        this.slaDeadline = Objects.requireNonNull(slaDeadline, "slaDeadline must not be null");
        this.assignedAt = Objects.requireNonNull(assignedAt, "assignedAt must not be null");
        this.actedAt = actedAt;
        this.escalated = escalated;
        this.escalatedFrom = escalatedFrom;
        this.camundaTaskId = camundaTaskId == null || camundaTaskId.isBlank() ? null : camundaTaskId.trim();
    }

    public static ApprovalStep pending(
            UUID processId,
            int stepIndex,
            ApprovalStepType stepType,
            String requiredPermission,
            UUID approverId,
            UUID delegateId,
            Instant slaDeadline,
            Instant assignedAt) {
        return new ApprovalStep(
                UUID.randomUUID(),
                processId,
                stepIndex,
                stepType,
                requiredPermission,
                approverId,
                delegateId,
                ApprovalStepStatus.PENDING,
                null,
                null,
                slaDeadline,
                assignedAt,
                null,
                false,
                null,
                null);
    }

    public static ApprovalStep pending(
            UUID processId,
            int stepIndex,
            ApprovalStepType stepType,
            String requiredPermission,
            UUID approverId,
            Instant slaDeadline,
            Instant assignedAt) {
        return pending(processId, stepIndex, stepType, requiredPermission, approverId, null, slaDeadline, assignedAt);
    }

    public static ApprovalStep restore(
            UUID id,
            UUID processId,
            int stepIndex,
            ApprovalStepType stepType,
            String requiredPermission,
            UUID approverId,
            UUID delegateId,
            ApprovalStepStatus status,
            ApprovalAction action,
            String comment,
            Instant slaDeadline,
            Instant assignedAt,
            Instant actedAt,
            boolean escalated,
            UUID escalatedFrom,
            String camundaTaskId) {
        return new ApprovalStep(
                id,
                processId,
                stepIndex,
                stepType,
                requiredPermission,
                approverId,
                delegateId,
                status,
                action,
                comment,
                slaDeadline,
                assignedAt,
                actedAt,
                escalated,
                escalatedFrom,
                camundaTaskId);
    }

    public void approve(String comment, Instant actedAt) {
        complete(ApprovalStepStatus.APPROVED, ApprovalAction.APPROVE, comment, actedAt);
    }

    public void reject(String comment, Instant actedAt) {
        complete(ApprovalStepStatus.REJECTED, ApprovalAction.REJECT, comment, actedAt);
    }

    public void requestChanges(String comment, Instant actedAt) {
        complete(ApprovalStepStatus.REJECTED, ApprovalAction.REQUEST_CHANGES, comment, actedAt);
    }

    public void forward(String reason, Instant actedAt) {
        complete(ApprovalStepStatus.FORWARDED, ApprovalAction.FORWARD, reason, actedAt);
    }

    public void escalateTo(UUID escalationTargetId, Instant escalatedAt) {
        if (status != ApprovalStepStatus.PENDING) {
            throw new IllegalStateException("approval step has already been processed");
        }
        UUID targetId = Objects.requireNonNull(escalationTargetId, "escalationTargetId must not be null");
        Objects.requireNonNull(escalatedAt, "escalatedAt must not be null");
        if (!escalated) {
            escalatedFrom = approverId;
        }
        approverId = targetId;
        delegateId = null;
        escalated = true;
    }

    public void skip(Instant actedAt) {
        if (status == ApprovalStepStatus.PENDING) {
            this.status = ApprovalStepStatus.SKIPPED;
            this.actedAt = Objects.requireNonNull(actedAt, "actedAt must not be null");
        }
    }

    public void assignCamundaTaskId(String camundaTaskId) {
        this.camundaTaskId = camundaTaskId == null || camundaTaskId.isBlank() ? null : camundaTaskId.trim();
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

    public String getRequiredPermission() {
        return requiredPermission;
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

    public Optional<ApprovalAction> getAction() {
        return Optional.ofNullable(action);
    }

    public Optional<String> getComment() {
        return Optional.ofNullable(comment);
    }

    public Instant getSlaDeadline() {
        return slaDeadline;
    }

    public Instant getAssignedAt() {
        return assignedAt;
    }

    public Optional<Instant> getActedAt() {
        return Optional.ofNullable(actedAt);
    }

    public boolean isEscalated() {
        return escalated;
    }

    public Optional<UUID> getEscalatedFrom() {
        return Optional.ofNullable(escalatedFrom);
    }

    public Optional<String> getCamundaTaskId() {
        return Optional.ofNullable(camundaTaskId);
    }

    public boolean isAssignedTo(UUID actorId) {
        return approverId.equals(actorId) || actorId.equals(delegateId);
    }

    private void complete(ApprovalStepStatus status, ApprovalAction action, String comment, Instant actedAt) {
        if (this.status != ApprovalStepStatus.PENDING) {
            throw new IllegalStateException("approval step has already been processed");
        }
        this.status = Objects.requireNonNull(status, "status must not be null");
        this.action = Objects.requireNonNull(action, "action must not be null");
        this.comment = normalizeOptional(comment);
        this.actedAt = Objects.requireNonNull(actedAt, "actedAt must not be null");
    }

    private static String normalizeOptional(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
