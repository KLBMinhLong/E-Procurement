package com.eprocure.iam.application.port.in;

public record ForgotPasswordCommand(String email, String idempotencyKey) {
    public ForgotPasswordCommand {
        email = email == null ? "" : email.trim().toLowerCase();
    }
}
