package com.eprocure.iam.presentation.role;

import jakarta.validation.constraints.NotNull;
import java.util.List;

public record UpdateRolePermissionsRequest(@NotNull List<String> permissions) {
}
