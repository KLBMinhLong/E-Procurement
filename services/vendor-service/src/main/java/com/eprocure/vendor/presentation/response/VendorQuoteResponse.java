package com.eprocure.vendor.presentation.response;

import java.util.UUID;

public record VendorQuoteResponse(
        UUID id,
        UUID rfqId,
        UUID vendorId,
        String vendorName) {
}
