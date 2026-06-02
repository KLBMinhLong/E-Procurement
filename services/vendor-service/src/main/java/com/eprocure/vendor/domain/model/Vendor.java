package com.eprocure.vendor.domain.model;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public record Vendor(
        UUID id,
        String vendorCode,
        String name,
        String taxCode,
        String email,
        String phone,
        String addressStreet,
        String addressDistrict,
        String addressCity,
        String addressCountry,
        List<String> categories,
        List<VendorContact> contacts,
        VendorScorecard scorecard,
        VendorStatus status,
        boolean onApprovedVendorList,
        String notes,
        UUID approvedBy,
        Instant approvedAt,
        UUID idempotencyKey,
        Instant createdAt,
        UUID createdBy,
        UUID updatedBy) {

    public Vendor {
        id = Objects.requireNonNull(id, "id must not be null");
        vendorCode = requireText(vendorCode, "vendorCode");
        name = requireText(name, "name");
        taxCode = requireText(taxCode, "taxCode");
        email = requireText(email, "email");
        phone = requireText(phone, "phone");
        addressStreet = normalizeNullable(addressStreet);
        addressDistrict = normalizeNullable(addressDistrict);
        addressCity = normalizeNullable(addressCity);
        addressCountry = addressCountry == null || addressCountry.isBlank() ? "Vietnam" : addressCountry.trim();
        categories = normalizeCategories(categories);
        contacts = List.copyOf(contacts == null ? List.of() : contacts);
        status = Objects.requireNonNull(status, "status must not be null");
        if (status != VendorStatus.APPROVED && onApprovedVendorList) {
            throw new IllegalArgumentException("only approved vendor can be on AVL");
        }
        notes = normalizeNullable(notes);
        createdAt = Objects.requireNonNull(createdAt, "createdAt must not be null");
        createdBy = Objects.requireNonNull(createdBy, "createdBy must not be null");
    }

    public static Vendor create(
            UUID id,
            String vendorCode,
            String name,
            String taxCode,
            String email,
            String phone,
            String addressStreet,
            String addressDistrict,
            String addressCity,
            String addressCountry,
            List<String> categories,
            List<VendorContact> contacts,
            String notes,
            UUID actorId,
            UUID idempotencyKey,
            Instant createdAt) {
        return new Vendor(
                id,
                vendorCode,
                name,
                taxCode,
                email,
                phone,
                addressStreet,
                addressDistrict,
                addressCity,
                addressCountry,
                categories,
                contacts,
                null,
                VendorStatus.PENDING,
                false,
                notes,
                null,
                null,
                Objects.requireNonNull(idempotencyKey, "idempotencyKey must not be null"),
                createdAt,
                actorId,
                null);
    }

    public Vendor approve(UUID approverId, Instant approvedAt) {
        Objects.requireNonNull(approverId, "approverId must not be null");
        Objects.requireNonNull(approvedAt, "approvedAt must not be null");
        if (status == VendorStatus.BLACKLISTED) {
            throw new IllegalStateException("blacklisted vendor cannot be approved");
        }
        if (status == VendorStatus.APPROVED && onApprovedVendorList) {
            return this;
        }
        return new Vendor(
                id,
                vendorCode,
                name,
                taxCode,
                email,
                phone,
                addressStreet,
                addressDistrict,
                addressCity,
                addressCountry,
                categories,
                contacts,
                scorecard,
                VendorStatus.APPROVED,
                true,
                notes,
                approverId,
                approvedAt,
                idempotencyKey,
                createdAt,
                createdBy,
                approverId);
    }

    private static List<String> normalizeCategories(List<String> values) {
        List<String> normalized = values == null ? List.of() : values.stream()
                .filter(value -> value != null && !value.isBlank())
                .map(value -> value.trim().toUpperCase())
                .distinct()
                .toList();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException("categories must not be empty");
        }
        return normalized;
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
