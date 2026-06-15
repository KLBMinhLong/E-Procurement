package com.eprocure.admin.application.port.in;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

public record UpdateServiceConfigCommand(
        UUID actorId,
        String serviceName,
        List<ConfigVariableChange> variables,
        String confirmationCode,
        boolean requiresRestart,
        String changeReason) {

    public UpdateServiceConfigCommand {
        Objects.requireNonNull(actorId, "actorId must not be null");
        serviceName = serviceName == null ? "" : serviceName.trim();
        variables = List.copyOf(variables == null ? List.of() : variables);
        confirmationCode = confirmationCode == null ? "" : confirmationCode.trim();
        changeReason = changeReason == null ? "" : changeReason.trim();
    }
}
