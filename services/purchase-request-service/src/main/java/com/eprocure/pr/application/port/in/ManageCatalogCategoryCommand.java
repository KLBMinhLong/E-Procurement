package com.eprocure.pr.application.port.in;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.UUID;

public record ManageCatalogCategoryCommand(
        UUID actorId,
        String code,
        String name,
        String parentCode,
        boolean requiresSpecialApproval,
        String specialApproverRole,
        BigDecimal requiresRfqAbove,
        boolean capex) {

    public ManageCatalogCategoryCommand {
        actorId = Objects.requireNonNull(actorId, "actorId must not be null");
        code = normalize(code);
        name = normalize(name);
        parentCode = normalizeNullable(parentCode);
        specialApproverRole = normalizeNullable(specialApproverRole);
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim();
    }

    private static String normalizeNullable(String value) {
        String normalized = normalize(value);
        return normalized.isBlank() ? null : normalized;
    }
}
