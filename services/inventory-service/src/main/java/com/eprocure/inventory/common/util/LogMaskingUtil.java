package com.eprocure.inventory.common.util;

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

    public static String maskEmail(String email) {
        return Optional.ofNullable(email)
                .filter(value -> !value.isBlank() && value.contains("@"))
                .map(value -> {
                    String[] parts = value.split("@", 2);
                    String local = parts[0].isBlank() ? "*" : parts[0].substring(0, 1) + "***";
                    String domain = parts[1].isBlank() ? "***" : parts[1].substring(0, 1) + "***";
                    return local + "@" + domain;
                })
                .orElse("***");
    }

    public static String maskToken(String token) {
        return Optional.ofNullable(token)
                .filter(value -> value.length() >= 8)
                .map(value -> value.substring(0, 8) + "...")
                .orElse("***");
    }

    public static String maskClientIp(String ipAddress) {
        return Optional.ofNullable(ipAddress)
                .filter(value -> !value.isBlank())
                .map(value -> value.replaceAll("\\d+$", "***"))
                .orElse("***");
    }
}
