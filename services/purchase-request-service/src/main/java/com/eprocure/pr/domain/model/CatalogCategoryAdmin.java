package com.eprocure.pr.domain.model;

import com.eprocure.pr.domain.model.vo.Money;
import java.util.Objects;
import java.util.Optional;

public record CatalogCategoryAdmin(
        String code,
        String name,
        Optional<String> parentCode,
        boolean requiresSpecialApproval,
        Optional<String> specialApproverRole,
        Optional<Money> requiresRfqAbove,
        boolean capex,
        long itemCount,
        boolean deleted) {

    public CatalogCategoryAdmin {
        code = requireText(code, "code");
        name = requireText(name, "name");
        parentCode = normalize(parentCode);
        specialApproverRole = normalize(specialApproverRole);
        requiresRfqAbove = requiresRfqAbove == null ? Optional.empty() : requiresRfqAbove;
        itemCount = Math.max(0, itemCount);
    }

    private static String requireText(String value, String fieldName) {
        String normalized = Objects.requireNonNull(value, fieldName + " must not be null").trim();
        if (normalized.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return normalized;
    }

    private static Optional<String> normalize(Optional<String> value) {
        if (value == null || value.isEmpty() || value.get().isBlank()) {
            return Optional.empty();
        }
        return Optional.of(value.get().trim());
    }
}
