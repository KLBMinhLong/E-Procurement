package com.eprocure.analytics.domain.model.dashboard;

public record MyPurchaseRequestStats(
        int draft,
        int pendingApproval,
        int changesRequested,
        int approved,
        int rejected) {

    public MyPurchaseRequestStats {
        draft = Math.max(draft, 0);
        pendingApproval = Math.max(pendingApproval, 0);
        changesRequested = Math.max(changesRequested, 0);
        approved = Math.max(approved, 0);
        rejected = Math.max(rejected, 0);
    }

    public static MyPurchaseRequestStats empty() {
        return new MyPurchaseRequestStats(0, 0, 0, 0, 0);
    }
}
