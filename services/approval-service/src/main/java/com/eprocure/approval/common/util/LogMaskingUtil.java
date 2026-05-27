package com.eprocure.approval.common.util;

import java.util.UUID;

public final class LogMaskingUtil {

    private LogMaskingUtil() {
    }

    public static String maskId(UUID id) {
        if (id == null) {
            return "null";
        }
        return id.toString().substring(0, 8) + "...";
    }

    public static String maskClientIp(String ipAddress) {
        if (ipAddress == null || ipAddress.isBlank()) {
            return "***";
        }
        if (ipAddress.contains(".")) {
            String[] parts = ipAddress.split("\\.");
            if (parts.length == 4) {
                return parts[0] + "." + parts[1] + ".***.***";
            }
        }
        return "***";
    }
}
