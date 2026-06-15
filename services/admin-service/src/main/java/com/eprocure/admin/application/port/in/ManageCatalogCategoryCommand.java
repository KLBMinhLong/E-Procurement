package com.eprocure.admin.application.port.in;

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
        code = code == null ? "" : code.trim();
        name = name == null ? "" : name.trim();
        parentCode = normalizeNullable(parentCode);
        specialApproverRole = normalizeNullable(specialApproverRole);
    }

    private static String normalizeNullable(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
