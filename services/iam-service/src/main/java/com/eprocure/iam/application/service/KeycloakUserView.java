package com.eprocure.iam.application.service;

import java.util.UUID;

public record KeycloakUserView(
        UUID id,
        String username,
        String email,
        String fullName,
        boolean enabled) {
}
