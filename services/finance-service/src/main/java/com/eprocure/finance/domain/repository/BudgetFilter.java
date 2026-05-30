package com.eprocure.finance.domain.repository;

import com.eprocure.finance.domain.model.BudgetStatus;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public record BudgetFilter(
        UUID departmentId,
        Integer fiscalYear,
        Integer quarter,
        String glAccountCode,
        BudgetStatus status,
        int page,
        int size,
        int offset,
        String sortField,
        String sortDirection) {

    public BudgetFilter {
        if (page < 1) {
            throw new IllegalArgumentException("page must be >= 1");
        }
        if (size < 1) {
            throw new IllegalArgumentException("size must be >= 1");
        }
        if (offset < 0) {
            throw new IllegalArgumentException("offset must be >= 0");
        }
        sortField = Objects.requireNonNull(sortField, "sortField must not be null");
        sortDirection = Objects.requireNonNull(sortDirection, "sortDirection must not be null");
    }

    public Optional<UUID> departmentIdOpt() {
        return Optional.ofNullable(departmentId);
    }
}
