package com.eprocure.approval.domain.model.vo;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

public record Money(BigDecimal amount, String currency) implements Comparable<Money> {

    public Money {
        Objects.requireNonNull(amount, "amount must not be null");
        if (amount.scale() > 4) {
            throw new IllegalArgumentException("Money scale must not exceed 4");
        }
        amount = amount.setScale(4, RoundingMode.UNNECESSARY);
        currency = currency == null || currency.isBlank() ? "VND" : currency.trim().toUpperCase();
    }

    public static Money vnd(String amount) {
        return new Money(new BigDecimal(amount), "VND");
    }

    @Override
    public int compareTo(Money other) {
        Objects.requireNonNull(other, "other must not be null");
        if (!currency.equals(other.currency())) {
            throw new IllegalArgumentException("Cannot compare money with different currencies");
        }
        return amount.compareTo(other.amount());
    }
}
