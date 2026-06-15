package com.eprocure.admin.presentation.response;

import java.time.Instant;
import java.util.UUID;

public record EncryptionKeyRotationResponse(
        UUID actionId,
        String status,
        String newKeyVersion,
        Instant rotatedAt,
        Instant oldKeyRetiredAt,
        boolean applied) {
}
