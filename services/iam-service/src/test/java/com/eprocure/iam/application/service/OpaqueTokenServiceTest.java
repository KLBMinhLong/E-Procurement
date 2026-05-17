package com.eprocure.iam.application.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class OpaqueTokenServiceTest {
    private final OpaqueTokenService opaqueTokenService = new OpaqueTokenService();

    @Test
    void should_generate_64_char_opaque_token_when_requested() {
        String token = opaqueTokenService.generate();

        assertThat(token)
                .hasSize(64)
                .matches("^[a-f0-9]{64}$");
    }

    @Test
    void should_hash_token_to_sha256_hex_when_token_provided() {
        String hash = opaqueTokenService.hash("test-token");

        assertThat(hash)
                .hasSize(64)
                .matches("^[a-f0-9]{64}$");
    }
}
