package com.eprocure.analytics.presentation.response;

public record PendingApprovalsResponse(
        int count,
        int overdueCount,
        int urgentCount) {
}
