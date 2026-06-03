package com.eprocure.finance.application.port.in;

import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

public record UpdatePurchaseOrderDraftCommand(
        UUID actorId,
        UUID purchaseOrderId,
        String deliveryAddress,
        LocalDate deliveryDeadline,
        String paymentTerms) {

    public UpdatePurchaseOrderDraftCommand {
        actorId = Objects.requireNonNull(actorId, "actorId must not be null");
        purchaseOrderId = Objects.requireNonNull(purchaseOrderId, "purchaseOrderId must not be null");
        deliveryAddress = deliveryAddress == null ? null : deliveryAddress.trim();
        paymentTerms = paymentTerms == null || paymentTerms.isBlank() ? null : paymentTerms.trim();
    }
}
