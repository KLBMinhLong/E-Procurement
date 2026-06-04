package com.eprocure.analytics.domain.model.dashboard;

public record PoPipeline(
        int draft,
        int pendingApproval,
        int sentToVendor,
        int partiallyReceived) {

    public PoPipeline {
        draft = Math.max(draft, 0);
        pendingApproval = Math.max(pendingApproval, 0);
        sentToVendor = Math.max(sentToVendor, 0);
        partiallyReceived = Math.max(partiallyReceived, 0);
    }

    public static PoPipeline empty() {
        return new PoPipeline(0, 0, 0, 0);
    }
}
