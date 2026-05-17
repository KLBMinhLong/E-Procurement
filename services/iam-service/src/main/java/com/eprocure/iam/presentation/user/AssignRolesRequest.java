package com.eprocure.iam.presentation.user;

import jakarta.validation.constraints.NotNull;
import java.util.List;

public record AssignRolesRequest(@NotNull List<String> roles) {
}
