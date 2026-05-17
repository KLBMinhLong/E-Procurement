package com.eprocure.iam.domain.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public class Delegation {
    private UUID id;
    private UUID delegatorId;
    private UUID delegateId;
    private Instant startAt;
    private Instant endAt;
    private BigDecimal maxValue;
    private String currency;
    private List<String> allowedCategories;
    private DelegationScope scope;
    private DelegationStatus status;
    private Instant createdAt;

    private Delegation() {
    }

    private Delegation(
            UUID id,
            UUID delegatorId,
            UUID delegateId,
            Instant startAt,
            Instant endAt,
            BigDecimal maxValue,
            String currency,
            List<String> allowedCategories,
            DelegationScope scope,
            DelegationStatus status,
            Instant createdAt) {
        this.id = Objects.requireNonNull(id, "id must not be null");
        this.delegatorId = Objects.requireNonNull(delegatorId, "delegatorId must not be null");
        this.delegateId = Objects.requireNonNull(delegateId, "delegateId must not be null");
        if (delegatorId.equals(delegateId)) {
            throw new IllegalArgumentException("delegatorId and delegateId must be different");
        }
        this.startAt = Objects.requireNonNull(startAt, "startAt must not be null");
        this.endAt = Objects.requireNonNull(endAt, "endAt must not be null");
        if (!endAt.isAfter(startAt)) {
            throw new IllegalArgumentException("endAt must be after startAt");
        }
        if (maxValue != null && maxValue.signum() < 0) {
            throw new IllegalArgumentException("maxValue must not be negative");
        }
        this.maxValue = maxValue;
        this.currency = normalizeCurrency(currency);
        this.allowedCategories = allowedCategories == null ? null : List.copyOf(allowedCategories);
        this.scope = scope == null ? DelegationScope.ALL : scope;
        this.status = Objects.requireNonNull(status, "status must not be null");
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt must not be null");
    }

    public static Delegation create(
            UUID id,
            UUID delegatorId,
            UUID delegateId,
            Instant startAt,
            Instant endAt,
            BigDecimal maxValue,
            String currency,
            List<String> allowedCategories,
            DelegationScope scope,
            Instant createdAt) {
        return new Delegation(
                id,
                delegatorId,
                delegateId,
                startAt,
                endAt,
                maxValue,
                currency,
                allowedCategories,
                scope,
                DelegationStatus.ACTIVE,
                createdAt);
    }

    public static Delegation reconstitute(
            UUID id,
            UUID delegatorId,
            UUID delegateId,
            Instant startAt,
            Instant endAt,
            BigDecimal maxValue,
            String currency,
            List<String> allowedCategories,
            DelegationScope scope,
            DelegationStatus status,
            Instant createdAt) {
        return new Delegation(
                id,
                delegatorId,
                delegateId,
                startAt,
                endAt,
                maxValue,
                currency,
                allowedCategories,
                scope,
                status,
                createdAt);
    }

    public boolean overlaps(Instant requestedStartAt, Instant requestedEndAt) {
        return startAt.isBefore(requestedEndAt) && endAt.isAfter(requestedStartAt);
    }

    public DelegationStatus effectiveStatus(Instant now) {
        if (status == DelegationStatus.REVOKED) {
            return DelegationStatus.REVOKED;
        }
        return endAt.isBefore(now) ? DelegationStatus.EXPIRED : DelegationStatus.ACTIVE;
    }

    public void revoke() {
        this.status = DelegationStatus.REVOKED;
    }

    public UUID getId() {
        return id;
    }

    public UUID getDelegatorId() {
        return delegatorId;
    }

    public UUID getDelegateId() {
        return delegateId;
    }

    public Instant getStartAt() {
        return startAt;
    }

    public Instant getEndAt() {
        return endAt;
    }

    public Optional<BigDecimal> getMaxValue() {
        return Optional.ofNullable(maxValue);
    }

    public String getCurrency() {
        return currency;
    }

    public Optional<List<String>> getAllowedCategories() {
        return Optional.ofNullable(allowedCategories);
    }

    public DelegationScope getScope() {
        return scope;
    }

    public DelegationStatus getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
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
