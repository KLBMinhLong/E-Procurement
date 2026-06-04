package com.eprocure.analytics.presentation.response;

public record PoPipelineResponse(
        int draft,
        int pendingApproval,
        int sentToVendor,
        int partiallyReceived) {
}
