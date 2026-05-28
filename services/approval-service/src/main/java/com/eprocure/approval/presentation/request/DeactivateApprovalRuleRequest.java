package com.eprocure.approval.presentation.request;

import jakarta.validation.constraints.NotBlank;

public record DeactivateApprovalRuleRequest(
        @NotBlank String reason) {
}
