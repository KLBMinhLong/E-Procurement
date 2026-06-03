package com.eprocure.finance.application.port.in;

import java.util.Objects;
import java.util.UUID;

public record DisputeInvoiceCommand(UUID actorId, UUID invoiceId, String reason) {
    public DisputeInvoiceCommand {
        actorId = Objects.requireNonNull(actorId, "actorId must not be null");
        invoiceId = Objects.requireNonNull(invoiceId, "invoiceId must not be null");
        reason = requireText(reason, "reason");
        if (reason.length() < 20) {
            throw new IllegalArgumentException("reason must be at least 20 characters");
        }
    }

    private static String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value.trim();
    }
}
