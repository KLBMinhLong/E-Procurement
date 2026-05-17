package com.eprocure.iam.common.util;

import java.util.Optional;
import java.util.UUID;

public final class LogMaskingUtil {
    private LogMaskingUtil() {
    }

    public static String maskEmail(String email) {
        return Optional.ofNullable(email)
                .filter(value -> value.contains("@"))
                .map(LogMaskingUtil::maskEmailParts)
                .orElse("***");
    }

    public static String maskId(UUID id) {
        return Optional.ofNullable(id)
                .map(UUID::toString)
                .map(value -> value.substring(0, Math.min(value.length(), 8)) + "...")
                .orElse("***");
    }

    public static String maskPhone(String phone) {
        return Optional.ofNullable(phone)
                .filter(value -> value.length() >= 6)
                .map(value -> value.substring(0, 2)
                        + "*".repeat(Math.max(0, value.length() - 5))
                        + value.substring(value.length() - 3))
                .orElse("***");
    }

    public static String maskToken(String token) {
        return Optional.ofNullable(token)
                .filter(value -> value.length() >= 8)
                .map(value -> value.substring(0, 8) + "...")
                .orElse("***");
    }

    public static String maskName(String fullName) {
        return Optional.ofNullable(fullName)
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .map(LogMaskingUtil::maskNameParts)
                .orElse("***");
    }

    public static String maskClientIp(String ipAddress) {
        return Optional.ofNullable(ipAddress)
                .filter(value -> !value.isBlank())
                .map(value -> value.replaceAll("\\d+$", "***"))
                .orElse("***");
    }

    private static String maskNameParts(String fullName) {
        String[] parts = fullName.split("\\s+");
        if (parts.length == 1) {
            return parts[0].charAt(0) + "***";
        }
        StringBuilder masked = new StringBuilder(parts[0]);
        for (int index = 1; index < parts.length; index++) {
            masked.append(' ').append(parts[index].charAt(0)).append('.');
        }
        return masked.toString();
    }

    private static String maskEmailParts(String email) {
        String[] parts = email.split("@", 2);
        String local = parts[0].isEmpty() ? "***" : parts[0].charAt(0) + "***";
        if (parts[1].isEmpty()) {
            return local + "@***";
        }
        int tldSeparator = parts[1].lastIndexOf('.');
        if (tldSeparator <= 0 || tldSeparator == parts[1].length() - 1) {
            return local + "@" + parts[1].charAt(0) + "***";
        }
        return local + "@" + parts[1].charAt(0) + "***" + parts[1].substring(tldSeparator);
    }
}
