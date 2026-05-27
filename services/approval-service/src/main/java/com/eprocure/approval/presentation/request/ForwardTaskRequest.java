package com.eprocure.approval.presentation.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record ForwardTaskRequest(
        @NotNull UUID forwardToUserId,
        @NotBlank @Size(min = 10, max = 2000) String reason) {
}
