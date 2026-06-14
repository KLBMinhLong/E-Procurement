package com.eprocure.approval.application.port.in;

import com.eprocure.approval.domain.model.ApprovalStepType;

public record ApprovalRuleStepCommand(
        int stepIndex,
        String requiredPermission,
        ApprovalStepType stepType,
        int slaHours,
        boolean required) {
}
