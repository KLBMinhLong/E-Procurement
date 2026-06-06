package com.eprocure.finance.application.port.in;

import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

public record CreateManualPurchaseOrderCommand(
        UUID actorId,
        String purchasingOfficerFullName,
        UUID prId,
        UUID vendorId,
        String deliveryAddress,
        LocalDate deliveryDeadline,
        String paymentTerms,
        String notes) {

    public CreateManualPurchaseOrderCommand {
        actorId = Objects.requireNonNull(actorId, "actorId must not be null");
        prId = Objects.requireNonNull(prId, "prId must not be null");
        vendorId = Objects.requireNonNull(vendorId, "vendorId must not be null");
        if (deliveryAddress == null || deliveryAddress.isBlank()) {
            throw new IllegalArgumentException("deliveryAddress must not be blank");
        }
        deliveryAddress = deliveryAddress.trim();
        purchasingOfficerFullName = normalizeOptional(purchasingOfficerFullName);
        paymentTerms = normalizeOptional(paymentTerms);
        notes = normalizeOptional(notes);
    }

    private static String normalizeOptional(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
