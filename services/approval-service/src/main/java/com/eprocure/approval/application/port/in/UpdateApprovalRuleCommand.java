package com.eprocure.approval.application.port.in;

import com.eprocure.approval.domain.model.ApprovalRuleType;
import java.util.List;
import java.util.UUID;

public record UpdateApprovalRuleCommand(
        UUID id,
        UUID actorId,
        String ruleName,
        Integer priority,
        Boolean active,
        ApprovalRuleType ruleType,
        ApprovalRuleConditionCommand conditions,
        List<ApprovalRuleStepCommand> steps,
        String description) {
    public UpdateApprovalRuleCommand {
        steps = steps == null ? List.of() : List.copyOf(steps);
    }
}
