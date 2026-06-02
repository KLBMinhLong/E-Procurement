package com.eprocure.vendor.application.service;

import java.util.UUID;

public record VendorQuoteView(
        UUID id,
        UUID rfqId,
        UUID vendorId,
        String vendorName) {
}
