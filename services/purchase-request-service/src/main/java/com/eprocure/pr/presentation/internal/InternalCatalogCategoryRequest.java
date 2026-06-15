package com.eprocure.pr.presentation.internal;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.UUID;

public record InternalCatalogCategoryRequest(
        @NotNull UUID actorId,
        @NotBlank String code,
        @NotBlank String name,
        String parentCode,
        boolean requiresSpecialApproval,
        String specialApproverRole,
        BigDecimal requiresRfqAbove,
        Boolean isCapex) {
}
