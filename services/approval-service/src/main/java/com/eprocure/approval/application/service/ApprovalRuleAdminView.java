package com.eprocure.approval.application.service;

import com.eprocure.approval.domain.model.ApprovalRule;
import com.eprocure.approval.domain.model.ApprovalRuleType;
import com.eprocure.approval.domain.model.ApprovalStepTemplate;
import com.eprocure.approval.domain.model.ApprovalStepType;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public record ApprovalRuleAdminView(
        UUID id,
        String ruleName,
        int priority,
        boolean active,
        ApprovalRuleType ruleType,
        ConditionView conditions,
        List<StepTemplateView> steps,
        String description) {

    public ApprovalRuleAdminView {
        steps = List.copyOf(steps);
    }

    public static ApprovalRuleAdminView from(ApprovalRule rule) {
        return new ApprovalRuleAdminView(
                rule.getId(),
                rule.getRuleName(),
                rule.getPriority(),
                rule.isActive(),
                rule.getRuleType(),
                new ConditionView(
                        rule.getCondition().getMinValue() == null ? null : rule.getCondition().getMinValue().amount().toPlainString(),
                        rule.getCondition().getMaxValue() == null ? null : rule.getCondition().getMaxValue().amount().toPlainString(),
                        rule.getCondition().getCategories(),
                        rule.getCondition().getDepartmentIds(),
                        rule.getCondition().getPriorities().stream().map(Enum::name).collect(java.util.stream.Collectors.toUnmodifiableSet())),
                rule.getStepTemplates().stream().map(StepTemplateView::from).toList(),
                rule.getDescription());
    }

    public record ConditionView(
            String minValue,
            String maxValue,
            Set<String> categories,
            Set<UUID> departmentIds,
            Set<String> priorities) {
        public ConditionView {
            categories = categories == null ? Set.of() : Set.copyOf(categories);
            departmentIds = departmentIds == null ? Set.of() : Set.copyOf(departmentIds);
            priorities = priorities == null ? Set.of() : Set.copyOf(priorities);
        }
    }

    public record StepTemplateView(
            int stepIndex,
            String approverRole,
            ApprovalStepType stepType,
            int slaHours,
            boolean required) {
        private static StepTemplateView from(ApprovalStepTemplate step) {
            return new StepTemplateView(
                    step.stepIndex(),
                    step.approverRole(),
                    step.stepType(),
                    step.slaHours(),
                    step.required());
        }
    }
}
