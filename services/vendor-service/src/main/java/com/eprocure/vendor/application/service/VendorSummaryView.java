package com.eprocure.vendor.application.service;

import com.eprocure.vendor.domain.model.Vendor;
import com.eprocure.vendor.domain.model.VendorStatus;
import java.util.List;
import java.util.UUID;

public record VendorSummaryView(
        UUID id,
        String vendorCode,
        String name,
        String taxCode,
        String email,
        String phone,
        VendorStatus status,
        boolean onApprovedVendorList,
        List<String> categories,
        Integer overallScore) {

    public static VendorSummaryView from(Vendor vendor) {
        return new VendorSummaryView(
                vendor.id(),
                vendor.vendorCode(),
                vendor.name(),
                vendor.taxCode(),
                vendor.email(),
                vendor.phone(),
                vendor.status(),
                vendor.onApprovedVendorList(),
                vendor.categories(),
                vendor.scorecard() == null ? null : vendor.scorecard().overallScore());
    }
}
