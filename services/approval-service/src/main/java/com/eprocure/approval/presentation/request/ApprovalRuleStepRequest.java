package com.eprocure.approval.presentation.request;

import com.eprocure.approval.domain.model.ApprovalStepType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ApprovalRuleStepRequest(
        @Min(1) int stepIndex,
        @NotBlank String requiredPermission,
        @NotNull ApprovalStepType stepType,
        @Min(1) int slaHours,
        boolean required) {
}
