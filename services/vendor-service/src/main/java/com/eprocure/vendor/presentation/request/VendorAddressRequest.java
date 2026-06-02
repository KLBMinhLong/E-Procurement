package com.eprocure.vendor.presentation.request;

public record VendorAddressRequest(
        String street,
        String district,
        String city,
        String country) {
}
