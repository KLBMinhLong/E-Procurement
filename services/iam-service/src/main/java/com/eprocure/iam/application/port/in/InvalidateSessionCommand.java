package com.eprocure.iam.application.port.in;

import java.util.Objects;
import java.util.UUID;

public record InvalidateSessionCommand(
        UUID sessionId,
        UUID actorId,
        String reason) {

    public InvalidateSessionCommand {
        sessionId = Objects.requireNonNull(sessionId, "sessionId must not be null");
        actorId = Objects.requireNonNull(actorId, "actorId must not be null");
        reason = reason == null ? "" : reason.trim();
    }
}
