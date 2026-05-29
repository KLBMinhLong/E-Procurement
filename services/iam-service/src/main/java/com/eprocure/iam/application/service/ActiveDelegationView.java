package com.eprocure.iam.application.service;

import java.util.UUID;

public record ActiveDelegationView(
        boolean active,
        UUID delegationId,
        UUID delegatorId,
        UUID delegateId,
        UserSummaryView delegate) {
    public static ActiveDelegationView inactive() {
        return new ActiveDelegationView(false, null, null, null, null);
    }

    public static ActiveDelegationView active(
            UUID delegationId,
            UUID delegatorId,
            UUID delegateId,
            UserSummaryView delegate) {
        return new ActiveDelegationView(true, delegationId, delegatorId, delegateId, delegate);
    }
}
