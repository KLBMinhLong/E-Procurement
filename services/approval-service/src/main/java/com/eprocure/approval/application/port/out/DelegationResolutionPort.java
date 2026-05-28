package com.eprocure.approval.application.port.out;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public interface DelegationResolutionPort {
    Optional<ActiveDelegation> resolveActiveDelegation(ResolveDelegationQuery query);

    record ResolveDelegationQuery(
            UUID delegatorId,
            UUID requesterId,
            UUID requesterDepartmentId,
            BigDecimal totalAmount,
            String currency,
            Set<String> categories) {
        public ResolveDelegationQuery {
            delegatorId = Objects.requireNonNull(delegatorId, "delegatorId must not be null");
            requesterId = Objects.requireNonNull(requesterId, "requesterId must not be null");
            requesterDepartmentId = Objects.requireNonNull(requesterDepartmentId, "requesterDepartmentId must not be null");
            totalAmount = Objects.requireNonNull(totalAmount, "totalAmount must not be null");
            currency = normalizeCurrency(currency);
            categories = categories == null ? Set.of() : Set.copyOf(categories);
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

    record ActiveDelegation(
            UUID delegationId,
            UUID delegatorId,
            UUID delegateId) {
        public ActiveDelegation {
            delegationId = Objects.requireNonNull(delegationId, "delegationId must not be null");
            delegatorId = Objects.requireNonNull(delegatorId, "delegatorId must not be null");
            delegateId = Objects.requireNonNull(delegateId, "delegateId must not be null");
        }
    }
}
