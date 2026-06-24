package com.eprocure.admin.presentation.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.List;

public record ServiceConfigUpdateRequest(
        @NotEmpty List<@Valid ConfigVariableChangeRequest> variables,
        @NotBlank String confirmationCode,
        boolean requiresRestart,
        @NotBlank @Size(min = 10) String changeReason) {

    public record ConfigVariableChangeRequest(
            @NotBlank String key,
            String value,
            boolean isSensitive,
            String description) {
    }
}
