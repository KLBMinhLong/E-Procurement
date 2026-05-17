package com.eprocure.iam.application.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class TotpSecretCipherTest {
    private static final String TEST_KEY = "MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=";

    @Test
    void should_round_trip_encrypted_secret_without_returning_plaintext() {
        TotpSecretCipher cipher = new TotpSecretCipher(TEST_KEY);

        String encrypted = cipher.encrypt("JBSWY3DPEHPK3PXP");

        assertThat(encrypted).startsWith("v1:");
        assertThat(encrypted).doesNotContain("JBSWY3DPEHPK3PXP");
        assertThat(cipher.decrypt(encrypted)).isEqualTo("JBSWY3DPEHPK3PXP");
    }
}
