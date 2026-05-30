package com.eprocure.finance.application.port.in;

import com.eprocure.finance.domain.model.vo.Money;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record RecordBudgetCommitmentCommand(
        String eventId,
        String topic,
        Integer partitionId,
        Long offsetValue,
        UUID purchaseRequestId,
        String prNumber,
        UUID departmentId,
        int fiscalYear,
        String glAccountCode,
        Money amount,
        Instant occurredAt) {

    public RecordBudgetCommitmentCommand {
        eventId = requireText(eventId, "eventId");
        topic = requireText(topic, "topic");
        purchaseRequestId = Objects.requireNonNull(purchaseRequestId, "purchaseRequestId must not be null");
        prNumber = requireText(prNumber, "prNumber");
        departmentId = Objects.requireNonNull(departmentId, "departmentId must not be null");
        if (fiscalYear < 2000 || fiscalYear > 2100) {
            throw new IllegalArgumentException("fiscalYear is out of range");
        }
        glAccountCode = requireText(glAccountCode, "glAccountCode").toUpperCase();
        amount = Objects.requireNonNull(amount, "amount must not be null");
        occurredAt = occurredAt == null ? Instant.now() : occurredAt;
    }

    private static String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value.trim();
    }
}
