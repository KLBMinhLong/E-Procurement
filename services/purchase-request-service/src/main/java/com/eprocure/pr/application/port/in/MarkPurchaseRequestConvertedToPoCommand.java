package com.eprocure.pr.application.port.in;

import java.util.Objects;
import java.util.UUID;

public record MarkPurchaseRequestConvertedToPoCommand(
        UUID purchaseRequestId,
        UUID poId,
        String poNumber) {

    public MarkPurchaseRequestConvertedToPoCommand {
        purchaseRequestId = Objects.requireNonNull(purchaseRequestId, "purchaseRequestId must not be null");
        poId = Objects.requireNonNull(poId, "poId must not be null");
        poNumber = requireText(poNumber, "poNumber");
    }

    private static String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value.trim();
    }
}
