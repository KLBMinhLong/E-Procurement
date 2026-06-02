package com.eprocure.vendor.common.api;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Optional;

public final class RequestIdUtil {
    private static final String HEADER = "X-Request-ID";

    private RequestIdUtil() {
    }

    public static String resolve(HttpServletRequest request) {
        return Optional.ofNullable(request.getHeader(HEADER))
                .filter(value -> !value.isBlank())
                .orElseGet(() -> Optional.ofNullable(request.getAttribute(HEADER))
                        .map(Object::toString)
                        .orElse(null));
    }
}
