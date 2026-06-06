package com.eprocure.vendor.application.service;

import com.eprocure.vendor.common.exception.BusinessException;
import com.eprocure.vendor.common.exception.ErrorCode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class InternalApiKeyGuard {
    private final String expectedApiKey;

    public InternalApiKeyGuard(@Value("${eprocure.internal.api-key:dev-internal-api-key}") String expectedApiKey) {
        this.expectedApiKey = expectedApiKey;
    }

    public void verify(String providedApiKey) {
        if (expectedApiKey == null || expectedApiKey.isBlank()) {
            throw new BusinessException(ErrorCode.IAM_004);
        }
        byte[] expected = expectedApiKey.getBytes(StandardCharsets.UTF_8);
        byte[] provided = providedApiKey == null
                ? new byte[0]
                : providedApiKey.getBytes(StandardCharsets.UTF_8);
        if (!MessageDigest.isEqual(expected, provided)) {
            throw new BusinessException(ErrorCode.IAM_004);
        }
    }
}
