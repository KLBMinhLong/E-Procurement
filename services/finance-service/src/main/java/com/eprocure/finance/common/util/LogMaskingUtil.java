package com.eprocure.finance.common.util;

import java.util.Optional;
import java.util.UUID;

public final class LogMaskingUtil {
    private LogMaskingUtil() {
    }

    public static String maskId(UUID id) {
        return Optional.ofNullable(id)
                .map(UUID::toString)
                .map(value -> value.substring(0, Math.min(value.length(), 8)) + "...")
                .orElse("***");
    }

    public static String maskClientIp(String ipAddress) {
        return Optional.ofNullable(ipAddress)
                .filter(value -> !value.isBlank())
                .map(value -> value.replaceAll("\\d+$", "***"))
                .orElse("***");
    }
}
