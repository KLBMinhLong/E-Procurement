package com.eprocure.iam.application.port.in;

public record ResetPasswordCommand(
        String resetToken,
        String newPassword,
        String confirmPassword,
        String idempotencyKey) {
    public ResetPasswordCommand {
        resetToken = resetToken == null ? "" : resetToken.trim();
        newPassword = newPassword == null ? "" : newPassword;
        confirmPassword = confirmPassword == null ? "" : confirmPassword;
    }
}
