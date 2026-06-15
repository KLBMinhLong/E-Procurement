package com.eprocure.admin.application.port.in;

import java.util.Optional;

public record ConfigVariableChange(
        String key,
        String value,
        boolean sensitive,
        Optional<String> description) {

    public ConfigVariableChange {
        key = key == null ? "" : key.trim();
        value = value == null ? "" : value;
        description = normalize(description);
    }

    private static Optional<String> normalize(Optional<String> value) {
        if (value == null || value.isEmpty() || value.get().isBlank()) {
            return Optional.empty();
        }
        return Optional.of(value.get().trim());
    }
}
