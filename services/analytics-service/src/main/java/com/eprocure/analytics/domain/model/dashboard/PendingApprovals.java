package com.eprocure.analytics.domain.model.dashboard;

public record PendingApprovals(
        int count,
        int overdueCount,
        int urgentCount) {

    public PendingApprovals {
        count = Math.max(count, 0);
        overdueCount = Math.max(overdueCount, 0);
        urgentCount = Math.max(urgentCount, 0);
    }

    public static PendingApprovals empty() {
        return new PendingApprovals(0, 0, 0);
    }
}
