package com.eprocure.iam.application.service;

import java.util.Objects;
import java.util.UUID;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class PasswordHashService {
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(12);

    public String hash(String rawPassword, UUID userId) {
        return encoder.encode(salted(rawPassword, userId));
    }

    public boolean matches(String rawPassword, UUID userId, String passwordHash) {
        if (passwordHash == null || passwordHash.isBlank()) {
            return false;
        }
        try {
            return encoder.matches(salted(rawPassword, userId), passwordHash);
        } catch (IllegalArgumentException exception) {
            return false;
        }
    }

    private String salted(String rawPassword, UUID userId) {
        return Objects.requireNonNull(rawPassword, "rawPassword must not be null")
                + Objects.requireNonNull(userId, "userId must not be null");
    }
}
