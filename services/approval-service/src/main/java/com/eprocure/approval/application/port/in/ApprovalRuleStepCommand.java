package com.eprocure.approval.application.port.in;

import com.eprocure.approval.domain.model.ApprovalStepType;

public record ApprovalRuleStepCommand(
        int stepIndex,
        String approverRole,
        ApprovalStepType stepType,
        int slaHours,
        boolean required) {
}
