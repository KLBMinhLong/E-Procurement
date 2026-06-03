package com.eprocure.inventory.common.api;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Optional;
import java.util.UUID;

public final class RequestIdUtil {
    private static final String REQUEST_ID_HEADER = "X-Request-Id";

    private RequestIdUtil() {
    }

    public static String resolve(HttpServletRequest request) {
        return Optional.ofNullable(request.getHeader(REQUEST_ID_HEADER))
                .filter(value -> !value.isBlank())
                .orElseGet(() -> UUID.randomUUID().toString());
    }
}
