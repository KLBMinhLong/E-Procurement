package com.eprocure.admin.application.port.in;

import java.util.Objects;
import java.util.UUID;

public record DeactivateCatalogCategoryCommand(UUID actorId, String code) {
    public DeactivateCatalogCategoryCommand {
        actorId = Objects.requireNonNull(actorId, "actorId must not be null");
        code = code == null ? "" : code.trim();
    }
}
