package com.eprocure.notification.common.api;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Optional;
import java.util.UUID;

public final class RequestIdUtil {
    private static final String REQUEST_ID_HEADER = "X-Request-ID";

    private RequestIdUtil() {
    }

    public static String resolve(HttpServletRequest request) {
        return Optional.ofNullable(request.getHeader(REQUEST_ID_HEADER))
                .filter(value -> !value.isBlank())
                .orElseGet(() -> UUID.randomUUID().toString());
    }
}
