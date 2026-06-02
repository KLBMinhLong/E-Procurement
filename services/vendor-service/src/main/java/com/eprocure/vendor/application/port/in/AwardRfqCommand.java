package com.eprocure.vendor.application.port.in;

import java.util.Objects;
import java.util.UUID;

public record AwardRfqCommand(
        UUID actorId,
        UUID rfqId,
        UUID awardedQuoteId,
        String awardReason) {

    public AwardRfqCommand {
        Objects.requireNonNull(actorId, "actorId must not be null");
        Objects.requireNonNull(rfqId, "rfqId must not be null");
        Objects.requireNonNull(awardedQuoteId, "awardedQuoteId must not be null");
    }
}
