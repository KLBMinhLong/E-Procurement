package com.eprocure.analytics.presentation.response;

public record MyPurchaseRequestStatsResponse(
        int draft,
        int pendingApproval,
        int changesRequested,
        int approved,
        int rejected) {
}
