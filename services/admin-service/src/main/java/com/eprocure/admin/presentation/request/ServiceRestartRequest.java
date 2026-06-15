package com.eprocure.admin.presentation.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ServiceRestartRequest(
        @NotBlank String confirmationCode,
        @NotBlank @Size(min = 10) String reason) {
}
