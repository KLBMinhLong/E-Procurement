package com.eprocure.iam.common.util;

import java.util.Optional;
import java.util.UUID;

public final class LogMaskingUtil {
    private LogMaskingUtil() {
    }

    public static String maskEmail(String email) {
        return Optional.ofNullable(email)
                .filter(value -> value.contains("@"))
                .map(value -> {
                    String[] parts = value.split("@", 2);
                    String local = parts[0].isEmpty() ? "*" : parts[0].charAt(0) + "***";
                    String domain = parts[1].isEmpty() ? "*" : parts[1].charAt(0) + "***";
                    return local + "@" + domain;
                })
                .orElse("***");
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
