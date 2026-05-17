package com.eprocure.iam.presentation.user;

import jakarta.validation.constraints.Size;
import java.util.UUID;

public record UpdateUserRequest(
        @Size(max = 200) String fullName,
        @Size(max = 30) String phone,
        UUID departmentId,
        UUID orgNodeId) {
}
