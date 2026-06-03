package com.eprocure.finance.application.port.in;

import java.util.Objects;
import java.util.UUID;

public record MatchInvoiceCommand(UUID actorId, UUID invoiceId) {
    public MatchInvoiceCommand {
        actorId = Objects.requireNonNull(actorId, "actorId must not be null");
        invoiceId = Objects.requireNonNull(invoiceId, "invoiceId must not be null");
    }
}
