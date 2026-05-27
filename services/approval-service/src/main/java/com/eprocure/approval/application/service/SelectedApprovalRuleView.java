package com.eprocure.approval.application.service;

import com.eprocure.approval.domain.model.ApprovalStepType;
import java.util.List;
import java.util.UUID;

public record SelectedApprovalRuleView(
        UUID primaryRuleId,
        String primaryRuleName,
        List<String> appliedRuleNames,
        List<StepView> steps) {

    public SelectedApprovalRuleView {
        appliedRuleNames = List.copyOf(appliedRuleNames);
        steps = List.copyOf(steps);
    }

    public record StepView(
            int sequence,
            int sourceStepIndex,
            String sourceRuleName,
            String approverRole,
            ApprovalStepType stepType,
            int slaHours,
            boolean required) {
    }
}
