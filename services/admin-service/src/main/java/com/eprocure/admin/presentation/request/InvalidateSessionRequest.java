package com.eprocure.admin.presentation.request;

import jakarta.validation.constraints.NotBlank;

public record InvalidateSessionRequest(@NotBlank String reason) {
}
