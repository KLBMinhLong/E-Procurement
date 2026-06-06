package com.eprocure.vendor.application.service;

import com.eprocure.vendor.domain.model.Vendor;
import com.eprocure.vendor.domain.model.VendorContact;
import com.eprocure.vendor.domain.model.VendorStatus;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

public record VendorPoSourceView(
        UUID id,
        String vendorCode,
        String name,
        String taxCode,
        String email,
        String phone,
        VendorStatus status,
        boolean onApprovedVendorList,
        List<String> categories,
        PrimaryContact primaryContact) {

    public VendorPoSourceView {
        categories = List.copyOf(categories == null ? List.of() : categories);
    }

    public static VendorPoSourceView from(Vendor vendor) {
        return new VendorPoSourceView(
                vendor.id(),
                vendor.vendorCode(),
                vendor.name(),
                vendor.taxCode(),
                vendor.email(),
                vendor.phone(),
                vendor.status(),
                vendor.onApprovedVendorList(),
                vendor.categories(),
                vendor.contacts().stream()
                        .min(Comparator.comparing(VendorContact::primary).reversed()
                                .thenComparing(VendorContact::name))
                        .map(PrimaryContact::from)
                        .orElse(null));
    }

    public record PrimaryContact(
            UUID id,
            String name,
            String role,
            String email,
            String phone) {

        private static PrimaryContact from(VendorContact contact) {
            return new PrimaryContact(
                    contact.id(),
                    contact.name(),
                    contact.role(),
                    contact.email(),
                    contact.phone());
        }
    }
}
