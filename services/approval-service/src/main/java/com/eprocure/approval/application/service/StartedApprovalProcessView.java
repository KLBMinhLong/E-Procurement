package com.eprocure.approval.application.service;

import com.eprocure.approval.domain.model.ApprovalProcessStatus;
import com.eprocure.approval.domain.model.ApprovalStepStatus;
import com.eprocure.approval.domain.model.ApprovalStepType;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record StartedApprovalProcessView(
        UUID processId,
        UUID purchaseRequestId,
        String prNumber,
        ApprovalProcessStatus status,
        int currentStepIndex,
        String camundaProcessInstanceId,
        String primaryRuleName,
        Instant startedAt,
        List<StartedApprovalStepView> steps) {

    public StartedApprovalProcessView {
        steps = List.copyOf(steps);
    }

    public record StartedApprovalStepView(
            UUID stepId,
            int sequence,
            ApprovalStepType stepType,
            String approverRole,
            UUID approverId,
            ApprovalStepStatus status,
            Instant assignedAt,
            Instant slaDeadline) {
    }
}
