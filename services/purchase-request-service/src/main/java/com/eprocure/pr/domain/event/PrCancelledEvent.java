package com.eprocure.pr.domain.event;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record PrCancelledEvent(
        String eventId,
        String eventType,
        String version,
        String source,
        Instant timestamp,
        UUID traceId,
        Payload payload) {

    public PrCancelledEvent(UUID purchaseRequestId, String prNumber, UUID actorId, String reason) {
        this(
                UUID.randomUUID().toString(),
                "PURCHASE_REQUEST_CANCELLED",
                "1.0",
                "purchase-request-service",
                Instant.now(),
                UUID.randomUUID(),
                new Payload(purchaseRequestId, prNumber, actorId, reason));
    }

    public PrCancelledEvent {
        Objects.requireNonNull(eventId, "eventId must not be null");
        Objects.requireNonNull(eventType, "eventType must not be null");
        Objects.requireNonNull(version, "version must not be null");
        Objects.requireNonNull(source, "source must not be null");
        Objects.requireNonNull(timestamp, "timestamp must not be null");
        Objects.requireNonNull(traceId, "traceId must not be null");
        Objects.requireNonNull(payload, "payload must not be null");
    }

    public record Payload(UUID purchaseRequestId, String prNumber, UUID actorId, String reason) {
        public Payload {
            Objects.requireNonNull(purchaseRequestId, "purchaseRequestId must not be null");
            Objects.requireNonNull(prNumber, "prNumber must not be null");
            Objects.requireNonNull(actorId, "actorId must not be null");
            reason = reason == null ? "" : reason.trim();
        }
    }
}
