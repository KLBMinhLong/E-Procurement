package com.eprocure.vendor.application.port.in;

public record VendorContactCommand(
        String name,
        String role,
        String email,
        String phone,
        boolean primary) {
}
