package com.eprocure.vendor.application.service;

import com.eprocure.vendor.domain.model.VendorContact;
import java.util.UUID;

public record VendorContactView(
        UUID id,
        String name,
        String role,
        String email,
        String phone,
        boolean primary) {

    public static VendorContactView from(VendorContact contact) {
        return new VendorContactView(
                contact.id(),
                contact.name(),
                contact.role(),
                contact.email(),
                contact.phone(),
                contact.primary());
    }
}
