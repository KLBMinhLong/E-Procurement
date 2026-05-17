package com.eprocure.iam.infrastructure.encryption;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.spec.MGF1ParameterSpec;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.OAEPParameterSpec;
import javax.crypto.spec.PSource;
import javax.crypto.spec.SecretKeySpec;

final class EncryptionTestSupport {
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final String RSA_TRANSFORMATION = "RSA/ECB/OAEPWithSHA-256AndMGF1Padding";
    private static final String AES_TRANSFORMATION = "AES/GCM/NoPadding";
    private static final int GCM_TAG_BITS = 128;

    private EncryptionTestSupport() {
    }

    static KeyPair generateRsaKeyPair() throws Exception {
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
        keyPairGenerator.initialize(2048);
        return keyPairGenerator.generateKeyPair();
    }

    static String privateKeyPem(PrivateKey privateKey) {
        String encoded = Base64.getMimeEncoder(64, "\n".getBytes(StandardCharsets.UTF_8))
                .encodeToString(privateKey.getEncoded());
        return "-----BEGIN PRIVATE KEY-----\n" + encoded + "\n-----END PRIVATE KEY-----";
    }

    static EncryptedRequest encryptedRequest(ObjectMapper objectMapper, PublicKey publicKey, String plainBody) throws Exception {
        byte[] aesKey = new byte[32];
        byte[] iv = new byte[12];
        SECURE_RANDOM.nextBytes(aesKey);
        SECURE_RANDOM.nextBytes(iv);

        Cipher aesCipher = Cipher.getInstance(AES_TRANSFORMATION);
        aesCipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(aesKey, "AES"), new GCMParameterSpec(GCM_TAG_BITS, iv));
        byte[] encryptedPayload = aesCipher.doFinal(plainBody.getBytes(StandardCharsets.UTF_8));

        Cipher rsaCipher = Cipher.getInstance(RSA_TRANSFORMATION);
        rsaCipher.init(Cipher.ENCRYPT_MODE, publicKey, oaepSha256Spec());
        byte[] encryptedAesKey = rsaCipher.doFinal(aesKey);

        return new EncryptedRequest(
                Base64.getEncoder().encodeToString(encryptedPayload),
                Base64.getEncoder().encodeToString(encryptedAesKey),
                Base64.getEncoder().encodeToString(iv),
                "v2025-test");
    }

    static String encryptedRequestJson(ObjectMapper objectMapper, PublicKey publicKey, String plainBody) throws Exception {
        return objectMapper.writeValueAsString(encryptedRequest(objectMapper, publicKey, plainBody));
    }

    static RsaKeyProvider keyProvider(KeyPair keyPair) {
        return new RsaKeyProvider("", privateKeyPem(keyPair.getPrivate()), "v2025-test", RSA_TRANSFORMATION);
    }

    private static OAEPParameterSpec oaepSha256Spec() {
        return new OAEPParameterSpec(
                "SHA-256",
                "MGF1",
                MGF1ParameterSpec.SHA256,
                PSource.PSpecified.DEFAULT);
    }
}
