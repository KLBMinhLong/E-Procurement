package com.eprocure.iam.infrastructure.security;

import com.eprocure.iam.application.port.out.PublicKeyInfo;
import com.eprocure.iam.application.port.out.PublicKeyProviderPort;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class ConfiguredPublicKeyProvider implements PublicKeyProviderPort {
    private final String publicKey;
    private final String keyVersion;
    private final String algorithm;

    public ConfiguredPublicKeyProvider(
            @Value("${eprocure.encryption.public-key:}") String publicKey,
            @Value("${eprocure.encryption.key-version:v2025-01}") String keyVersion,
            @Value("${eprocure.encryption.algorithm:RSA/ECB/OAEPWithSHA-256AndMGF1Padding}") String algorithm) {
        this.publicKey = publicKey == null || publicKey.isBlank()
                ? "-----BEGIN PUBLIC KEY-----\nDEV_PUBLIC_KEY_NOT_CONFIGURED\n-----END PUBLIC KEY-----"
                : publicKey;
        this.keyVersion = keyVersion;
        this.algorithm = algorithm;
    }

    @Override
    public PublicKeyInfo current() {
        return new PublicKeyInfo(publicKey, keyVersion, algorithm);
    }
}
