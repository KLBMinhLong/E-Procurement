package com.eprocure.iam.presentation.user;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record UpdateUserRequest(
        @Size(max = 200) String fullName,
        @Size(max = 30) @Pattern(regexp = "^$|^\\+?[0-9]{9,15}$", message = "Phone number must contain only digits and optional leading +, with length between 9 and 15") String phone,
        UUID departmentId,
        UUID orgNodeId) {
}
