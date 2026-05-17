package com.eprocure.iam.application.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.eprocure.iam.common.exception.BusinessException;
import com.eprocure.iam.common.exception.ErrorCode;
import org.junit.jupiter.api.Test;

class InternalApiKeyGuardTest {
    @Test
    void should_accept_matching_key() {
        InternalApiKeyGuard guard = new InternalApiKeyGuard("dev-internal-api-key");

        guard.verify("dev-internal-api-key");
    }

    @Test
    void should_throw_iam_004_when_key_does_not_match() {
        InternalApiKeyGuard guard = new InternalApiKeyGuard("dev-internal-api-key");

        assertThatThrownBy(() -> guard.verify("wrong"))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.IAM_004);
    }
}
