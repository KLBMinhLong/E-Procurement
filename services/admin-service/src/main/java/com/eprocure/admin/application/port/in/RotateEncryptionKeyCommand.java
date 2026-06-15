package com.eprocure.admin.application.port.in;

import java.util.Objects;
import java.util.UUID;

public record RotateEncryptionKeyCommand(
        UUID actorId,
        String confirmationCode,
        int keySize) {

    public RotateEncryptionKeyCommand {
        Objects.requireNonNull(actorId, "actorId must not be null");
        confirmationCode = confirmationCode == null ? "" : confirmationCode.trim();
    }
}
