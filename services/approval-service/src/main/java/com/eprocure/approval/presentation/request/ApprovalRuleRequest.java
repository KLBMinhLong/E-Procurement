package com.eprocure.approval.presentation.request;

import com.eprocure.approval.domain.model.ApprovalRuleType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record ApprovalRuleRequest(
        @NotBlank String ruleName,
        @Min(1) Integer priority,
        Boolean active,
        @NotNull ApprovalRuleType ruleType,
        @Valid ApprovalRuleConditionRequest conditions,
        @NotEmpty @Valid List<ApprovalRuleStepRequest> steps,
        String description) {
}
