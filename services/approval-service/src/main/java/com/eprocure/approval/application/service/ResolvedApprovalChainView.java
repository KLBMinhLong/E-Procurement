package com.eprocure.approval.application.service;

import com.eprocure.approval.domain.model.ApprovalStepType;
import java.util.List;
import java.util.UUID;

public record ResolvedApprovalChainView(
        UUID purchaseRequestId,
        UUID primaryRuleId,
        String primaryRuleName,
        List<String> appliedRuleNames,
        List<ResolvedApprovalStepView> steps) {

    public ResolvedApprovalChainView {
        appliedRuleNames = List.copyOf(appliedRuleNames);
        steps = List.copyOf(steps);
    }

    public record ResolvedApprovalStepView(
            int sequence,
            int sourceStepIndex,
            String sourceRuleName,
            String approverRole,
            ApprovalStepType stepType,
            int slaHours,
            boolean required,
            ApproverView approver) {
    }

    public record ApproverView(
            UUID id,
            String employeeCode,
            String username,
            String fullName,
            String email,
            UUID departmentId) {
    }
}
