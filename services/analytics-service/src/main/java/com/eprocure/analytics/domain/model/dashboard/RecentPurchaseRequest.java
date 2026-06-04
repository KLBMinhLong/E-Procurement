package com.eprocure.analytics.domain.model.dashboard;

import java.math.BigDecimal;
import java.time.Instant;

public record RecentPurchaseRequest(
        String prNumber,
        String title,
        String status,
        BigDecimal totalAmount,
        String requester,
        String currentApprover,
        Instant createdAt) {

    public RecentPurchaseRequest {
        prNumber = normalize(prNumber);
        title = normalize(title);
        status = normalize(status);
        totalAmount = totalAmount == null ? BigDecimal.ZERO : totalAmount;
        requester = normalize(requester);
        currentApprover = currentApprover == null || currentApprover.isBlank() ? null : currentApprover.trim();
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim();
    }
}
