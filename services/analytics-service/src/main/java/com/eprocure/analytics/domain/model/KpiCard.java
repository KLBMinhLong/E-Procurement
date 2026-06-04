package com.eprocure.analytics.domain.model;

public record KpiCard(
        String label,
        String value,
        String unit,
        Trend trend,
        KpiStatus status) {

    public KpiCard {
        label = requireText(label, "label");
        value = requireText(value, "value");
        unit = unit == null || unit.isBlank() ? null : unit.trim();
    }

    private static String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value.trim();
    }
}
