package com.eprocure.finance.domain.event;

import com.eprocure.finance.domain.model.BudgetAlertType;
import com.eprocure.finance.domain.model.vo.Money;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record BudgetAlertEvent(
        String eventId,
        String eventType,
        String version,
        String source,
        Instant timestamp,
        UUID traceId,
        Payload payload) {

    public static BudgetAlertEvent create(BudgetAlertType alertType, Instant timestamp, Payload payload) {
        Objects.requireNonNull(alertType, "alertType must not be null");
        return new BudgetAlertEvent(
                UUID.randomUUID().toString(),
                alertType == BudgetAlertType.EXCEEDED ? "FINANCE_BUDGET_EXCEEDED" : "FINANCE_BUDGET_WARNING",
                "1.0",
                "finance-service",
                timestamp,
                UUID.randomUUID(),
                payload);
    }

    public BudgetAlertEvent {
        eventId = requireText(eventId, "eventId");
        eventType = requireText(eventType, "eventType");
        version = requireText(version, "version");
        source = requireText(source, "source");
        timestamp = Objects.requireNonNull(timestamp, "timestamp must not be null");
        traceId = Objects.requireNonNull(traceId, "traceId must not be null");
        payload = Objects.requireNonNull(payload, "payload must not be null");
    }

    public record Payload(
            BudgetAlertType alertType,
            UUID budgetId,
            UUID departmentId,
            int fiscalYear,
            Integer quarter,
            String glAccountCode,
            Money allocated,
            Money committed,
            Money spent,
            Money available,
            Money projectedAvailable,
            BigDecimal projectedAvailablePercent,
            Money impactAmount,
            String referenceType,
            UUID referenceId,
            String sourceEventId,
            String reason) {
        public Payload {
            alertType = Objects.requireNonNull(alertType, "alertType must not be null");
            budgetId = Objects.requireNonNull(budgetId, "budgetId must not be null");
            departmentId = Objects.requireNonNull(departmentId, "departmentId must not be null");
            glAccountCode = requireText(glAccountCode, "glAccountCode").toUpperCase();
            allocated = Objects.requireNonNull(allocated, "allocated must not be null");
            committed = Objects.requireNonNull(committed, "committed must not be null");
            spent = Objects.requireNonNull(spent, "spent must not be null");
            available = Objects.requireNonNull(available, "available must not be null");
            projectedAvailable = Objects.requireNonNull(projectedAvailable, "projectedAvailable must not be null");
            projectedAvailablePercent = Objects.requireNonNull(
                    projectedAvailablePercent,
                    "projectedAvailablePercent must not be null");
            impactAmount = Objects.requireNonNull(impactAmount, "impactAmount must not be null");
            referenceType = requireText(referenceType, "referenceType").toUpperCase();
            referenceId = Objects.requireNonNull(referenceId, "referenceId must not be null");
            reason = requireText(reason, "reason");
        }
    }

    private static String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value.trim();
    }
}
