package com.eprocure.finance.application.service;

import java.util.List;
import java.util.UUID;

public record VendorPoSource(
        UUID id,
        String vendorCode,
        String name,
        String taxCode,
        String email,
        String phone,
        String status,
        boolean onApprovedVendorList,
        List<String> categories,
        PrimaryContact primaryContact) {

    public VendorPoSource {
        categories = List.copyOf(categories == null ? List.of() : categories);
    }

    public record PrimaryContact(
            UUID id,
            String name,
            String role,
            String email,
            String phone) {
    }
}
