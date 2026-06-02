package com.eprocure.vendor.application.service;

import com.eprocure.vendor.domain.model.Vendor;
import com.eprocure.vendor.domain.model.VendorStatus;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record VendorDetailView(
        UUID id,
        String vendorCode,
        String name,
        String taxCode,
        String email,
        String phone,
        VendorStatus status,
        boolean onApprovedVendorList,
        List<String> categories,
        Integer overallScore,
        VendorAddressView address,
        List<VendorContactView> contacts,
        VendorScorecardView scorecard,
        String notes,
        Instant createdAt) {

    public static VendorDetailView from(Vendor vendor) {
        return new VendorDetailView(
                vendor.id(),
                vendor.vendorCode(),
                vendor.name(),
                vendor.taxCode(),
                vendor.email(),
                vendor.phone(),
                vendor.status(),
                vendor.onApprovedVendorList(),
                vendor.categories(),
                vendor.scorecard() == null ? null : vendor.scorecard().overallScore(),
                new VendorAddressView(
                        vendor.addressStreet(),
                        vendor.addressDistrict(),
                        vendor.addressCity(),
                        vendor.addressCountry()),
                vendor.contacts().stream().map(VendorContactView::from).toList(),
                vendor.scorecard() == null ? null : VendorScorecardView.from(vendor.scorecard()),
                vendor.notes(),
                vendor.createdAt());
    }
}
