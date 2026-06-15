package com.eprocure.admin.application.service;

import com.eprocure.admin.domain.model.AdminConfigActionStatus;
import java.time.Instant;
import java.util.UUID;

public record EncryptionKeyRotationResult(
        UUID actionId,
        AdminConfigActionStatus status,
        String newKeyVersion,
        Instant rotatedAt,
        Instant oldKeyRetiredAt,
        boolean applied,
        boolean replayed) {
}
