package com.eprocure.iam.application.service;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.eprocure.iam.common.exception.BusinessException;
import com.eprocure.iam.common.exception.ErrorCode;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class IdempotencyGuardTest {
    private final IdempotencyGuard guard = new IdempotencyGuard();

    @Test
    void should_accept_uuid_v4_key() {
        assertThatCode(() -> guard.verify(UUID.randomUUID().toString()))
                .doesNotThrowAnyException();
    }

    @Test
    void should_throw_sys_005_when_key_missing() {
        assertThatThrownBy(() -> guard.verify(null))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.SYS_005);
    }
}
