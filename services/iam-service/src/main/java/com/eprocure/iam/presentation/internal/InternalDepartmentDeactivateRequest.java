package com.eprocure.iam.presentation.internal;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record InternalDepartmentDeactivateRequest(@NotNull UUID actorId) {
}
