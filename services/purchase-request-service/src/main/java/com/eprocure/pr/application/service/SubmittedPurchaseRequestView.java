package com.eprocure.pr.application.service;

import com.eprocure.pr.domain.model.PrStatus;
import java.util.Objects;
import java.util.UUID;

public record SubmittedPurchaseRequestView(
        UUID id,
        String prNumber,
        PrStatus status) {
    public SubmittedPurchaseRequestView {
        id = Objects.requireNonNull(id, "id must not be null");
        prNumber = requireText(prNumber, "prNumber");
        status = Objects.requireNonNull(status, "status must not be null");
    }

    private static String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value.trim();
    }
}
