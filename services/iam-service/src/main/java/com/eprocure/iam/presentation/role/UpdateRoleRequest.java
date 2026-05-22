package com.eprocure.iam.presentation.role;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record UpdateRoleRequest(
        @NotBlank @Pattern(regexp = "^[A-Z][A-Z0-9_]+$") String code,
        @NotBlank String name,
        String description) {
}
