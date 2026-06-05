package com.eprocure.analytics.application.port.out;

import java.util.Objects;

public record RenderedReport(
        String storagePath,
        String downloadUrl) {

    public RenderedReport {
        storagePath = requireText(storagePath, "storagePath");
        downloadUrl = requireText(downloadUrl, "downloadUrl");
    }

    private static String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return Objects.requireNonNull(value).trim();
    }
}
