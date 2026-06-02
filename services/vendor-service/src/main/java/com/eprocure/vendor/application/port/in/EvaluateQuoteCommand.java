package com.eprocure.vendor.application.port.in;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.UUID;

public record EvaluateQuoteCommand(
        UUID actorId,
        UUID rfqId,
        UUID quoteId,
        BigDecimal evaluationScore,
        String evaluationNote) {

    public EvaluateQuoteCommand {
        Objects.requireNonNull(actorId, "actorId must not be null");
        Objects.requireNonNull(rfqId, "rfqId must not be null");
        Objects.requireNonNull(quoteId, "quoteId must not be null");
        Objects.requireNonNull(evaluationScore, "evaluationScore must not be null");
    }
}
