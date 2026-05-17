package com.eprocure.iam.infrastructure.encryption;

import static org.assertj.core.api.Assertions.assertThat;

import com.eprocure.iam.common.api.ApiResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.servlet.FilterChain;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class EncryptionRequestFilterTest {
    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @Test
    void should_replace_encrypted_json_body_with_plain_json_when_payload_is_valid() throws Exception {
        KeyPair keyPair = EncryptionTestSupport.generateRsaKeyPair();
        HybridEncryptionService service = new HybridEncryptionService(EncryptionTestSupport.keyProvider(keyPair));
        EncryptionRequestFilter filter = new EncryptionRequestFilter(objectMapper, service);
        String plainBody = "{\"username\":\"requester\",\"password\":\"Password@123\"}";
        String encryptedJson = EncryptionTestSupport.encryptedRequestJson(objectMapper, keyPair.getPublic(), plainBody);
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/auth/login");
        request.setContentType(MediaType.APPLICATION_JSON_VALUE);
        request.setContent(encryptedJson.getBytes(StandardCharsets.UTF_8));
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicReference<String> capturedBody = new AtomicReference<>();
        FilterChain chain = (servletRequest, servletResponse) ->
                capturedBody.set(new String(servletRequest.getInputStream().readAllBytes(), StandardCharsets.UTF_8));

        filter.doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(capturedBody.get()).isEqualTo(plainBody);
    }

    @Test
    void should_return_bad_request_when_encrypted_payload_is_invalid() throws Exception {
        HybridEncryptionService service = new HybridEncryptionService(new RsaKeyProvider("", "", "v2025-test", "RSA/ECB/OAEPWithSHA-256AndMGF1Padding"));
        EncryptionRequestFilter filter = new EncryptionRequestFilter(objectMapper, service);
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/auth/login");
        request.setContentType(MediaType.APPLICATION_JSON_VALUE);
        request.setContent("{\"encryptedPayload\":\"bad\",\"encryptedAesKey\":\"bad\",\"iv\":\"bad\",\"keyVersion\":\"v2025-test\"}".getBytes(StandardCharsets.UTF_8));
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, (servletRequest, servletResponse) -> {
        });

        assertThat(response.getStatus()).isEqualTo(400);
        ApiResponse<?> apiResponse = objectMapper.readValue(response.getContentAsString(), ApiResponse.class);
        assertThat(apiResponse.success()).isFalse();
        assertThat(apiResponse.message()).isEqualTo("Invalid encrypted payload");
    }
}
