package com.eprocure.iam.application.port.out;

public record GoogleOAuthProfile(String subject, String email, boolean emailVerified, String fullName, String avatarUrl) {
}
