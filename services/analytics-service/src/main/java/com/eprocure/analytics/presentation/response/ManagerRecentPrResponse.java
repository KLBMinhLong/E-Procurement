package com.eprocure.analytics.presentation.response;

import java.time.Instant;

public record ManagerRecentPrResponse(
        String prNumber,
        String title,
        String status,
        String totalAmount,
        String requester,
        Instant createdAt) {
}
