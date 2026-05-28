package com.eprocure.approval.domain.model;

import com.eprocure.approval.domain.model.vo.Money;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

public class ApprovalRule {
    public static final Comparator<ApprovalRule> PRIORITY_ORDER =
            Comparator.comparingInt(ApprovalRule::getPriority).reversed()
                    .thenComparing(ApprovalRule::getRuleName);

    private final UUID id;
    private final String ruleName;
    private final int priority;
    private final boolean active;
    private final ApprovalRuleType ruleType;
    private final ApprovalCondition condition;
    private final List<ApprovalStepTemplate> stepTemplates;
    private final String description;

    private ApprovalRule(
            UUID id,
            String ruleName,
            int priority,
            boolean active,
            ApprovalRuleType ruleType,
            ApprovalCondition condition,
            List<ApprovalStepTemplate> stepTemplates,
            String description) {
        this.id = Objects.requireNonNull(id, "id must not be null");
        if (ruleName == null || ruleName.isBlank()) {
            throw new IllegalArgumentException("ruleName must not be blank");
        }
        this.ruleName = ruleName.trim().toUpperCase();
        this.priority = priority;
        this.active = active;
        this.ruleType = Objects.requireNonNull(ruleType, "ruleType must not be null");
        this.condition = Objects.requireNonNull(condition, "condition must not be null");
        if (stepTemplates == null || stepTemplates.isEmpty()) {
            throw new IllegalArgumentException("Approval rule must have at least one step template");
        }
        this.stepTemplates = stepTemplates.stream()
                .sorted(Comparator.comparingInt(ApprovalStepTemplate::stepIndex))
                .toList();
        this.description = description;
    }

    public static ApprovalRule restore(
            UUID id,
            String ruleName,
            int priority,
            boolean active,
            ApprovalRuleType ruleType,
            ApprovalCondition condition,
            List<ApprovalStepTemplate> stepTemplates,
            String description) {
        return new ApprovalRule(id, ruleName, priority, active, ruleType, condition, stepTemplates, description);
    }

    public static ApprovalRule create(
            UUID id,
            String ruleName,
            int priority,
            ApprovalRuleType ruleType,
            ApprovalCondition condition,
            List<ApprovalStepTemplate> stepTemplates,
            String description) {
        return new ApprovalRule(id, ruleName, priority, true, ruleType, condition, stepTemplates, description);
    }

    public ApprovalRule update(
            String ruleName,
            int priority,
            boolean active,
            ApprovalRuleType ruleType,
            ApprovalCondition condition,
            List<ApprovalStepTemplate> stepTemplates,
            String description) {
        return new ApprovalRule(id, ruleName, priority, active, ruleType, condition, stepTemplates, description);
    }

    public ApprovalRule deactivate() {
        return new ApprovalRule(id, ruleName, priority, false, ruleType, condition, stepTemplates, description);
    }

    public boolean matches(
            Money totalAmount,
            Set<String> categories,
            UUID departmentId,
            PurchaseRequestPriority requestPriority) {
        return active && condition.matches(totalAmount, categories, departmentId, requestPriority);
    }

    public boolean isAdditive() {
        return ruleType == ApprovalRuleType.CATEGORY;
    }

    public UUID getId() {
        return id;
    }

    public String getRuleName() {
        return ruleName;
    }

    public int getPriority() {
        return priority;
    }

    public boolean isActive() {
        return active;
    }

    public ApprovalRuleType getRuleType() {
        return ruleType;
    }

    public ApprovalCondition getCondition() {
        return condition;
    }

    public List<ApprovalStepTemplate> getStepTemplates() {
        return stepTemplates;
    }

    public String getDescription() {
        return description;
    }
}
