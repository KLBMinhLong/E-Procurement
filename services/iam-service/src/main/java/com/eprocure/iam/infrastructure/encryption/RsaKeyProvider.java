package com.eprocure.iam.infrastructure.encryption;

import com.eprocure.iam.application.port.out.PublicKeyInfo;
import com.eprocure.iam.application.port.out.PublicKeyProviderPort;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.interfaces.RSAPrivateCrtKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.RSAPublicKeySpec;
import java.util.Base64;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class RsaKeyProvider implements PublicKeyProviderPort {
    private static final String DEV_PUBLIC_KEY = "-----BEGIN PUBLIC KEY-----\nDEV_PUBLIC_KEY_NOT_CONFIGURED\n-----END PUBLIC KEY-----";
    private static final String RSA_ALGORITHM = "RSA";
    private final String configuredPublicKey;
    private final String configuredPrivateKey;
    private final String keyVersion;
    private final String algorithm;

    public RsaKeyProvider(
            @Value("${eprocure.encryption.public-key:}") String configuredPublicKey,
            @Value("${eprocure.encryption.private-key:}") String configuredPrivateKey,
            @Value("${eprocure.encryption.key-version:v2025-01}") String keyVersion,
            @Value("${eprocure.encryption.algorithm:RSA/ECB/OAEPWithSHA-256AndMGF1Padding}") String algorithm) {
        this.configuredPublicKey = normalizeConfiguredPem(configuredPublicKey).orElse("");
        this.configuredPrivateKey = normalizeConfiguredPem(configuredPrivateKey).orElse("");
        this.keyVersion = keyVersion;
        this.algorithm = algorithm;
    }

    @Override
    public PublicKeyInfo current() {
        return new PublicKeyInfo(publicKeyPem(), keyVersion, algorithm);
    }

    public PrivateKey privateKey() {
        if (configuredPrivateKey.isBlank()) {
            throw new InvalidEncryptedPayloadException();
        }
        try {
            byte[] keyBytes = Base64.getDecoder().decode(extractPemBody(configuredPrivateKey));
            return KeyFactory.getInstance(RSA_ALGORITHM).generatePrivate(new PKCS8EncodedKeySpec(keyBytes));
        } catch (IllegalArgumentException | GeneralSecurityException exception) {
            throw new InvalidEncryptedPayloadException(exception);
        }
    }

    public String keyVersion() {
        return keyVersion;
    }

    public String algorithm() {
        return algorithm;
    }

    private String publicKeyPem() {
        if (!configuredPublicKey.isBlank()) {
            return configuredPublicKey;
        }
        if (!configuredPrivateKey.isBlank()) {
            return derivePublicKeyFromPrivateKey()
                    .map(this::formatPublicKey)
                    .orElse(DEV_PUBLIC_KEY);
        }
        return DEV_PUBLIC_KEY;
    }

    private Optional<PublicKey> derivePublicKeyFromPrivateKey() {
        try {
            PrivateKey privateKey = privateKey();
            if (!(privateKey instanceof RSAPrivateCrtKey rsaPrivateKey)) {
                return Optional.empty();
            }
            RSAPublicKeySpec publicKeySpec = new RSAPublicKeySpec(
                    rsaPrivateKey.getModulus(),
                    rsaPrivateKey.getPublicExponent());
            return Optional.of(KeyFactory.getInstance(RSA_ALGORITHM).generatePublic(publicKeySpec));
        } catch (InvalidEncryptedPayloadException | GeneralSecurityException exception) {
            return Optional.empty();
        }
    }

    private String formatPublicKey(PublicKey publicKey) {
        String encoded = Base64.getMimeEncoder(64, "\n".getBytes(StandardCharsets.UTF_8))
                .encodeToString(publicKey.getEncoded());
        return "-----BEGIN PUBLIC KEY-----\n" + encoded + "\n-----END PUBLIC KEY-----";
    }

    private static Optional<String> normalizeConfiguredPem(String value) {
        return Optional.ofNullable(value)
                .map(text -> text.replace("\\n", "\n").trim())
                .filter(text -> !text.isBlank());
    }

    private String extractPemBody(String pem) {
        return pem
                .replace("-----BEGIN PRIVATE KEY-----", "")
                .replace("-----END PRIVATE KEY-----", "")
                .replace("-----BEGIN PUBLIC KEY-----", "")
                .replace("-----END PUBLIC KEY-----", "")
                .replace("\\", "")
                .replaceAll("\\s", "");
    }
}
