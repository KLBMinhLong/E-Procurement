package com.eprocure.vendor.application.service;

public record VendorMutationResult(VendorDetailView view, boolean replayed) {
    public static VendorMutationResult fresh(VendorDetailView view) {
        return new VendorMutationResult(view, false);
    }

    public static VendorMutationResult replayed(VendorDetailView view) {
        return new VendorMutationResult(view, true);
    }
}
