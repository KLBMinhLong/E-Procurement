package com.eprocure.approval.application.service;

import com.eprocure.approval.domain.model.ApprovalAction;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ApprovalTaskActionView(
        UUID processId,
        UUID taskId,
        ApprovalAction action,
        int currentStepIndex,
        boolean completed,
        List<AssignedStepView> nextSteps) {

    public record AssignedStepView(
            UUID stepId,
            int stepIndex,
            String requiredPermission,
            UUID approverId,
            Instant slaDeadline) {
    }
}
