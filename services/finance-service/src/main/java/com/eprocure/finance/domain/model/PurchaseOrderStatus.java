package com.eprocure.finance.domain.model;

public enum PurchaseOrderStatus {
    DRAFT,
    PENDING_APPROVAL,
    APPROVED,
    SENT_TO_VENDOR,
    PARTIALLY_RECEIVED,
    FULLY_RECEIVED,
    INVOICED,
    PAID,
    CLOSED,
    CANCELLED
}
