package com.eprocure.approval.domain.model;

import com.eprocure.approval.application.service.ResolvedApprovalChainView;
import com.eprocure.approval.domain.model.vo.Money;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public class ApprovalProcess {
    private final UUID id;
    private final ApprovalEntityType entityType;
    private final UUID entityId;
    private final String entityNumber;
    private final String entityTitle;
    private final UUID requesterId;
    private final UUID requesterDepartmentId;
    private final Money totalAmount;
    private final PurchaseRequestPriority priority;
    private final String camundaProcessInstanceId;
    private ApprovalProcessStatus status;
    private int currentStepIndex;
    private final Map<String, Object> entitySnapshot;
    private final Instant startedAt;
    private Instant completedAt;
    private final List<ApprovalStep> steps;

    private ApprovalProcess(
            UUID id,
            ApprovalEntityType entityType,
            UUID entityId,
            String entityNumber,
            String entityTitle,
            UUID requesterId,
            UUID requesterDepartmentId,
            Money totalAmount,
            PurchaseRequestPriority priority,
            String camundaProcessInstanceId,
            ApprovalProcessStatus status,
            int currentStepIndex,
            Map<String, Object> entitySnapshot,
            Instant startedAt,
            Instant completedAt,
            List<ApprovalStep> steps) {
        this.id = Objects.requireNonNull(id, "id must not be null");
        this.entityType = Objects.requireNonNull(entityType, "entityType must not be null");
        this.entityId = Objects.requireNonNull(entityId, "entityId must not be null");
        this.entityNumber = requireText(entityNumber, "entityNumber");
        this.entityTitle = requireText(entityTitle, "entityTitle");
        this.requesterId = Objects.requireNonNull(requesterId, "requesterId must not be null");
        this.requesterDepartmentId = Objects.requireNonNull(requesterDepartmentId, "requesterDepartmentId must not be null");
        this.totalAmount = Objects.requireNonNull(totalAmount, "totalAmount must not be null");
        this.priority = Objects.requireNonNull(priority, "priority must not be null");
        this.camundaProcessInstanceId = requireText(camundaProcessInstanceId, "camundaProcessInstanceId");
        this.status = Objects.requireNonNull(status, "status must not be null");
        if (currentStepIndex < 1) {
            throw new IllegalArgumentException("currentStepIndex must be positive");
        }
        this.currentStepIndex = currentStepIndex;
        this.entitySnapshot = entitySnapshot == null ? Map.of() : Map.copyOf(entitySnapshot);
        this.startedAt = Objects.requireNonNull(startedAt, "startedAt must not be null");
        this.completedAt = completedAt;
        if (steps == null || steps.isEmpty()) {
            throw new IllegalArgumentException("Approval process must have at least one step");
        }
        this.steps = new ArrayList<>(steps);
    }

    public static ApprovalProcess createForPurchaseRequest(
            UUID purchaseRequestId,
            String prNumber,
            String title,
            UUID requesterId,
            UUID requesterDepartmentId,
            Money totalAmount,
            PurchaseRequestPriority priority,
            String camundaProcessInstanceId,
            Map<String, Object> entitySnapshot,
            ResolvedApprovalChainView chain,
            Instant startedAt) {
        UUID processId = UUID.randomUUID();
        List<ApprovalStep> steps = chain.steps().stream()
                .map(step -> ApprovalStep.pending(
                        processId,
                        step.sourceStepIndex(),
                        step.stepType(),
                        step.approverRole(),
                        step.approver().id(),
                        step.slaDeadline(),
                        startedAt))
                .toList();
        return new ApprovalProcess(
                processId,
                ApprovalEntityType.PURCHASE_REQUEST,
                purchaseRequestId,
                prNumber,
                title,
                requesterId,
                requesterDepartmentId,
                totalAmount,
                priority,
                camundaProcessInstanceId,
                ApprovalProcessStatus.RUNNING,
                1,
                entitySnapshot,
                startedAt,
                null,
                steps);
    }

    public static ApprovalProcess restore(
            UUID id,
            ApprovalEntityType entityType,
            UUID entityId,
            String entityNumber,
            String entityTitle,
            UUID requesterId,
            UUID requesterDepartmentId,
            Money totalAmount,
            PurchaseRequestPriority priority,
            String camundaProcessInstanceId,
            ApprovalProcessStatus status,
            int currentStepIndex,
            Map<String, Object> entitySnapshot,
            Instant startedAt,
            Instant completedAt,
            List<ApprovalStep> steps) {
        return new ApprovalProcess(
                id,
                entityType,
                entityId,
                entityNumber,
                entityTitle,
                requesterId,
                requesterDepartmentId,
                totalAmount,
                priority,
                camundaProcessInstanceId,
                status,
                currentStepIndex,
                entitySnapshot,
                startedAt,
                completedAt,
                steps);
    }

    public ApprovalActionResult approve(UUID taskId, UUID actorId, String comment, Instant actedAt) {
        ApprovalStep step = activeStep(taskId, actorId);
        step.approve(comment, actedAt);
        if (hasPendingStepAtCurrentIndex()) {
            return ApprovalActionResult.inProgress(List.of());
        }
        Optional<Integer> nextIndex = nextStepIndex();
        if (nextIndex.isPresent()) {
            currentStepIndex = nextIndex.orElseThrow();
            return ApprovalActionResult.inProgress(stepsAt(currentStepIndex));
        }
        status = ApprovalProcessStatus.COMPLETED;
        completedAt = Objects.requireNonNull(actedAt, "actedAt must not be null");
        return ApprovalActionResult.done();
    }

    public ApprovalActionResult reject(UUID taskId, UUID actorId, String comment, Instant actedAt) {
        ApprovalStep step = activeStep(taskId, actorId);
        step.reject(comment, actedAt);
        skipOtherCurrentPendingSteps(actedAt);
        status = ApprovalProcessStatus.CANCELLED;
        completedAt = Objects.requireNonNull(actedAt, "actedAt must not be null");
        return ApprovalActionResult.done();
    }

    public ApprovalActionResult requestChanges(UUID taskId, UUID actorId, String comment, Instant actedAt) {
        ApprovalStep step = activeStep(taskId, actorId);
        step.requestChanges(comment, actedAt);
        skipOtherCurrentPendingSteps(actedAt);
        status = ApprovalProcessStatus.CANCELLED;
        completedAt = Objects.requireNonNull(actedAt, "actedAt must not be null");
        return ApprovalActionResult.done();
    }

    public ApprovalActionResult forward(UUID taskId, UUID actorId, UUID forwardToUserId, String reason, Instant actedAt) {
        if (requesterId.equals(forwardToUserId)) {
            throw new IllegalArgumentException("approval task cannot be forwarded to requester");
        }
        ApprovalStep step = activeStep(taskId, actorId);
        step.forward(reason, actedAt);
        ApprovalStep forwardedStep = ApprovalStep.pending(
                id,
                step.getStepIndex(),
                step.getStepType(),
                step.getApproverRole(),
                Objects.requireNonNull(forwardToUserId, "forwardToUserId must not be null"),
                step.getSlaDeadline(),
                actedAt);
        steps.add(forwardedStep);
        return ApprovalActionResult.inProgress(List.of(forwardedStep));
    }

    public Optional<ApprovalStep> findStep(UUID taskId) {
        return steps.stream()
                .filter(step -> step.getId().equals(taskId))
                .findFirst();
    }

    public UUID getId() {
        return id;
    }

    public ApprovalEntityType getEntityType() {
        return entityType;
    }

    public UUID getEntityId() {
        return entityId;
    }

    public String getEntityNumber() {
        return entityNumber;
    }

    public String getEntityTitle() {
        return entityTitle;
    }

    public UUID getRequesterId() {
        return requesterId;
    }

    public UUID getRequesterDepartmentId() {
        return requesterDepartmentId;
    }

    public Money getTotalAmount() {
        return totalAmount;
    }

    public PurchaseRequestPriority getPriority() {
        return priority;
    }

    public String getCamundaProcessInstanceId() {
        return camundaProcessInstanceId;
    }

    public ApprovalProcessStatus getStatus() {
        return status;
    }

    public int getCurrentStepIndex() {
        return currentStepIndex;
    }

    public Map<String, Object> getEntitySnapshot() {
        return entitySnapshot;
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public Optional<Instant> getCompletedAt() {
        return Optional.ofNullable(completedAt);
    }

    public List<ApprovalStep> getSteps() {
        return Collections.unmodifiableList(steps);
    }

    private ApprovalStep activeStep(UUID taskId, UUID actorId) {
        if (status != ApprovalProcessStatus.RUNNING) {
            throw new IllegalStateException("approval process is not running");
        }
        ApprovalStep step = findStep(taskId)
                .orElseThrow(() -> new IllegalArgumentException("approval step not found"));
        if (step.getStepIndex() != currentStepIndex || step.getStatus() != ApprovalStepStatus.PENDING) {
            throw new IllegalStateException("approval step has already been processed");
        }
        if (!step.isAssignedTo(Objects.requireNonNull(actorId, "actorId must not be null"))) {
            throw new SecurityException("actor is not assigned to approval step");
        }
        return step;
    }

    private boolean hasPendingStepAtCurrentIndex() {
        return steps.stream()
                .anyMatch(step -> step.getStepIndex() == currentStepIndex && step.getStatus() == ApprovalStepStatus.PENDING);
    }

    private Optional<Integer> nextStepIndex() {
        return steps.stream()
                .map(ApprovalStep::getStepIndex)
                .filter(index -> index > currentStepIndex)
                .min(Comparator.naturalOrder());
    }

    private List<ApprovalStep> stepsAt(int stepIndex) {
        return steps.stream()
                .filter(step -> step.getStepIndex() == stepIndex && step.getStatus() == ApprovalStepStatus.PENDING)
                .toList();
    }

    private void skipOtherCurrentPendingSteps(Instant actedAt) {
        steps.stream()
                .filter(step -> step.getStepIndex() == currentStepIndex)
                .filter(step -> step.getStatus() == ApprovalStepStatus.PENDING)
                .forEach(step -> step.skip(actedAt));
    }

    public record ApprovalActionResult(boolean completed, List<ApprovalStep> assignedSteps) {
        public static ApprovalActionResult done() {
            return new ApprovalActionResult(true, List.of());
        }

        public static ApprovalActionResult inProgress(List<ApprovalStep> assignedSteps) {
            return new ApprovalActionResult(false, List.copyOf(assignedSteps));
        }
    }

    private static String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value.trim();
    }
}
