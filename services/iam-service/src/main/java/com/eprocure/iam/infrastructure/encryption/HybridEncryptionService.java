package com.eprocure.iam.infrastructure.encryption;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.PrivateKey;
import java.security.spec.MGF1ParameterSpec;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.OAEPParameterSpec;
import javax.crypto.spec.PSource;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.stereotype.Service;

@Service
public class HybridEncryptionService {
    private static final String AES_GCM_TRANSFORMATION = "AES/GCM/NoPadding";
    private static final String AES_ALGORITHM = "AES";
    private static final int GCM_TAG_BITS = 128;
    private final RsaKeyProvider rsaKeyProvider;

    public HybridEncryptionService(RsaKeyProvider rsaKeyProvider) {
        this.rsaKeyProvider = rsaKeyProvider;
    }

    public String decrypt(EncryptedRequest encryptedRequest) {
        if (encryptedRequest == null
                || isBlank(encryptedRequest.encryptedPayload())
                || isBlank(encryptedRequest.encryptedAesKey())
                || isBlank(encryptedRequest.iv())
                || isBlank(encryptedRequest.keyVersion())) {
            throw new InvalidEncryptedPayloadException();
        }
        if (!rsaKeyProvider.keyVersion().equals(encryptedRequest.keyVersion())) {
            throw new InvalidEncryptedPayloadException();
        }

        try {
            byte[] aesKey = decryptAesKey(encryptedRequest.encryptedAesKey(), rsaKeyProvider.privateKey());
            byte[] iv = Base64.getDecoder().decode(encryptedRequest.iv());
            byte[] encryptedPayload = Base64.getDecoder().decode(encryptedRequest.encryptedPayload());
            SecretKey secretKey = new SecretKeySpec(aesKey, AES_ALGORITHM);
            Cipher cipher = Cipher.getInstance(AES_GCM_TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, new GCMParameterSpec(GCM_TAG_BITS, iv));
            return new String(cipher.doFinal(encryptedPayload), StandardCharsets.UTF_8);
        } catch (IllegalArgumentException | GeneralSecurityException exception) {
            throw new InvalidEncryptedPayloadException(exception);
        }
    }

    private byte[] decryptAesKey(String encryptedAesKey, PrivateKey privateKey) throws GeneralSecurityException {
        Cipher cipher = Cipher.getInstance(rsaKeyProvider.algorithm());
        cipher.init(Cipher.DECRYPT_MODE, privateKey, oaepSha256Spec());
        return cipher.doFinal(Base64.getDecoder().decode(encryptedAesKey));
    }

    private OAEPParameterSpec oaepSha256Spec() {
        return new OAEPParameterSpec(
                "SHA-256",
                "MGF1",
                MGF1ParameterSpec.SHA256,
                PSource.PSpecified.DEFAULT);
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
