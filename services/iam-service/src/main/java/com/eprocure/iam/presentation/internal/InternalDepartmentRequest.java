package com.eprocure.iam.presentation.internal;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record InternalDepartmentRequest(
        @NotNull UUID actorId,
        @NotBlank @Size(max = 20) String code,
        @NotBlank @Size(max = 200) String name,
        UUID parentId,
        UUID headUserId,
        String glAccountPrefix) {
}
