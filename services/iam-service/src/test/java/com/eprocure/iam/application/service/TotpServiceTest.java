package com.eprocure.iam.application.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

class TotpServiceTest {
    private static final Instant FIXED_TIME = Instant.parse("2026-05-17T00:00:00Z");

    @Test
    void should_verify_current_totp_code_when_secret_matches() {
        TotpService service = new TotpService("eProcure Test", Clock.fixed(FIXED_TIME, ZoneOffset.UTC));
        String secret = "JBSWY3DPEHPK3PXP";
        String code = service.generateCode(secret, FIXED_TIME);

        assertThat(service.verifyCode(secret, code)).isTrue();
        assertThat(service.verifyCode(secret, "000000")).isFalse();
    }

    @Test
    void should_generate_setup_uri_and_backup_codes() {
        TotpService service = new TotpService("eProcure Test", Clock.fixed(FIXED_TIME, ZoneOffset.UTC));
        String secret = service.generateSecret();

        assertThat(secret).matches("^[A-Z2-7]+$");
        assertThat(service.provisioningUri("requester", secret))
                .startsWith("otpauth://totp/eProcure%20Test%3Arequester")
                .contains("secret=" + secret)
                .contains("issuer=eProcure%20Test");
        BackupCodeSet backupCodes = service.generateBackupCodes();
        assertThat(backupCodes.rawCodes()).hasSize(8).allMatch(code -> code.matches("^[A-Z2-9]{4}-[A-Z2-9]{4}$"));
        assertThat(backupCodes.codeHashes()).hasSize(8).allMatch(hash -> hash.matches("^[a-f0-9]{64}$"));
    }
}
