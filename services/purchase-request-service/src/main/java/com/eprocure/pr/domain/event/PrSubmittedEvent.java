package com.eprocure.pr.domain.event;

import com.eprocure.pr.domain.model.PrPriority;
import com.eprocure.pr.domain.model.vo.Money;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record PrSubmittedEvent(
        String eventId,
        String eventType,
        String version,
        String source,
        Instant timestamp,
        UUID traceId,
        Payload payload) {

    public PrSubmittedEvent(
            UUID purchaseRequestId,
            String prNumber,
            UUID requesterId,
            UUID departmentId,
            PrPriority priority,
            Money totalAmount) {
        this(
                UUID.randomUUID().toString(),
                "PURCHASE_REQUEST_SUBMITTED",
                "1.0",
                "purchase-request-service",
                Instant.now(),
                UUID.randomUUID(),
                new Payload(purchaseRequestId, prNumber, requesterId, departmentId, priority, totalAmount));
    }

    public PrSubmittedEvent {
        Objects.requireNonNull(eventId, "eventId must not be null");
        Objects.requireNonNull(eventType, "eventType must not be null");
        Objects.requireNonNull(version, "version must not be null");
        Objects.requireNonNull(source, "source must not be null");
        Objects.requireNonNull(timestamp, "timestamp must not be null");
        Objects.requireNonNull(traceId, "traceId must not be null");
        Objects.requireNonNull(payload, "payload must not be null");
    }

    public record Payload(
            UUID purchaseRequestId,
            String prNumber,
            UUID requesterId,
            UUID departmentId,
            PrPriority priority,
            Money totalAmount) {
        public Payload {
            Objects.requireNonNull(purchaseRequestId, "purchaseRequestId must not be null");
            Objects.requireNonNull(prNumber, "prNumber must not be null");
            Objects.requireNonNull(requesterId, "requesterId must not be null");
            Objects.requireNonNull(departmentId, "departmentId must not be null");
            Objects.requireNonNull(priority, "priority must not be null");
            Objects.requireNonNull(totalAmount, "totalAmount must not be null");
        }
    }
}
