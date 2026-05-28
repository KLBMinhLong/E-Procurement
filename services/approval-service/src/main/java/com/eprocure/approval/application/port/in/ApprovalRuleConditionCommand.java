package com.eprocure.approval.application.port.in;

import java.util.List;
import java.util.UUID;

public record ApprovalRuleConditionCommand(
        String minValue,
        String maxValue,
        List<String> categories,
        List<UUID> departmentIds,
        List<String> priorities) {
    public ApprovalRuleConditionCommand {
        categories = categories == null ? List.of() : List.copyOf(categories);
        departmentIds = departmentIds == null ? List.of() : List.copyOf(departmentIds);
        priorities = priorities == null ? List.of() : List.copyOf(priorities);
    }
}
