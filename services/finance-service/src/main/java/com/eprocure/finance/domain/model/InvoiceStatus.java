package com.eprocure.finance.domain.model;

public enum InvoiceStatus {
    PENDING_MATCH,
    MATCHED,
    MISMATCHED,
    APPROVED,
    DISPUTED,
    PAID,
    CANCELLED
}
