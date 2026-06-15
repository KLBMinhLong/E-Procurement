package com.eprocure.iam.presentation.internal;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record InvalidateSessionInternalRequest(
        @NotNull UUID actorId,
        @NotBlank String reason) {
}
