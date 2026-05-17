package com.eprocure.iam.common.util;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;
import org.junit.jupiter.api.Test;

class LogMaskingUtilTest {
    @Test
    void should_mask_contact_and_sensitive_values() {
        assertThat(LogMaskingUtil.maskEmail("nguyen.van.a@company.com")).isEqualTo("n***@c***.com");
        assertThat(LogMaskingUtil.maskPhone("0912345678")).isEqualTo("09*****678");
        assertThat(LogMaskingUtil.maskToken("0123456789abcdef")).isEqualTo("01234567...");
        assertThat(LogMaskingUtil.maskName("Nguyen Van A")).isEqualTo("Nguyen V. A.");
        assertThat(LogMaskingUtil.maskId(UUID.fromString("550e8400-e29b-41d4-a716-446655440000")))
                .isEqualTo("550e8400...");
    }

    @Test
    void should_fallback_when_value_is_missing_or_too_short() {
        assertThat(LogMaskingUtil.maskEmail(null)).isEqualTo("***");
        assertThat(LogMaskingUtil.maskPhone("12345")).isEqualTo("***");
        assertThat(LogMaskingUtil.maskToken("1234567")).isEqualTo("***");
        assertThat(LogMaskingUtil.maskName("")).isEqualTo("***");
        assertThat(LogMaskingUtil.maskId(null)).isEqualTo("***");
    }
}
