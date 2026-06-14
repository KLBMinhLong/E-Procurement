package com.eprocure.approval.application.service;

import com.eprocure.approval.application.port.in.ApprovalRuleConditionCommand;
import com.eprocure.approval.application.port.in.ApprovalRuleStepCommand;
import com.eprocure.approval.common.exception.BusinessException;
import com.eprocure.approval.common.exception.ErrorCode;
import com.eprocure.approval.domain.model.ApprovalCondition;
import com.eprocure.approval.domain.model.ApprovalStepTemplate;
import com.eprocure.approval.domain.model.PurchaseRequestPriority;
import com.eprocure.approval.domain.model.vo.Money;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

public class ApprovalRuleCommandAssembler {
    private ApprovalRuleCommandAssembler() {
    }

    public static ApprovalCondition toCondition(ApprovalRuleConditionCommand command) {
        if (command == null) {
            return ApprovalCondition.of(null, null, Set.of(), Set.of(), Set.of());
        }
        return ApprovalCondition.of(
                toMoney(command.minValue()),
                toMoney(command.maxValue()),
                command.categories().stream()
                        .filter(value -> value != null && !value.isBlank())
                        .collect(Collectors.toUnmodifiableSet()),
                Set.copyOf(command.departmentIds()),
                command.priorities().stream()
                        .filter(value -> value != null && !value.isBlank())
                        .map(String::trim)
                        .map(String::toUpperCase)
                        .map(PurchaseRequestPriority::valueOf)
                        .collect(Collectors.toUnmodifiableSet()));
    }

    public static List<ApprovalStepTemplate> toStepTemplates(List<ApprovalRuleStepCommand> steps) {
        if (steps == null || steps.isEmpty()) {
            throw new BusinessException(ErrorCode.APR_007);
        }
        return steps.stream()
                .map(step -> new ApprovalStepTemplate(
                        step.stepIndex(),
                        step.requiredPermission(),
                        step.stepType(),
                        step.slaHours(),
                        step.required()))
                .toList();
    }

    private static Money toMoney(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Money.vnd(value.trim());
        } catch (RuntimeException exception) {
            throw new BusinessException(ErrorCode.APR_007);
        }
    }
}
