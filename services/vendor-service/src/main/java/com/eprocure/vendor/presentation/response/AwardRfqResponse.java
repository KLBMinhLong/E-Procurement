package com.eprocure.vendor.presentation.response;

public record AwardRfqResponse(
        AwardedVendorResponse awardedVendor,
        VendorQuoteResponse awardedQuote) {
}
