package com.eprocure.iam.infrastructure.encryption;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.security.KeyPair;
import org.junit.jupiter.api.Test;

class HybridEncryptionServiceTest {
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void should_decrypt_payload_when_rsa_and_aes_data_are_valid() throws Exception {
        KeyPair keyPair = EncryptionTestSupport.generateRsaKeyPair();
        HybridEncryptionService service = new HybridEncryptionService(EncryptionTestSupport.keyProvider(keyPair));
        String plainBody = "{\"username\":\"requester\",\"password\":\"Password@123\"}";
        EncryptedRequest encryptedRequest = EncryptionTestSupport.encryptedRequest(objectMapper, keyPair.getPublic(), plainBody);

        String result = service.decrypt(encryptedRequest);

        assertThat(result).isEqualTo(plainBody);
    }

    @Test
    void should_reject_payload_when_key_version_does_not_match() throws Exception {
        KeyPair keyPair = EncryptionTestSupport.generateRsaKeyPair();
        HybridEncryptionService service = new HybridEncryptionService(EncryptionTestSupport.keyProvider(keyPair));
        EncryptedRequest encryptedRequest = EncryptionTestSupport.encryptedRequest(objectMapper, keyPair.getPublic(), "{}");
        EncryptedRequest wrongVersion = new EncryptedRequest(
                encryptedRequest.encryptedPayload(),
                encryptedRequest.encryptedAesKey(),
                encryptedRequest.iv(),
                "wrong-version");

        assertThatThrownBy(() -> service.decrypt(wrongVersion))
                .isInstanceOf(InvalidEncryptedPayloadException.class);
    }
}
