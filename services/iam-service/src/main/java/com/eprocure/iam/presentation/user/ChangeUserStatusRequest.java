package com.eprocure.iam.presentation.user;

import com.eprocure.iam.domain.model.UserStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ChangeUserStatusRequest(
        @NotNull UserStatus status,
        @Size(max = 500) String reason) {
}
