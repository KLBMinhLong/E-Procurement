package com.eprocure.iam.domain.model;

import java.util.Locale;

public enum SortDirection {
    ASC,
    DESC;

    public static SortDirection from(String value) {
        if (value == null || value.isBlank()) {
            return DESC;
        }
        return "ASC".equals(value.trim().toUpperCase(Locale.ROOT)) ? ASC : DESC;
    }
}
