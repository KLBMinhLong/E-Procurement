package com.eprocure.admin.application.service;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public record AdminAuditContext(
        UUID actorId,
        String actorName,
        List<String> actorRoles,
        Optional<String> actorIp,
        Optional<String> httpMethod,
        Optional<String> endpoint,
        Optional<String> requestId) {

    public AdminAuditContext {
        Objects.requireNonNull(actorId, "actorId must not be null");
        actorName = actorName == null ? "" : actorName.trim();
        actorRoles = List.copyOf(actorRoles == null ? List.of() : actorRoles);
        actorIp = normalize(actorIp);
        httpMethod = normalize(httpMethod);
        endpoint = normalize(endpoint);
        requestId = normalize(requestId);
    }

    public static AdminAuditContext system(UUID actorId) {
        return new AdminAuditContext(
                actorId,
                actorId.toString(),
                List.of(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty());
    }

    private static Optional<String> normalize(Optional<String> value) {
        if (value == null || value.isEmpty() || value.get().isBlank()) {
            return Optional.empty();
        }
        return Optional.of(value.get().trim());
    }
}
