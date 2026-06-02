package com.eprocure.vendor.application.service;

import java.util.UUID;

public record AwardRfqResult(
        AwardedVendorView awardedVendor,
        VendorQuoteView awardedQuote,
        boolean replayed) {

    public static AwardRfqResult fresh(AwardedVendorView vendor, VendorQuoteView quote) {
        return new AwardRfqResult(vendor, quote, false);
    }

    public static AwardRfqResult replayed(AwardedVendorView vendor, VendorQuoteView quote) {
        return new AwardRfqResult(vendor, quote, true);
    }

    public record AwardedVendorView(UUID id, String name) {
    }
}
