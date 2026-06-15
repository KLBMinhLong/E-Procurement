package com.eprocure.admin.application.port.in;

import java.util.Objects;
import java.util.UUID;

public record RestartServiceCommand(
        UUID actorId,
        String serviceName,
        String confirmationCode,
        String reason) {

    public RestartServiceCommand {
        Objects.requireNonNull(actorId, "actorId must not be null");
        serviceName = serviceName == null ? "" : serviceName.trim();
        confirmationCode = confirmationCode == null ? "" : confirmationCode.trim();
        reason = reason == null ? "" : reason.trim();
    }
}
