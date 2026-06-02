package com.eprocure.vendor.application.service;

public record VendorQuoteMutationResult(VendorQuoteView view, boolean replayed) {
    public static VendorQuoteMutationResult fresh(VendorQuoteView view) {
        return new VendorQuoteMutationResult(view, false);
    }

    public static VendorQuoteMutationResult replayed(VendorQuoteView view) {
        return new VendorQuoteMutationResult(view, true);
    }
}
