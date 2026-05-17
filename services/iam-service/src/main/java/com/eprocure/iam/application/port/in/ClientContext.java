package com.eprocure.iam.application.port.in;

import java.util.Optional;

public record ClientContext(String ipAddress, String userAgent) {
    public static ClientContext of(String ipAddress, String userAgent) {
        return new ClientContext(normalize(ipAddress).orElse(null), normalize(userAgent).orElse(null));
    }

    private static Optional<String> normalize(String value) {
        return Optional.ofNullable(value).map(String::trim).filter(text -> !text.isBlank());
    }
}
