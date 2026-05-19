package com.eprocure.pr.domain.model.vo;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

public record Money(BigDecimal amount, String currency) {
    private static final int SCALE = 4;
    private static final String DEFAULT_CURRENCY = "VND";

    public Money {
        amount = normalizeAmount(amount);
        currency = normalizeCurrency(currency);
    }

    public static Money zero(String currency) {
        return new Money(BigDecimal.ZERO, currency);
    }

    public Money add(Money other) {
        Objects.requireNonNull(other, "other must not be null");
        requireSameCurrency(other);
        return new Money(amount.add(other.amount), currency);
    }

    public Money multiply(BigDecimal multiplier) {
        Objects.requireNonNull(multiplier, "multiplier must not be null");
        if (multiplier.signum() < 0) {
            throw new IllegalArgumentException("multiplier must not be negative");
        }
        BigDecimal calculated = amount.multiply(multiplier).setScale(SCALE, RoundingMode.HALF_UP);
        return new Money(calculated, currency);
    }

    public boolean isGreaterThan(Money other) {
        Objects.requireNonNull(other, "other must not be null");
        requireSameCurrency(other);
        return amount.compareTo(other.amount) > 0;
    }

    private void requireSameCurrency(Money other) {
        if (!currency.equals(other.currency)) {
            throw new IllegalArgumentException("currency mismatch");
        }
    }

    private static BigDecimal normalizeAmount(BigDecimal value) {
        Objects.requireNonNull(value, "amount must not be null");
        if (value.signum() < 0) {
            throw new IllegalArgumentException("amount must not be negative");
        }
        BigDecimal withoutTrailingZeros = value.stripTrailingZeros();
        if (withoutTrailingZeros.scale() < 0) {
            withoutTrailingZeros = withoutTrailingZeros.setScale(0);
        }
        if (withoutTrailingZeros.scale() > SCALE) {
            throw new IllegalArgumentException("amount scale must not exceed 4");
        }
        return value.setScale(SCALE, RoundingMode.UNNECESSARY);
    }

    private static String normalizeCurrency(String value) {
        if (value == null || value.isBlank()) {
            return DEFAULT_CURRENCY;
        }
        String normalized = value.trim().toUpperCase();
        if (normalized.length() != 3) {
            throw new IllegalArgumentException("currency must be ISO 4217 alpha-3");
        }
        return normalized;
    }
}
