package com.eprocure.pr.presentation.internal;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record ApprovalResultRequest(
        @NotNull UUID approvalProcessId,
        String comment) {
}
