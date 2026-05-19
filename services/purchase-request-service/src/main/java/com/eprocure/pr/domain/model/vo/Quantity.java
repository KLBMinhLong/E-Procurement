package com.eprocure.pr.domain.model.vo;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

public record Quantity(BigDecimal amount, String unit) {
    private static final int SCALE = 2;

    public Quantity {
        amount = normalizeAmount(amount);
        unit = requireText(unit, "unit");
    }

    private static BigDecimal normalizeAmount(BigDecimal value) {
        Objects.requireNonNull(value, "amount must not be null");
        if (value.signum() <= 0) {
            throw new IllegalArgumentException("quantity must be greater than zero");
        }
        BigDecimal withoutTrailingZeros = value.stripTrailingZeros();
        if (withoutTrailingZeros.scale() < 0) {
            withoutTrailingZeros = withoutTrailingZeros.setScale(0);
        }
        if (withoutTrailingZeros.scale() > SCALE) {
            throw new IllegalArgumentException("quantity scale must not exceed 2");
        }
        return value.setScale(SCALE, RoundingMode.UNNECESSARY);
    }

    private static String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value.trim();
    }
}
