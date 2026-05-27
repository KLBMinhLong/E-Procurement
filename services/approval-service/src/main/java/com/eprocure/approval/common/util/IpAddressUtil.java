package com.eprocure.approval.common.util;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;

public final class IpAddressUtil {
    private static final List<String> CANDIDATE_HEADERS = List.of(
            "X-Forwarded-For",
            "X-Real-IP",
            "CF-Connecting-IP");

    private IpAddressUtil() {
    }

    public static String getClientIp(HttpServletRequest request) {
        for (String header : CANDIDATE_HEADERS) {
            String value = request.getHeader(header);
            if (value != null && !value.isBlank()) {
                return value.split(",")[0].trim();
            }
        }
        return request.getRemoteAddr();
    }
}
