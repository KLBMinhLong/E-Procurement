package com.eprocure.vendor.presentation.response;

import com.eprocure.vendor.domain.model.VendorStatus;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record VendorDetailResponse(
        UUID id,
        String vendorCode,
        String name,
        String taxCode,
        String email,
        String phone,
        VendorStatus status,
        @JsonProperty("isOnApprovedVendorList")
        boolean onApprovedVendorList,
        List<String> categories,
        Integer overallScore,
        VendorAddressResponse address,
        List<VendorContactResponse> contacts,
        VendorScorecardResponse scorecard,
        List<UUID> contractIds,
        String notes,
        Instant createdAt) {
}
