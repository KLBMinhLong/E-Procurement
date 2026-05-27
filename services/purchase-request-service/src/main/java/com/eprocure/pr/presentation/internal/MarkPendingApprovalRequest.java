package com.eprocure.pr.presentation.internal;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record MarkPendingApprovalRequest(
        @NotNull UUID approvalProcessId,
        @NotBlank String camundaProcessInstanceId) {
}
