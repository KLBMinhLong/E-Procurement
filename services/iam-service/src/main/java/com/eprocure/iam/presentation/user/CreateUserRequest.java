package com.eprocure.iam.presentation.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.UUID;

public record CreateUserRequest(
        @NotBlank @Size(max = 20) String employeeCode,
        @NotBlank @Size(min = 3, max = 50) @Pattern(regexp = "^[a-z0-9._-]+$") String username,
        @NotBlank @Email(message = "Invalid email format") @Size(max = 255) String email,
        @NotBlank @Size(max = 200) String fullName,
        @Size(max = 30) @Pattern(regexp = "^$|^\\+?[0-9]{9,15}$", message = "Phone number must contain only digits and optional leading +, with length between 9 and 15") String phone,
        @NotNull UUID departmentId,
        UUID orgNodeId,
        List<@NotBlank String> roles) {
}
