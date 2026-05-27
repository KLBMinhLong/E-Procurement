package com.eprocure.iam.common.util;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Optional;

public final class IpAddressUtil {

    private IpAddressUtil() {
    }

    public static String getClientIp(HttpServletRequest request) {
        if (request == null) {
            return "***";
        }
        
        // 1. Prioritize X-Forwarded-For
        String ipAddress = request.getHeader("X-Forwarded-For");
        
        if (ipAddress == null || ipAddress.isEmpty() || "unknown".equalsIgnoreCase(ipAddress)) {
            // 2. Fall back to X-Real-IP
            ipAddress = request.getHeader("X-Real-IP");
        }
        
        if (ipAddress == null || ipAddress.isEmpty() || "unknown".equalsIgnoreCase(ipAddress)) {
            // 3. Fall back to remote address
            ipAddress = request.getRemoteAddr();
        } else {
            // X-Forwarded-For can contain multiple comma-separated IPs (e.g. "client, proxy1, proxy2").
            // The first one is always the original client IP.
            if (ipAddress.contains(",")) {
                ipAddress = ipAddress.split(",")[0].trim();
            }
        }
        
        return ipAddress;
    }
}
