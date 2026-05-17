package com.eprocure.iam.infrastructure.encryption;

public record EncryptedRequest(
        String encryptedPayload,
        String encryptedAesKey,
        String iv,
        String keyVersion) {
}
