package com.eprocure.vendor.presentation.response;

public record VendorAddressResponse(
        String street,
        String district,
        String city,
        String country) {
}
