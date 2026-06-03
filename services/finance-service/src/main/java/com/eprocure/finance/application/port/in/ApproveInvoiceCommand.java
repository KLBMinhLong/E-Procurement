package com.eprocure.finance.application.port.in;

import java.util.Objects;
import java.util.UUID;

public record ApproveInvoiceCommand(UUID actorId, UUID invoiceId, String comment) {
    public ApproveInvoiceCommand {
        actorId = Objects.requireNonNull(actorId, "actorId must not be null");
        invoiceId = Objects.requireNonNull(invoiceId, "invoiceId must not be null");
        comment = comment == null || comment.isBlank() ? null : comment.trim();
    }
}
