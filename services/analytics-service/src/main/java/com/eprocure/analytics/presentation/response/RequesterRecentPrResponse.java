package com.eprocure.analytics.presentation.response;

public record RequesterRecentPrResponse(
        String prNumber,
        String title,
        String status,
        String currentApprover,
        String totalAmount) {
}
