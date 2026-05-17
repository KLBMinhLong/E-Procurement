package com.eprocure.iam.infrastructure.encryption;

public class InvalidEncryptedPayloadException extends RuntimeException {
    public InvalidEncryptedPayloadException() {
        super("Invalid encrypted payload");
    }

    public InvalidEncryptedPayloadException(Throwable cause) {
        super("Invalid encrypted payload", cause);
    }
}
