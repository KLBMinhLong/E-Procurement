package com.eprocure.admin.presentation.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record DepartmentRequest(
        @NotBlank @Size(max = 20) String code,
        @NotBlank @Size(max = 200) String name,
        UUID parentId,
        UUID headUserId,
        String glAccountPrefix) {
}
