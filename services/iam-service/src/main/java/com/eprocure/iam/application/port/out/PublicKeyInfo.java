package com.eprocure.iam.application.port.out;

public record PublicKeyInfo(String publicKey, String keyVersion, String algorithm) {
}
