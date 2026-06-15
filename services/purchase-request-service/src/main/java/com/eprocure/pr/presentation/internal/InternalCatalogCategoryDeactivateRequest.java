package com.eprocure.pr.presentation.internal;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record InternalCatalogCategoryDeactivateRequest(@NotNull UUID actorId) {
}
