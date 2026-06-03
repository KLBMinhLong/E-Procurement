package com.eprocure.inventory.application.service;

import com.eprocure.inventory.common.exception.BusinessException;
import com.eprocure.inventory.common.exception.ErrorCode;
import java.util.Locale;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class IdempotencyService {

    public void verify(String idempotencyKey) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            throw new BusinessException(ErrorCode.SYS_005);
        }
        try {
            UUID key = UUID.fromString(idempotencyKey);
            if (key.version() != 4 || !idempotencyKey.equals(idempotencyKey.toLowerCase(Locale.ROOT))) {
                throw new BusinessException(ErrorCode.SYS_005);
            }
        } catch (IllegalArgumentException exception) {
            throw new BusinessException(ErrorCode.SYS_005);
        }
    }
}
