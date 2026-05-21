package com.eprocure.iam.presentation.user;

import jakarta.validation.constraints.Size;

public record UpdateMyProfileRequest(
        @Size(max = 200) String fullName,
        @Size(max = 30) String phone,
        String avatarUrl) {
}
