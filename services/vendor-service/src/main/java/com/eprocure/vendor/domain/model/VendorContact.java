package com.eprocure.vendor.domain.model;

import java.util.Objects;
import java.util.UUID;

public record VendorContact(
        UUID id,
        String name,
        String role,
        String email,
        String phone,
        boolean primary) {

    public VendorContact {
        id = Objects.requireNonNull(id, "id must not be null");
        name = requireText(name, "name");
        email = requireText(email, "email");
        phone = requireText(phone, "phone");
        role = normalizeNullable(role);
    }

    public static VendorContact create(String name, String role, String email, String phone, boolean primary) {
        return new VendorContact(UUID.randomUUID(), name, role, email, phone, primary);
    }

    private static String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value.trim();
    }

    private static String normalizeNullable(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
