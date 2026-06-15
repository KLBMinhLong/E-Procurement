package com.eprocure.admin.application.port.out;

import java.util.Objects;

public record RenderedAuditExport(
        String fileName,
        String storagePath) {

    public RenderedAuditExport {
        fileName = requireText(fileName, "fileName");
        storagePath = requireText(storagePath, "storagePath");
    }

    private static String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return Objects.requireNonNull(value).trim();
    }
}
