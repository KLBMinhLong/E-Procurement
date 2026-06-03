package com.eprocure.finance.application.port.in;

import java.util.Objects;
import java.util.UUID;

public record CancelPurchaseOrderCommand(
        UUID actorId,
        UUID purchaseOrderId,
        String reason) {

    public CancelPurchaseOrderCommand {
        actorId = Objects.requireNonNull(actorId, "actorId must not be null");
        purchaseOrderId = Objects.requireNonNull(purchaseOrderId, "purchaseOrderId must not be null");
        reason = reason == null ? null : reason.trim();
    }
}
