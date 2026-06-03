package com.eprocure.finance.application.port.in;

import java.util.Objects;
import java.util.UUID;

public record SendPurchaseOrderCommand(
        UUID actorId,
        UUID purchaseOrderId,
        String additionalNote) {

    public SendPurchaseOrderCommand {
        actorId = Objects.requireNonNull(actorId, "actorId must not be null");
        purchaseOrderId = Objects.requireNonNull(purchaseOrderId, "purchaseOrderId must not be null");
        additionalNote = additionalNote == null || additionalNote.isBlank() ? null : additionalNote.trim();
    }
}
