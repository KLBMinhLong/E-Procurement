package com.eprocure.finance.domain.model;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public record BudgetCheckCriteria(
        UUID departmentId,
        int fiscalYear,
        String glAccountCode) {

    public BudgetCheckCriteria {
        departmentId = Objects.requireNonNull(departmentId, "departmentId must not be null");
        if (fiscalYear < 2000 || fiscalYear > 2100) {
            throw new IllegalArgumentException("fiscalYear is out of supported range");
        }
        glAccountCode = normalize(glAccountCode);
    }

    public Optional<String> glAccountCodeOpt() {
        return Optional.ofNullable(glAccountCode);
    }

    private static String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim().toUpperCase();
    }
}
