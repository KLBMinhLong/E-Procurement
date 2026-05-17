package com.eprocure.iam.application.service;

public record TwoFactorVerificationResult(UserSummaryView user, String rawToken) {
}
