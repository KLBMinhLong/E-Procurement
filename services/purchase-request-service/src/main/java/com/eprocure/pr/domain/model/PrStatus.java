package com.eprocure.pr.domain.model;

public enum PrStatus {
    DRAFT,
    SUBMITTED,
    PENDING_APPROVAL,
    CHANGES_REQUESTED,
    APPROVED,
    REJECTED,
    CONVERTED_TO_PO,
    CANCELLED,
    CLOSED;

    public boolean isEditableByRequester() {
        return this == DRAFT || this == CHANGES_REQUESTED;
    }

    public boolean isCancelableByRequester() {
        return this == DRAFT || this == SUBMITTED || this == CHANGES_REQUESTED;
    }
}
