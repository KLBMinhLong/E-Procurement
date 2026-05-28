package com.eprocure.approval.application.port.in;

import com.eprocure.approval.domain.model.ApprovalRuleType;
import java.util.List;
import java.util.UUID;

public record CreateApprovalRuleCommand(
        UUID actorId,
        String ruleName,
        Integer priority,
        ApprovalRuleType ruleType,
        ApprovalRuleConditionCommand conditions,
        List<ApprovalRuleStepCommand> steps,
        String description) {
    public CreateApprovalRuleCommand {
        steps = steps == null ? List.of() : List.copyOf(steps);
    }
}
