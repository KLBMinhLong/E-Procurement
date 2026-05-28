package com.eprocure.approval.presentation.request;

import java.util.List;
import java.util.UUID;

public record ApprovalRuleConditionRequest(
        String minValue,
        String maxValue,
        List<String> categories,
        List<UUID> departmentIds,
        List<String> priorities) {
}
