package com.eprocure.approval.domain.model;

import com.eprocure.approval.application.service.ResolvedApprovalChainView;
import com.eprocure.approval.domain.model.vo.Money;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
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
    private final ApprovalProcessStatus status;
    private final int currentStepIndex;
    private final Map<String, Object> entitySnapshot;
    private final Instant startedAt;
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
        if (steps == null || steps.isEmpty()) {
            throw new IllegalArgumentException("Approval process must have at least one step");
        }
        this.steps = List.copyOf(steps);
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
                        step.sequence(),
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
                steps);
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

    public List<ApprovalStep> getSteps() {
        return steps;
    }

    private static String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value.trim();
    }
}
