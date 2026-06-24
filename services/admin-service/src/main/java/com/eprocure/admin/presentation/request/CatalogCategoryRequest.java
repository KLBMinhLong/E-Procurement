package com.eprocure.admin.presentation.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import java.math.BigDecimal;

public record CatalogCategoryRequest(
        @NotBlank
        @Pattern(regexp = "^[A-Z][A-Z0-9_]+$")
        String code,
        @NotBlank
        String name,
        String parentCode,
        boolean requiresSpecialApproval,
        String specialApproverRole,
        BigDecimal requiresRfqAbove,
        Boolean isCapex) {
}
