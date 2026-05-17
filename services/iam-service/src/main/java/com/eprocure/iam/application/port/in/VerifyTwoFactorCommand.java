package com.eprocure.iam.application.port.in;

public record VerifyTwoFactorCommand(
        String challengeToken,
        String code,
        String idempotencyKey,
        ClientContext clientContext) {
}
