package com.eprocure.pr.application.port.in;

import java.util.Objects;
import java.util.UUID;

public record SubmitPurchaseRequestCommand(
        UUID purchaseRequestId,
        UUID actorId) {
    public SubmitPurchaseRequestCommand {
        purchaseRequestId = Objects.requireNonNull(purchaseRequestId, "purchaseRequestId must not be null");
        actorId = Objects.requireNonNull(actorId, "actorId must not be null");
    }
}
