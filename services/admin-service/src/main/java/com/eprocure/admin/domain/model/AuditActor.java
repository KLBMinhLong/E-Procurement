package com.eprocure.admin.domain.model;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

public record AuditActor(
        UUID id,
        String name,
        List<String> roles,
        String ip) {

    public AuditActor {
        id = Objects.requireNonNull(id, "id must not be null");
        name = Objects.requireNonNullElse(name, "");
        roles = List.copyOf(roles == null ? List.of() : roles);
        ip = ip == null || ip.isBlank() ? null : ip;
    }
}
