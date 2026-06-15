package com.eprocure.iam.application.port.in;

import java.util.Objects;
import java.util.UUID;

public record VerifyUserTotpCommand(UUID userId, String code) {

    public VerifyUserTotpCommand {
        Objects.requireNonNull(userId, "userId must not be null");
        code = code == null ? "" : code.trim();
    }
}
