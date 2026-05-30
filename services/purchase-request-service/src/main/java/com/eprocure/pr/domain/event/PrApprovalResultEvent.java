package com.eprocure.pr.domain.event;

import com.eprocure.pr.domain.model.PrStatus;
import com.eprocure.pr.domain.model.vo.Money;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record PrApprovalResultEvent(
        String eventId,
        String eventType,
        String version,
        String source,
        Instant timestamp,
        UUID traceId,
        Payload payload) {

    public PrApprovalResultEvent(
            UUID purchaseRequestId,
            String prNumber,
            UUID departmentId,
            int fiscalYear,
            Money totalAmount,
            PrStatus status) {
        this(
                UUID.randomUUID().toString(),
                eventType(status),
                "1.0",
                "purchase-request-service",
                Instant.now(),
                UUID.randomUUID(),
                new Payload(purchaseRequestId, prNumber, departmentId, fiscalYear, totalAmount, status));
    }

    public PrApprovalResultEvent {
        Objects.requireNonNull(eventId, "eventId must not be null");
        Objects.requireNonNull(eventType, "eventType must not be null");
        Objects.requireNonNull(version, "version must not be null");
        Objects.requireNonNull(source, "source must not be null");
        Objects.requireNonNull(timestamp, "timestamp must not be null");
        Objects.requireNonNull(traceId, "traceId must not be null");
        Objects.requireNonNull(payload, "payload must not be null");
    }

    private static String eventType(PrStatus status) {
        return switch (Objects.requireNonNull(status, "status must not be null")) {
            case APPROVED -> "PURCHASE_REQUEST_APPROVED";
            case REJECTED -> "PURCHASE_REQUEST_REJECTED";
            case CHANGES_REQUESTED -> "PURCHASE_REQUEST_CHANGES_REQUESTED";
            default -> throw new IllegalArgumentException("Unsupported PR approval result status");
        };
    }

    public record Payload(
            UUID purchaseRequestId,
            String prNumber,
            UUID departmentId,
            int fiscalYear,
            Money totalAmount,
            PrStatus status) {
        public Payload {
            Objects.requireNonNull(purchaseRequestId, "purchaseRequestId must not be null");
            Objects.requireNonNull(prNumber, "prNumber must not be null");
            Objects.requireNonNull(departmentId, "departmentId must not be null");
            if (fiscalYear < 2000 || fiscalYear > 2100) {
                throw new IllegalArgumentException("fiscalYear is out of range");
            }
            Objects.requireNonNull(totalAmount, "totalAmount must not be null");
            Objects.requireNonNull(status, "status must not be null");
        }
    }
}
