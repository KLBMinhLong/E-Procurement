package com.eprocure.iam.application.port.in;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public record ResolveActiveDelegationQuery(
        UUID delegatorId,
        UUID requesterId,
        UUID requesterDepartmentId,
        BigDecimal totalAmount,
        String currency,
        List<String> categories) {
    public ResolveActiveDelegationQuery {
        delegatorId = Objects.requireNonNull(delegatorId, "delegatorId must not be null");
        requesterId = Objects.requireNonNull(requesterId, "requesterId must not be null");
        requesterDepartmentId = Objects.requireNonNull(requesterDepartmentId, "requesterDepartmentId must not be null");
        totalAmount = Objects.requireNonNull(totalAmount, "totalAmount must not be null");
        currency = normalizeCurrency(currency);
        categories = categories == null ? List.of() : categories.stream()
                .filter(category -> category != null && !category.isBlank())
                .map(category -> category.trim().toUpperCase())
                .distinct()
                .toList();
    }

    private static String normalizeCurrency(String value) {
        if (value == null || value.isBlank()) {
            return "VND";
        }
        String normalized = value.trim().toUpperCase();
        if (normalized.length() != 3) {
            throw new IllegalArgumentException("currency must be ISO 4217 alpha-3");
        }
        return normalized;
    }
}
