package com.eprocure.approval.common.api;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Optional;

public final class RequestIdUtil {
    private static final String REQUEST_ID_HEADER = "X-Request-ID";

    private RequestIdUtil() {
    }

    public static String resolve(HttpServletRequest request) {
        return Optional.ofNullable(request.getHeader(REQUEST_ID_HEADER))
                .filter(value -> !value.isBlank())
                .orElseGet(() -> Optional.ofNullable(request.getAttribute(REQUEST_ID_HEADER))
                        .map(Object::toString)
                        .orElse(""));
    }
}
